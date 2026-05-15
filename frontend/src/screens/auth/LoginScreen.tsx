import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  Alert,
  Modal,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { useAlert } from '../../context/AlertContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'Login'>;
  route: RouteProp<AuthStackParamList, 'Login'>;
};

export const LoginScreen: React.FC<Props> = ({ navigation, route }) => {
  const { login } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [emailSentModal, setEmailSentModal] = useState(false);
  const [sentEmail, setSentEmail] = useState('');

  // Mostrar modal si venimos de ForgotPassword
  useEffect(() => {
    if (route.params?.showEmailSent) {
      setSentEmail(route.params.emailSent ?? '');
      setEmailSentModal(true);
    }
  }, [route.params]);

  const validate = () => {
    const e: typeof errors = {};
    if (!email.trim()) e.email = 'El correo es obligatorio';
    else if (!/\S+@\S+\.\S+/.test(email)) e.email = 'Correo inválido';
    if (!password) e.password = 'La contraseña es obligatoria';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleLogin = async () => {
    if (!validate()) return;
    setLoading(true);
    try {
      await login(email.trim(), password);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'No se pudo iniciar sesión';
      showAlert({ title: 'Error', message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      {/* Flecha volver */}
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.flex}
      >
        <View style={styles.container}>
          {/* Título */}
          <Text style={[styles.title, { color: colors.text }]}>¡Bienvenido de vuelta!</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            Inicia sesión para continuar practicando.
          </Text>

          {/* Formulario */}
          <WaviiInput
            label="Correo electrónico"
            placeholder="Introduce tu correo"
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
            value={email}
            onChangeText={setEmail}
            error={errors.email}
            leftIcon={<Ionicons name="mail-outline" size={20} color={colors.textSecondary} />}
          />

          <WaviiInput
            label="Contraseña"
            placeholder="Introduce tu contraseña"
            isPassword
            value={password}
            onChangeText={setPassword}
            error={errors.password}
            leftIcon={<Ionicons name="lock-closed-outline" size={20} color={colors.textSecondary} />}
          />

          <TouchableOpacity
            onPress={() => navigation.navigate('ForgotPassword')}
            style={styles.forgotLink}
          >
            <Text style={styles.forgotText}>¿Se te ha olvidado la contraseña?</Text>
          </TouchableOpacity>

          <WaviiButton
            title="Iniciar sesión"
            onPress={handleLogin}
            loading={loading}
          />

          {/* Divisor */}
          <View style={styles.dividerRow}>
            <View style={[styles.dividerLine, { backgroundColor: colors.border }]} />
            <Text style={[styles.dividerText, { color: colors.textSecondary }]}>
              o continuar con
            </Text>
            <View style={[styles.dividerLine, { backgroundColor: colors.border }]} />
          </View>

          {/* Botón Google */}
          <TouchableOpacity
            style={[styles.googleBtn, { borderColor: colors.border }]}
            onPress={() => showAlert({ title: 'Google Login', message: 'Próximamente disponible' })}
            activeOpacity={0.8}
          >
            <View style={styles.googleIconWrapper}>
              <Text style={[styles.googleLetter, { color: '#4285F4' }]}>G</Text>
              <Text style={[styles.googleLetter, { color: '#EA4335' }]}>o</Text>
              <Text style={[styles.googleLetter, { color: '#FBBC05' }]}>o</Text>
              <Text style={[styles.googleLetter, { color: '#4285F4' }]}>g</Text>
              <Text style={[styles.googleLetter, { color: '#34A853' }]}>l</Text>
              <Text style={[styles.googleLetter, { color: '#EA4335' }]}>e</Text>
            </View>
            <Text style={[styles.googleText, { color: '#1A1A1A' }]}>
              Continuar con Google
            </Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>

      {/* Modal — correo de recuperación enviado */}
      <Modal
        visible={emailSentModal}
        transparent
        animationType="slide"
        onRequestClose={() => setEmailSentModal(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setEmailSentModal(false)}
        >
          <View style={styles.modalSheet}>
            <View style={styles.modalHandle} />
            <Text style={[styles.modalTitle, { color: colors.text }]}>Revisa tu correo</Text>
            <Text style={styles.modalBody}>
              Te enviamos un enlace para restablecer tu contraseña a{' '}
              <Text style={[styles.modalEmail, { color: colors.text }]}>{sentEmail}</Text>.{' '}
              ¿No lo ves? Revisa la carpeta de spam.
            </Text>
            <TouchableOpacity
              style={styles.modalBtn}
              onPress={() => setEmailSentModal(false)}
              activeOpacity={0.85}
            >
              <Text style={styles.modalBtnText}>Entendido</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,

  },
  flex: { flex: 1 },

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
    justifyContent: 'center',
  },

  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
    textAlign: 'center',
    marginBottom: Spacing.xs,
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    marginBottom: Spacing.lg,
  },

  forgotLink: {
    alignSelf: 'flex-start',
    marginBottom: Spacing.base,
    marginTop: -4,
  },
  forgotText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    color: Colors.primary,
  },

  dividerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: Spacing.base,
    marginBottom: Spacing.sm,
    gap: Spacing.sm,
  },
  dividerLine: { flex: 1, height: 1 },
  dividerText: {
    fontFamily: FontFamily.medium,
    fontSize: FontSize.xs,
  },

  googleBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    height: 56,
    backgroundColor: Colors.white,
    gap: Spacing.sm,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  googleIconWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  googleLetter: {
    fontSize: 20,
    fontWeight: '700',
    fontFamily: FontFamily.black,
  },
  googleText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },

  /* Modal correo enviado */
  modalOverlay: {
    flex: 1,
    justifyContent: 'flex-end',
    backgroundColor: 'rgba(0,0,0,0.45)',
  },
  modalSheet: {
    backgroundColor: Colors.white,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: Spacing.xl,
    paddingTop: Spacing.md,
    paddingBottom: 40,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: -4 },
    shadowOpacity: 0.12,
    shadowRadius: 16,
    elevation: 20,
  },
  modalHandle: {
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: '#E0E0E0',
    marginBottom: Spacing.lg,
  },
  modalTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xl,
    textAlign: 'center',
    marginBottom: Spacing.md,
  },
  modalBody: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    color: Colors.textSecondary,
    textAlign: 'center',
    lineHeight: 24,
    marginBottom: Spacing.xl,
  },
  modalEmail: {
    fontFamily: FontFamily.bold,
  },
  modalBtn: {
    width: '100%',
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.lg,
    height: 56,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalBtnText: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.md,
    color: Colors.white,
    letterSpacing: 0.5,
  },
});
