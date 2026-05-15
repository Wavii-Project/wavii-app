import React, { useState, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  KeyboardAvoidingView,
  ScrollView,
  Platform,
  Alert,
  ActivityIndicator,
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
import { apiCheckNameAvailable } from '../../api/authApi';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'Register'>;
  route: RouteProp<AuthStackParamList, 'Register'>;
};

const getPasswordStrength = (pw: string) => {
  if (!pw) return null;
  let score = 0;
  if (pw.length >= 8) score++;
  if (/[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw)) score++;
  if (/[^A-Za-z0-9]/.test(pw)) score++;
  const map = [
    { label: 'Débil',      color: '#EF4444', bars: 1 },
    { label: 'Media',      color: '#F59E0B', bars: 2 },
    { label: 'Fuerte',     color: '#22C55E', bars: 3 },
    { label: 'Muy fuerte', color: '#22C55E', bars: 4 },
  ];
  return map[Math.min(score - 1, 3)] ?? map[0];
};

export const RegisterScreen: React.FC<Props> = ({ navigation, route }) => {
  const { register } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const role = route.params?.role ?? 'usuario';
  const level = route.params?.level;
  const pendingPlan = route.params?.pendingPlan;
  const teacherType = route.params?.teacherType;

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [repeatPassword, setRepeatPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [nameStatus, setNameStatus] = useState<'idle' | 'checking' | 'available' | 'taken'>('idle');
  const nameTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const strength = getPasswordStrength(password);
  const isProfesor = role === 'profesor_particular' || role === 'profesor_certificado';

  const handleNameChange = (text: string) => {
    setName(text);
    if (nameTimer.current) clearTimeout(nameTimer.current);
    if (text.trim().length < 3) {
      setNameStatus('idle');
      return;
    }
    setNameStatus('checking');
    nameTimer.current = setTimeout(async () => {
      try {
        const available = await apiCheckNameAvailable(text.trim());
        setNameStatus(available ? 'available' : 'taken');
      } catch {
        setNameStatus('idle');
      }
    }, 500);
  };
  const subtitle = isProfesor ? 'Empieza a enseñar en Wavii' : 'Únete a la comunidad de músicos';

  const validate = () => {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = 'El nombre de usuario es obligatorio';
    else if (name.length < 3) e.name = 'Mínimo 3 caracteres';
    else if (nameStatus === 'taken') e.name = 'Este nombre ya está en uso';
    if (!email.trim()) e.email = 'El correo es obligatorio';
    else if (!/\S+@\S+\.\S+/.test(email)) e.email = 'Correo inválido';
    if (!password) e.password = 'La contraseña es obligatoria';
    else if (password.length < 8) e.password = 'Mínimo 8 caracteres con letra, número y símbolo';
    if (password !== repeatPassword) e.repeatPassword = 'Las contraseñas no coinciden';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleRegister = async () => {
    if (nameStatus === 'checking') return; // esperar a que termine la comprobación
    if (!validate()) return;
    setLoading(true);
    try {
      await register({ name, email, password, role, level });
      navigation.navigate('EmailVerification', {
        email: email.trim(),
        pendingPlan,
        teacherType,
      });
    } catch (e: unknown) {
      const raw = e instanceof Error ? e.message : 'No se pudo crear la cuenta';
      if (raw.startsWith('__NAME__')) {
        setErrors((prev) => ({ ...prev, name: raw.substring(8) }));
        setNameStatus('taken');
      } else {
        showAlert({ title: 'Error', message: raw });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      {/* Flecha volver — misma posición que Login y ForgotPassword */}
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'padding'}
        style={styles.flex}
      >
        <ScrollView
          contentContainerStyle={styles.container}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          {/* Cabecera */}
          <Text style={[styles.title, { color: colors.text }]}>Crea tu cuenta</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>{subtitle}</Text>

          {/* Google */}
          <TouchableOpacity
            style={[styles.googleBtn, { borderColor: colors.border }]}
            onPress={() => showAlert({ title: 'Google', message: 'Próximamente disponible' })}
            activeOpacity={0.8}
          >
            <View style={styles.googleRow}>
              {(['G','o','o','g','l','e'] as const).map((letter, i) => (
                <Text key={i} style={[styles.googleLetter, { color: ['#4285F4','#EA4335','#FBBC05','#4285F4','#34A853','#EA4335'][i] }]}>
                  {letter}
                </Text>
              ))}
            </View>
            <Text style={[styles.googleText, { color: colors.text }]}>Continuar con Google</Text>
          </TouchableOpacity>

          {/* Divider */}
          <View style={styles.dividerRow}>
            <View style={[styles.dividerLine, { backgroundColor: colors.border }]} />
            <Text style={[styles.dividerText, { color: colors.textSecondary }]}>o con email</Text>
            <View style={[styles.dividerLine, { backgroundColor: colors.border }]} />
          </View>

          {/* Inputs */}
          <WaviiInput
            label="Nombre de usuario"
            placeholder="Elige un nombre de usuario"
            autoCapitalize="none"
            value={name}
            onChangeText={handleNameChange}
            error={errors.name}
            leftIcon={<Ionicons name="person-outline" size={20} color={colors.textSecondary} />}
          />
          {/* Indicador de disponibilidad del nombre */}
          {nameStatus !== 'idle' && !errors.name && (
            <View style={styles.nameStatusRow}>
              {nameStatus === 'checking' && (
                <>
                  <ActivityIndicator size={12} color={colors.textSecondary} />
                  <Text style={[styles.nameStatusText, { color: colors.textSecondary }]}>Comprobando...</Text>
                </>
              )}
              {nameStatus === 'available' && (
                <>
                  <Ionicons name="checkmark-circle" size={14} color="#22C55E" />
                  <Text style={[styles.nameStatusText, { color: '#22C55E' }]}>Nombre disponible</Text>
                </>
              )}
              {nameStatus === 'taken' && (
                <>
                  <Ionicons name="close-circle" size={14} color="#EF4444" />
                  <Text style={[styles.nameStatusText, { color: '#EF4444' }]}>Este nombre ya está en uso</Text>
                </>
              )}
            </View>
          )}

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
            placeholder="Crea una contraseña"
            isPassword
            value={password}
            onChangeText={setPassword}
            error={errors.password}
            leftIcon={<Ionicons name="lock-closed-outline" size={20} color={colors.textSecondary} />}
          />

          {/* Barra de fuerza */}
          {strength && (
            <View style={styles.strengthRow}>
              {[1, 2, 3, 4].map((i) => (
                <View
                  key={i}
                  style={[styles.strengthSeg, { backgroundColor: i <= strength.bars ? strength.color : colors.border }]}
                />
              ))}
              <Text style={[styles.strengthLabel, { color: strength.color }]}>{strength.label}</Text>
            </View>
          )}

          <WaviiInput
            label="Repetir contraseña"
            placeholder="Repite tu contraseña"
            isPassword
            value={repeatPassword}
            onChangeText={setRepeatPassword}
            error={errors.repeatPassword}
            leftIcon={<Ionicons name="lock-closed-outline" size={20} color={colors.textSecondary} />}
          />

          <WaviiButton
            title="Crear cuenta"
            onPress={handleRegister}
            loading={loading}
            disabled={nameStatus === 'checking' || nameStatus === 'taken'}
            style={styles.registerBtn}
          />
        </ScrollView>
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
    paddingHorizontal: Spacing.xl,
    paddingTop: Spacing['3xl'],
    paddingBottom: Spacing['2xl'],
    flexGrow: 1,
    justifyContent: 'center',
  },

  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xl,
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    marginTop: Spacing.xs,
    marginBottom: Spacing.md,
  },

  googleBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    height: 52,
    backgroundColor: Colors.white,
    gap: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  googleRow: { flexDirection: 'row', alignItems: 'center' },
  googleLetter: {
    fontSize: 18,
    fontWeight: '700',
    fontFamily: FontFamily.black,
  },
  googleText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },

  dividerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: Spacing.sm,
    gap: Spacing.sm,
  },
  dividerLine: { flex: 1, height: 1 },
  dividerText: {
    fontFamily: FontFamily.medium,
    fontSize: FontSize.xs,
  },

  strengthRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    marginTop: -Spacing.sm,
    marginBottom: Spacing.sm,
  },
  strengthSeg: {
    flex: 1,
    height: 5,
    borderRadius: 3,
  },
  strengthLabel: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    minWidth: 60,
  },

  registerBtn: {
    marginTop: Spacing.md,
  },

  nameStatusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    marginTop: -Spacing.xs,
    marginBottom: Spacing.xs,
  },
  nameStatusText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
});
