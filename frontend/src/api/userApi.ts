import { http, publicHttp } from './http';
import { PdfDocument } from './pdfApi';

// ─── Perfil público ───────────────────────────────────────────────────────────

export interface PublicUserProfile {
  id: string;
  name: string;
  level: string | null;
  role: string;
  xp: number;
  streak: number;
  bestStreak: number;
  tabsPublished: number;
  acceptsMessages: boolean;
  memberSince: string | null; // "2026-05"
}

export async function apiFetchPublicProfile(userId: string): Promise<PublicUserProfile> {
  const { data } = await publicHttp.get<PublicUserProfile>(`/api/users/${userId}`);
  return data;
}

export async function apiFetchUserTabs(userId: string, token?: string): Promise<PdfDocument[]> {
  const { data } = await publicHttp.get<PdfDocument[]>(`/api/users/${userId}/tabs`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return data;
}

export async function apiReportUser(userId: string, reason: string, token: string): Promise<void> {
  await http.post(`/api/users/${userId}/report`, { reason }, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function apiBlockUser(userId: string, token: string): Promise<void> {
  await http.post(`/api/users/${userId}/block`, {}, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function apiUnblockUser(userId: string, token: string): Promise<void> {
  await http.delete(`/api/users/${userId}/block`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function apiSetAcceptsMessages(accepts: boolean, token: string): Promise<void> {
  await http.patch('/api/users/me/accepts-messages', { acceptsMessages: accepts }, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function apiCheckNameForUpdate(name: string, token: string): Promise<boolean> {
  const { data } = await http.get<{ taken: boolean }>('/api/users/me/check-name', {
    params: { name: name.trim() },
    headers: { Authorization: `Bearer ${token}` },
  });
  return !data.taken;
}

export async function apiUpdateName(
  name: string,
  token: string,
  city?: string,
): Promise<{ name: string; city?: string }> {
  const { data } = await http.patch<{ name: string }>(
    '/api/users/me',
    { name, ...(city !== undefined ? { city } : {}) },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

export async function apiChangePassword(
  currentPassword: string,
  newPassword: string,
  token: string
): Promise<{ message: string }> {
  const { data } = await http.patch<{ message: string }>(
    '/api/users/me/password',
    { currentPassword, newPassword },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

export interface ScheduleDeletionResponse {
  deletionScheduledAt: string;
  message: string;
}

/** Programa la eliminación de la cuenta (cancela suscripción Stripe al final del periodo) */
export async function apiScheduleDeletion(token: string): Promise<ScheduleDeletionResponse> {
  const { data } = await http.delete<ScheduleDeletionResponse>('/api/users/me', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return data;
}

/** Cancela la eliminación programada y reactiva la suscripción si aplica */
export async function apiCancelDeletion(token: string): Promise<{ message: string }> {
  const { data } = await http.patch<{ message: string }>(
    '/api/users/me/deletion-cancel',
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}
