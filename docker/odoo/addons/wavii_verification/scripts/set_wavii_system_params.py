"""
Run inside Odoo shell to configure required Wavii system parameters.

Usage example:
odoo shell -d wavii_odoo < /mnt/extra-addons/wavii_verification/scripts/set_wavii_system_params.py
"""

backend_base_url = "http://backend:8080"
odoo_webhook_secret = "wavii_odoo_secret_2026"

params = env["ir.config_parameter"].sudo()

params.set_param("wavii.backend_base_url", backend_base_url)
params.set_param("wavii.odoo_webhook_secret", odoo_webhook_secret)

print("Wavii system parameters updated:")
print(f"- wavii.backend_base_url = {backend_base_url}")
print(f"- wavii.odoo_webhook_secret = {odoo_webhook_secret}")
