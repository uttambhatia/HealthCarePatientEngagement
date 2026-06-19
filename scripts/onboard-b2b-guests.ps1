param(
  [Parameter(Mandatory = $true)]
  [string]$CsvPath,

  [string]$ReportPath = "",

  [string]$InviteRedirectUrl = "https://myapplications.microsoft.com",

  [int]$ResolveRetries = 12,

  [int]$ResolveDelaySeconds = 10,

  [switch]$ValidateOnly,

  [switch]$Execute,

  [switch]$AllowPrivilegedWithoutApprover,

  [string]$SubscriptionId = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function New-ResultObject {
  param(
    [int]$RowNumber,
    [string]$Email,
    [string]$Role,
    [string]$Environment,
    [string]$Status,
    [string]$Message,
    [string]$UserId = "",
    [string]$GroupName = ""
  )

  [pscustomobject]@{
    timestampUtc = (Get-Date).ToUniversalTime().ToString('o')
    rowNumber = $RowNumber
    email = $Email
    role = $Role
    environment = $Environment
    status = $Status
    message = $Message
    userId = $UserId
    groupName = $GroupName
  }
}

function Test-AzCliAvailable {
  $null = Get-Command az -ErrorAction Stop
}

function Invoke-AzJson {
  param(
    [Parameter(Mandatory = $true)]
    [string[]]$Arguments,

    [switch]$AllowFailure,

    [switch]$AsTsv
  )

  $allArgs = @()
  $allArgs += $Arguments

  if ($AsTsv) {
    $allArgs += @('-o', 'tsv')
  }
  else {
    $allArgs += @('-o', 'json')
  }

  $output = & az @allArgs 2>&1
  $exitCode = $LASTEXITCODE

  if ($exitCode -ne 0) {
    if ($AllowFailure) {
      return $null
    }

    $joined = ($output | Out-String).Trim()
    throw "az command failed: az $($Arguments -join ' ') ; $joined"
  }

  if ($AsTsv) {
    return ($output | Out-String).Trim()
  }

  $text = ($output | Out-String).Trim()
  if ([string]::IsNullOrWhiteSpace($text)) {
    return $null
  }

  return $text | ConvertFrom-Json
}

function Normalize-Role {
  param([string]$Role)

  if ([string]::IsNullOrWhiteSpace($Role)) {
    return ""
  }

  $key = $Role.Trim().ToUpperInvariant().Replace('_', '-').Replace(' ', '-')

  switch ($key) {
    'PATIENT' { return 'PATIENT' }
    'DOCTOR' { return 'DOCTOR' }
    'ADMIN' { return 'ADMIN' }
    'COORDINATOR' { return 'COORDINATOR' }
    'CARE-COORDINATOR' { return 'COORDINATOR' }
    default { return '' }
  }
}

function Get-GroupNameForRole {
  param([string]$Role)
  return "HCPE-$Role"
}

function Get-UserByEmail {
  param([string]$Email)

  $escaped = $Email.Replace("'", "''")
  $url = "https://graph.microsoft.com/v1.0/users?`$filter=mail eq '$escaped' or userPrincipalName eq '$escaped'&`$select=id,userPrincipalName,mail,userType,displayName"

  $resp = Invoke-AzJson -Arguments @('rest', '--method', 'GET', '--url', $url)
  if ($null -eq $resp -or $null -eq $resp.value -or $resp.value.Count -eq 0) {
    return $null
  }

  return $resp.value | Select-Object -First 1
}

function Invite-GuestUser {
  param(
    [string]$Email,
    [string]$DisplayName,
    [string]$RedirectUrl
  )

  $bodyObj = @{
    invitedUserEmailAddress = $Email
    inviteRedirectUrl = $RedirectUrl
    sendInvitationMessage = $true
  }

  if (-not [string]::IsNullOrWhiteSpace($DisplayName)) {
    $bodyObj.invitedUserDisplayName = $DisplayName
  }

  $json = $bodyObj | ConvertTo-Json -Depth 4 -Compress

  $tmp = Join-Path $env:TEMP ("hcpe-invite-" + [guid]::NewGuid().ToString() + ".json")
  Set-Content -Path $tmp -Value $json -Encoding utf8

  try {
    $resp = Invoke-AzJson -Arguments @('rest', '--method', 'POST', '--url', 'https://graph.microsoft.com/v1.0/invitations', '--headers', 'Content-Type=application/json', '--body', "@$tmp")
    return $resp
  }
  finally {
    if (Test-Path $tmp) {
      Remove-Item $tmp -Force
    }
  }
}

function Wait-ForUserResolution {
  param(
    [string]$Email,
    [int]$Retries,
    [int]$DelaySeconds
  )

  for ($i = 1; $i -le $Retries; $i++) {
    $user = Get-UserByEmail -Email $Email
    if ($null -ne $user) {
      return $user
    }

    Start-Sleep -Seconds $DelaySeconds
  }

  return $null
}

function Add-UserToGroupIfNeeded {
  param(
    [string]$GroupId,
    [string]$UserId
  )

  $url = "https://graph.microsoft.com/v1.0/users/$UserId/memberOf?`$select=id"
  $memberOf = Invoke-AzJson -Arguments @('rest', '--method', 'GET', '--url', $url)

  if ($null -ne $memberOf -and $null -ne $memberOf.value) {
    foreach ($entry in $memberOf.value) {
      if ($entry.id -eq $GroupId) {
        return 'ALREADY_MEMBER'
      }
    }
  }

  Invoke-AzJson -Arguments @('ad', 'group', 'member', 'add', '--group', $GroupId, '--member-id', $UserId) | Out-Null
  return 'ADDED'
}

function Resolve-GroupId {
  param([string]$GroupName)

  $groupId = Invoke-AzJson -Arguments @('ad', 'group', 'show', '--group', $GroupName, '--query', 'id') -AsTsv -AllowFailure
  if ([string]::IsNullOrWhiteSpace($groupId)) {
    return ""
  }

  return $groupId
}

if (-not (Test-Path $CsvPath)) {
  throw "CsvPath does not exist: $CsvPath"
}

$rows = Import-Csv -Path $CsvPath
if ($null -eq $rows -or $rows.Count -eq 0) {
  throw "CSV has no rows: $CsvPath"
}

$requiredColumns = @('email', 'displayName', 'role', 'environment', 'sponsor', 'approver', 'expiryDate')
$headers = @($rows[0].PSObject.Properties.Name)
foreach ($c in $requiredColumns) {
  if ($headers -notcontains $c) {
    throw "CSV missing required column: $c"
  }
}

$results = New-Object System.Collections.Generic.List[object]

if ($ValidateOnly) {
  Write-Host "Running in validation-only mode. No Azure operations will be executed."
}
elseif (-not $Execute) {
  Write-Host "Running in dry-run mode. Use -Execute to perform invitations and group assignments."
}

if (-not $ValidateOnly) {
  Test-AzCliAvailable

  if (-not [string]::IsNullOrWhiteSpace($SubscriptionId)) {
    & az account set --subscription $SubscriptionId | Out-Null
    if ($LASTEXITCODE -ne 0) {
      throw "Failed to set subscription: $SubscriptionId"
    }
  }

  $accountInfo = Invoke-AzJson -Arguments @('account', 'show')
  if ($null -eq $accountInfo) {
    throw 'Azure CLI is not logged in. Run az login first.'
  }

  Write-Host "Azure context tenant=$($accountInfo.tenantId) subscription=$($accountInfo.id)"
}

$rowNumber = 0
foreach ($row in $rows) {
  $rowNumber++

  $email = ([string]$row.email).Trim()
  $displayName = ([string]$row.displayName).Trim()
  $roleRaw = ([string]$row.role).Trim()
  $environment = ([string]$row.environment).Trim().ToLowerInvariant()
  $sponsor = ([string]$row.sponsor).Trim()
  $approver = ([string]$row.approver).Trim()
  $expiryDate = ([string]$row.expiryDate).Trim()

  if ([string]::IsNullOrWhiteSpace($email)) {
    $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $roleRaw -Environment $environment -Status 'FAILED' -Message 'Missing email'))
    continue
  }

  $role = Normalize-Role -Role $roleRaw
  if ([string]::IsNullOrWhiteSpace($role)) {
    $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $roleRaw -Environment $environment -Status 'FAILED' -Message 'Unsupported role. Use PATIENT, DOCTOR, COORDINATOR/care-coordinator, ADMIN'))
    continue
  }

  if ($environment -ne 'dev' -and $environment -ne 'prod') {
    $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'FAILED' -Message 'Environment must be dev or prod'))
    continue
  }

  if ([string]::IsNullOrWhiteSpace($sponsor)) {
    $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'FAILED' -Message 'Missing sponsor'))
    continue
  }

  $isPrivileged = ($role -eq 'ADMIN' -or $role -eq 'COORDINATOR')
  if ($isPrivileged -and -not $AllowPrivilegedWithoutApprover -and [string]::IsNullOrWhiteSpace($approver)) {
    $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'FAILED' -Message 'Missing approver for privileged role'))
    continue
  }

  if ($ValidateOnly) {
    $groupNameValidate = Get-GroupNameForRole -Role $role
    $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'VALID' -Message 'Row validation passed' -GroupName $groupNameValidate))
    continue
  }

  $groupName = Get-GroupNameForRole -Role $role

  try {
    $groupId = Resolve-GroupId -GroupName $groupName
    if ([string]::IsNullOrWhiteSpace($groupId)) {
      $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'FAILED' -Message "Group not found: $groupName" -GroupName $groupName))
      continue
    }

    if (-not $Execute) {
      $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'DRY_RUN' -Message 'Would invite guest if missing and assign to role group' -GroupName $groupName))
      continue
    }

    $user = Get-UserByEmail -Email $email
    $wasInvited = $false

    if ($null -eq $user) {
      $inviteResp = Invite-GuestUser -Email $email -DisplayName $displayName -RedirectUrl $InviteRedirectUrl
      $wasInvited = $true

      if ($null -ne $inviteResp -and $null -ne $inviteResp.invitedUser -and $null -ne $inviteResp.invitedUser.id) {
        $user = $inviteResp.invitedUser
      }
      else {
        $user = Wait-ForUserResolution -Email $email -Retries $ResolveRetries -DelaySeconds $ResolveDelaySeconds
      }
    }

    if ($null -eq $user -or [string]::IsNullOrWhiteSpace([string]$user.id)) {
      $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'FAILED' -Message 'Unable to resolve guest user object after invitation' -GroupName $groupName))
      continue
    }

    $membershipAction = Add-UserToGroupIfNeeded -GroupId $groupId -UserId $user.id

    if ($wasInvited -and $membershipAction -eq 'ADDED') {
      $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'INVITED_AND_ASSIGNED' -Message 'Guest invited and group membership created' -UserId $user.id -GroupName $groupName))
    }
    elseif ($wasInvited -and $membershipAction -eq 'ALREADY_MEMBER') {
      $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'INVITED_ALREADY_MEMBER' -Message 'Guest invited; group membership already existed' -UserId $user.id -GroupName $groupName))
    }
    elseif ($membershipAction -eq 'ADDED') {
      $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'ASSIGNED_EXISTING_USER' -Message 'Existing guest/member assigned to role group' -UserId $user.id -GroupName $groupName))
    }
    else {
      $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'ALREADY_ASSIGNED' -Message 'User already in target role group' -UserId $user.id -GroupName $groupName))
    }
  }
  catch {
    $results.Add((New-ResultObject -RowNumber $rowNumber -Email $email -Role $role -Environment $environment -Status 'FAILED' -Message $_.Exception.Message -GroupName $groupName))
  }
}

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
  $stamp = (Get-Date).ToString('yyyyMMdd-HHmmss')
  $ReportPath = Join-Path $PWD ("b2b-onboarding-report-$stamp.csv")
}

$results | Export-Csv -Path $ReportPath -NoTypeInformation -Encoding UTF8
Write-Host "Report written: $ReportPath"

$summary = $results | Group-Object -Property status | Sort-Object -Property Name
Write-Host 'Summary:'
foreach ($s in $summary) {
  Write-Host (" - {0}: {1}" -f $s.Name, $s.Count)
}

$failedRows = @($results | Where-Object { $_.status -eq 'FAILED' })
$hasFailures = $failedRows.Count -gt 0
if ($hasFailures) {
  Write-Error 'One or more rows failed. Inspect the report for details.'
  exit 1
}

exit 0
