import { http } from './http';

export type ListingType = 'BANDA_BUSCA_MUSICOS' | 'MUSICO_BUSCA_BANDA';

export type MusicalGenre =
  | 'ROCK' | 'METAL' | 'POP' | 'JAZZ' | 'BLUES' | 'CLASICA'
  | 'ELECTRONICA' | 'REGGAETON' | 'SALSA' | 'CUMBIA' | 'BACHATA'
  | 'HIP_HOP' | 'REGGAE' | 'FOLK' | 'INDIE' | 'PUNK' | 'FUNK'
  | 'R_AND_B' | 'LATIN' | 'OTRO';

export type MusicianRole =
  | 'VOCALISTA' | 'GUITARRISTA' | 'BAJISTA' | 'BATERISTA'
  | 'PERCUSIONISTA' | 'PIANISTA' | 'TECLADISTA' | 'PRODUCTOR'
  | 'DJ' | 'VIOLINISTA' | 'TROMPETISTA' | 'SAXOFONISTA' | 'OTRO';

export interface BandListing {
  id: string;
  title: string;
  description: string | null;
  type: ListingType;
  genre: MusicalGenre;
  city: string;
  roles: MusicianRole[];
  creatorId: string;
  creatorName: string;
  contactInfo: string | null;
  coverImageUrl: string | null;
  imageUrls: string[];
  createdAt: string;
}

export interface BandListingsPage {
  content: BandListing[];
  totalPages: number;
  number: number;
  last: boolean;
}

const authHeader = (token?: string) => (token ? { Authorization: `Bearer ${token}` } : {});

export const apiGetBandListings = (
  params: { genre?: string; city?: string; role?: string; page?: number },
  token?: string,
) =>
  http
    .get<BandListingsPage>('/api/bands', {
      params: { ...params, page: params.page ?? 0 },
      headers: authHeader(token),
    })
    .then(r => r.data);

export const apiGetMyBandListings = (token: string) =>
  http
    .get<BandListing[]>('/api/bands/my', { headers: authHeader(token) })
    .then(r => r.data);

export const apiGetBandListing = (id: string, token: string) =>
  http
    .get<BandListing>(`/api/bands/${id}`, { headers: authHeader(token) })
    .then(r => r.data);

export const apiCreateBandListing = (
  payload: {
    title: string;
    description: string;
    type: ListingType;
    genre: MusicalGenre;
    city: string;
    roles: MusicianRole[];
    contactInfo?: string;
    coverImageUrl?: string;
    imageUrls?: string[];
  },
  token: string,
) =>
  http
    .post<BandListing>('/api/bands', payload, { headers: authHeader(token) })
    .then(r => r.data);

export interface BandImageUploadResponse {
  url: string;
  fileName: string;
}

export const apiUploadBandImage = (
  asset: { uri: string; name: string; type: string },
  token: string,
) => {
  const form = new FormData();
  form.append('file', {
    uri: asset.uri,
    name: asset.name,
    type: asset.type,
  } as any);

  return http
    .post<BandImageUploadResponse>('/api/bands/images', form, {
      headers: {
        ...authHeader(token),
      },
    })
    .then(r => r.data);
};

export const apiDeleteBandListing = (id: string, token: string) =>
  http.delete(`/api/bands/${id}`, { headers: authHeader(token) });
