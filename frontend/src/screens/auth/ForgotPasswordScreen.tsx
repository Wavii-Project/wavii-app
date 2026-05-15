import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  Image,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { useTheme } from '../../context/ThemeContext';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';
import { Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { apiForgotPassword, getApiErrorMessage } from '../../api/authApi';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'ForgotPassword'>;
};

const WAVII_FORGOT = require('../../../assets/wavii/wavii_olvido_contrasena.png');

export const ForgotPasswordScreen: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const [email, setEmail] = useState('');
  const [emailError, setEmailError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSend = async () => {
    if (!email.trim()) { setEmailError('El correo es obligatorio'); return; }
    if (!/\S+@\S+\.\S+/.test(email)) { setEmailError('Correo inválido'); return; }
    setEmailError('');
    setLoading(true);
    try {
      await apiForgotPassword(email.trim());
    } catch (err) {
      // El backend siempre devuelve 200 (seguridad), el error es de red
      console.warn('[ForgotPassword] Error de red:', getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
    // Navegar a Login y mostrar el modal allí
    navigation.navigate('Login', { showEmailSent: true, emailSent: email.trim() });
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      {/* Flecha volver — misma posición que LoginScreen */}
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
          <Text style={[styles.title, { color: colors.text }]}>¿Olvidaste la contraseña?</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            No te preocupes, podemos ayudarte.
          </Text>

          <Image source={WAVII_FORGOT} style={styles.mascot} resizeMode="contain" />

          <Text style={[styles.description, { color: colors.textSecondary }]}>
            Introduce el correo asociado a tu cuenta y te enviaremos un enlace para restablecer tu contraseña.
          </Text>

          <WaviiInput
            label="Correo electrónico"
            placeholder="Introduce tu correo electrónico"
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
            value={email}
            onChangeText={setEmail}
            error={emailError}
            leftIcon={<Ionicons name="mail-outline" size={20} color={colors.textSecondary} />}
          />

          <WaviiButton
            title="Continuar"
            onPress={handleSend}
            loading={loading}
          />
        </View>
      </KeyboardAvoidingView>
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
    marginBottom: 2,
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    marginBottom: -30,
  },
  mascot: {
    width: 320,
    height: 280,
    alignSelf: 'center',
    marginBottom: -40,
  },
  description: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: Spacing.base,
    paddingHorizontal: Spacing.sm,
  },
});
