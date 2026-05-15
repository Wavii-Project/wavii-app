import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { useAuth } from '../../context/AuthContext';
import { apiChangePassword } from '../../api/userApi';
import { WaviiInput } from '../../components/common/WaviiInput';
import { WaviiButton } from '../../components/common/WaviiButton';
import { FontFamily, FontSize, Spacing } from '../../theme';

const STRONG_PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/;

export const ChangePasswordScreen = () => {
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const { token } = useAuth();
  const navigation = useNavigation();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [currentPasswordError, setCurrentPasswordError] = useState<string>();
  const [newPasswordError, setNewPasswordError] = useState<string>();
  const [confirmPasswordError, setConfirmPasswordError] = useState<string>();
  const [loading, setLoading] = useState(false);

  const resetErrors = () => {
    setCurrentPasswordError(undefined);
    setNewPasswordError(undefined);
    setConfirmPasswordError(undefined);
  };

  const validate = () => {
    resetErrors();
    let valid = true;

    if (!currentPassword) {
      setCurrentPasswordError('Introduce tu contraseña actual.');
      valid = false;
    }
    if (!newPassword) {
      setNewPasswordError('Introduce una nueva contraseña.');
      valid = false;
    } else if (!STRONG_PASSWORD_REGEX.test(newPassword)) {
      setNewPasswordError('Debe tener 8 caracteres o más, mayúscula, minúscula, número y carácter especial.');
      valid = false;
    }
    if (!confirmPassword) {
      setConfirmPasswordError('Repite la nueva contraseña.');
      valid = false;
    } else if (newPassword !== confirmPassword) {
      setConfirmPasswordError('Las contraseñas nuevas no coinciden.');
      valid = false;
    }
    if (currentPassword && newPassword && currentPassword === newPassword) {
      setNewPasswordError('La nueva contraseña debe ser distinta a la actual.');
      valid = false;
    }

    return valid;
  };

  const handleSubmit = async () => {
    if (!validate() || !token) return;

    setLoading(true);
    try {
      await apiChangePassword(currentPassword, newPassword, token);
      showAlert({
        title: 'Contraseña actualizada',
        message: 'Tu contraseña se ha guardado correctamente.',
        buttons: [{ text: 'OK', onPress: () => navigation.goBack() }],
      });
    } catch (err: any) {
      const msg: string =
        err?.response?.data?.message ?? 'No se pudo actualizar la contraseña. Inténtalo de nuevo.';

      if (err?.response?.data?.code === 'WRONG_PASSWORD') {
        setCurrentPasswordError(msg);
      } else if (err?.response?.data?.code === 'VALIDATION_ERROR' || err?.response?.data?.code === 'SAME_PASSWORD') {
        setNewPasswordError(msg);
      } else {
        showAlert({ title: 'Error', message: msg });
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Cambiar contraseña</Text>
        <View style={{ width: 26 }} />
      </View>

      <View style={styles.form}>
        <WaviiInput
          label="Contraseña actual"
          value={currentPassword}
          onChangeText={(text) => {
            setCurrentPassword(text);
            if (currentPasswordError) setCurrentPasswordError(undefined);
          }}
          placeholder="Escribe tu contraseña actual"
          isPassword
          error={currentPasswordError}
          autoCapitalize="none"
        />

        <WaviiInput
          label="Nueva contraseña"
          value={newPassword}
          onChangeText={(text) => {
            setNewPassword(text);
            if (newPasswordError) setNewPasswordError(undefined);
          }}
          placeholder="Escribe tu nueva contraseña"
          isPassword
          error={newPasswordError}
          autoCapitalize="none"
          hint="Mínimo 8 caracteres con mayúscula, minúscula, número y símbolo."
        />

        <WaviiInput
          label="Confirmar nueva contraseña"
          value={confirmPassword}
          onChangeText={(text) => {
            setConfirmPassword(text);
            if (confirmPasswordError) setConfirmPasswordError(undefined);
          }}
          placeholder="Repite la nueva contraseña"
          isPassword
          error={confirmPasswordError}
          autoCapitalize="none"
        />

        <WaviiButton title="Actualizar contraseña" onPress={handleSubmit} loading={loading} />
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  form: {
    padding: Spacing.xl,
  },
});
