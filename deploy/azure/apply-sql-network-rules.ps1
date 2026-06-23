# Apply SQL Server VNet rules directly via Azure CLI
# This script adds network rules to SQL Server restricting access to specified subnets

param(
    [string]$ResourceGroup = 'rg-azuser7080_mml.local-1nLQA',
    [string]$SqlServerName = 'sqlhpedev2ae8ee0',
    [string]$DataSubnetId = '/subscriptions/4116346b-5ded-4f3b-8387-f8d055802adc/resourceGroups/rg-azuser7080_mml.local-1nLQA/providers/Microsoft.Network/virtualNetworks/hpe-primary-vnet-dev/subnets/data-subnet',
    [string]$AppSubnetId = '/subscriptions/4116346b-5ded-4f3b-8387-f8d055802adc/resourceGroups/rg-azuser7080_mml.local-1nLQA/providers/Microsoft.Network/virtualNetworks/hpe-primary-vnet-dev/subnets/application-subnet'
)

Write-Host "Applying SQL Server VNet rules..." -ForegroundColor Cyan

# Add VNet rule for data-subnet
Write-Host "  - Adding rule for data-subnet..." -ForegroundColor Gray
az sql server vnet-rule create `
    --resource-group $ResourceGroup `
    --server $SqlServerName `
    --name data-subnet-rule `
    --subnet $DataSubnetId `
    --ignore-missing-endpoint true `
    --output none 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "    ✓ Data-subnet rule added" -ForegroundColor Green
} else {
    Write-Host "    ✗ Failed to add data-subnet rule" -ForegroundColor Yellow
}

# Add VNet rule for application-subnet
Write-Host "  - Adding rule for application-subnet..." -ForegroundColor Gray
az sql server vnet-rule create `
    --resource-group $ResourceGroup `
    --server $SqlServerName `
    --name application-subnet-rule `
    --subnet $AppSubnetId `
    --ignore-missing-endpoint true `
    --output none 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "    ✓ Application-subnet rule added" -ForegroundColor Green
} else {
    Write-Host "    ✗ Failed to add application-subnet rule" -ForegroundColor Yellow
}

# List all rules
Write-Host "`nVerifying SQL Server VNet rules:" -ForegroundColor Cyan
az sql server vnet-rule list `
    --resource-group $ResourceGroup `
    --server $SqlServerName `
    --query "[].{name:name, subnet:virtualNetworkSubnetId}" `
    --output table

Write-Host "`n✓ SQL Server network rules configuration complete" -ForegroundColor Green
