import { http } from './http';

export interface StartSubscriptionResponse {
  subscriptionId: string;
  status: string;
  clientSecret?: string;
  cancelAtPeriodEnd?: boolean;
  currentPeriodEnd?: string;
  devMode?: boolean;
}

export interface SetupIntentResponse {
  ephemeralKey: string;
  setupIntentClientSecret: string;
  setupIntentId: string;
  customerId: string;
  trialUsed?: boolean;
  devMode?: boolean;
}

export interface SubscriptionStatusResponse {
  subscription: string;
  subscriptionStatus: string;
  stripeSubscriptionId: string;
  cancelAtPeriodEnd: boolean;
  currentPeriodEnd: string;
  trialUsed: boolean;
  deletionScheduledAt: string;
}

export interface CancelSubscriptionResponse {
  subscriptionId?: string;
  status?: string;
  cancelAtPeriodEnd: boolean;
  currentPeriodEnd: string;
  devMode?: boolean;
}

export interface ChangeSubscriptionResponse {
  subscriptionId?: string;
  status?: string;
  cancelAtPeriodEnd?: boolean;
  currentPeriodEnd?: string;
  promoApplied?: boolean;
  /** Si true, el frontend debe iniciar el flujo de payment sheet */
  needsPaymentSheet?: boolean;
  trialUsed?: boolean;
  devMode?: boolean;
}

/** Paso 1 del Payment Sheet: obtiene SetupIntent + EphemeralKey */
export async function apiCreateSetupIntent(
  plan: 'plus' | 'scholar',
  token: string
): Promise<SetupIntentResponse> {
  const { data } = await http.post<SetupIntentResponse>(
    '/api/subscription/setup-intent',
    { plan },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

/** Paso 2 del Payment Sheet: confirma y crea la suscripción */
export async function apiConfirmSubscription(
  plan: 'plus' | 'scholar',
  setupIntentId: string,
  token: string
): Promise<StartSubscriptionResponse> {
  const { data } = await http.post<StartSubscriptionResponse>(
    '/api/subscription/confirm',
    { plan, setupIntentId },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

/** Flujo legacy: crea suscripción directamente con paymentMethodId */
export async function apiStartSubscription(
  plan: 'plus' | 'scholar',
  paymentMethodId: string,
  token: string
): Promise<StartSubscriptionResponse> {
  const { data } = await http.post<StartSubscriptionResponse>(
    '/api/subscription/start',
    { plan, paymentMethodId },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

/** Cancela la suscripción al final del periodo (el usuario conserva el plan hasta entonces) */
export async function apiCancelSubscription(token: string): Promise<CancelSubscriptionResponse> {
  const { data } = await http.post<CancelSubscriptionResponse>(
    '/api/subscription/cancel',
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

/** Reactiva una suscripción que estaba programada para cancelarse */
export async function apiReactivateSubscription(token: string): Promise<{ cancelAtPeriodEnd: boolean }> {
  const { data } = await http.post<{ cancelAtPeriodEnd: boolean }>(
    '/api/subscription/reactivate',
    {},
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

/**
 * Cambia el plan de una suscripción activa directamente (sin payment sheet).
 * Si el backend devuelve needsPaymentSheet=true, el frontend debe iniciar el flujo de pago.
 */
export async function apiChangeSubscription(
  plan: 'plus' | 'scholar',
  token: string
): Promise<ChangeSubscriptionResponse> {
  const { data } = await http.post<ChangeSubscriptionResponse>(
    '/api/subscription/change',
    { plan },
    { headers: { Authorization: `Bearer ${token}` } }
  );
  return data;
}

export async function apiGetSubscriptionStatus(token: string): Promise<SubscriptionStatusResponse> {
  const { data } = await http.get<SubscriptionStatusResponse>('/api/subscription/status', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return data;
}
