import { http } from './http';

export interface ClassEnrollment {
  id: string;
  teacherId: string;
  teacherName: string;
  studentId: string;
  studentName: string;
  instrument: string | null;
  city: string;
  province: string;
  modality: string;
  requestedModality: string;
  unitPrice: number;
  paymentStatus: string;
  requestMessage: string;
  requestAvailability: string;
  classLink: string;
  createdAt: string;
  teacherRole: string;
  hoursPurchased: number;
  hoursUsed: number;
  hoursRemaining: number;
  canRefund: boolean;
  canChat: boolean;
  nextSession: ClassSession | null;
}

export interface ClassSession {
  id: string;
  enrollmentId: string;
  teacherId: string;
  teacherName: string;
  studentId: string;
  studentName: string;
  scheduledAt: string;
  durationMinutes: number;
  status: string;
  meetingUrl: string;
  notes: string;
}

export interface ClassMessage {
  id: string;
  enrollmentId: string;
  senderId: string;
  senderName: string;
  content: string;
  createdAt: string;
}

export interface ClassPost {
  id: string;
  teacherId: string;
  teacherName: string;
  title: string;
  content: string;
  createdAt: string;
}

export interface ClassManageResponse {
  classes: ClassEnrollment[];
  sessions: ClassSession[];
  posts: ClassPost[];
}

export async function apiRequestClass(
  _token: string,
  teacherId: string,
  payload: { message?: string; availability?: string; requestedModality?: string }
): Promise<ClassEnrollment> {
  const { data } = await http.post<ClassEnrollment>(`/api/classes/${teacherId}/request`, payload);
  return data;
}

export async function apiFetchClasses(_token: string): Promise<ClassEnrollment[]> {
  const { data } = await http.get<ClassEnrollment[]>('/api/classes');
  return data;
}

export async function apiCreateClassCheckout(_token: string, teacherId: string): Promise<{
  enrollmentId: string;
  paymentIntentId?: string;
  clientSecret?: string;
  devMode?: boolean;
}> {
  const { data } = await http.post(`/api/classes/${teacherId}/checkout`, {});
  return data;
}

export async function apiConfirmClass(_token: string, enrollmentId: string): Promise<ClassEnrollment> {
  const { data } = await http.post<ClassEnrollment>(`/api/classes/${enrollmentId}/confirm`, {});
  return data;
}

export async function apiUpdateClassStatus(
  _token: string,
  enrollmentId: string,
  payload: { status: string; reason?: string }
): Promise<ClassEnrollment> {
  const { data } = await http.patch<ClassEnrollment>(`/api/classes/${enrollmentId}/status`, payload);
  return data;
}

export async function apiFetchClassMessages(_token: string, enrollmentId: string): Promise<ClassMessage[]> {
  const { data } = await http.get<ClassMessage[]>(`/api/classes/${enrollmentId}/messages`);
  return data;
}

export async function apiSendClassMessage(_token: string, enrollmentId: string, content: string): Promise<ClassMessage> {
  const { data } = await http.post<ClassMessage>(`/api/classes/${enrollmentId}/messages`, { content });
  return data;
}

export async function apiFetchClassPosts(_token: string, teacherId: string): Promise<ClassPost[]> {
  const { data } = await http.get<ClassPost[]>(`/api/classes/${teacherId}/posts`);
  return data;
}

export async function apiFetchStudentClassPosts(_token: string): Promise<ClassPost[]> {
  const { data } = await http.get<ClassPost[]>('/api/classes/posts');
  return data;
}

export async function apiFetchManageClasses(_token: string): Promise<ClassManageResponse> {
  const { data } = await http.get<ClassManageResponse>('/api/classes/manage');
  return data;
}

export async function apiCreateClassPost(
  _token: string,
  teacherId: string,
  payload: { title: string; content: string }
): Promise<ClassPost> {
  const { data } = await http.post<ClassPost>(`/api/classes/${teacherId}/posts`, payload);
  return data;
}

export async function apiRequestExtraHour(_token: string, enrollmentId: string): Promise<{
  enrollmentId: string;
  paymentIntentId?: string;
  clientSecret?: string;
  devMode?: boolean;
}> {
  const { data } = await http.post(`/api/classes/${enrollmentId}/request-extra-hour`, {});
  return data;
}

export async function apiRequestClassRefund(_token: string, enrollmentId: string): Promise<ClassEnrollment> {
  const { data } = await http.post<ClassEnrollment>(`/api/classes/${enrollmentId}/refund-request`, {});
  return data;
}

export async function apiCreateClassSession(
  _token: string,
  enrollmentId: string,
  payload: { scheduledAt: string; durationMinutes?: string; meetingUrl?: string; notes?: string }
): Promise<ClassSession> {
  const { data } = await http.post<ClassSession>(`/api/classes/${enrollmentId}/sessions`, payload);
  return data;
}

export async function apiUpdateClassSession(
  _token: string,
  sessionId: string,
  payload: { scheduledAt?: string; durationMinutes?: string; meetingUrl?: string; notes?: string; status?: string }
): Promise<ClassSession> {
  const { data } = await http.patch<ClassSession>(`/api/classes/sessions/${sessionId}`, payload);
  return data;
}
