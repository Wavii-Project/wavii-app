import { PUBLIC_BASE_URL } from './config';
import { getApiErrorMessage } from './authApi';
import { http } from './http';

export interface PdfDocument {
  id: number;
  originalName: string;
  fileName: string;
  fileSize: number;
  pageCount: number;
  uploadedAt: string;
  songTitle: string | null;
  description: string | null;
  coverImageUrl: string | null;
  difficulty: number;
  likeCount: number;
  likedByMe: boolean;
  ownerName: string | null;
  ownerId: string;
}

export type PdfSortOption = 'NEWEST' | 'OLDEST' | 'MOST_LIKED' | 'LEAST_LIKED';

function normalizeUrl(url: string | null): string | null {
  if (!url) return null;
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  return `${PUBLIC_BASE_URL}${url}`;
}

function normalizePdf(pdf: PdfDocument): PdfDocument {
  return {
    ...pdf,
    coverImageUrl: normalizeUrl(pdf.coverImageUrl),
  };
}

export async function apiFetchPublicPdfs(
  params: { search?: string; difficulty?: number; sort?: PdfSortOption },
  token?: string,
): Promise<PdfDocument[]> {
  const { data } = await http.get<PdfDocument[]>('/api/pdfs/public', {
    params: {
      search: params.search || undefined,
      difficulty: params.difficulty || undefined,
      sort: params.sort || undefined,
    },
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return data.map(normalizePdf);
}

export async function apiFetchMyPdfs(token: string): Promise<PdfDocument[]> {
  const { data } = await http.get<PdfDocument[]>('/api/pdfs', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return data.map(normalizePdf);
}

export async function apiFetchPdfById(id: number, token?: string): Promise<PdfDocument> {
  const { data } = await http.get<PdfDocument>(`/api/pdfs/${id}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  return normalizePdf(data);
}

export async function apiUploadPdf(
  uri: string,
  name: string,
  songTitle: string,
  description: string,
  coverImageUri: string | null,
  difficulty: number,
  token: string,
): Promise<PdfDocument> {
  const form = new FormData();
  const safeName = name?.trim() || 'tablatura.pdf';
  form.append('file', { uri, name: safeName, type: 'application/pdf' } as any);
  form.append('songTitle', songTitle);
  form.append('description', description);
  form.append('difficulty', String(difficulty));
  if (coverImageUri) {
    form.append('coverImage', {
      uri: coverImageUri,
      name: 'cover.jpg',
      type: 'image/jpeg',
    } as any);
  }

  try {
    const { data } = await http.post<PdfDocument>('/api/pdfs', form, {
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'multipart/form-data',
      },
    });
    return normalizePdf(data);
  } catch (err) {
    throw new Error(getApiErrorMessage(err, 'No se pudo subir la tablatura.'));
  }
}

export async function apiDeletePdf(id: number, token: string): Promise<void> {
  await http.delete(`/api/pdfs/${id}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
}

export async function apiLikePdf(id: number, token: string): Promise<PdfDocument> {
  const { data } = await http.post<PdfDocument>(`/api/pdfs/${id}/like`, null, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return normalizePdf(data);
}

export async function apiUnlikePdf(id: number, token: string): Promise<PdfDocument> {
  const { data } = await http.delete<PdfDocument>(`/api/pdfs/${id}/like`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  return normalizePdf(data);
}

export function pdfDownloadUrl(id: number): string {
  return `${PUBLIC_BASE_URL}/api/pdfs/${id}/download`;
}

export async function apiReportPdf(
  id: number,
  payload: { reason: string; details?: string },
  token: string,
): Promise<{ message: string }> {
  const { data } = await http.post<{ message: string }>(
    `/api/pdfs/${id}/report`,
    payload,
    { headers: { Authorization: `Bearer ${token}` } },
  );
  return data;
}
