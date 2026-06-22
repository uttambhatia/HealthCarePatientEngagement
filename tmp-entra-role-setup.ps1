$ErrorActionPreference='Stop'
$tenantId='65087c47-0017-4258-8086-72832006d566'
$subId='4116346b-5ded-4f3b-8387-f8d055802adc'
$targetAud='api://65087c47-0017-4258-8086-72832006d566/hpe-devx-api'
$spaAppId='526100bc-f963-4edd-8890-9011252bb554'
$apiAppName='hcpe-api-backend'
$roleValues=@('PATIENT','DOCTOR','COORDINATOR','ADMIN')
$roleMap=@{}
$groupMap=@{}

az account set --subscription $subId | Out-Null

Write-Host '=== API app lookup/create ==='
$apiApp = az ad app list --filter "identifierUris/any(u:u eq '$targetAud')" --query '[0]' -o json | ConvertFrom-Json
if (-not $apiApp) { $apiApp = az ad app list --display-name $apiAppName --query '[0]' -o json | ConvertFrom-Json }
if ($apiApp) { $apiAppId=$apiApp.appId; Write-Host "API app exists: $($apiApp.displayName) $apiAppId" } else { $new=az ad app create --display-name $apiAppName --sign-in-audience AzureADMyOrg --query '{appId:appId}' -o json | ConvertFrom-Json; $apiAppId=$new.appId; Write-Host "API app created: $apiAppId" }

$uris = az ad app show --id $apiAppId --query 'identifierUris' -o json | ConvertFrom-Json
if ($uris -notcontains $targetAud) { az ad app update --id $apiAppId --identifier-uris $targetAud | Out-Null; Write-Host "Identifier URI set: $targetAud" } else { Write-Host 'Identifier URI already set' }

Write-Host '=== API delegated scope ==='
$apiMeta=az ad app show --id $apiAppId --query 'api' -o json | ConvertFrom-Json
$existScope=$null
if ($apiMeta -and $apiMeta.oauth2PermissionScopes) { $existScope = $apiMeta.oauth2PermissionScopes | Where-Object { $_.value -eq 'access_as_user' } | Select-Object -First 1 }
if ($existScope) { $scopeGuid=$existScope.id; Write-Host "Scope exists: access_as_user $scopeGuid" } else {
  $scopeGuid=[guid]::NewGuid().ToString()
  $scopeObj=@{adminConsentDescription='Allow the SPA to call HCPE API on behalf of signed-in user';adminConsentDisplayName='Access HCPE API as user';id=$scopeGuid;isEnabled=$true;type='User';userConsentDescription='Allow this app to access HCPE API on your behalf';userConsentDisplayName='Access HCPE API';value='access_as_user'}
  $body=@{api=@{oauth2PermissionScopes=@($scopeObj)}} | ConvertTo-Json -Depth 8
  $tmp=Join-Path $PWD 'tmp-api-scope.json'
  Set-Content -Path $tmp -Value $body -Encoding utf8
  $objId=az ad app show --id $apiAppId --query id -o tsv
  az rest --method PATCH --url "https://graph.microsoft.com/v1.0/applications/$objId" --headers "Content-Type=application/json" --body "@$tmp" | Out-Null
  Remove-Item $tmp -Force
  Write-Host "Scope created: access_as_user $scopeGuid"
}

$apiPatch = @{ 
  api = @{ 
    preAuthorizedApplications = @(
      @{ appId = $spaAppId; delegatedPermissionIds = @($scopeGuid) }
    )
  }
} | ConvertTo-Json -Depth 8
$apiPatchFile = Join-Path $PWD 'tmp-api-preauth.json'
Set-Content -Path $apiPatchFile -Value $apiPatch -Encoding utf8
az rest --method PATCH --url "https://graph.microsoft.com/v1.0/applications/$objId" --headers "Content-Type=application/json" --body "@$apiPatchFile" | Out-Null
Remove-Item $apiPatchFile -Force
Write-Host "Pre-authorized SPA client: $spaAppId"

Write-Host '=== App roles ==='
$existRoles=az ad app show --id $apiAppId --query 'appRoles' -o json | ConvertFrom-Json
$finalRoles=[System.Collections.ArrayList]@()
foreach ($rv in $roleValues) {
  $found=$null
  if ($existRoles) { $found = $existRoles | Where-Object { $_.value -eq $rv } | Select-Object -First 1 }
  if ($found) { $roleMap[$rv]=$found.id; $finalRoles.Add($found) | Out-Null; Write-Host "Role exists: $rv" }
  else { $rid=[guid]::NewGuid().ToString(); $roleMap[$rv]=$rid; $finalRoles.Add(@{allowedMemberTypes=@('User');description="HCPE $rv role";displayName="HCPE $rv";id=$rid;isEnabled=$true;value=$rv}) | Out-Null; Write-Host "Role add planned: $rv" }
}
$roleJson=$finalRoles | ConvertTo-Json -Depth 8 -Compress
az ad app update --id $apiAppId --app-roles $roleJson | Out-Null
$liveRoles=az ad app show --id $apiAppId --query 'appRoles' -o json | ConvertFrom-Json
foreach ($rv in $roleValues) { $r=$liveRoles | Where-Object { $_.value -eq $rv } | Select-Object -First 1; if ($r) { $roleMap[$rv]=$r.id } }

