import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  Pressable,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useFocusEffect } from '@react-navigation/native';
import { isBackendSessionToken } from '../../auth/session';
import { GuestBlockedView } from '../../components/common/GuestBlockedView';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import {
  apiFetchTodayChallenges,
  apiCompleteChallenge,
  DailyChallengeDto,
} from '../../api/challengeApi';

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

function iconForDifficulty(difficulty: DailyChallengeDto['difficulty']): {
  iconName: IoniconsName;
  iconColor: string;
  iconBg: string;
  color: string;
  label: string;
} {
  switch (difficulty) {
    case 'PRINCIPIANTE':
      return { iconName: 'musical-note', iconColor: Colors.success, iconBg: '#E8F5E9', color: Colors.success, label: 'Principiante' };
    case 'INTERMEDIO':
      return { iconName: 'musical-notes', iconColor: Colors.warning, iconBg: '#FFF3E0', color: Colors.warning, label: 'Intermedio' };
    case 'AVANZADO':
      return { iconName: 'star', iconColor: Colors.primary, iconBg: '#E3F2FD', color: Colors.primary, label: 'Avanzado' };
  }
}

function getTimeUntilNextReset(): string {
  const now = new Date();
  const nextReset = new Date(now);
  nextReset.setHours(24, 0, 0, 0);
  const diffMs = Math.max(0, nextReset.getTime() - now.getTime());
  const totalSeconds = Math.floor(diffMs / 1000);
  const h = Math.floor(totalSeconds / 3600);
  const m = Math.floor((totalSeconds % 3600) / 60);
  const s = totalSeconds % 60;
  return [h, m, s].map((v) => String(v).padStart(2, '0')).join(':');
}

export const ChallengesScreen = () => {
  const { token, updateUser } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const navigation = useNavigation<any>();
  const hasBackendSession = isBackendSessionToken(token);

  const [challenges, setChallenges] = useState<DailyChallengeDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [completingId, setCompletingId] = useState<number | null>(null);
  const [resetCountdown, setResetCountdown] = useState(getTimeUntilNextReset());

  const loadChallenges = useCallback(async () => {
    if (!hasBackendSession) {
      setChallenges([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await apiFetchTodayChallenges();
      setChallenges(data);
    } catch {
      showAlert({ title: 'Error', message: 'No se pudieron cargar los desafios. Intentalo de nuevo.' });
    } finally {
      setLoading(false);
    }
  }, [hasBackendSession]);

  useFocusEffect(
    useCallback(() => {
      loadChallenges();

      setResetCountdown(getTimeUntilNextReset());
      const timer = setInterval(() => setResetCountdown(getTimeUntilNextReset()), 1000);
      return () => clearInterval(timer);
    }, [loadChallenges]),
  );

  const handleComplete = async (challengeId: number) => {
    if (completingId !== null) return;
    setCompletingId(challengeId);
    try {
      const result = await apiCompleteChallenge(challengeId);
      updateUser({ xp: result.totalXp, streak: result.streak, bestStreak: result.bestStreak });
      await loadChallenges();
      if (result.leveledUp) {
        showAlert({ title: '¡Subiste de nivel!', message: `Ahora eres nivel ${result.newLevel}. Sigue practicando.` });
      }
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo completar el desafio. Intentalo de nuevo.' });
    } finally {
      setCompletingId(null);
    }
  };

  const confirmComplete = (challengeId: number) => {
    showAlert({
      title: 'Completar desafío',
      message: '¿Seguro que quieres marcar este desafío como hecho?',
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Confirmar', style: 'destructive', delaySeconds: 10, onPress: () => handleComplete(challengeId) },
      ],
    });
  };

  const completedCount = challenges.filter((c) => c.completedByMe).length;

  if (!hasBackendSession) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
        <View style={[styles.header, { borderBottomColor: colors.border }]}>
          <Pressable onPress={() => navigation.goBack()} style={styles.backBtn} hitSlop={12}>
            <Ionicons name="chevron-back" size={26} color={colors.text} />
          </Pressable>
          <Text style={[styles.headerTitle, { color: colors.text }]}>Desafios Diarios</Text>
          <View style={styles.headerSpacer} />
        </View>
        <GuestBlockedView feature="para ver y completar los desafios diarios" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Pressable onPress={() => navigation.goBack()} style={styles.backBtn} hitSlop={12}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.text }]}>Desafios Diarios</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scroll}
      >
        {/* Summary card */}
        <View style={[styles.summaryCard, { backgroundColor: colors.surface }]}>
          <View style={styles.summaryRow}>
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryValue, { color: colors.text }]}>{completedCount}</Text>
              <Text style={[styles.summaryLabel, { color: colors.textSecondary }]}>Completados</Text>
            </View>
            <View style={[styles.summaryDivider, { backgroundColor: colors.border }]} />
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryValue, { color: colors.text }]}>{challenges.length - completedCount}</Text>
              <Text style={[styles.summaryLabel, { color: colors.textSecondary }]}>Pendientes</Text>
            </View>
            <View style={[styles.summaryDivider, { backgroundColor: colors.border }]} />
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryValue, { color: colors.text }]}>{challenges.length}</Text>
              <Text style={[styles.summaryLabel, { color: colors.textSecondary }]}>Total</Text>
            </View>
          </View>
          <View style={styles.resetRow}>
            <Ionicons name="time-outline" size={14} color={colors.textSecondary} />
            <Text style={[styles.resetText, { color: colors.textSecondary }]}>
              Se reinician en {resetCountdown}
            </Text>
          </View>
        </View>

        {/* Wavii hint card */}
        <View style={[styles.waviiCard, { backgroundColor: colors.surface }]}>
          <Image
            source={require('../../../assets/wavii/wavii_bienvenida.png')}
            style={styles.waviiImage}
            resizeMode="contain"
          />
          <View style={styles.waviiBody}>
            <Text style={[styles.waviiTitle, { color: colors.text }]}>
              {completedCount === 0
                ? '¡A por ello!'
                : completedCount === challenges.length
                  ? '¡Todo completado! 🎉'
                  : `¡Vas genial! ${challenges.length - completedCount} más`}
            </Text>
            <Text style={[styles.waviiSub, { color: colors.textSecondary }]}>
              {completedCount === 0
                ? 'Completa tus desafíos de hoy para ganar XP y mantener la racha.'
                : completedCount === challenges.length
                  ? 'Has completado todos los desafíos de hoy. Vuelve mañana.'
                  : 'Cada desafío que completes suma XP y alarga tu racha.'}
            </Text>
          </View>
        </View>

        {/* Challenges list */}
        {loading ? (
          <ActivityIndicator color={Colors.primary} style={styles.loader} />
        ) : challenges.length === 0 ? (
          <View style={[styles.emptyCard, { backgroundColor: colors.surface }]}>
            <Ionicons name="musical-notes-outline" size={48} color={colors.textSecondary} />
            <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
              No hay desafios disponibles hoy.{'\n'}Vuelve mañana para nuevos retos.
            </Text>
          </View>
        ) : (
          challenges.map((challenge) => (
            <ChallengeCard
              key={challenge.id}
              challenge={challenge}
              colors={colors}
              completing={completingId === challenge.id}
              onOpen={() => navigation.navigate('PdfViewer', {
                pdfId: challenge.tabId,
                title: challenge.tabTitle ?? 'Tablatura',
              })}
              onComplete={() => confirmComplete(challenge.id)}
            />
          ))
        )}
      </ScrollView>
    </SafeAreaView>
  );
};

