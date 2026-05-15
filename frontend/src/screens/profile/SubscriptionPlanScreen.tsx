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
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { usePaymentSheet } from '@stripe/stripe-react-native';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiSpeech } from '../../components/common/WaviiSpeech';
import { WaviiPromoBanner } from '../../components/common/WaviiPromoBanner';
import { useAlert } from '../../context/AlertContext';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import {
  apiCancelSubscription,
  apiChangeSubscription,
  apiConfirmSubscription,
  apiCreateSetupIntent,
  apiGetSubscriptionStatus,
  apiReactivateSubscription,
  SubscriptionStatusResponse,
} from '../../api/subscriptionApi';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { PLAN_BY_ID, SubscriptionPlanId } from './subscriptionPlans';
import { normalizeSubscription } from '../../utils/subscription';

type NavigationProp = NativeStackNavigationProp<AppStackParamList, 'SubscriptionPlan'>;
type ScreenRouteProp = RouteProp<AppStackParamList, 'SubscriptionPlan'>;

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

export const SubscriptionPlanScreen = () => {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<ScreenRouteProp>();
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const { token, user, updateUser } = useAuth();
  const { initPaymentSheet, presentPaymentSheet } = usePaymentSheet();

  const planId = route.params.planId;
  const plan = PLAN_BY_ID[planId];

  const [status, setStatus] = useState<SubscriptionStatusResponse | null>(null);
  const [loadingStatus, setLoadingStatus] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const currentPlan = normalizeSubscription(status?.subscription ?? user?.subscription ?? 'free') as SubscriptionPlanId;
  const cancelAtPeriodEnd = status?.cancelAtPeriodEnd ?? user?.subscriptionCancelAtPeriodEnd ?? false;
  const currentPeriodEnd = status?.currentPeriodEnd ?? user?.subscriptionCurrentPeriodEnd;
  const subscriptionStatus = status?.subscriptionStatus ?? '';
  const hasActiveStripeSubscription =
    !!status?.stripeSubscriptionId && (subscriptionStatus === 'active' || subscriptionStatus === 'trialing');
  const scholarPromoEligible = currentPlan === 'plus' && subscriptionStatus !== 'trialing';
  const isCurrentPlan = currentPlan === planId;
  const shouldSuggestVerification = planId === 'education' && !user?.teacherVerified;
  const isFreePlan = plan.outlined;

  const loadStatus = useCallback(async () => {
    if (!token) {
      setLoadingStatus(false);
      return;
    }

    setLoadingStatus(true);
    try {
      const response = await apiGetSubscriptionStatus(token);
      const normalizedSubscription = normalizeSubscription(response.subscription);
      setStatus({ ...response, subscription: normalizedSubscription });
      updateUser({
        subscription: normalizedSubscription,
        subscriptionCancelAtPeriodEnd: response.cancelAtPeriodEnd,
        subscriptionCurrentPeriodEnd: response.currentPeriodEnd || undefined,
        trialUsed: response.trialUsed,
        deletionScheduledAt: response.deletionScheduledAt || undefined,
      });
      return { ...response, subscription: normalizedSubscription };
    } catch (err: any) {
      showAlert({
        title: 'No se pudo cargar tu plan',
        message: err?.response?.data?.message ?? 'Vuelve a intentarlo en unos segundos.',
      });
      return null;
    } finally {
      setLoadingStatus(false);
    }
  }, [showAlert, token, updateUser]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  const planHeadline = useMemo(() => {
    if (planId === 'education' && scholarPromoEligible && !isCurrentPlan) {
      return 'Primer mes a 2,99 € y luego 7,99 €/mes.';
    }

    if (planId === 'free' && currentPlan !== 'free') {
      return 'Al pasar a Free no se te cobrará el próximo ciclo y mantendrás tu plan actual hasta la fecha final.';
    }

    if (planId === 'plus') {
      return 'Incluye prueba gratis de 14 días la primera vez que lo actives.';
    }

    return plan.priceLine;
  }, [currentPlan, isCurrentPlan, plan.priceLine, planId, scholarPromoEligible]);

  const handlePostActivation = useCallback(
    (targetPlanId: 'plus' | 'education') => {
      if (targetPlanId === 'education' && !user?.teacherVerified) {
        showAlert({
          title: 'Scholar activado',
          message: 'Ya puedes enseñar con Scholar. Si quieres conseguir la insignia de profesor certificado, sube ahora tu documento para revisarlo en Odoo.',
          buttons: [
            { text: 'Más tarde', onPress: () => navigation.goBack() },
            { text: 'Subir documento', onPress: () => navigation.navigate('TeacherVerification') },
          ],
        });
        return;
      }

      showAlert({
        title: 'Plan activado',
        message: `Bienvenido a ${PLAN_BY_ID[targetPlanId].name}.`,
        buttons: [{ text: 'Aceptar', onPress: () => navigation.goBack() }],
      });
    },
    [navigation, showAlert, user?.teacherVerified],
  );

  const startPaymentSheetFlow = useCallback(
    async (targetPlanId: 'plus' | 'education') => {
      if (!token) return;

      const apiPlan = targetPlanId === 'education' ? 'scholar' : 'plus';
      setActionLoading(true);

      try {
        const setupIntent = await apiCreateSetupIntent(apiPlan, token);

        if (setupIntent.devMode) {
          await apiConfirmSubscription(apiPlan, 'dev_mock', token);
          const refreshedStatus = await loadStatus();
          if (normalizeSubscription(refreshedStatus?.subscription ?? targetPlanId) !== targetPlanId) {
            showAlert({
              title: 'Suscripcion pendiente',
              message: 'El pago se ha procesado, pero el plan aun no aparece actualizado. Vuelve a abrir esta pantalla en unos segundos.',
            });
            return;
          }
          handlePostActivation(targetPlanId);
          return;
        }

        const { error: initError } = await initPaymentSheet({
          customerId: setupIntent.customerId,
          customerEphemeralKeySecret: setupIntent.ephemeralKey,
          setupIntentClientSecret: setupIntent.setupIntentClientSecret,
          merchantDisplayName: 'Wavii',
          appearance: { colors: { primary: Colors.primary } },
          returnURL: 'wavii://stripe-redirect',
        });

        if (initError) {
          showAlert({ title: 'Error', message: initError.message });
          return;
        }

        const { error: presentError } = await presentPaymentSheet();
        if (presentError) {
          if (presentError.code !== 'Canceled') {
            showAlert({ title: 'Error de pago', message: presentError.message });
          }
          return;
        }

        await apiConfirmSubscription(apiPlan, setupIntent.setupIntentId, token);
        const refreshedStatus = await loadStatus();
        if (normalizeSubscription(refreshedStatus?.subscription ?? targetPlanId) !== targetPlanId) {
          showAlert({
            title: 'Suscripcion pendiente',
            message: 'El pago se ha procesado, pero el plan aun no aparece actualizado. Vuelve a abrir esta pantalla en unos segundos.',
          });
          return;
        }
        handlePostActivation(targetPlanId);
      } catch (err: any) {
        showAlert({
          title: 'Error',
          message: err?.response?.data?.message ?? 'No se pudo activar el plan.',
        });
      } finally {
        setActionLoading(false);
      }
    },
    [handlePostActivation, initPaymentSheet, loadStatus, presentPaymentSheet, showAlert, token, updateUser]
  );

  const handleChangePlan = useCallback(async () => {
    if (!token || planId === 'free') return;

    const apiPlan = planId === 'education' ? 'scholar' : 'plus';
    setActionLoading(true);

    try {
      const result = await apiChangeSubscription(apiPlan, token);

      if (result.needsPaymentSheet) {
        setActionLoading(false);
        await startPaymentSheetFlow(planId);
        return;
      }

      updateUser({
        subscription: planId,
        subscriptionCancelAtPeriodEnd: false,
        subscriptionCurrentPeriodEnd: result.currentPeriodEnd || undefined,
      });
      const refreshedStatus = await loadStatus();
      if (normalizeSubscription(refreshedStatus?.subscription ?? planId) !== planId) {
        showAlert({
          title: 'Cambio pendiente',
          message: 'Stripe ha aceptado el cambio, pero el plan aun no aparece actualizado. Vuelve a revisar esta pantalla en unos segundos.',
        });
        setActionLoading(false);
        return;
      }

      const message =
        planId === 'education' && result.promoApplied
          ? 'Scholar costará 2,99 € el primer mes y 7,99 € a partir del siguiente.'
          : `El cambio a ${plan.name} se ha realizado correctamente.`;

      showAlert({
        title: 'Plan cambiado',
        message,
        buttons: [{ text: 'Aceptar', onPress: () => navigation.goBack() }],
      });
    } catch (err: any) {
      if (err?.response?.status === 402) {
        setActionLoading(false);
        await startPaymentSheetFlow(planId);
        return;
      }

      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo cambiar el plan.',
      });
      setActionLoading(false);
    }
  }, [loadStatus, navigation, plan.name, planId, showAlert, startPaymentSheetFlow, token, updateUser]);

  const confirmCancel = useCallback(async () => {
    if (!token) return;

    setActionLoading(true);
    try {
      const result = await apiCancelSubscription(token);
      updateUser({
        subscriptionCancelAtPeriodEnd: true,
        subscriptionCurrentPeriodEnd: result.currentPeriodEnd || undefined,
      });
      await loadStatus();
      showAlert({
        title: 'Suscripción cancelada',
        message: `Tu plan seguirá activo hasta el ${formatDate(result.currentPeriodEnd)} y después pasarás a Free.`,
        buttons: [{ text: 'Aceptar', onPress: () => navigation.goBack() }],
      });
    } catch (err: any) {
      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo cancelar la suscripción.',
      });
    } finally {
      setActionLoading(false);
    }
  }, [loadStatus, navigation, showAlert, token, updateUser]);

  const handleReactivate = useCallback(async () => {
    if (!token) return;

    setActionLoading(true);
    try {
      await apiReactivateSubscription(token);
      updateUser({ subscriptionCancelAtPeriodEnd: false });
      await loadStatus();
      showAlert({
        title: 'Suscripción reactivada',
        message: 'Tu suscripción seguirá renovándose con normalidad.',
        buttons: [{ text: 'Aceptar', onPress: () => navigation.goBack() }],
      });
    } catch (err: any) {
      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo reactivar la suscripción.',
      });
    } finally {
      setActionLoading(false);
    }
  }, [loadStatus, navigation, showAlert, token, updateUser]);

  const handlePrimaryAction = useCallback(() => {
    if (!token) {
      showAlert({
        title: 'Inicia sesión de nuevo',
        message: 'Necesitamos una sesión válida para gestionar tu suscripción.',
      });
      return;
    }

    if (planId === 'free') {
      if (currentPlan === 'free') {
        navigation.goBack();
        return;
      }

      showAlert({
        title: 'Pasar a Free',
        message: `Mantendrás tu plan actual hasta el ${formatDate(currentPeriodEnd || new Date().toISOString())} y no se renovará el próximo ciclo.`,
        buttons: [
          { text: 'Volver', style: 'cancel' },
          { text: 'Confirmar', style: 'destructive', onPress: confirmCancel, delaySeconds: 10 },
        ],
      });
      return;
    }

    if (isCurrentPlan) {
      if (planId === 'plus' || planId === 'education') {
        if (cancelAtPeriodEnd) {
          handleReactivate();
        } else {
          showAlert({
            title: 'Cancelar suscripción',
            message: `Tu plan seguirá activo hasta el ${formatDate(
              currentPeriodEnd || new Date().toISOString()
            )} y después pasarás a Free.`,
            buttons: [
              { text: 'Volver', style: 'cancel' },
              { text: 'Cancelar suscripción', style: 'destructive', onPress: confirmCancel, delaySeconds: 10 },
            ],
          });
        }
      }
      return;
    }

    if (hasActiveStripeSubscription) {
      showAlert({
        title: 'Confirmar cambio de plan',
        message: `Vas a cambiar a ${plan.name}. Revisa bien el cambio antes de confirmarlo.`,
        buttons: [
          { text: 'Cancelar', style: 'cancel' },
          { text: 'Confirmar', style: 'destructive', onPress: handleChangePlan, delaySeconds: 10 },
        ],
      });
      return;
    }

    showAlert({
      title: 'Activar plan',
      message: `Vas a activar ${plan.name}. Si continúas, se abrirá el flujo de pago.`,
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Continuar', style: 'destructive', onPress: () => startPaymentSheetFlow(planId), delaySeconds: 10 },
      ],
    });
  }, [
    cancelAtPeriodEnd,
    confirmCancel,
    currentPeriodEnd,
    currentPlan,
    handleChangePlan,
    handleReactivate,
    hasActiveStripeSubscription,
    isCurrentPlan,
    navigation,
    planId,
    showAlert,
    startPaymentSheetFlow,
    token,
  ]);

  const primaryButtonLabel = useMemo(() => {
    if (planId === 'free') {
      return currentPlan === 'free' ? 'Seguir con Free' : 'Programar cambio a Free';
    }

    if (isCurrentPlan) {
      return cancelAtPeriodEnd ? 'Reactivar suscripción' : 'Cancelar suscripción';
    }

    return currentPlan === 'free' ? `Elegir ${plan.name}` : `Cambiar a ${plan.name}`;
  }, [cancelAtPeriodEnd, currentPlan, isCurrentPlan, plan.name, planId]);

  if (loadingStatus) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
        <TouchableOpacity
          style={styles.backBtn}
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={28} color={colors.text} />
        </TouchableOpacity>
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>

      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.speechWrap}>
          <WaviiSpeech text={`Así funciona ${plan.name}`} />
        </View>

        <LinearGradient
          colors={isFreePlan ? [Colors.backgroundLight, Colors.white] : plan.gradient}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={[
            styles.heroCard,
            isFreePlan && styles.freeHeroCard,
          ]}
        >
          <View style={styles.blobTopRight} />
          <View style={styles.blobBottomLeft} />

          <Text style={[styles.heroTitle, isFreePlan && styles.freeHeroTitle]}>{plan.name}</Text>
          <Text style={[styles.heroPrice, isFreePlan && styles.freeHeroPrice]}>{planHeadline}</Text>

          <View style={styles.heroFeatures}>
            {plan.features.map((feature) => (
              <View key={feature} style={styles.featureRow}>
                <Ionicons
                  name="checkmark-circle-outline"
                  size={17}
                  color={isFreePlan ? Colors.primary : 'rgba(255,255,255,0.94)'}
                />
                <Text style={[styles.featureText, isFreePlan && styles.freeFeatureText]}>{feature}</Text>
              </View>
            ))}
          </View>
        </LinearGradient>

        {shouldSuggestVerification ? (
          <WaviiPromoBanner
            title="Consigue tu insignia docente"
            body="Con Scholar ya puedes enseñar y publicar en el tablón. Si subes tu certificado, Odoo podrá revisarlo y darte la insignia de profesor certificado."
            icon="shield-checkmark-outline"
            tone="education"
            ctaLabel="Subir documento"
            onPress={() => navigation.navigate('TeacherVerification')}
          />
        ) : null}

        <View style={[styles.detailCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          {isCurrentPlan ? (
            <View style={[styles.statusChip, { backgroundColor: Colors.primaryOpacity10 }]}>
              <Text style={styles.statusChipText}>
                {cancelAtPeriodEnd ? 'Se cancelará al final del periodo' : 'Es tu plan actual'}
              </Text>
            </View>
          ) : null}

          {planId === 'education' && scholarPromoEligible && !isCurrentPlan ? (
            <View style={[styles.infoBox, { backgroundColor: Colors.successLight, borderColor: Colors.success }]}>
              <Ionicons name="sparkles-outline" size={18} color={Colors.success} />
              <Text style={[styles.infoText, { color: Colors.success }]}>
                Como vienes desde Plus fuera del trial, Scholar te costará 2,99 € el primer mes.
              </Text>
            </View>
          ) : null}

          {cancelAtPeriodEnd && currentPeriodEnd ? (
            <View style={[styles.infoBox, { backgroundColor: Colors.warningLight, borderColor: Colors.warning }]}>
              <Ionicons name="time-outline" size={18} color={Colors.warning} />
              <Text style={[styles.infoText, { color: Colors.warning }]}>
                Seguirás disfrutando del plan hasta el {formatDate(currentPeriodEnd)}.
              </Text>
            </View>
          ) : null}

          <Text style={[styles.sectionTitle, { color: colors.text }]}>Qué ocurre al elegir este plan</Text>
          <Text style={[styles.bodyText, { color: colors.textSecondary }]}>
            {planId === 'free'
              ? 'Tu suscripción actual dejará de renovarse y el cambio efectivo a Free se hará al final del periodo ya cobrado.'
              : isCurrentPlan
              ? cancelAtPeriodEnd
                ? 'Puedes reactivar la renovación automática en cualquier momento antes de la fecha final.'
                : 'Puedes mantener tu plan o cancelarlo para que no se cobre el próximo ciclo.'
              : currentPlan === 'free'
              ? 'Se abrirá el flujo de Stripe para confirmar el método de pago y activar la suscripción.'
              : 'Intentaremos cambiar tu plan directamente. Si Stripe necesita confirmar pago, abriremos el flujo de pago.'}
          </Text>

          <WaviiButton
            title={primaryButtonLabel}
            onPress={handlePrimaryAction}
            loading={actionLoading}
            variant={planId === 'free' && currentPlan !== 'free' ? 'danger' : 'primary'}
            style={styles.primaryButton}
          />

          <WaviiButton
            title="Volver a planes"
            onPress={() => navigation.goBack()}
            variant="ghost"
            style={styles.secondaryButton}
            textStyle={{ color: Colors.primary }}
          />
        </View>
      </ScrollView>
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
  heroCard: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    overflow: 'hidden',
    gap: Spacing.sm,
  },
  freeHeroCard: {
    borderWidth: 1.5,
    borderColor: Colors.primaryOpacity20,
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
  heroTitle: {
    color: Colors.white,
    fontFamily: FontFamily.black,
    fontSize: FontSize['2xl'],
  },
  freeHeroTitle: {
    color: Colors.textPrimary,
  },
  heroPrice: {
    color: 'rgba(255,255,255,0.92)',
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
    lineHeight: 24,
  },
  freeHeroPrice: {
    color: Colors.textSecondary,
  },
  heroFeatures: {
    gap: Spacing.xs,
    marginTop: Spacing.xs,
  },
  featureRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  featureText: {
    color: 'rgba(255,255,255,0.94)',
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    flex: 1,
  },
  freeFeatureText: {
    color: Colors.textPrimary,
  },
  detailCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    gap: Spacing.base,
  },
  statusChip: {
    alignSelf: 'flex-start',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 4,
    borderRadius: BorderRadius.sm,
  },
  statusChipText: {
    color: Colors.primary,
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xs,
  },
  infoBox: {
    flexDirection: 'row',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
  },
  infoText: {
    flex: 1,
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  sectionTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  bodyText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 22,
  },
  primaryButton: {
    marginTop: Spacing.sm,
  },
  secondaryButton: {
    minHeight: 48,
  },
});
