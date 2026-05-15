import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet, Image } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiSpeech } from '../../components/common/WaviiSpeech';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

const WAVII_TABS = require('../../../assets/wavii/wavii_tablatura.png');

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingTabsExplanation'>;
};

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

const TOTAL_STEPS = 7;
const CURRENT_STEP = 4;

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

interface FeatureItem {
  icon: IoniconsName;
  text: string;
}

const FEATURES: FeatureItem[] = [
  { icon: 'musical-notes-outline', text: 'Explora tablaturas de la comunidad' },
  { icon: 'share-social-outline', text: 'Comparte y contribuye tus propias tablaturas' },
  { icon: 'search-outline', text: 'Encuentra tablaturas por artista o estilo' },
  { icon: 'download-outline', text: 'Descarga offline con Wavii Plus' },
];

export const OnboardingTabsExplanation: React.FC<Props> = ({ navigation }) => {
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
          <WaviiSpeech text="¡Explora miles de tablaturas y empieza a practicar!" />
        </View>

        <View style={styles.imageSection}>
          <Text style={[styles.title, { color: colors.text }]}>Funciones</Text>
        </View>

        <View style={styles.featuresList}>
          {FEATURES.map((feature) => (
            <View
              key={feature.text}
              style={[styles.featureRow, { backgroundColor: colors.surface, borderColor: colors.border }]}
            >
              <View style={styles.featureIconWrapper}>
                <Ionicons name={feature.icon} size={22} color={Colors.primary} />
              </View>
              <Text style={[styles.featureText, { color: colors.text }]}>{feature.text}</Text>
            </View>
          ))}
        </View>

        <View style={styles.btnSection}>
          <WaviiButton
            title="Continuar"
            onPress={() => navigation.navigate('OnboardingSocialExplanation')}
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
  imageSection: {
    alignItems: 'flex-start',
    gap: Spacing.sm,
    paddingBottom: Spacing.base,
  },
  headerImage: {
    width: 160,
    height: 120,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
    textAlign: 'center',
  },
  description: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 20,
  },
  descriptionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    width: '100%',
  },
  descriptionCardText: {
    flex: 1,
    fontFamily: FontFamily.medium,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  featuresList: {
    flex: 1,
    gap: Spacing.sm,
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
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.primaryOpacity10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  featureText: {
    flex: 1,
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
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
