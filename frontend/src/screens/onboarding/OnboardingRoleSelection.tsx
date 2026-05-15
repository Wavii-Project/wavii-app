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

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingRoleSelection'>;
};

type RoleOption = 'usuario' | 'profesor';

const TOTAL_STEPS = 7;
const CURRENT_STEP = 2;

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

const ROLES: { id: RoleOption; icon: React.ComponentProps<typeof Ionicons>['name']; title: string; desc: string }[] = [
  {
    id: 'usuario',
    icon: 'library-outline',
    title: 'Buscar Tablaturas, Practicar y Conectar',
    desc: 'Aprende a tu ritmo y conecta con músicos de todo el mundo',
  },
  {
    id: 'profesor',
    icon: 'mic-outline',
    title: 'Quiero Enseñar o Publicar Clases',
    desc: 'Comparte tu conocimiento con músicos de todo el mundo',
  },
];

export const OnboardingRoleSelection: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const [selected, setSelected] = useState<RoleOption | null>(null);

  const handleContinue = () => {
    if (!selected) return;
    if (selected === 'usuario') {
      navigation.navigate('OnboardingTabsExplanation');
    } else {
      navigation.navigate('OnboardingTeacherTools');
    }
  };

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
          <WaviiSpeech text="¿Cuál es tu objetivo en Wavii?" />
        </View>

        <View style={styles.cardsSection}>
          {ROLES.map((role) => {
            const isSelected = selected === role.id;
            return (
              <TouchableOpacity
                key={role.id}
                style={[
                  styles.card,
                  {
                    borderColor: isSelected ? Colors.primary : colors.border,
                    backgroundColor: isSelected ? Colors.primaryOpacity10 : colors.surface,
                  },
                ]}
                onPress={() => setSelected(role.id)}
                activeOpacity={0.8}
              >
                <View style={[styles.iconCircle, { backgroundColor: isSelected ? Colors.primary : colors.border }]}>
                  <Ionicons name={role.icon} size={28} color={isSelected ? '#FFF' : colors.textSecondary} />
                </View>
                <View style={styles.cardText}>
                  <Text style={[styles.cardTitle, { color: isSelected ? Colors.primary : colors.text }]}>
                    {role.title}
                  </Text>
                  <Text style={[styles.cardDesc, { color: colors.textSecondary }]}>{role.desc}</Text>
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
          <WaviiButton title="Continuar" onPress={handleContinue} disabled={!selected} />
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
    width: 52,
    height: 52,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardText: {
    flex: 1,
    gap: Spacing.xs,
  },
  cardTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  cardDesc: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 18,
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
