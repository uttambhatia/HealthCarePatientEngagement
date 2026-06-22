# Hub-Spoke Azure IaC (Phase 1/2)

This folder contains initial Bicep templates to bootstrap the hub-spoke network model required for regional DR.

## Templates

- `01-hub-vnet.bicep`: Hub VNet and shared service subnets.
- `02-primary-vnet.bicep`: Primary spoke VNet with UI, IoT, application, and data subnets.
- `03-secondary-vnet.bicep`: Secondary spoke VNet with mirrored subnet model for DR.
- `04-vnet-peering.bicep`: Hub <-> primary and hub <-> secondary peering.
- `05-traffic-manager.bicep`: Traffic Manager profile for failover between primary and secondary Front Door endpoints.
- `06-front-door.bicep`: Regional Azure Front Door profile with App Service as origin.
- `07-network-security-groups.bicep`: NSGs for primary and secondary spoke subnets.
- `08-private-dns.bicep`: Private DNS zones and VNet links for private endpoints.
- `09-private-endpoints.bicep`: Optional private endpoints for SQL, Storage, Key Vault, and Service Bus.
- `10-apim.bicep`: APIM instance in hub VNet with global JWT and rate-limit policy.
- `11-apim-api-routing.bicep`: APIM API, primary/secondary backends, and backend selection policy wiring.
- `apim/global-policy.xml`: APIM global policy used by `10-apim.bicep`.
- `apim/api-routing-policy.xml`: APIM API-level routing policy used by `11-apim-api-routing.bicep`.
- `main.bicep`: Orchestrates deployment of the templates above.
- `main.parameters.example.json`: Example parameter file.
- `parameters/dev.parameters.json`: Environment parameter baseline for dev.
- `parameters/prod.parameters.json`: Environment parameter baseline for prod.
- `parameters/secondary.parameters.json`: Environment parameter baseline for secondary/DR rehearsal.

## Notes

- This baseline now includes network topology, edge routing, NSG baseline, private DNS, and optional private endpoints.
- Advanced data/messaging DR automations are added in subsequent increments.
- Address spaces are intentionally parameterized and should be reviewed before applying in shared subscriptions.
- Deploy with `az deployment group create` and validate with `az deployment group what-if`.

## Example

```powershell
az deployment group what-if `
  --resource-group <rg-name> `
  --template-file deploy/azure/main.bicep `
  --parameters @deploy/azure/main.parameters.example.json

az deployment group create `
  --resource-group <rg-name> `
  --template-file deploy/azure/main.bicep `
  --parameters @deploy/azure/main.parameters.example.json
```
