import React from 'react';
import { View, Text, Image, Pressable, StyleSheet } from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

interface Props {
  /** Texto que completa la frase "Necesitas una cuenta ___". Ej: "para ver los desafíos" */
  feature: string;
}

/**
 * Pantalla de bloqueo para usuarios invitados.
 * Muestra la mascota Wavii, un texto explicativo y dos botones:
 * "Crear cuenta gratis" y "Ya tengo cuenta".
 * Al pulsar cualquier botón, se llama a logout() para ir al flujo de autenticación.
 */
export const GuestBlockedView: React.FC<Props> = ({ feature }) => {
  const { colors } = useTheme();
  const { logout } = useAuth();

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Image
        source={require('../../../assets/wavii/wavii_bienvenida.png')}
        style={styles.wavii}
        resizeMode="contain"
      />
      <Text style={[styles.title, { color: colors.text }]}>Función exclusiva</Text>
      <Text style={[styles.sub, { color: colors.textSecondary }]}>
        Necesitas una cuenta {feature}.
      </Text>
      <Pressable
        style={[styles.btn, { backgroundColor: Colors.primary }]}
        onPress={logout}
      >
        <Text style={styles.btnText}>Crear cuenta gratis</Text>
      </Pressable>
      <Pressable style={styles.loginBtn} onPress={logout}>
        <Text style={[styles.loginText, { color: Colors.primary }]}>Ya tengo cuenta</Text>
      </Pressable>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.xl,
    gap: Spacing.sm,
  },
  wavii: {
    width: 180,
    height: 180,
    marginBottom: Spacing.md,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xl,
    textAlign: 'center',
  },
  sub: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    textAlign: 'center',
    lineHeight: 22,
    marginBottom: Spacing.sm,
  },
  btn: {
    width: '100%',
    paddingVertical: 16,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    marginTop: Spacing.xs,
  },
  btnText: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.base,
    color: '#fff',
  },
  loginBtn: {
    paddingVertical: 12,
    alignItems: 'center',
  },
  loginText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
});
