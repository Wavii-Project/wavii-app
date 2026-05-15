import { http } from './http';

export interface NewsArticle {
  articleId: string;
  title: string;
  description: string | null;
  url: string;
  imageUrl: string | null;
  sourceName: string | null;
  publishedAt: string | null;
}

export async function apiFetchNews(params?: {
  q?: string;
  size?: number;
}): Promise<NewsArticle[]> {
  const { data } = await http.get<NewsArticle[]>('/api/news', {
    params: {
      q: params?.q ?? 'music',
      size: params?.size ?? 6,
    },
  });
  return data;
}

/** Formatea una fecha ISO de NewsData.io a texto legible en español */
export function formatNewsDate(isoDate: string | null): string {
  if (!isoDate) return '';
  try {
    return new Date(isoDate).toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  } catch {
    return isoDate;
  }
}
