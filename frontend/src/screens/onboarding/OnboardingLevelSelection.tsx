import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiSpeech } from '../../components/common/WaviiSpeech';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { User } from '../../context/AuthContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingLevelSelection'>;
};

type LevelOption = User['level'];
type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

const TOTAL_STEPS = 7;
const CURRENT_STEP = 6;

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

interface LevelCard {
  value: LevelOption;
  icon: IoniconsName;
  title: string;
  description: string;
}

const LEVELS: LevelCard[] = [
  {
    value: 'principiante',
    icon: 'musical-note-outline',
    title: 'Principiante',
    description: 'Nunca he tocado un instrumento',
  },
  {
    value: 'intermedio',
    icon: 'musical-notes-outline',
    title: 'Intermedio',
    description: 'Llevo un tiempo practicando',
  },
  {
    value: 'avanzado',
    icon: 'star-outline',
    title: 'Avanzado',
    description: 'Tengo experiencia y quiero mejorar',
  },
];

export const OnboardingLevelSelection: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const [selected, setSelected] = useState<LevelOption | null>(null);

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
          <WaviiSpeech text="¿Cuál es tu nivel musical?" />
        </View>

        <View style={styles.cardsSection}>
          {LEVELS.map((level) => {
            const isSelected = selected === level.value;
            return (
              <TouchableOpacity
                key={level.value}
                style={[
                  styles.card,
                  {
                    borderColor: isSelected ? Colors.primary : colors.border,
                    backgroundColor: isSelected ? Colors.primaryOpacity10 : colors.surface,
                  },
                ]}
                onPress={() => setSelected(level.value)}
                activeOpacity={0.8}
              >
                <View style={[styles.iconCircle, { backgroundColor: isSelected ? Colors.primary : colors.border }]}>
                  <Ionicons name={level.icon} size={24} color={isSelected ? '#FFF' : colors.textSecondary} />
                </View>
                <View style={styles.cardTextCol}>
                  <Text style={[styles.cardTitle, { color: isSelected ? Colors.primary : colors.text }]}>
                    {level.title}
                  </Text>
                  <Text style={[styles.cardDesc, { color: colors.textSecondary }]}>
                    {level.description}
                  </Text>
                </View>
                <Ionicons
                  name="checkmark-circle"
                  size={22}
                  color={Colors.primary}
                  style={{ opacity: isSelected ? 1 : 0 }}
                />
              </TouchableOpacity>
            );
          })}
        </View>

        <View style={styles.btnSection}>
          <WaviiButton
            title="Empezar a Practicar"
            onPress={() => {
              if (selected) {
                navigation.navigate('OnboardingSubscription', { level: selected });
              }
            }}
            disabled={!selected}
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
    justifyContent: 'space-between',
  },
  speechSection: {
    paddingTop: Spacing['2xl'],
    paddingBottom: Spacing.xl,
  },
  cardsSection: {
    flex: 1,
    gap: Spacing.base,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 2,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    gap: Spacing.base,
  },
  iconCircle: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardTextCol: {
    flex: 1,
  },
  cardTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    marginBottom: 2,
  },
  cardDesc: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
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
