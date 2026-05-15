export type SubscriptionPlanId = 'free' | 'plus' | 'education';

export interface SubscriptionPlanDefinition {
  id: SubscriptionPlanId;
  name: string;
  tagline: string;
  priceLine: string;
  features: string[];
  gradient: [string, string];
  outlined: boolean;
  recommendedLabel?: string;
}

export const SUBSCRIPTION_PLANS: SubscriptionPlanDefinition[] = [
  {
    id: 'plus',
    name: 'Wavii Plus',
    tagline: 'Ideal para avanzar sin límites en tu práctica diaria.',
    priceLine: '7,99 €/mes · 14 días gratis la primera vez',
    gradient: ['#FF8A00', '#FF6A00'],
    outlined: false,
    recommendedLabel: 'RECOMENDADO',
    features: [
      'Descarga offline para practicar cuando quieras',
      'Sin anuncios en toda la experiencia',
      'Estadísticas y herramientas premium',
    ],
  },
  {
    id: 'education',
    name: 'Wavii Scholar',
    tagline: 'Pensado para aprender con profesores y acceder al tablón completo.',
    priceLine: '7,99 €/mes · Promo desde Plus: 2,99 € el primer mes',
    gradient: ['#8B5CF6', '#6D28D9'],
    outlined: false,
    features: [
      'Acceso completo al tablón de anuncios',
      'Publicación de anuncios para profesores',
      'Plan educativo con gestión desde Stripe',
    ],
  },
  {
    id: 'free',
    name: 'Wavii Free',
    tagline: 'Empieza gratis y mantén tus funciones esenciales.',
    priceLine: 'Gratis para siempre',
    gradient: ['#6B7280', '#4B5563'],
    outlined: false,
    features: [
      'Acceso básico a contenido y perfil',
      'Funciones sociales esenciales',
      'Puedes subir de plan cuando quieras',
    ],
  },
];

export const PLAN_BY_ID = Object.fromEntries(
  SUBSCRIPTION_PLANS.map((plan) => [plan.id, plan])
) as Record<SubscriptionPlanId, SubscriptionPlanDefinition>;
