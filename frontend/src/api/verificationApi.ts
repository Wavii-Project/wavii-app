// timeout mayor (30 s) para subidas de documentos de verificación
import { http } from './http';

export interface VerificationStatusResponse {
  status: 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';
  fileName?: string;
  createdAt?: string;
}

export async function apiUploadDocument(
  uri: string,
  fileName: string,
  mimeType: string,
  token: string
): Promise<{ status: string; fileName: string }> {
  const form = new FormData();
  form.append('document', { uri, name: fileName, type: mimeType } as any);

  const { data } = await http.post('/api/verification/upload-document', form, {
    headers: {
      'Content-Type': 'multipart/form-data',
      Authorization: `Bearer ${token}`,
    },
  });
  return data;
}

export async function apiGetVerificationStatus(token: string): Promise<VerificationStatusResponse> {
  const { data } = await http.get<VerificationStatusResponse>('/api/verification/status', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return data;
}
