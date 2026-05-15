import React, { useState } from 'react';
import { View, TouchableOpacity, Text, StyleSheet, Image } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiSpeech } from '../../components/common/WaviiSpeech';
import { Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { useTheme } from '../../context/ThemeContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingPresentation'>;
};

const TOTAL_STEPS = 7;
const CURRENT_STEP = 1;

const WAVII_IMAGE = require('../../../assets/wavii/wavii_bienvenida.png');

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

export const OnboardingPresentation: React.FC<Props> = ({ navigation }) => {
  const [phase, setPhase] = useState<0 | 1>(0);
  const { colors } = useTheme();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => (phase === 1 ? setPhase(0) : navigation.goBack())}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>
      <ProgressDots />

      {phase === 0 ? (
        <View style={styles.container}>
          <View style={styles.heroSection}>
            <Image source={WAVII_IMAGE} style={styles.mascotImage} resizeMode="contain" />
            <Text style={[styles.title, { color: colors.text }]}>¡Hola! Soy Wavii</Text>
            <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
              Tu coach musical personal. Aprende, practica y conecta con otros músicos.
            </Text>
          </View>
          <View style={styles.btnSection}>
            <WaviiButton title="Continuar" onPress={() => setPhase(1)} size="lg" />
          </View>
        </View>
      ) : (
        <View style={styles.container}>
          <View style={styles.introSection}>
            <WaviiSpeech
              text="Antes de empezar, te haré unas preguntas rápidas para personalizar tu experiencia."
              variant="above"
              mascotSize={200}
            />
          </View>
          <View style={styles.btnSection}>
            <WaviiButton
              title="¡Vamos!"
              onPress={() => navigation.navigate('OnboardingRoleSelection')}
              size="lg"
            />
          </View>
        </View>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,

  },
  dotsRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
    paddingTop: Spacing.base,
    paddingBottom: Spacing.sm,
  },
  dot: {
    height: 8,
    borderRadius: 4,
  },
  dotActive: {
    width: 24,
    backgroundColor: Colors.primary,
  },
  dotInactive: {
    width: 8,
    backgroundColor: Colors.border,
  },
  container: {
    flex: 1,
    paddingHorizontal: Spacing.xl,
    justifyContent: 'space-between',
  },
  heroSection: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.sm,
  },
  mascotImage: {
    width: 300,
    height: 240,
    marginBottom: Spacing.xs,
  },
  title: {
    fontFamily: FontFamily.black,
    fontSize: FontSize['3xl'],
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    textAlign: 'center',
    lineHeight: 24,
    paddingHorizontal: Spacing.sm,
  },
  introSection: {
    flex: 1,
    justifyContent: 'center',
  },
  btnSection: {
    paddingBottom: Spacing.lg,
    paddingTop: Spacing.sm,
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});
