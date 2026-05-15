// Usamos el http de http.ts que incluye el interceptor de refresco automático
// de token. Así, si el access token (15 min) caduca mientras el usuario está
// en la app, se renueva transparentemente y la petición se reintenta sin 401.
import { http } from './http';

export type ForumCategory =
  | 'FANDOM'
  | 'COMUNIDAD_MUSICAL'
  | 'TEORIA'
  | 'INSTRUMENTOS'
  | 'BANDAS'
  | 'ARTISTAS'
  | 'GENERAL';

export type ForumSortOption = 'NEWEST' | 'OLDEST' | 'MOST_LIKED' | 'LEAST_LIKED';
export type ForumMembershipRole = 'OWNER' | 'ADMIN' | 'MEMBER';

export interface ForumMember {
  userId: string;
  name: string;
  avatarUrl: string | null;
  role: ForumMembershipRole;
  joinedAt: string;
}

export interface ForumSummary {
  id: string;
  name: string;
  description: string | null;
  category: ForumCategory;
  memberCount: number;
  joined: boolean;
  coverImageUrl: string | null;
  creatorName: string;
  city: string | null;
  likeCount: number;
  likedByMe: boolean;
}

export interface ForumDetail extends ForumSummary {
  creatorId: string;
  createdAt: string;
  currentUserRole: ForumMembershipRole | null;
  members: ForumMember[];
}

export interface Post {
  id: string;
  content: string;
  authorId: string;
  authorName: string;
  authorAvatarUrl: string | null;
  createdAt: string;
}

export interface PostsPage {
  content: Post[];
  totalPages: number;
  number: number;
  last: boolean;
}

const authHeader = (token: string) => ({ Authorization: `Bearer ${token}` });

export const apiGetForums = (
  params: { search?: string; city?: string; category?: ForumCategory; sort?: ForumSortOption },
  token: string,
) =>
  http
    .get<ForumSummary[]>('/api/forums', {
      params: {
        search: params.search?.trim() || undefined,
        city: params.city?.trim() || undefined,
        category: params.category,
        sort: params.sort,
      },
      headers: authHeader(token),
    })
    .then((response) => response.data);

export const apiGetMyForums = (token: string) =>
  http
    .get<ForumSummary[]>('/api/forums/my', { headers: authHeader(token) })
    .then((response) => response.data);

export const apiGetForum = (id: string, token: string) =>
  http
    .get<ForumDetail>(`/api/forums/${id}`, { headers: authHeader(token) })
    .then((response) => response.data);

export const apiCreateForum = (
  payload: {
    name: string;
    description: string;
    category: ForumCategory;
    coverImageUrl?: string;
    city?: string;
  },
  token: string,
) =>
  http
    .post<ForumDetail>('/api/forums', payload, { headers: authHeader(token) })
    .then((response) => response.data);

export interface ImageUploadResponse {
  url: string;
  fileName: string;
}

export const apiUploadForumImage = (
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
    .post<ImageUploadResponse>('/api/forums/images', form, {
      headers: {
        ...authHeader(token),
        'Content-Type': 'multipart/form-data',
      },
    })
    .then((response) => response.data);
};

export const apiUpdateForum = (
  id: string,
  payload: { name?: string; description?: string; coverImageUrl?: string; category?: ForumCategory },
  token: string,
) =>
  http
    .patch<ForumDetail>(`/api/forums/${id}`, payload, { headers: authHeader(token) })
    .then((response) => response.data);

export const apiJoinForum = (id: string, token: string) =>
  http.post(`/api/forums/${id}/join`, {}, { headers: authHeader(token) });

export const apiLeaveForum = (id: string, token: string) =>
  http.delete(`/api/forums/${id}/join`, { headers: authHeader(token) });

export const apiLikeForum = (id: string, token: string) =>
  http.post<ForumDetail>(`/api/forums/${id}/like`, {}, { headers: authHeader(token) }).then((response) => response.data);

export const apiUnlikeForum = (id: string, token: string) =>
  http.delete<ForumDetail>(`/api/forums/${id}/like`, { headers: authHeader(token) }).then((response) => response.data);

export const apiGetForumMembers = (id: string, token: string) =>
  http.get<ForumMember[]>(`/api/forums/${id}/members`, { headers: authHeader(token) }).then((response) => response.data);

export const apiUpdateForumMemberRole = (id: string, userId: string, role: 'ADMIN' | 'MEMBER', token: string) =>
  http
    .patch<ForumDetail>(`/api/forums/${id}/members/${userId}/role`, { role }, { headers: authHeader(token) })
    .then((response) => response.data);

export const apiRemoveForumMember = (id: string, userId: string, token: string) =>
  http.delete(`/api/forums/${id}/members/${userId}`, { headers: authHeader(token) });

export const apiGetPosts = (id: string, page: number, token: string) =>
  http
    .get<PostsPage>(`/api/forums/${id}/posts`, {
      params: { page },
      headers: authHeader(token),
    })
    .then((response) => response.data);

export const apiCreatePost = (id: string, content: string, token: string) =>
  http
    .post<Post>(`/api/forums/${id}/posts`, { content }, { headers: authHeader(token) })
    .then((response) => response.data);