const ChallengeCard = ({
  challenge,
  colors,
  completing,
  onOpen,
  onComplete,
}: {
  challenge: DailyChallengeDto;
  colors: any;
  completing: boolean;
  onOpen: () => void;
  onComplete: () => void;
}) => {
  const { iconName, iconColor, iconBg, color, label } = iconForDifficulty(challenge.difficulty);

  return (
    <View style={[styles.card, { backgroundColor: colors.surface }]}>
      <TouchableOpacity style={styles.cardInfo} activeOpacity={0.85} onPress={onOpen}>
      {challenge.tabCoverImageUrl ? (
        <Image source={{ uri: challenge.tabCoverImageUrl }} style={[styles.cardIcon, { borderRadius: BorderRadius.md }]} resizeMode="cover" />
      ) : (
        <View style={[styles.cardIcon, { backgroundColor: iconBg }]}>
          <Ionicons name={iconName} size={24} color={iconColor} />
        </View>
      )}
      <View style={styles.cardBody}>
        <Text style={[styles.cardTitle, { color: colors.text }]} numberOfLines={1}>
          {challenge.tabTitle ?? 'Tablatura sin titulo'}
        </Text>
        <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
          {label} · por {challenge.tabOwnerName ?? 'comunidad'}
        </Text>
      </View>
      </TouchableOpacity>
      <View style={styles.cardRight}>
        {challenge.completedByMe ? (
          <Ionicons name="checkmark-circle" size={28} color={Colors.success} />
        ) : completing ? (
          <ActivityIndicator size="small" color={Colors.primary} />
        ) : (
          <TouchableOpacity
            onPress={onComplete}
            style={[styles.doneBtn, { backgroundColor: color }]}
            activeOpacity={0.8}
          >
            <Text style={styles.doneBtnText}>Hecho</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.xpText}>+{challenge.xpReward} XP</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 10,
    borderBottomWidth: 1,
    gap: Spacing.xs,
  },
  backBtn: { padding: Spacing.xs },
  headerTitle: {
    flex: 1,
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    textAlign: 'center',
  },
  headerSpacer: { width: 34 },

  scroll: {
    padding: Spacing.base,
    gap: Spacing.sm,
  },

  summaryCard: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  summaryRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  summaryItem: { flex: 1, alignItems: 'center', gap: 2 },
  summaryValue: { fontFamily: FontFamily.black, fontSize: FontSize['2xl'] },
  summaryLabel: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  summaryDivider: { width: 1, height: 36 },
  resetRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
  },
  resetText: { fontFamily: FontFamily.medium, fontSize: FontSize.xs },

  loader: { marginTop: Spacing.xl },

  emptyCard: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    alignItems: 'center',
    gap: Spacing.sm,
    marginTop: Spacing.sm,
  },
  emptyText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 22,
  },

  card: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    gap: Spacing.sm,
  },
  cardInfo: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  cardIcon: {
    width: 52,
    height: 52,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardBody: { flex: 1 },
  cardTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, marginBottom: 2 },
  cardMeta: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },

  cardRight: { alignItems: 'center', gap: 4, minWidth: 64 },
  doneBtn: {
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 5,
  },
  doneBtnText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs, color: Colors.white },
  xpText: { fontFamily: FontFamily.extraBold, fontSize: FontSize.sm, color: Colors.primary },

  waviiCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
  },
  waviiImage: {
    width: 56,
    height: 56,
    flexShrink: 0,
  },
  waviiBody: { flex: 1, gap: 3 },
  waviiTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  waviiSub: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 17 },
});
