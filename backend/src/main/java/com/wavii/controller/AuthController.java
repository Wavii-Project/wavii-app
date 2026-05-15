package com.wavii.controller;

import com.wavii.dto.auth.*;
import com.wavii.repository.UserRepository;
import com.wavii.service.AuthService;
import com.wavii.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            boolean isNameConflict = msg != null && msg.startsWith("__NAME__");
            String cleanMsg = isNameConflict ? msg.substring(8) : msg;
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "code", "CONFLICT",
                            "field", isNameConflict ? "name" : "email",
                            "message", cleanMsg != null ? cleanMsg : "Conflicto al registrar"
                    ));
        } catch (Exception e) {
            log.error("Error en registro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SERVER_ERROR", "Error interno del servidor"));
        }
    }

    @GetMapping("/check-name")
    public ResponseEntity<Map<String, Boolean>> checkName(
            @RequestParam String name,
            @AuthenticationPrincipal com.wavii.model.User currentUser) {
        String trimmed = name.strip();
        boolean taken = userRepository.existsByNameIgnoreCase(trimmed)
                && (currentUser == null || !currentUser.getName().equalsIgnoreCase(trimmed));
        return ResponseEntity.ok(Map.of("taken", taken));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (AuthService.EmailNotVerifiedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(errorBody(e.getCode(), e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorBody("INVALID_CREDENTIALS", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SERVER_ERROR", "Error interno del servidor"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            AuthResponse response = authService.refreshToken(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorBody("INVALID_TOKEN", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en refresh token", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SERVER_ERROR", "Error interno del servidor"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Si el email existe, recibirás un enlace para restablecer tu contraseña"
            ));
        } catch (Exception e) {
            log.error("Error en forgot-password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SERVER_ERROR", "Error interno del servidor"));
        }
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> resetPasswordForm(@RequestParam String token) {
        String html = """
            <!DOCTYPE html><html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1.0">
              <title>Wavii — Restablecer contraseña</title>
              <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800;900&display=swap" rel="stylesheet">
              <style>
                *{box-sizing:border-box;margin:0;padding:0}
                html,body{height:100%}
                body{font-family:'Nunito',sans-serif;background:#FFF7ED;min-height:100dvh;display:flex;align-items:center;justify-content:center;padding:20px;overflow-y:auto}
                .card{background:#fff;border-radius:24px;padding:36px 28px 40px;text-align:center;box-shadow:0 8px 40px rgba(255,122,0,0.12);max-width:420px;width:100%;margin:auto}
                .logo{font-size:28px;font-weight:900;color:#FF7A00;letter-spacing:-0.5px;margin-bottom:24px}
                h1{font-size:20px;font-weight:800;color:#1A1A2E;margin-bottom:8px}
                p{font-size:14px;color:#666680;margin-bottom:28px;line-height:1.5}
                label{display:block;text-align:left;font-size:13px;font-weight:700;color:#1A1A2E;margin-bottom:6px}
                input{width:100%;padding:14px 16px;border:1.5px solid #E5E7EB;border-radius:12px;font-size:15px;font-family:inherit;outline:none;transition:border-color .2s;margin-bottom:16px}
                input:focus{border-color:#FF7A00}
                button{width:100%;padding:16px;background:#FF7A00;color:#fff;border:none;border-radius:12px;font-size:16px;font-weight:800;font-family:inherit;cursor:pointer;margin-top:4px;transition:opacity .2s}
                button:disabled{opacity:.6;cursor:default}
                .msg{margin-top:20px;padding:14px 16px;border-radius:12px;font-size:14px;font-weight:600;display:none}
                .msg.ok{background:rgba(34,197,94,0.1);color:#16A34A;display:block}
                .msg.err{background:rgba(239,68,68,0.1);color:#DC2626;display:block}
              </style>
            </head>
            <body>
              <div class="card">
                <div class="logo">Wavii</div>
                <h1>Nueva contraseña</h1>
                <p>Introduce tu nueva contraseña.<br>Debe tener al menos 6 caracteres.</p>
                <form id="form">
                  <label for="pwd">Nueva contraseña</label>
                  <input id="pwd" type="password" placeholder="••••••••" minlength="6" required>
                  <label for="pwd2">Repetir contraseña</label>
                  <input id="pwd2" type="password" placeholder="••••••••" minlength="6" required>
                  <button type="submit" id="btn">Restablecer contraseña</button>
                </form>
                <div class="msg" id="msg"></div>
              </div>
              <script>
                const token = {{TOKEN}};
                document.getElementById('form').addEventListener('submit', async e => {
                  e.preventDefault();
                  const pwd = document.getElementById('pwd').value;
                  const pwd2 = document.getElementById('pwd2').value;
                  const msg = document.getElementById('msg');
                  const btn = document.getElementById('btn');
                  msg.className = 'msg';
                  if (pwd !== pwd2) {
                    msg.textContent = 'Las contraseñas no coinciden.';
                    msg.className = 'msg err';
                    return;
                  }
                  btn.disabled = true;
                  btn.textContent = 'Guardando...';
                  try {
                    const res = await fetch('/api/auth/reset-password', {
                      method: 'POST',
                      headers: {
                        'Content-Type': 'application/json',
                        'ngrok-skip-browser-warning': 'true'
                      },
                      body: JSON.stringify({ token, newPassword: pwd })
                    });
                    if (res.ok) {
                      document.getElementById('form').style.display = 'none';
                      msg.textContent = '✅ Contraseña actualizada. Ya puedes iniciar sesión en la app.';
                      msg.className = 'msg ok';
                    } else {
                      let errMsg = 'El enlace ha expirado o ya fue usado.';
                      try { const d = await res.json(); if (d.message) errMsg = d.message; } catch {}
                      msg.textContent = errMsg;
                      msg.className = 'msg err';
                      btn.disabled = false;
                      btn.textContent = 'Restablecer contraseña';
                    }
                  } catch {
                    msg.textContent = 'Error de red. Inténtalo de nuevo.';
                    msg.className = 'msg err';
                    btn.disabled = false;
                    btn.textContent = 'Restablecer contraseña';
                  }
                });
              </script>
            </body></html>
            """.replace("{{TOKEN}}", quoteJs(token));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message", "Contraseña restablecida correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody("INVALID_TOKEN", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en reset-password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SERVER_ERROR", "Error interno del servidor"));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1.0">
                  <title>Wavii — Cuenta verificada</title>
                  <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800;900&display=swap" rel="stylesheet">
                  <style>
                    *{box-sizing:border-box;margin:0;padding:0}
                    body{font-family:'Nunito',sans-serif;background:#FFF7ED;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px}
                    .card{
                      background:#fff;
                      border-radius:24px;
                      padding:52px 40px 56px;
                      text-align:center;
                      box-shadow:0 8px 40px rgba(255,122,0,0.12);
                      max-width:400px;
                      width:100%%;
                      display:flex;
                      flex-direction:column;
                      align-items:center;
                      gap:0;
                    }
                    .brand{
                      font-size:64px;
                      font-weight:900;
                      color:#FF7A00;
                      letter-spacing:-3px;
                      line-height:1;
                      margin-bottom:36px;
                    }
                    .badge{
                      display:inline-block;
                      background:rgba(34,197,94,0.12);
                      color:#16A34A;
                      font-weight:700;
                      font-size:13px;
                      padding:6px 18px;
                      border-radius:999px;
                      margin-bottom:24px;
                    }
                    h1{
                      font-size:28px;
                      font-weight:800;
                      color:#1A1A2E;
                      margin-bottom:14px;
                    }
                    .body-text{
                      font-size:15px;
                      color:#666680;
                      line-height:1.7;
                      margin-bottom:0;
                    }
                    .body-text strong{color:#1A1A2E;font-weight:700}
                    .hint{
                      font-size:12px;
                      color:#C0C0C0;
                      margin-top:28px;
                    }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="brand">Wavii</div>
                    <div class="badge">Cuenta verificada</div>
                    <h1>Ya estas dentro</h1>
                    <p class="body-text">Tu correo ha sido confirmado.<br>Vuelve a la app: <strong>entrara automaticamente</strong> en unos segundos.</p>
                    <p class="hint">Puedes cerrar esta ventana.</p>
                  </div>
                </body>
                </html>
                """;
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (Exception e) {
            String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1.0">
                  <title>Wavii — Enlace invalido</title>
                  <link href="https://fonts.googleapis.com/css2?family=Nunito:wght@400;600;700;800;900&display=swap" rel="stylesheet">
                  <style>
                    *{box-sizing:border-box;margin:0;padding:0}
                    body{font-family:'Nunito',sans-serif;background:#FFF7ED;min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px}
                    .card{
                      background:#fff;
                      border-radius:24px;
                      padding:52px 40px 56px;
                      text-align:center;
                      box-shadow:0 8px 40px rgba(255,122,0,0.12);
                      max-width:400px;
                      width:100%%;
                      display:flex;
                      flex-direction:column;
                      align-items:center;
                      gap:0;
                    }
                    .brand{
                      font-size:64px;
                      font-weight:900;
                      color:#FF7A00;
                      letter-spacing:-3px;
                      line-height:1;
                      margin-bottom:36px;
                    }
                    .badge{
                      display:inline-block;
                      background:rgba(239,68,68,0.10);
                      color:#DC2626;
                      font-weight:700;
                      font-size:13px;
                      padding:6px 18px;
                      border-radius:999px;
                      margin-bottom:24px;
                    }
                    h1{
                      font-size:28px;
                      font-weight:800;
                      color:#1A1A2E;
                      margin-bottom:14px;
                    }
                    .body-text{
                      font-size:15px;
                      color:#666680;
                      line-height:1.7;
                      margin-bottom:0;
                    }
                    .hint{
                      font-size:12px;
                      color:#C0C0C0;
                      margin-top:28px;
                    }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="brand">Wavii</div>
                    <div class="badge">Enlace invalido</div>
                    <h1>Algo ha salido mal</h1>
                    <p class="body-text">Este enlace ya fue usado o ha expirado.<br>Solicita uno nuevo desde la app.</p>
                    <p class="hint">Los enlaces expiran en 24 horas.</p>
                  </div>
                </body>
                </html>
                """;
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(html);
        }
    }

    @GetMapping("/check-verification")
    public ResponseEntity<Map<String, Boolean>> checkVerification(@RequestParam String email) {
        boolean verified = authService.isEmailVerified(email);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(errorBody("VALIDATION_ERROR", "El email es obligatorio"));
            }
            authService.resendVerification(email);
            return ResponseEntity.ok(Map.of("message", "Email de verificación reenviado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(errorBody("BAD_REQUEST", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en resend-verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SERVER_ERROR", "Error interno del servidor"));
        }
    }

    // Los endpoints verify-teacher-phone y confirm-teacher-phone han sido eliminados.
    // La verificación de profesor_particular ocurre automáticamente al verificar el email.

    @GetMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestParam String to) {
        try {
            emailService.sendTestEmail(to);
            return ResponseEntity.ok(Map.of("message", "Test email enviado a " + to));
        } catch (Exception e) {
            log.error("Error en test-email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("SMTP_ERROR", e.getMessage()));
        }
    }

    private Map<String, String> errorBody(String code, String message) {
        return Map.of("code", code, "message", message);
    }

    /** Escapa un String para incrustarlo como literal JS entre comillas simples. */
    private String quoteJs(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }
}
