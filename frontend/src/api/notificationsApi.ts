import { http } from './http';

const authHeader = (token: string) => ({ Authorization: `Bearer ${token}` });

export interface AppNotification {
  id: string;
  type: string;
  title: string;
  body: string;
  data: Record<string, unknown>;
  read: boolean;
  createdAt: string;
}

export interface NotificationListResponse {
  items: AppNotification[];
  summary: { unreadCount: number };
}

export async function apiFetchNotifications(token: string): Promise<NotificationListResponse> {
  const { data } = await http.get<NotificationListResponse>('/api/notifications', {
    headers: authHeader(token),
  });
  return data;
}

export async function apiMarkNotificationRead(
  notificationId: string,
  token: string,
): Promise<AppNotification> {
  const { data } = await http.patch<AppNotification>(
    `/api/notifications/${notificationId}/read`,
    {},
    { headers: authHeader(token) },
  );
  return data;
}

export async function apiClearNotifications(token: string): Promise<{ removed: number }> {
  const { data } = await http.delete<{ removed: number }>('/api/notifications', {
    headers: authHeader(token),
  });
  return data;
}

export async function apiMarkAllNotificationsRead(token: string): Promise<{ updated: number }> {
  const { data } = await http.patch<{ updated: number }>(
    '/api/notifications/read-all',
    {},
    { headers: authHeader(token) },
  );
  return data;
}
