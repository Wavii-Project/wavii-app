export type NormalizedSubscription = 'free' | 'plus' | 'education';

export function normalizeSubscription(value: string | null | undefined): NormalizedSubscription {
  const normalized = String(value ?? '').trim().toLowerCase();

  if (normalized === 'plus') {
    return 'plus';
  }

  if (normalized === 'education' || normalized === 'scholar') {
    return 'education';
  }

  return 'free';
}

export function hasScholarAccess(value: string | null | undefined): boolean {
  return normalizeSubscription(value) === 'education';
}
