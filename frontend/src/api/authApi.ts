import axios from 'axios';
import { publicHttp } from './http';

// ── Tipos ──────────────────────────────────────────────────

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface AuthApiResponse {
  accessToken: string;
  refreshToken: string;
  userId: string;
  name: string;
  email: string;
  city?: string;
  role: string;
  subscription: string;
  emailVerified: boolean;
  onboardingCompleted: boolean;
  teacherVerified: boolean;
}

// ── Llamadas ───────────────────────────────────────────────

/** Registra un nuevo usuario. El backend envía email de verificación automáticamente. */
export async function apiRegister(payload: RegisterPayload): Promise<AuthApiResponse> {
  const { data } = await publicHttp.post<AuthApiResponse>('/api/auth/register', payload);
  return data;
}

/** Inicia sesión. Lanza error si el email no está verificado (FORBIDDEN). */
export async function apiLogin(payload: LoginPayload): Promise<AuthApiResponse> {
  const { data } = await publicHttp.post<AuthApiResponse>('/api/auth/login', payload);
  return data;
}

export async function apiRefreshToken(refreshToken: string): Promise<AuthApiResponse> {
  const { data } = await publicHttp.post<AuthApiResponse>('/api/auth/refresh', { refreshToken });
  return data;
}

/** Solicita un correo para restablecer contraseña. */
export async function apiForgotPassword(email: string): Promise<void> {
  await publicHttp.post('/api/auth/forgot-password', { email });
}

/** Reenvía el correo de verificación. */
export async function apiResendVerification(email: string): Promise<void> {
  await publicHttp.post('/api/auth/resend-verification', { email });
}

/** Comprueba si un nombre de usuario ya está en uso (true = disponible). */
export async function apiCheckNameAvailable(name: string): Promise<boolean> {
  const { data } = await publicHttp.get<{ taken: boolean }>('/api/auth/check-name', {
    params: { name: name.trim() },
  });
  return !data.taken;
}

/** Comprueba si el email de un usuario ya está verificado en el backend. */
export async function apiCheckEmailVerified(email: string): Promise<boolean> {
  const { data } = await publicHttp.get<{ verified: boolean }>('/api/auth/check-verification', {
    params: { email },
  });
  return data.verified;
}

/** Extrae el mensaje de error del backend o del axios error. */
export function getApiErrorMessage(err: unknown, fallback = 'Error de red. Verifica tu conexión.'): string {
  if (axios.isAxiosError(err)) {
    const msg = err.response?.data?.message;
    if (msg) return msg;
    if (err.code === 'ECONNREFUSED' || err.code === 'ERR_NETWORK') {
      return 'No se puede conectar al servidor. ¿Está Docker corriendo?';
    }
  }
  return fallback;
}
