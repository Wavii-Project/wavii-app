import React from 'react';
import { View, Text, StyleSheet, Image } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { useTheme } from '../../context/ThemeContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'Welcome'>;
};

const WAVII_IMAGE = require('../../../assets/wavii/wavii_bienvenida.png');

export const WelcomeScreen: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={styles.spacerTop} />

      <View style={styles.logoBlock}>
        <Text style={styles.logo}>Wavii</Text>
        <Text style={[styles.tagline, { color: colors.textSecondary }]}>Tu música. Tu ritmo. Tu comunidad.</Text>
      </View>

      <Image source={WAVII_IMAGE} style={styles.mascot} resizeMode="contain" />

      <View style={styles.buttons}>
        <WaviiButton
          title="Comenzar"
          onPress={() => navigation.navigate('OnboardingPresentation')}
          variant="primary"
          size="lg"
        />
        <View style={styles.gap} />
        <WaviiButton
          title="Ya tengo una cuenta"
          onPress={() => navigation.navigate('Login')}
          variant="outline"
          size="lg"
          style={styles.outlineBtn}
          textStyle={styles.outlineBtnText}
          activeOpacity={0.6}
        />
      </View>

      <View style={styles.spacerBottom} />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,

    paddingHorizontal: Spacing.xl,
    alignItems: 'center',
    justifyContent: 'center',
  },
  spacerTop: { flex: 0 },
  spacerBottom: { flex: 0 },
  logoBlock: {
    alignItems: 'center',
    marginBottom: Spacing.lg,
  },
  logo: {
    fontFamily: FontFamily.black,
    fontSize: 52,
    color: Colors.primary,
    letterSpacing: 0,
  },
  tagline: {
    fontFamily: FontFamily.medium,
    fontSize: FontSize.base,
    textAlign: 'center',
    marginTop: Spacing.xs,
  },
  mascot: {
    width: 280,
    height: 230,
    marginBottom: Spacing.xl,
  },
  buttons: { width: '100%' },
  gap: { height: Spacing.sm },
  outlineBtn: {
    borderWidth: 2,
    borderColor: Colors.primary,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
  },
  outlineBtnText: {
    color: Colors.primary,
    fontFamily: FontFamily.bold,
  },
});
