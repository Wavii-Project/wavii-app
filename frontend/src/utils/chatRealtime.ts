export type ChatSocketState = 'connecting' | 'open' | 'closed' | 'error';

const RECONNECT_DELAYS_MS = [1000, 2000, 5000, 10000] as const;

export function getReconnectDelay(attempt: number): number {
  return RECONNECT_DELAYS_MS[Math.min(attempt, RECONNECT_DELAYS_MS.length - 1)];
}

export function getPollingInterval(state: ChatSocketState): number {
  return state === 'open' ? 25000 : 3000;
}

export function getSocketStatusLabel(state: ChatSocketState): string | null {
  if (state === 'connecting') return 'Reconectando...';
  if (state === 'error' || state === 'closed') return 'Sin conexion en tiempo real';
  return null;
}

export function sanitizeWsUrl(url: string): string {
  return url.replace(/(token=)[^&]+/i, '$1***');
}
