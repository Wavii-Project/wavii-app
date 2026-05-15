import { http } from './http';

export interface DirectMessage {
  id: string;
  senderId: string;
  senderName: string;
  content: string;
  createdAt: string;
}

export async function apiFetchConversation(userId: string, token: string): Promise<DirectMessage[]> {
  const { data } = await http.get<DirectMessage[]>(`/api/messages/direct/${userId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return data;
}

export async function apiSendDirectMessage(
  userId: string,
  content: string,
  token: string
): Promise<DirectMessage> {
  const { data } = await http.post<DirectMessage>(
    `/api/messages/direct/${userId}`,
    { content },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}
