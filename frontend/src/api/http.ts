import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from './config';
import { clearStoredSession, getStoredAccessToken, getStoredRefreshToken, isBackendSessionToken, saveStoredSession } from '../auth/session';

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };
const NGROK_SKIP_BROWSER_WARNING_HEADER = 'ngrok-skip-browser-warning';

const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
  [NGROK_SKIP_BROWSER_WARNING_HEADER]: 'true',
};

const AUTH_FREE_PATHS = new Set([
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/refresh',
  '/api/auth/forgot-password',
  '/api/auth/reset-password',
  '/api/auth/resend-verification',
  '/api/auth/check-verification',
]);

export const publicHttp = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: DEFAULT_HEADERS,
});

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15_000,
  headers: DEFAULT_HEADERS,
});

let refreshPromise: Promise<string | null> | null = null;

function setHeader(headers: InternalAxiosRequestConfig['headers'], name: string, value: string) {
  if (typeof headers.set === 'function') {
    headers.set(name, value);
    return;
  }

  (headers as Record<string, string>)[name] = value;
}

function getHeader(headers: InternalAxiosRequestConfig['headers'], name: string) {
  if (typeof headers.get === 'function') {
    return headers.get(name);
  }

  const record = headers as Record<string, unknown>;
  return record[name] ?? record[name.toLowerCase()];
}

function ensureNgrokHeader(config: InternalAxiosRequestConfig) {
  const nextConfig = { ...config };
  nextConfig.headers = nextConfig.headers ?? {};
  setHeader(nextConfig.headers, NGROK_SKIP_BROWSER_WARNING_HEADER, 'true');
  return nextConfig;
}

function rejectNgrokBrowserWarning<T>(response: AxiosResponse<T>) {
  const contentType = String(response.headers?.['content-type'] ?? '');
  const body = typeof response.data === 'string' ? response.data.toLowerCase() : '';

  if (contentType.includes('text/html') && body.includes('ngrok')) {
    return Promise.reject(new Error('NGROK_BROWSER_WARNING'));
  }

  return response;
}

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = await getStoredRefreshToken();
  if (!refreshToken) {
    await clearStoredSession();
    return null;
  }

  if (!refreshPromise) {
    refreshPromise = publicHttp
      .post('/api/auth/refresh', { refreshToken })
      .then(async ({ data }) => {
        const nextAccessToken = data?.accessToken as string | undefined;
        const nextRefreshToken = (data?.refreshToken as string | undefined) ?? refreshToken;
        if (!nextAccessToken) {
          throw new Error('No se recibió un access token nuevo');
        }
        await saveStoredSession(nextAccessToken, nextRefreshToken);
        return nextAccessToken;
      })
      .catch(async () => {
        await clearStoredSession();
        return null;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }

  return refreshPromise;
}

publicHttp.interceptors.request.use((config) => ensureNgrokHeader(config));
publicHttp.interceptors.response.use(rejectNgrokBrowserWarning);

http.interceptors.request.use(async (config) => {
  const nextConfig = ensureNgrokHeader(config);
  const headers = nextConfig.headers;
  const token = await getStoredAccessToken();
  const authorization = getHeader(headers, 'Authorization');
  const explicitAuthorization = typeof authorization === 'string' && authorization.trim().length > 0;

  if (!explicitAuthorization && token) {
    setHeader(headers, 'Authorization', `Bearer ${token}`);
  }

  nextConfig.headers = headers;
  return nextConfig;
});

http.interceptors.response.use(
  rejectNgrokBrowserWarning,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableConfig | undefined;
    const status = error.response?.status;
    const path = originalRequest?.url ?? '';

    if (!originalRequest || originalRequest._retry || !status || AUTH_FREE_PATHS.has(path)) {
      return Promise.reject(error);
    }

    if (status !== 401 && status !== 403) {
      return Promise.reject(error);
    }

    const currentToken = await getStoredAccessToken();
    if (!isBackendSessionToken(currentToken)) {
      return Promise.reject(new Error('SESSION_INVALID'));
    }

    const nextToken = await refreshAccessToken();
    if (!nextToken) {
      return Promise.reject(error);
    }

    originalRequest._retry = true;
    originalRequest.headers = originalRequest.headers ?? {};
    setHeader(originalRequest.headers, NGROK_SKIP_BROWSER_WARNING_HEADER, 'true');
    setHeader(originalRequest.headers, 'Authorization', `Bearer ${nextToken}`);
    return http(originalRequest);
  }
);
