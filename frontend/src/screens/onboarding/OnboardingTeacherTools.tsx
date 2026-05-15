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
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingTeacherTools'>;
};

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

const TOTAL_STEPS = 7;
const CURRENT_STEP = 3;

const ProgressDots = () => (
  <View style={styles.dotsRow}>
    {Array.from({ length: TOTAL_STEPS }).map((_, i) => (
      <View key={i} style={[styles.dot, i + 1 === CURRENT_STEP ? styles.dotActive : styles.dotInactive]} />
    ))}
  </View>
);

interface ToolItem {
  icon: IoniconsName;
  title: string;
  description: string;
}

const TOOLS: ToolItem[] = [
  { icon: 'calendar-outline', title: 'Tablón de Anuncios', description: 'Publica clases y horarios' },
  { icon: 'people-outline', title: 'Aulas Virtuales', description: 'Gestiona grupos de alumnos' },
  { icon: 'stats-chart-outline', title: 'Seguimiento', description: 'Progreso de cada alumno' },
  { icon: 'cash-outline', title: 'Facturación', description: 'Gestión de pagos integrada' },
];

export const OnboardingTeacherTools: React.FC<Props> = ({ navigation }) => {
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
          <WaviiSpeech text="¡Mira todo lo que tendrás como profesor!" />
        </View>

        <View style={styles.toolsList}>
          {TOOLS.map((tool) => (
            <View
              key={tool.title}
              style={[styles.toolItem, { backgroundColor: colors.surface, borderColor: colors.border }]}
            >
              <View style={styles.toolIconWrapper}>
                <Ionicons name={tool.icon} size={24} color={Colors.primary} />
              </View>
              <View style={styles.toolTextCol}>
                <Text style={[styles.toolTitle, { color: colors.text }]}>{tool.title}</Text>
                <Text style={[styles.toolDesc, { color: colors.textSecondary }]}>{tool.description}</Text>
              </View>
            </View>
          ))}
        </View>

        <View style={[styles.warningBox, { backgroundColor: Colors.warningLight }]}>
          <Ionicons name="warning-outline" size={16} color={Colors.warning} />
          <Text style={[styles.warningText, { color: colors.text }]}>
            Requiere Wavii Scholar — 7,99 €/mes
          </Text>
        </View>

        <View style={styles.btnSection}>
          <WaviiButton
            title="Elegir mi perfil de profesor"
            onPress={() => navigation.navigate('OnboardingTeacherTypeSelection')}
          />
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
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
  toolsList: {
    flex: 1,
    gap: Spacing.sm,
  },
  toolItem: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.base,
    gap: Spacing.base,
  },
  toolIconWrapper: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: Colors.primaryOpacity10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  toolTextCol: { flex: 1 },
  toolTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    marginBottom: 1,
  },
  toolDesc: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },
  warningBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderRadius: BorderRadius.md,
    padding: Spacing.md,
    marginVertical: Spacing.base,
  },
  warningText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },
  btnSection: { paddingBottom: Spacing.lg },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});
