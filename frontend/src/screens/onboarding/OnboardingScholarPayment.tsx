import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
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
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingScholarPayment'>;
  route: RouteProp<AuthStackParamList, 'OnboardingScholarPayment'>;
};

const TOTAL_STEPS = 7;
const CURRENT_STEP = 5;

const ProgressDots = () => (
  <View style={styles.dotsRow}>
    {Array.from({ length: TOTAL_STEPS }).map((_, i) => (
      <View
        key={i}
        style={[styles.dot, i + 1 === CURRENT_STEP ? styles.dotActive : styles.dotInactive]}
      />
    ))}
  </View>
);

const BENEFITS = [
  'Tablón de Anuncios',
  'Aulas Virtuales',
  'Seguimiento de alumnos',
  'Facturación con Odoo',
];

export const OnboardingScholarPayment: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const { pendingToken } = useAuth();
  const insets = useSafeAreaInsets();
  const { initPaymentSheet, presentPaymentSheet } = usePaymentSheet();

  const [loading, setLoading] = useState(false);

  const afterPayment = async () => {
    navigation.navigate('OnboardingCertifiedVerification');
  };

  const handleActivate = async () => {
    setLoading(true);
    try {
      // Paso 1: obtener SetupIntent + EphemeralKey del backend
      const siData = await apiCreateSetupIntent('scholar', pendingToken ?? '');

      if (siData.devMode) {
        // Modo dev: Stripe no configurado, simular el flujo
        await apiConfirmSubscription('scholar', 'dev_mock', pendingToken ?? '');
        await afterPayment();
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
      await apiConfirmSubscription('scholar', siData.setupIntentId, pendingToken ?? '');
      await afterPayment();

    } catch (err) {
      const msg = err instanceof Error ? err.message : 'No se pudo activar la suscripción.';
      showAlert({ title: 'Error', message: msg });
    } finally {
      setLoading(false);
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
      <ProgressDots />

      <ScrollView
        contentContainerStyle={styles.container}
        showsVerticalScrollIndicator={false}
      >
        {/* Header */}
        <View style={styles.headerSection}>
          <Text style={[styles.title, { color: colors.text }]}>Activa Wavii Scholar</Text>
          <View style={styles.priceRow}>
            <Text style={styles.price}>7,99 €</Text>
            <Text style={[styles.priceLabel, { color: colors.textSecondary }]}>/mes</Text>
          </View>
          <Text style={[styles.description, { color: colors.textSecondary }]}>
            Acceso completo a todas las herramientas de gestión para profesores
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

        {/* Nota seguridad */}
        <View style={styles.secureRow}>
          <Ionicons name="lock-closed-outline" size={13} color={colors.textSecondary} style={{ marginTop: 2 }} />
          <Text style={[styles.secureNote, { color: colors.textSecondary }]}>
            Pago seguro procesado por Stripe
          </Text>
        </View>

        <Text style={[styles.noRefundNote, { color: colors.textSecondary }]}>
          Al suscribirte aceptas que los pagos no son reembolsables.
        </Text>
      </ScrollView>

      {/* Botones fijos abajo */}
      <View style={[styles.footer, { paddingBottom: insets.bottom + Spacing.sm }]}>
        <WaviiButton
          title="Activar Scholar — 7,99 €/mes"
          onPress={handleActivate}
          loading={loading}
        />
        <TouchableOpacity
          style={styles.cancelLink}
          onPress={() => navigation.goBack()}
          activeOpacity={0.7}
        >
          <Text style={[styles.cancelText, { color: colors.textSecondary }]}>
            Cancelar y volver
          </Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  dotsRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
    paddingTop: Spacing.base,
    paddingBottom: Spacing.sm,
  },
  dot: { height: 8, borderRadius: 4 },
  dotActive: { width: 24, backgroundColor: Colors.primary },
  dotInactive: { width: 8, backgroundColor: Colors.border },
  container: {
    flexGrow: 1,
    paddingHorizontal: Spacing.xl,
    paddingTop: 90,
    paddingBottom: Spacing.base,
    gap: Spacing.base,
  },
  headerSection: { gap: Spacing.xs, alignItems: 'flex-start' },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize['2xl'] },
  priceRow: { flexDirection: 'row', alignItems: 'flex-end', gap: 4 },
  price: { fontFamily: FontFamily.black, fontSize: 40, color: Colors.primary },
  priceLabel: { fontFamily: FontFamily.semiBold, fontSize: FontSize.base, marginBottom: 6 },
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
  cancelLink: { alignItems: 'center', paddingVertical: Spacing.sm },
  cancelText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});
