package com.wavii.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Servicio para el envío de correos electrónicos.
 * Maneja el envío de verificaciones, recuperaciones de contraseña y notificaciones.
 * 
 * @author eduglezexp
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${wavii.app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Envía un email para verificar la cuenta del usuario.
     * 
     * @param toEmail Email del destinatario.
     * @param userName Nombre del usuario.
     * @param token Token de verificación.
     */
    @Async
    public void sendVerificationEmail(String toEmail, String userName, String token) {
        String verificationLink = baseUrl + "/api/auth/verify-email?token=" + token;
        String html = buildHtml(
            "Verifica tu cuenta en Wavii",
            "Hola <strong>" + userName + "</strong>,",
            "Gracias por registrarte en Wavii. Haz clic en el botón de abajo para verificar tu correo electrónico y activar tu cuenta.",
            verificationLink,
            "Verificar mi cuenta",
            "Este enlace expirará en 24 horas."
        );
        sendHtmlEmail(toEmail, "✅ Verifica tu cuenta en Wavii", html);
    }

    /**
     * Envía un email para restablecer la contraseña del usuario.
     * 
     * @param toEmail Email del destinatario.
     * @param userName Nombre del usuario.
     * @param token Token de recuperación.
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String token) {
        String resetLink = baseUrl + "/api/auth/reset-password?token=" + token;
        String html = buildHtml(
            "Restablece tu contraseña de Wavii",
            "Hola <strong>" + userName + "</strong>,",
            "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta. Haz clic en el botón de abajo para crear una nueva contraseña.",
            resetLink,
            "Restablecer contraseña",
            "Este enlace expirará en 1 hora. Si no solicitaste este cambio, ignora este correo."
        );
        sendHtmlEmail(toEmail, "🔑 Restablece tu contraseña de Wavii", html);
    }

    /**
     * Envía un email informando que la verificación de profesor ha sido aprobada.
     * 
     * @param toEmail Email del destinatario.
     * @param userName Nombre del usuario.
     */
    @Async
    public void sendVerificationApprovedEmail(String toEmail, String userName) {
        String html = buildInfoHtml(
            "¡Ya eres Profesor Certificado en Wavii!",
            "Hola <strong>" + userName + "</strong>,",
            "Tu documentación ha sido revisada y aprobada por nuestro equipo. " +
            "Ya tienes la insignia de <strong>Profesor Certificado</strong> en tu perfil de Wavii.",
            "Puedes abrir la app y ver tu insignia en la sección de perfil."
        );
        sendHtmlEmail(toEmail, "Tu verificación ha sido aprobada — Wavii", html);
    }

    /**
     * Envía un email informando que la verificación de profesor ha sido rechazada.
     * 
     * @param toEmail Email del destinatario.
     * @param userName Nombre del usuario.
     */
    @Async
    public void sendVerificationRejectedEmail(String toEmail, String userName) {
        String html = buildInfoHtml(
            "Sobre tu verificación en Wavii",
            "Hola <strong>" + userName + "</strong>,",
            "Hemos revisado tu documentación pero no hemos podido verificarla en este momento. " +
            "Tu cuenta sigue activa como Profesor con acceso completo a Scholar. " +
            "Puedes volver a intentarlo en cualquier momento desde tu perfil.",
            "Si tienes dudas, escríbenos a soporte@wavii.app."
        );
        sendHtmlEmail(toEmail, "Sobre tu verificación en Wavii", html);
    }

    /**
     * Envía una notificación genérica relacionada con una clase por email.
     * 
     * @param toEmail Email del destinatario.
     * @param userName Nombre del destinatario.
     * @param title Título del mensaje.
     * @param body Cuerpo del mensaje.
     */
    @Async
    public void sendClassNotificationEmail(String toEmail, String userName, String title, String body) {
        String html = buildInfoHtml(
            title,
            "Hola <strong>" + userName + "</strong>,",
            body,
            "Puedes revisar los detalles dentro de la app de Wavii."
        );
        sendHtmlEmail(toEmail, title + " - Wavii", html);
    }

    /**
     * Envía un email de prueba para validar la configuración SMTP.
     * 
     * @param to Email del destinatario.
     */
    public void sendTestEmail(String to) {
        String html = buildHtml(
            "Test de SMTP — Wavii",
            "Hola,",
            "Este es un correo de prueba para verificar que el servidor SMTP está configurado correctamente.",
            baseUrl,
            "Ir a Wavii",
            "Puedes ignorar este correo."
        );
        sendHtmlEmail(to, "🧪 Test SMTP Wavii", html);
        log.info("[EMAIL] Test email enviado a {}", to);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("[EMAIL] Enviado a {} — {}", to, subject);
        } catch (MessagingException e) {
            log.error("[EMAIL] Error al enviar a {}: {}", to, e.getMessage());
        }
    }

    private String buildInfoHtml(String title, String greeting, String body, String footer) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background:#FFF7ED;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#FFF7ED;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                    <tr><td style="background:#FF7A00;padding:32px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:32px;font-weight:900;letter-spacing:-1px;">Wavii</h1>
                      <p style="color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:14px;">Tu música. Tu ritmo. Tu comunidad.</p>
                    </td></tr>
                    <tr><td style="padding:40px 48px;">
                      <h2 style="color:#1A1A2E;font-size:22px;margin:0 0 16px;">%s</h2>
                      <p style="color:#1A1A2E;font-size:16px;margin:0 0 12px;">%s</p>
                      <p style="color:#666680;font-size:15px;line-height:1.6;margin:0 0 24px;">%s</p>
                      <p style="color:#999999;font-size:13px;text-align:center;margin:0;">%s</p>
                    </td></tr>
                    <tr><td style="background:#F9F9F9;padding:20px 48px;text-align:center;border-top:1px solid #EEEEEE;">
                      <p style="color:#AAAAAA;font-size:12px;margin:0;">© 2026 Wavii · wavii.app</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(title, greeting, body, footer);
    }

    private String buildHtml(String title, String greeting, String body, String link, String btnText, String footer) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"></head>
            <body style="margin:0;padding:0;background:#FFF7ED;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#FFF7ED;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
                    <!-- Header naranja -->
                    <tr><td style="background:#FF7A00;padding:32px;text-align:center;">
                      <h1 style="color:#ffffff;margin:0;font-size:32px;font-weight:900;letter-spacing:-1px;">Wavii</h1>
                      <p style="color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:14px;">Tu música. Tu ritmo. Tu comunidad.</p>
                    </td></tr>
                    <!-- Cuerpo -->
                    <tr><td style="padding:40px 48px;">
                      <h2 style="color:#1A1A2E;font-size:22px;margin:0 0 16px;">%s</h2>
                      <p style="color:#1A1A2E;font-size:16px;margin:0 0 12px;">%s</p>
                      <p style="color:#666680;font-size:15px;line-height:1.6;margin:0 0 32px;">%s</p>
                      <div style="text-align:center;margin-bottom:32px;">
                        <a href="%s" style="display:inline-block;background:#FF7A00;color:#ffffff;text-decoration:none;padding:16px 40px;border-radius:12px;font-size:16px;font-weight:700;letter-spacing:0.3px;">%s</a>
                      </div>
                      <p style="color:#999999;font-size:13px;text-align:center;margin:0;">%s</p>
                    </td></tr>
                    <!-- Footer -->
                    <tr><td style="background:#F9F9F9;padding:20px 48px;text-align:center;border-top:1px solid #EEEEEE;">
                      <p style="color:#AAAAAA;font-size:12px;margin:0;">© 2026 Wavii · wavii.app</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(title, greeting, body, link, btnText, footer);
    }
}
