import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiSpeech } from '../../components/common/WaviiSpeech';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingSocialExplanation'>;
};

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

const TOTAL_STEPS = 7;
const CURRENT_STEP = 5;

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

interface SocialFeature {
  icon: IoniconsName;
  title: string;
  description: string;
}

const SOCIAL_FEATURES: SocialFeature[] = [
  {
    icon: 'people-outline',
    title: 'Comunidades y fandoms',
    description: 'Únete a grupos por género musical, artista o estilo y conecta con músicos afines',
  },
  {
    icon: 'musical-notes-outline',
    title: 'Comparte tus tablaturas',
    description: 'Publica y descubre tablaturas creadas por la comunidad Wavii',
  },
  {
    icon: 'school-outline',
    title: 'Busca profesores',
    description: 'Encuentra profesores certificados o particulares que te enseñen tu instrumento favorito',
  },
];

export const OnboardingSocialExplanation: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>
      <ProgressDots />

      <View style={styles.container}>
        <View style={styles.speechSection}>
          <WaviiSpeech text="¡Conecta con músicos de toda España!" />
        </View>

        <View style={styles.headerSection}>
          <Text style={[styles.title, { color: colors.text }]}>La red social musical</Text>
        </View>

        <View style={styles.featuresList}>
          {SOCIAL_FEATURES.map((feature) => (
            <View
              key={feature.title}
              style={[styles.featureRow, { backgroundColor: colors.surface, borderColor: colors.border }]}
            >
              <View style={styles.featureIconWrapper}>
                <Ionicons name={feature.icon} size={24} color={Colors.primary} />
              </View>
              <View style={styles.featureTextBlock}>
                <Text style={[styles.featureTitle, { color: colors.text }]}>{feature.title}</Text>
                <Text style={[styles.featureDescription, { color: colors.textSecondary }]}>{feature.description}</Text>
              </View>
            </View>
          ))}
        </View>

        <Text style={[styles.note, { color: colors.textSecondary }]}>
          Puedes desactivar las funciones sociales desde tu perfil en cualquier momento
        </Text>

        <View style={styles.btnSection}>
          <WaviiButton
            title="Continuar"
            onPress={() => navigation.navigate('OnboardingLevelSelection')}
          />
        </View>
      </View>
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
  dot: { height: 8, borderRadius: 4 },
  dotActive: { width: 24, backgroundColor: Colors.primary },
  dotInactive: { width: 8, backgroundColor: Colors.border },
  container: {
    flex: 1,
    paddingHorizontal: Spacing.xl,
  },
  speechSection: {
    paddingTop: Spacing['2xl'],
    paddingBottom: Spacing.base,
  },
  headerSection: {
    paddingBottom: Spacing.base,
    gap: Spacing.xs,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
  },
  description: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  featuresList: {
    gap: Spacing.sm,
    flex: 1,
    justifyContent: 'flex-start',
  },
  featureRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    gap: Spacing.base,
  },
  featureIconWrapper: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: Colors.primaryOpacity10,
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  featureTextBlock: {
    flex: 1,
    gap: 4,
  },
  featureTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  featureDescription: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 16,
  },
  note: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
    lineHeight: 16,
    marginVertical: Spacing.base,
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
