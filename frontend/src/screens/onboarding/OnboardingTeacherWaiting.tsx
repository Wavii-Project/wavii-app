import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet, Image } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingTeacherWaiting'>;
};

const WAVII_IMAGE = require('../../../assets/wavii/wavii_bienvenida.png');

export const OnboardingTeacherWaiting: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const { confirmEmailVerified } = useAuth();

  const handleGoToApp = async () => {
    try {
      await confirmEmailVerified();
    } catch {
      await confirmEmailVerified();
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
        <Ionicons name="chevron-back" size={28} color={Colors.textPrimary} />
      </TouchableOpacity>
      <View style={styles.container}>
        <View style={styles.topSection}>
          <Image source={WAVII_IMAGE} style={styles.mascotImage} resizeMode="contain" />

          <Text style={styles.title}>¡Solicitud enviada!</Text>
          <Text style={[styles.description, { color: colors.textSecondary }]}>
            Revisaremos tu documentación en un plazo de 24-48 horas. Te notificaremos por email cuando tu cuenta esté verificada.
          </Text>

          <View style={[styles.infoBox, { backgroundColor: colors.surface, borderColor: colors.border }]}>
            <Ionicons name="information-circle-outline" size={24} color={Colors.gray} />
            <Text style={[styles.infoText, { color: colors.textSecondary }]}>
              Mientras tanto, tienes acceso a todas las herramientas de gestión en modo Scholar Pendiente.
            </Text>
          </View>
        </View>

        <View style={styles.btnSection}>
          <WaviiButton title="Continuar a la app" onPress={handleGoToApp} size="lg" />
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,

  },
  container: {
    flex: 1,
    paddingHorizontal: Spacing.xl,
    justifyContent: 'space-between',
  },
  topSection: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.base,
  },
  mascotImage: {
    width: 240,
    height: 190,
    marginBottom: Spacing.sm,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
    color: Colors.primary,
    textAlign: 'center',
  },
  description: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    textAlign: 'center',
    lineHeight: 24,
  },
  infoBox: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.sm,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    width: '100%',
  },
  infoText: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  btnSection: {
    paddingBottom: Spacing.lg,
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});