import { http } from './http';

export interface BulletinTeacher {
  id: string;
  name: string;
  role: 'profesor_particular' | 'profesor_certificado';
  bio: string | null;
  instrument: string | null;
  pricePerHour: number | null;
  city: string | null;
  latitude: number | null;
  longitude: number | null;
  address: string | null;
  province: string | null;
  contactEmail: string | null;
  contactPhone: string | null;
  instagramUrl: string | null;
  tiktokUrl: string | null;
  youtubeUrl: string | null;
  facebookUrl: string | null;
  bannerImageUrl: string | null;
  placeImageUrls: string[];
  availabilityPreference: string | null;
  availabilityNotes: string | null;
  /** PRESENCIAL | ONLINE | AMBAS */
  classModality: string | null;
}

export interface BulletinBoardResponse {
  teachers: BulletinTeacher[];
  hasFullAccess: boolean;
  canPublish: boolean;
  visibleLimit: number;
  totalCount: number;
  hiddenCount: number;
  requiredPlan: 'scholar';
}

export interface BulletinPostPayload {
  instrument: string;
  /** null -> Gratis */
  pricePerHour: number | null;
  bio: string;
  city?: string | null;
  latitude?: number;
  longitude?: number;
  address?: string | null;
  province?: string | null;
  contactEmail?: string | null;
  contactPhone?: string | null;
  instagramUrl?: string | null;
  tiktokUrl?: string | null;
  youtubeUrl?: string | null;
  facebookUrl?: string | null;
  bannerImageUrl?: string | null;
  placeImageUrls?: string[] | null;
  availabilityPreference?: string | null;
  availabilityNotes?: string | null;
  /** PRESENCIAL | ONLINE | AMBAS */
  classModality?: string | null;
}

export interface BulletinImageUploadResponse {
  url: string;
  fileName: string;
}

export async function apiFetchBulletin(_token: string, params?: {
  query?: string;
  instrument?: string;
  role?: string;
  modality?: string;
  availability?: string;
  city?: string;
}): Promise<BulletinBoardResponse> {
  const { data } = await http.get<BulletinBoardResponse>('/api/bulletin', {
    params,
  });
  return data;
}

export async function apiFetchTeacherProfile(_token: string, teacherId: string): Promise<BulletinTeacher> {
  const { data } = await http.get<BulletinTeacher>(`/api/bulletin/${teacherId}`);
  return data;
}

export async function apiPostBulletin(
  payload: BulletinPostPayload,
  _token: string
): Promise<BulletinTeacher> {
  const { data } = await http.post<BulletinTeacher>('/api/bulletin', payload);
  return data;
}

export async function apiUploadBulletinImage(
  file: { uri: string; name: string; type: string },
  _token: string,
  kind: 'banner' | 'place'
): Promise<BulletinImageUploadResponse> {
  const form = new FormData();
  form.append('file', {
    uri: file.uri,
    name: file.name,
    type: file.type,
  } as any);
  form.append('kind', kind);

  const { data } = await http.post<BulletinImageUploadResponse>('/api/bulletin/images', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function apiReportTeacher(
  teacherId: string,
  payload: { reason: string; details?: string }
): Promise<{ id: string; status: string }> {
  const { data } = await http.post<{ id: string; status: string }>(`/api/teachers/${teacherId}/reports`, payload);
  return data;
}
