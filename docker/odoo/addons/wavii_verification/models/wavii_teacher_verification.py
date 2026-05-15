from odoo import api, fields, models, _
from odoo.exceptions import UserError
import requests


class WaviiTeacherVerification(models.Model):
    _name = "wavii.teacher.verification"
    _description = "Wavii Teacher Verification"
    _inherit = ["mail.thread", "mail.activity.mixin"]
    _order = "created_at desc, id desc"

    user_id = fields.Char(string="Wavii User ID", required=True, tracking=True)
    teacher_name = fields.Char(string="Teacher Name", required=True, tracking=True)
    email = fields.Char(string="Email", required=True, tracking=True)
    document_filename = fields.Char(string="Document Filename", required=True)
    document_attachment_id = fields.Many2one("ir.attachment", string="Document", required=False)
    status = fields.Selection(
        [
            ("pending", "Pending"),
            ("approved", "Approved"),
            ("rejected", "Rejected"),
        ],
        default="pending",
        required=True,
        tracking=True,
    )
    created_at = fields.Datetime(
        string="Created At",
        required=True,
        default=lambda self: fields.Datetime.now(),
        tracking=True,
    )
    reviewed_at = fields.Datetime(string="Reviewed At", tracking=True)
    reviewer_id = fields.Many2one("res.users", string="Reviewed By", tracking=True)

    @api.model
    def create(self, vals):
        if vals.get("document_attachment_id"):
            attachment = self.env["ir.attachment"].browse(vals["document_attachment_id"])
            vals["document_filename"] = attachment.name or vals.get("document_filename")
        return super().create(vals)

    def action_open_document(self):
        self.ensure_one()
        if not self.document_attachment_id:
            raise UserError(_("This verification has no attached document."))
        return {
            "type": "ir.actions.act_url",
            "url": "/web/content/%s?download=true" % self.document_attachment_id.id,
            "target": "self",
        }

    def action_approve(self):
        for record in self:
            record._send_webhook_action("approve")
            record.write(
                {
                    "status": "approved",
                    "reviewed_at": fields.Datetime.now(),
                    "reviewer_id": self.env.user.id,
                }
            )

    def action_reject(self):
        for record in self:
            record._send_webhook_action("reject")
            record.write(
                {
                    "status": "rejected",
                    "reviewed_at": fields.Datetime.now(),
                    "reviewer_id": self.env.user.id,
                }
            )

    def _send_webhook_action(self, action):
        self.ensure_one()
        base_url = self.env["ir.config_parameter"].sudo().get_param("wavii.backend_base_url", "").strip()
        shared_secret = self.env["ir.config_parameter"].sudo().get_param("wavii.odoo_webhook_secret", "").strip()

        if not base_url:
            raise UserError(_("Missing system parameter: wavii.backend_base_url"))
        if not shared_secret:
            raise UserError(_("Missing system parameter: wavii.odoo_webhook_secret"))

        url = "%s/api/verification/odoo-webhook" % base_url.rstrip("/")
        payload = {"userId": self.user_id, "action": action}
        headers = {"X-Odoo-Secret": shared_secret, "Content-Type": "application/json"}

        try:
            response = requests.post(url, json=payload, headers=headers, timeout=15)
        except requests.RequestException as exc:
            raise UserError(_("Error calling Wavii backend: %s") % str(exc)) from exc

        if response.status_code >= 300:
            raise UserError(
                _(
                    "Wavii backend returned an error (%s): %s"
                )
                % (response.status_code, response.text[:500])
            )