Write-Host '=== Service principal ==='
$sp = az ad sp show --id $apiAppId --query '{id:id}' -o json 2>$null | ConvertFrom-Json
if ($sp) { $spOid=$sp.id; Write-Host "SP exists: $spOid" } else { $sp=az ad sp create --id $apiAppId --query '{id:id}' -o json | ConvertFrom-Json; $spOid=$sp.id; Write-Host "SP created: $spOid" }

Write-Host '=== Security groups ==='
foreach ($rv in $roleValues) {
  $gName="HCPE-$rv"
  $grp=az ad group show --group $gName --query '{id:id,displayName:displayName}' -o json 2>$null | ConvertFrom-Json
  if ($grp) { $groupMap[$rv]=$grp.id; Write-Host "Group exists: $gName" }
  else { $grp=az ad group create --display-name $gName --mail-nickname $gName --query '{id:id,displayName:displayName}' -o json | ConvertFrom-Json; $groupMap[$rv]=$grp.id; Write-Host "Group created: $gName" }
}

Write-Host '=== Group -> app role assignment ==='
foreach ($rv in $roleValues) {
  $gId=$groupMap[$rv]
  $rId=$roleMap[$rv]
  $existing=az rest --method GET --url "https://graph.microsoft.com/v1.0/servicePrincipals/$spOid/appRoleAssignedTo" --query "value[?principalId=='$gId' && appRoleId=='$rId']" -o json | ConvertFrom-Json
  if ($existing.Count -gt 0) { Write-Host "Assignment exists: HCPE-$rv -> $rv" }
  else {
    $assign=@{principalId=$gId;resourceId=$spOid;appRoleId=$rId} | ConvertTo-Json -Compress
    $tmp2=Join-Path $PWD 'tmp-assign.json'
    Set-Content -Path $tmp2 -Value $assign -Encoding utf8
    az rest --method POST --url "https://graph.microsoft.com/v1.0/servicePrincipals/$spOid/appRoleAssignedTo" --headers "Content-Type=application/json" --body "@$tmp2" | Out-Null
    Remove-Item $tmp2 -Force
    Write-Host "Assignment created: HCPE-$rv -> $rv"
  }
}

# Re-check the doctor assignment explicitly so a missed or stale admin action
# cannot leave the guest group without the DOCTOR app role.
$doctorGroupId = $groupMap['DOCTOR']
$doctorRoleId = $roleMap['DOCTOR']
if ($doctorGroupId -and $doctorRoleId) {
  $doctorExisting = az rest --method GET --url "https://graph.microsoft.com/v1.0/servicePrincipals/$spOid/appRoleAssignedTo" --query "value[?principalId=='$doctorGroupId' && appRoleId=='$doctorRoleId']" -o json | ConvertFrom-Json
  if ($doctorExisting.Count -eq 0) {
    $doctorAssign = @{principalId=$doctorGroupId;resourceId=$spOid;appRoleId=$doctorRoleId} | ConvertTo-Json -Compress
    $tmpDoctor = Join-Path $PWD 'tmp-doctor-assign.json'
    Set-Content -Path $tmpDoctor -Value $doctorAssign -Encoding utf8
    az rest --method POST --url "https://graph.microsoft.com/v1.0/servicePrincipals/$spOid/appRoleAssignedTo" --headers "Content-Type=application/json" --body "@$tmpDoctor" | Out-Null
    Remove-Item $tmpDoctor -Force
    Write-Host 'Assignment created: HCPE-DOCTOR -> DOCTOR'
  }
}

Write-Host '=== SPA required resource access ==='
$spaRra=az ad app show --id $spaAppId --query 'requiredResourceAccess' -o json | ConvertFrom-Json
$already=$null
if ($spaRra) { $already = $spaRra | Where-Object { $_.resourceAppId -eq $apiAppId } | Select-Object -First 1 }
if ($already) { Write-Host 'SPA already references API app' }
else {
  $newEntry=@{resourceAppId=$apiAppId;resourceAccess=@(@{id=$scopeGuid;type='Scope'})}
  $merged=@(); if ($spaRra) { $merged += $spaRra }; $merged += $newEntry
  $mJson=$merged | ConvertTo-Json -Depth 10 -Compress
  az ad app update --id $spaAppId --required-resource-accesses $mJson | Out-Null
  Write-Host 'SPA required resource access updated'
}

Write-Host '=== SUMMARY ==='
Write-Host "API_APP_ID=$apiAppId"
Write-Host "API_AUDIENCE=$targetAud"
Write-Host "API_SP_OBJECT_ID=$spOid"
Write-Host "SPA_APP_ID=$spaAppId"
Write-Host "FRONTEND_SCOPE=openid profile api://$tenantId/hpe-devx-api/access_as_user"
foreach ($rv in $roleValues) { Write-Host "GROUP_$rv=$($groupMap[$rv]) ROLE_$rv=$($roleMap[$rv])" }
