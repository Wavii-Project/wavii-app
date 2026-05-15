import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Image,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { useTheme } from '../../context/ThemeContext';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';
import { Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { useAlert } from '../../context/AlertContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'ResetPassword'>;
  route: RouteProp<AuthStackParamList, 'ResetPassword'>;
};

const WAVII_RESET = require('../../../assets/wavii/wavii_restablecer_contrasena.png');

const getStrength = (pw: string) => {
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  const map = [
    { label: 'Débil',      color: Colors.error,   bars: 1 },
    { label: 'Media',      color: Colors.caution, bars: 2 },
    { label: 'Fuerte',     color: Colors.success, bars: 3 },
    { label: 'Muy fuerte', color: Colors.success, bars: 4 },
  ];
  return map[Math.min(score - 1, 3)] ?? map[0];
};

export const ResetPasswordScreen: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const [password, setPassword] = useState('');
  const [repeatPassword, setRepeatPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  const strength = password.length > 0 ? getStrength(password) : null;

  const handleUpdate = async () => {
    const e: Record<string, string> = {};
    if (!password) e.password = 'La contraseña es obligatoria';
    else if (password.length < 8) e.password = 'Mínimo 8 caracteres';
    if (password !== repeatPassword) e.repeat = 'Las contraseñas no coinciden';
    setErrors(e);
    if (Object.keys(e).length > 0) return;
    setLoading(true);
    try {
      // TODO: authApi.resetPassword({ token: route.params.token, password })
      await new Promise((r) => setTimeout(r, 1000));
      showAlert({
        title: '¡Listo!',
        message: 'Tu contraseña ha sido actualizada.',
        buttons: [{ text: 'Iniciar sesión', onPress: () => navigation.navigate('Login') }],
      });
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo actualizar la contraseña' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <View style={styles.container}>
          {/* Header */}
          <View style={styles.headerRow}>
            <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
              <Ionicons name="chevron-back" size={26} color={colors.text} />
            </TouchableOpacity>
            <Text style={styles.logo}>Wavii</Text>
            <View style={styles.placeholder} />
          </View>

          <Text style={[styles.title, { color: colors.text }]}>Restablece tu contraseña</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            Crea una nueva contraseña segura para tu cuenta.
          </Text>

          {/* Mascota */}
          <View style={styles.mascotSection}>
            <Image source={WAVII_RESET} style={styles.mascotImage} resizeMode="contain" />
          </View>

          {/* Nueva contraseña */}
          <WaviiInput
            label="Nueva contraseña"
            placeholder="Crea tu nueva contraseña"
            isPassword
            value={password}
            onChangeText={setPassword}
            error={errors.password}
            leftIcon={<Ionicons name="lock-closed-outline" size={20} color={colors.textSecondary} />}
          />

          {/* 4 segmentos de fuerza */}
          {strength && (
            <View style={styles.strengthRow}>
              {[1, 2, 3, 4].map((i) => (
                <View
                  key={i}
                  style={[
                    styles.strengthSegment,
                    { backgroundColor: i <= strength.bars ? strength.color : colors.border },
                  ]}
                />
              ))}
              <Text style={[styles.strengthLabel, { color: strength.color }]}>
                {strength.label}
              </Text>
            </View>
          )}

          {/* Repetir contraseña */}
          <WaviiInput
            label="Repetir nueva contraseña"
            placeholder="Repite tu nueva contraseña"
            isPassword
            value={repeatPassword}
            onChangeText={setRepeatPassword}
            error={errors.repeat}
            leftIcon={<Ionicons name="lock-closed-outline" size={20} color={colors.textSecondary} />}
          />

          <WaviiButton
            title="Actualizar contraseña"
            onPress={handleUpdate}
            loading={loading}
            style={{ marginTop: Spacing.sm }}
          />

          <TouchableOpacity onPress={() => navigation.navigate('Login')} style={styles.backLink}>
            <Text style={styles.backLinkText}>Volver al inicio de sesión</Text>
          </TouchableOpacity>
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
  container: {
    flex: 1,
    paddingHorizontal: Spacing.xl,
    justifyContent: 'center',
  },

  headerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.sm,
  },
  backBtn: { padding: Spacing.xs },
  placeholder: { width: 34 },

  logo: {
    fontFamily: FontFamily.black,
    fontSize: 36,
    color: Colors.primary,
    textAlign: 'center',
    letterSpacing: -0.5,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xl,
    textAlign: 'center',
    marginTop: Spacing.sm,
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    marginTop: Spacing.xs,
  },
  mascotSection: {
    alignItems: 'center',
    marginVertical: Spacing.lg,
  },
  mascotImage: {
    width: 260,
    height: 210,
  },
  strengthRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    marginTop: -Spacing.sm,
    marginBottom: Spacing.base,
  },
  strengthSegment: {
    flex: 1,
    height: 5,
    borderRadius: 3,
  },
  strengthLabel: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    marginLeft: 4,
    minWidth: 60,
  },
  backLink: { alignItems: 'center', marginTop: Spacing.base },
  backLinkText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, color: Colors.primary },
});
