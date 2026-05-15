import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiSpeech } from '../../components/common/WaviiSpeech';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingSubscription'>;
  route: RouteProp<AuthStackParamList, 'OnboardingSubscription'>;
};

const TOTAL_STEPS = 7;
const CURRENT_STEP = 7;

const ProgressDots = () => (
  <View style={styles.dotsRow}>
    {Array.from({ length: TOTAL_STEPS }).map((_, i) => (
      <View key={i} style={[styles.dot, i + 1 === CURRENT_STEP ? styles.dotActive : styles.dotInactive]} />
    ))}
  </View>
);

const PLUS_FEATURES = ['Descarga offline', 'Sin anuncios', 'Estadísticas avanzadas'];
const FREE_FEATURES = ['Funciones esenciales para aprender', 'Acceso social básico', 'Puedes subir de plan cuando quieras'];

export const OnboardingSubscription: React.FC<Props> = ({ navigation, route }) => {
  const { colors } = useTheme();
  const { loginAsGuest } = useAuth();
  const level = route.params?.level ?? 'principiante';

  const handlePlus = () => navigation.navigate('Register', { role: 'usuario', level, pendingPlan: 'plus' });
  const handleFree = () => navigation.navigate('Register', { role: 'usuario', level });
  const handleGuest = async () => { await loginAsGuest(); };

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

      <ScrollView contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
        <View style={styles.speechSection}>
          <WaviiSpeech text="¿Cómo quieres empezar?" />
        </View>

        {/* Plus card */}
        <View style={styles.plusCard}>
          {/* Decorative blobs */}
          <View style={styles.blobTopRight} />
          <View style={styles.blobBottomLeft} />

          <View style={styles.plusTop}>
            <View style={styles.recommendedBadge}>
              <Text style={styles.recommendedText}>RECOMENDADO</Text>
            </View>
            <Text style={styles.plusTitle}>Wavii Plus</Text>
            <Text style={styles.plusPrice}>4,99 €/mes — 14 días GRATIS</Text>
            <View style={styles.featuresList}>
              {PLUS_FEATURES.map((f) => (
                <View key={f} style={styles.featureRow}>
                  <Ionicons name="checkmark-circle-outline" size={15} color="rgba(255,255,255,0.9)" />
                  <Text style={styles.featureText}>{f}</Text>
                </View>
              ))}
            </View>
          </View>

          <TouchableOpacity style={styles.plusBtn} onPress={handlePlus} activeOpacity={0.85}>
            <Text style={styles.plusBtnText}>Probar gratis 14 días</Text>
          </TouchableOpacity>
        </View>

        {/* Free card */}
        <View style={[styles.plusCard, { backgroundColor: Colors.freeTier }]}>
          <View style={styles.blobTopRight} />
          <View style={styles.blobBottomLeft} />

          <View style={styles.plusTop}>
            <View style={styles.recommendedBadge}>
              <Text style={styles.recommendedText}>PLAN BASE</Text>
            </View>
            <Text style={styles.plusTitle}>Wavii Free</Text>
            <Text style={styles.plusPrice}>Empieza gratis con lo esencial</Text>
            <View style={styles.featuresList}>
              {FREE_FEATURES.map((feature) => (
                <View key={feature} style={styles.featureRow}>
                  <Ionicons name="checkmark-circle-outline" size={15} color="rgba(255,255,255,0.9)" />
                  <Text style={styles.featureText}>{feature}</Text>
                </View>
              ))}
            </View>
          </View>

          <TouchableOpacity style={styles.plusBtn} onPress={handleFree} activeOpacity={0.85}>
            <Text style={[styles.plusBtnText, { color: Colors.freeTier }]}>Continuar gratis</Text>
          </TouchableOpacity>
        </View>

        {/* Guest link */}
        <TouchableOpacity style={styles.guestLink} onPress={handleGuest} activeOpacity={0.7}>
          <Text style={[styles.guestText, { color: colors.textSecondary }]}>
            Entrar como invitado (sin guardar progreso)
          </Text>
        </TouchableOpacity>
      </ScrollView>
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
    flexGrow: 1,
    paddingHorizontal: Spacing.xl,
    paddingBottom: Spacing.xl,
    gap: Spacing.base,
  },
  speechSection: {
    paddingTop: Spacing.xs,
    marginBottom: -25,
  },

  /* ── Plus card ── */
  plusCard: {
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.xl,
    padding: Spacing.lg,
    overflow: 'hidden',
    gap: 6,
  },
  plusTop: {
    gap: 6,
  },
  blobTopRight: {
    position: 'absolute',
    top: -40,
    right: -40,
    width: 140,
    height: 140,
    borderRadius: 70,
    backgroundColor: 'rgba(255,255,255,0.12)',
  },
  blobBottomLeft: {
    position: 'absolute',
    bottom: -30,
    left: -30,
    width: 100,
    height: 100,
    borderRadius: 50,
    backgroundColor: 'rgba(0,0,0,0.06)',
  },
  recommendedBadge: {
    alignSelf: 'flex-start',
    backgroundColor: 'rgba(255,255,255,0.22)',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 3,
    borderRadius: BorderRadius.sm,
  },
  recommendedText: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xs,
    color: Colors.white,
    letterSpacing: 0.6,
  },
  plusTitle: {
    fontFamily: FontFamily.black,
    fontSize: FontSize['2xl'],
    color: Colors.white,
  },
  plusPrice: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    color: 'rgba(255,255,255,0.88)',
  },
  featuresList: {
    gap: 4,
  },
  featureRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  featureText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    color: 'rgba(255,255,255,0.88)',
  },
  plusBtn: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: Spacing.xs,
  },
  plusBtnText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    color: Colors.primary,
  },

  /* ── Free card ── */
  freeCard: {
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    overflow: 'hidden',
    gap: Spacing.base,
  },
  freeHero: {
    padding: Spacing.xl,
    gap: Spacing.sm,
  },
  freeBlobTopRight: {
    position: 'absolute',
    top: -26,
    right: -22,
    width: 108,
    height: 108,
    borderRadius: 54,
    backgroundColor: Colors.primaryOpacity10,
  },
  freeBlobBottomLeft: {
    position: 'absolute',
    bottom: -24,
    left: -18,
    width: 84,
    height: 84,
    borderRadius: 42,
    backgroundColor: 'rgba(255,122,0,0.08)',
  },
  freeBadge: {
    alignSelf: 'flex-start',
    backgroundColor: Colors.primaryOpacity10,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 3,
    borderRadius: BorderRadius.sm,
  },
  freeBadgeText: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xs,
    color: Colors.primary,
  },
  freeTitle: {
    fontFamily: FontFamily.black,
    fontSize: FontSize['2xl'],
    color: Colors.textPrimary,
  },
  freeDesc: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    color: Colors.textSecondary,
    lineHeight: 20,
  },
  freeFeatures: {
    gap: 5,
  },
  freeFeatureRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  freeFeatureText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    color: Colors.textPrimary,
  },
  freeBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    backgroundColor: Colors.primaryOpacity10,
    borderRadius: BorderRadius.lg,
    height: 48,
  },
  freeBtnText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    color: Colors.primary,
  },

  /* ── Guest link ── */
  guestLink: {
    alignItems: 'center',
  },
  guestText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    textDecorationLine: 'underline',
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});
