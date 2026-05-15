import * as SecureStore from 'expo-secure-store';
import AsyncStorage from '@react-native-async-storage/async-storage';

export const TOKEN_KEY = 'wavii_token';
export const REFRESH_TOKEN_KEY = 'wavii_refresh_token';
export const USER_KEY = 'wavii_user';

type SessionListener = (session: { accessToken: string | null; refreshToken: string | null }) => void;

const sessionListeners = new Set<SessionListener>();

function notifySessionListeners(accessToken: string | null, refreshToken: string | null) {
  sessionListeners.forEach((listener) => listener({ accessToken, refreshToken }));
}

export async function getStoredAccessToken(): Promise<string | null> {
  return SecureStore.getItemAsync(TOKEN_KEY);
}

export async function getStoredRefreshToken(): Promise<string | null> {
  return SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
}

export async function saveStoredSession(accessToken: string, refreshToken: string | null): Promise<void> {
  await SecureStore.setItemAsync(TOKEN_KEY, accessToken);
  if (refreshToken) {
    await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, refreshToken);
  } else {
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
  }
  notifySessionListeners(accessToken, refreshToken);
}

export async function clearStoredSession(): Promise<void> {
  await SecureStore.deleteItemAsync(TOKEN_KEY);
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
  await AsyncStorage.removeItem(USER_KEY);
  notifySessionListeners(null, null);
}

export function subscribeSessionChanges(listener: SessionListener): () => void {
  sessionListeners.add(listener);
  return () => {
    sessionListeners.delete(listener);
  };
}

export function isBackendSessionToken(token: string | null | undefined): token is string {
  if (!token) {
    return false;
  }

  if (
    token.startsWith('mock-') ||
    token.startsWith('guest-') ||
    token.startsWith('verified-token-')
  ) {
    return false;
  }

  return token.split('.').length === 3;
}
