import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { useAuth } from '../../context/AuthContext';
import { useAlert } from '../../context/AlertContext';
import { useTheme } from '../../context/ThemeContext';
import { WaviiPromoBanner } from '../../components/common/WaviiPromoBanner';
import { WaviiSpeech } from '../../components/common/WaviiSpeech';
import { apiGetSubscriptionStatus, SubscriptionStatusResponse } from '../../api/subscriptionApi';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { SUBSCRIPTION_PLANS, SubscriptionPlanId } from './subscriptionPlans';

type NavigationProp = NativeStackNavigationProp<AppStackParamList, 'Subscription'>;
type RouteT = RouteProp<AppStackParamList, 'Subscription'>;

function formatDate(iso?: string): string {
  if (!iso) return '';
  try {
    return new Date(iso).toLocaleDateString('es-ES', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  } catch {
    return iso;
  }
}

export const SubscriptionScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<RouteT>();
  const isOnboarding = route.params?.isOnboarding ?? false;
  const { token, user, updateUser } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();

  const [status, setStatus] = useState<SubscriptionStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const currentPlan = (status?.subscription ?? user?.subscription ?? 'free') as SubscriptionPlanId;
  const cancelAtPeriodEnd = status?.cancelAtPeriodEnd ?? user?.subscriptionCancelAtPeriodEnd ?? false;
  const currentPeriodEnd = status?.currentPeriodEnd ?? user?.subscriptionCurrentPeriodEnd;
  const scholarPromoEligible =
    currentPlan === 'plus' && (status?.subscriptionStatus ?? '') !== 'trialing';

  const loadStatus = useCallback(async () => {
    if (!token) {
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const response = await apiGetSubscriptionStatus(token);
      setStatus(response);
      updateUser({
        subscription: response.subscription as SubscriptionPlanId,
        subscriptionCancelAtPeriodEnd: response.cancelAtPeriodEnd,
        subscriptionCurrentPeriodEnd: response.currentPeriodEnd || undefined,
        trialUsed: response.trialUsed,
        deletionScheduledAt: response.deletionScheduledAt || undefined,
      });
    } catch (err: any) {
      showAlert({
        title: 'No se pudo cargar tu suscripción',
        message: err?.response?.data?.message ?? 'Vuelve a intentarlo en unos segundos.',
      });
    } finally {
      setLoading(false);
    }
  }, [showAlert, token, updateUser]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  const cards = useMemo(
    () =>
      SUBSCRIPTION_PLANS.map((plan) => {
        const isCurrent = plan.id === currentPlan;
        const isScholarPromo = plan.id === 'education' && scholarPromoEligible;

        return {
          ...plan,
          isCurrent,
          cta:
            plan.id === 'free'
              ? currentPlan === 'free'
                ? 'Seguir con Free'
                : 'Ver cómo pasar a Free'
              : isCurrent
              ? 'Gestionar plan'
              : `Ver ${plan.name}`,
          badge: isCurrent
            ? cancelAtPeriodEnd && plan.id !== 'free'
              ? 'Finaliza pronto'
              : 'Tu plan actual'
            : isScholarPromo
            ? 'Promo activa'
            : plan.recommendedLabel,
          helper:
            plan.id === 'education' && isScholarPromo
              ? 'Primer mes a 2,99 € si vienes desde Plus fuera del trial.'
              : plan.tagline,
        };
      }),
    [cancelAtPeriodEnd, currentPlan, scholarPromoEligible]
  );

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>

      {loading ? (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : (
        <ScrollView
          contentContainerStyle={styles.content}
          showsVerticalScrollIndicator={false}
        >
          <View style={styles.speechWrap}>
            <WaviiSpeech text="¿Qué plan quieres usar ahora?" />
          </View>

          {cancelAtPeriodEnd && currentPeriodEnd ? (
            <View
              style={[
                styles.banner,
                { backgroundColor: Colors.warningLight, borderColor: Colors.warning },
              ]}
            >
              <Ionicons name="time-outline" size={18} color={Colors.warning} />
              <Text style={[styles.bannerText, { color: Colors.warning }]}>
                Tu suscripción seguirá activa hasta el {formatDate(currentPeriodEnd)} y después pasará a Free.
              </Text>
            </View>
          ) : null}

          {currentPlan === 'plus' ? (
            <WaviiPromoBanner
              title="Da el salto a Scholar"
              body="Si quieres enseñar, publicar en el tablón y gestionar tu perfil docente, Scholar es el siguiente paso."
              icon="school-outline"
              tone="education"
              ctaLabel="Ver Scholar"
              onPress={() => navigation.navigate('SubscriptionPlan', { planId: 'education' })}
            />
          ) : null}

          {currentPlan === 'education' && !user?.teacherVerified ? (
            <WaviiPromoBanner
              title="Activa tu insignia certificada"
              body="Ya tienes Scholar. Sube tu título o diploma para que Odoo revise tu perfil y aparezcas como profesor certificado."
              icon="shield-checkmark-outline"
              tone="education"
              ctaLabel="Subir documento"
              onPress={() => navigation.navigate('TeacherVerification')}
            />
          ) : null}

          {cards.map((plan) =>
            plan.outlined ? (
              <TouchableOpacity
                key={plan.id}
                style={[
                  styles.freeCard,
                  {
                    backgroundColor: colors.surface,
                    borderColor: plan.isCurrent ? Colors.primaryOpacity20 : colors.border,
                  },
                ]}
                activeOpacity={0.9}
                onPress={() => navigation.navigate('SubscriptionPlan', { planId: plan.id })}
              >
                <LinearGradient
                  colors={[Colors.backgroundLight, Colors.white]}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 1 }}
                  style={styles.freeHero}
                >
                  <View style={styles.freeBlobTopRight} />
                  <View style={styles.freeBlobBottomLeft} />
                  {plan.badge ? (
                    <View style={[styles.freeBadge, { backgroundColor: Colors.primaryOpacity10 }]}>
                      <Text style={styles.freeBadgeText}>{plan.badge}</Text>
                    </View>
                  ) : null}
                  <Text style={styles.freeTitle}>{plan.name}</Text>
                  <Text style={styles.freeCopy}>{plan.helper}</Text>
                </LinearGradient>
                <View style={styles.freeButton}>
                  <Text style={styles.freeButtonText}>{plan.cta}</Text>
                  <Ionicons name="chevron-forward" size={16} color={Colors.primary} />
                </View>
              </TouchableOpacity>
            ) : (
              <TouchableOpacity
                key={plan.id}
                activeOpacity={0.92}
                onPress={() => navigation.navigate('SubscriptionPlan', { planId: plan.id })}
              >
                <LinearGradient
                  colors={plan.gradient}
                  start={{ x: 0, y: 0 }}
                  end={{ x: 1, y: 1 }}
                  style={styles.premiumCard}
                >
                  <View style={styles.blobTopRight} />
                  <View style={styles.blobBottomLeft} />

                  {plan.badge ? (
                    <View style={styles.premiumBadge}>
                      <Text style={styles.premiumBadgeText}>{plan.badge}</Text>
                    </View>
                  ) : null}

                  <Text style={styles.premiumTitle}>{plan.name}</Text>
                  <Text style={styles.premiumPrice}>{plan.priceLine}</Text>

                  <View style={styles.featureList}>
                    {plan.features.map((feature) => (
                      <View key={feature} style={styles.featureRow}>
                        <Ionicons
                          name="checkmark-circle-outline"
                          size={16}
                          color="rgba(255,255,255,0.92)"
                        />
                        <Text style={styles.featureText}>{feature}</Text>
                      </View>
                    ))}
                  </View>

                  <View style={styles.premiumButton}>
                    <Text style={styles.premiumButtonText}>{plan.cta}</Text>
                  </View>
                </LinearGradient>
              </TouchableOpacity>
            )
          )}

          <Text style={[styles.footer, { color: colors.textSecondary }]}>
            Los pagos se gestionan con Stripe. Si cancelas, mantendrás tu plan hasta el final del periodo ya cobrado.
          </Text>

          {isOnboarding && (
            <TouchableOpacity
              style={[styles.skipBtn, { borderColor: colors.border }]}
              onPress={() => navigation.navigate('MainTabs', undefined)}
              activeOpacity={0.8}
            >
              <Text style={[styles.skipBtnText, { color: colors.textSecondary }]}>Continuar con Free por ahora</Text>
              <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
            </TouchableOpacity>
          )}
        </ScrollView>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
  content: {
    paddingTop: 50,
    paddingHorizontal: Spacing.xl,
    paddingBottom: Spacing.xl,
    gap: Spacing.base,
  },
  speechWrap: {
    marginTop: -Spacing.xs,
    marginBottom: -15,
  },
  banner: {
    flexDirection: 'row',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
  },
  bannerText: {
    flex: 1,
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  premiumCard: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    overflow: 'hidden',
    gap: Spacing.sm,
  },
  blobTopRight: {
    position: 'absolute',
    top: -36,
    right: -32,
    width: 132,
    height: 132,
    borderRadius: 66,
    backgroundColor: 'rgba(255,255,255,0.14)',
  },
  blobBottomLeft: {
    position: 'absolute',
    left: -28,
    bottom: -28,
    width: 104,
    height: 104,
    borderRadius: 52,
    backgroundColor: 'rgba(0,0,0,0.08)',
  },
  premiumBadge: {
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(255,255,255,0.22)',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 4,
    borderRadius: BorderRadius.sm,
  },
  premiumBadgeText: {
    color: Colors.white,
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xs,
    letterSpacing: 0.6,
  },
  premiumTitle: {
    color: Colors.white,
    fontFamily: FontFamily.black,
    fontSize: FontSize['2xl'],
    marginTop: Spacing.xs,
  },
  premiumPrice: {
    color: 'rgba(255,255,255,0.9)',
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
  },
  featureList: {
    gap: Spacing.xs,
    marginTop: Spacing.xs,
  },
  featureRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  featureText: {
    color: 'rgba(255,255,255,0.92)',
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    flex: 1,
  },
  premiumButton: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    minHeight: 50,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: Spacing.sm,
  },
  premiumButtonText: {
    color: Colors.primary,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  freeCard: {
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    overflow: 'hidden',
    gap: Spacing.base,
  },
  freeHero: {
    padding: Spacing.xl,
    gap: Spacing.sm,
  },
  freeBlobTopRight: {
    position: 'absolute',
    top: -28,
    right: -20,
    width: 110,
    height: 110,
    borderRadius: 55,
    backgroundColor: Colors.primaryOpacity10,
  },
  freeBlobBottomLeft: {
    position: 'absolute',
    left: -16,
    bottom: -24,
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: 'rgba(255,122,0,0.08)',
  },
  freeBadge: {
    alignSelf: 'flex-start',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 4,
    borderRadius: BorderRadius.sm,
  },
  freeBadgeText: {
    color: Colors.primary,
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xs,
  },
  freeTitle: {
    color: Colors.textPrimary,
    fontFamily: FontFamily.bold,
    fontSize: FontSize['2xl'],
  },
  freeCopy: {
    color: Colors.textSecondary,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 21,
  },
  freeButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.xs,
    backgroundColor: Colors.primaryOpacity10,
    borderRadius: BorderRadius.lg,
    minHeight: 50,
    marginTop: Spacing.xs,
  },
  freeButtonText: {
    color: Colors.primary,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  footer: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 18,
    textAlign: 'center',
    paddingHorizontal: Spacing.base,
  },
  skipBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingVertical: 14,
    marginTop: Spacing.xs,
  },
  skipBtnText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
  },
});
