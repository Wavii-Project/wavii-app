# Wavii Teacher Verification (Odoo 17)

## What this module adds

- Model: `wavii.teacher.verification`
- Menu: `Wavii > Verificaciones Wavii`
- Status workflow: `pending`, `approved`, `rejected`
- Buttons in form view:
  - `Descargar PDF`
  - `Aprobar`
  - `Rechazar`

## Required Odoo system parameters

Set these in **Settings > Technical > Parameters > System Parameters**:

- `wavii.backend_base_url` = backend URL reachable from Odoo  
  Example: `http://backend:8080`
- `wavii.odoo_webhook_secret` = same value as `ODOO_WEBHOOK_SECRET` in backend

## Backend integration

Backend creates verification records through JSON-RPC on model
`wavii.teacher.verification`. If module is not installed yet, backend falls
back to creating `project.task` in `Verificaciones Wavii`.
