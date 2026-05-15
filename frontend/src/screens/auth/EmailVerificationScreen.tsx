import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Image,
  ActivityIndicator,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { apiResendVerification, apiCheckEmailVerified, getApiErrorMessage } from '../../api/authApi';
import { WaviiButton } from '../../components/common/WaviiButton';
import { Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { useAlert } from '../../context/AlertContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'EmailVerification'>;
  route: RouteProp<AuthStackParamList, 'EmailVerification'>;
};

const WAVII_EMAIL = require('../../../assets/wavii/wavii_enviar_email.png');
const RESEND_COOLDOWN = 45;
const POLL_INTERVAL = 4000; // comprueba cada 4 segundos

export const EmailVerificationScreen: React.FC<Props> = ({ navigation, route }) => {
  const { confirmEmailVerified } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const { email, pendingPlan, teacherType } = route.params;

  const [countdown, setCountdown] = useState(RESEND_COOLDOWN);
  const [loading, setLoading] = useState(false);
  const [verified, setVerified] = useState(false);

  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const activatingRef = useRef(false);

  // ── Countdown reenvío ──────────────────────────────────────
  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  // ── Polling automático ─────────────────────────────────────
  useEffect(() => {
    const poll = async () => {
      if (activatingRef.current) return;
      try {
        const isVerified = await apiCheckEmailVerified(email);
        if (isVerified) {
          activatingRef.current = true;
          stopPolling();
          setVerified(true);
          // Pequeña pausa para mostrar el tick ✅ antes de continuar
          setTimeout(async () => {
            if (pendingPlan === 'plus') {
              navigation.replace('OnboardingPlusPayment');
            } else if (pendingPlan === 'scholar') {
              navigation.replace('OnboardingScholarPayment', {
                teacherType: teacherType ?? 'particular',
              });
            } else {
              try {
                await confirmEmailVerified();
              } catch {
                await confirmEmailVerified();
              }
            }
          }, 1200);
        }
      } catch {
        // Silencioso — backend no disponible o endpoint no existe aún
      }
    };

    pollingRef.current = setInterval(poll, POLL_INTERVAL);
    poll(); // primera comprobación inmediata

    return () => stopPolling();
  }, [email]);

  const stopPolling = () => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current);
      pollingRef.current = null;
    }
  };

  // ── Reenviar correo ────────────────────────────────────────
  const handleResend = async () => {
    if (countdown > 0) return;
    setLoading(true);
    try {
      await apiResendVerification(email);
      setCountdown(RESEND_COOLDOWN);
      showAlert({ title: 'Enviado', message: 'Hemos reenviado el correo de verificación.' });
    } catch (err) {
      showAlert({ title: 'Error', message: getApiErrorMessage(err, 'No se pudo reenviar el correo') });
    } finally {
      setLoading(false);
    }
  };

  const countdownStr =
    `${String(Math.floor(countdown / 60)).padStart(2, '0')}:${String(countdown % 60).padStart(2, '0')}`;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      {/* Flecha volver */}
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => { stopPolling(); navigation.goBack(); }}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>

      <View style={styles.container}>
        {/* Mascota */}
        <View style={styles.mascotSection}>
          <Image source={WAVII_EMAIL} style={styles.mascotImage} resizeMode="contain" />
        </View>

        {verified ? (
          /* ── Estado: verificado ── */
          <View style={styles.verifiedContainer}>
            <Text style={styles.verifiedIcon}>✅</Text>
            <Text style={[styles.title, { color: colors.text }]}>¡Email verificado!</Text>
            <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
              Entrando a Wavii...
            </Text>
            <ActivityIndicator color={Colors.primary} style={{ marginTop: Spacing.base }} />
          </View>
        ) : (
          /* ── Estado: esperando ── */
          <>
            <Text style={[styles.title, { color: colors.text }]}>¡Ya casi está listo!</Text>
            <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
              Te hemos enviado un enlace de verificación{'\n'}a tu correo electrónico:
            </Text>
            <Text style={styles.email}>{email}</Text>

            {/* Indicador de espera */}
            <View style={styles.waitingRow}>
              <ActivityIndicator size="small" color={Colors.primary} />
              <Text style={[styles.waitingText, { color: colors.textSecondary }]}>
                Esperando verificación...
              </Text>
            </View>

            {/* Botón reenviar */}
            <View style={styles.buttonsContainer}>
              <WaviiButton
                title={loading ? 'Reenviando...' : 'Reenviar correo'}
                onPress={handleResend}
                loading={loading}
                disabled={countdown > 0}
                variant={countdown > 0 ? 'outline' : 'primary'}
              />
              {countdown > 0 && (
                <Text style={[styles.countdownText, { color: colors.textSecondary }]}>
                  Puedes reenviar en{' '}
                  <Text style={styles.countdownNum}>{countdownStr}</Text>
                </Text>
              )}
            </View>
          </>
        )}
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,

  },

  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },

  container: {
    flex: 1,
    paddingHorizontal: Spacing.xl,
    alignItems: 'center',
    justifyContent: 'center',
  },

  mascotSection: {
    alignItems: 'center',
    marginBottom: -40,
  },
  mascotImage: {
    width: 320,
    height: 270,
  },

  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xl,
    textAlign: 'center',
    marginBottom: Spacing.xs,
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 20,
  },
  email: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    color: Colors.primary,
    textAlign: 'center',
    marginTop: Spacing.xs,
    marginBottom: Spacing.lg,
  },

  waitingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    marginBottom: Spacing.xl,
  },
  waitingText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },

  buttonsContainer: {
    width: '100%',
    gap: Spacing.sm,
  },

  countdownText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
  },
  countdownNum: {
    fontFamily: FontFamily.bold,
    color: Colors.primary,
  },

  // Estado verificado
  verifiedContainer: {
    alignItems: 'center',
    gap: Spacing.sm,
  },
  verifiedIcon: {
    fontSize: 56,
  },
});
