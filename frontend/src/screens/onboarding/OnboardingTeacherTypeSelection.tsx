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
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingTeacherTypeSelection'>;
};

type TeacherType = 'particular' | 'certificado';

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

export const OnboardingTeacherTypeSelection: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const [selected, setSelected] = useState<TeacherType | null>(null);

  const handleContinue = () => {
    if (!selected) return;
    navigation.navigate('Register', {
      role: selected === 'certificado' ? 'profesor_certificado' : 'profesor_particular',
      pendingPlan: 'scholar',
      teacherType: selected,
    });
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>

      {/* Flecha volver */}
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
          <WaviiSpeech text="¿Qué tipo de profesor eres?" />
        </View>

        <View style={styles.cardsSection}>
          {/* Profesor Certificado */}
          <TouchableOpacity
            style={[
              styles.card,
              {
                borderColor: selected === 'certificado' ? Colors.primary : colors.border,
                backgroundColor: selected === 'certificado' ? Colors.primaryOpacity10 : colors.surface,
              },
            ]}
            onPress={() => setSelected('certificado')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="ribbon-outline"
              size={36}
              color={selected === 'certificado' ? Colors.primary : colors.textSecondary}
            />
            <View style={styles.cardTextCol}>
              <View style={styles.cardTitleRow}>
                <Text style={[styles.cardTitle, { color: selected === 'certificado' ? Colors.primary : colors.text }]}>
                  Profesor Certificado
                </Text>
                <View style={styles.priorityBadge}>
                  <Text style={styles.priorityBadgeText}>Alta prioridad</Text>
                </View>
              </View>
              <Text style={[styles.cardDesc, { color: colors.textSecondary }]}>
                Tienes titulación oficial. Apareces primero en los resultados y obtienes insignia verificada.
              </Text>
              <Text style={[styles.cardNote, { color: colors.textSecondary }]}>
                Requiere verificación manual de títulos (24-48h)
              </Text>
            </View>
          </TouchableOpacity>

          {/* Profesor Particular */}
          <TouchableOpacity
            style={[
              styles.card,
              {
                borderColor: selected === 'particular' ? Colors.primary : colors.border,
                backgroundColor: selected === 'particular' ? Colors.primaryOpacity10 : colors.surface,
              },
            ]}
            onPress={() => setSelected('particular')}
            activeOpacity={0.8}
          >
            <Ionicons
              name="person-outline"
              size={36}
              color={selected === 'particular' ? Colors.primary : colors.textSecondary}
            />
            <View style={styles.cardTextCol}>
              <Text style={[styles.cardTitle, { color: selected === 'particular' ? Colors.primary : colors.text }]}>
                Profesor Particular
              </Text>
              <Text style={[styles.cardDesc, { color: colors.textSecondary }]}>
                Comparte tu conocimiento sin titulación oficial. Tu cuenta se activa en el momento en que verificas tu email.
              </Text>
              <Text style={[styles.cardNote, { color: colors.textSecondary }]}>
                Verificación automática por email
              </Text>
            </View>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          style={styles.userLink}
          onPress={() => navigation.navigate('OnboardingLevelSelection')}
          activeOpacity={0.7}
        >
          <Text style={[styles.userLinkText, { color: colors.textSecondary }]}>
            Mejor continuar como usuario
          </Text>
        </TouchableOpacity>

        <View style={styles.btnSection}>
          <WaviiButton
            title="Continuar"
            onPress={handleContinue}
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
    alignItems: 'flex-start',
    borderWidth: 2,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    gap: Spacing.base,
  },
  cardTextCol: {
    flex: 1,
    gap: 4,
  },
  cardTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    flexWrap: 'wrap',
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
  cardNote: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    fontStyle: 'italic',
  },
  priorityBadge: {
    backgroundColor: Colors.success,
    paddingHorizontal: Spacing.xs,
    paddingVertical: 2,
    borderRadius: BorderRadius.sm,
  },
  priorityBadgeText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    color: Colors.white,
  },
  userLink: {
    alignItems: 'center',
    paddingVertical: Spacing.sm,
  },
  userLinkText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    textDecorationLine: 'underline',
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