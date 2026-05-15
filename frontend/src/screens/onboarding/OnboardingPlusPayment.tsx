import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { usePaymentSheet } from '@stripe/stripe-react-native';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { apiCreateSetupIntent, apiConfirmSubscription } from '../../api/subscriptionApi';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { useAlert } from '../../context/AlertContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingPlusPayment'>;
};

const BENEFITS = [
  'Descarga offline',
  'Sin anuncios',
  'Estadísticas avanzadas',
  'Acceso anticipado a nuevas funciones',
];

export const OnboardingPlusPayment: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const { pendingToken, confirmEmailVerified } = useAuth();
  const insets = useSafeAreaInsets();
  const { initPaymentSheet, presentPaymentSheet } = usePaymentSheet();

  const [loading, setLoading] = useState(false);

  const handleActivate = async () => {
    setLoading(true);
    try {
      // Paso 1: obtener SetupIntent + EphemeralKey del backend
      const siData = await apiCreateSetupIntent('plus', pendingToken ?? '');

      if (siData.devMode) {
        // Modo dev: Stripe no configurado, simular el flujo
        await apiConfirmSubscription('plus', 'dev_mock', pendingToken ?? '');
        await confirmEmailVerified();
        return;
      }

      // Paso 2: inicializar el Payment Sheet oficial de Stripe
      const { error: initError } = await initPaymentSheet({
        customerId: siData.customerId,
        customerEphemeralKeySecret: siData.ephemeralKey,
        setupIntentClientSecret: siData.setupIntentClientSecret,
        merchantDisplayName: 'Wavii',
        appearance: {
          colors: {
            primary: Colors.primary,
          },
        },
        returnURL: 'wavii://stripe-redirect',
      });

      if (initError) {
        showAlert({ title: 'Error', message: initError.message });
        return;
      }

      // Paso 3: mostrar el Payment Sheet nativo de Stripe
      const { error: presentError } = await presentPaymentSheet();

      if (presentError) {
        if (presentError.code !== 'Canceled') {
          showAlert({ title: 'Error de pago', message: presentError.message });
        }
        return;
      }

      // Paso 4: el sheet se completó — confirmar suscripción en backend
      await apiConfirmSubscription('plus', siData.setupIntentId, pendingToken ?? '');
      await confirmEmailVerified();

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'No se pudo activar la prueba gratuita.';
      showAlert({ title: 'Error', message: msg });
    } finally {
      setLoading(false);
    }
  };

  const handleSkip = async () => {
    try {
      await confirmEmailVerified();
    } catch {
      // ignorar
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={Colors.textPrimary} />
      </TouchableOpacity>

      <ScrollView
        contentContainerStyle={styles.container}
        showsVerticalScrollIndicator={false}
      >
        {/* Header */}
        <View style={styles.headerSection}>
          <View style={styles.trialBadge}>
            <Text style={styles.trialBadgeText}>14 DÍAS GRATIS</Text>
          </View>
          <Text style={[styles.title, { color: colors.text }]}>Wavii Plus</Text>
          <View style={styles.priceRow}>
            <Text style={styles.price}>4,99 €</Text>
            <Text style={[styles.priceLabel, { color: colors.textSecondary }]}>/mes tras la prueba</Text>
          </View>
          <Text style={[styles.description, { color: colors.textSecondary }]}>
            Introduce tu tarjeta ahora — no se te cobrará hasta que finalice el período de prueba.
          </Text>
        </View>

        {/* Beneficios */}
        <View style={styles.benefitsList}>
          {BENEFITS.map((b) => (
            <View key={b} style={styles.benefitRow}>
              <Ionicons name="checkmark-circle-outline" size={18} color={Colors.primary} />
              <Text style={[styles.benefitText, { color: colors.text }]}>{b}</Text>
            </View>
          ))}
        </View>

        {/* Nota */}
        <View style={styles.secureRow}>
          <Ionicons name="lock-closed-outline" size={13} color={colors.textSecondary} style={{ marginTop: 2 }} />
          <Text style={[styles.secureNote, { color: colors.textSecondary }]}>
            No se realizará ningún cargo durante los 14 días de prueba
          </Text>
        </View>

        <Text style={[styles.noRefundNote, { color: colors.textSecondary }]}>
          Al suscribirte aceptas que los pagos no son reembolsables.
        </Text>
      </ScrollView>

      {/* Botones fijos abajo */}
      <View style={[styles.footer, { paddingBottom: insets.bottom + Spacing.sm }]}>
        <WaviiButton
          title="Empezar prueba gratuita"
          onPress={handleActivate}
          loading={loading}
        />
        <TouchableOpacity
          style={styles.skipLink}
          onPress={handleSkip}
          activeOpacity={0.7}
        >
          <Text style={[styles.skipText, { color: colors.textSecondary }]}>
            Continuar sin Plus (plan gratuito)
          </Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  container: {
    flexGrow: 1,
    paddingHorizontal: Spacing.xl,
    paddingTop: 90,
    paddingBottom: Spacing.base,
    gap: Spacing.base,
  },
  headerSection: { gap: Spacing.xs },
  trialBadge: {
    alignSelf: 'flex-start',
    backgroundColor: Colors.success,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 4,
    borderRadius: BorderRadius.sm,
    marginBottom: Spacing.xs,
  },
  trialBadgeText: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xs,
    color: Colors.white,
    letterSpacing: 0.5,
  },
  title: { fontFamily: FontFamily.black, fontSize: FontSize['2xl'] },
  priceRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 4 },
  price: { fontFamily: FontFamily.black, fontSize: 40, color: Colors.primary },
  priceLabel: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm, marginBottom: 6 },
  description: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  benefitsList: { gap: Spacing.xs },
  benefitRow: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm },
  benefitText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, flex: 1 },
  secureRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 6,
    justifyContent: 'center',
    marginTop: Spacing.sm,
    paddingHorizontal: Spacing.xl,
  },
  secureNote: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, flexShrink: 1 },
  noRefundNote: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
    paddingHorizontal: Spacing.xl,
    marginTop: Spacing.xs,
  },
  footer: {
    paddingHorizontal: Spacing.xl,
    paddingTop: Spacing.sm,
    gap: Spacing.xs,

  },
  skipLink: { alignItems: 'center', paddingVertical: Spacing.sm },
  skipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    textDecorationLine: 'underline',
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});
