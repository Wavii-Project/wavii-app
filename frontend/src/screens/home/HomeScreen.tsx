import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Image,
  Modal,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useNavigation, useFocusEffect } from '@react-navigation/native';
import { isBackendSessionToken } from '../../auth/session';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { NotificationBell } from '../../components/common/NotificationBell';
import {
  apiFetchTodayChallenges,
  apiCompleteChallenge,
  DailyChallengeDto,
} from '../../api/challengeApi';
import { apiFetchNews, formatNewsDate, NewsArticle } from '../../api/newsApi';
import { apiFetchBulletin, BulletinTeacher } from '../../api/bulletinApi';
import { apiFetchPublicPdfs, PdfDocument } from '../../api/pdfApi';
type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

const WAVII_IMAGE = require('../../../assets/wavii/wavii_bienvenida.png');

const WEEKLY_LABELS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

/** Devuelve las fechas ISO de los 7 dias de la semana actual (lunes a domingo) */
function getCurrentWeekDates(): string[] {
  const today = new Date();
  const dayOfWeek = today.getDay(); // 0=dom, 1=lun...
  const mondayOffset = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(today);
    d.setDate(today.getDate() + mondayOffset + i);
    return d.toISOString().slice(0, 10); // "YYYY-MM-DD"
  });
}

/** Icono e iconBg segun la dificultad del desafio */
function iconForDifficulty(difficulty: DailyChallengeDto['difficulty']): {
  iconName: IoniconsName;
  iconColor: string;
  iconBg: string;
  color: string;
} {
  switch (difficulty) {
    case 'PRINCIPIANTE':
      return { iconName: 'musical-note', iconColor: Colors.success, iconBg: '#E8F5E9', color: Colors.success };
    case 'INTERMEDIO':
      return { iconName: 'musical-notes', iconColor: Colors.warning, iconBg: '#FFF3E0', color: Colors.warning };
    case 'AVANZADO':
      return { iconName: 'star', iconColor: Colors.primary, iconBg: '#E3F2FD', color: Colors.primary };
  }
}

/** Calcula el nivel numerico a partir del XP total. Misma formula que el backend. */
function calculateLevelFromXp(xp: number): number {
  let level = 1;
  let accumulated = 0;
  while (true) {
    const needed = Math.round(100 * Math.pow(level, 1.5));
    if (accumulated + needed > xp) break;
    accumulated += needed;
    level++;
  }
  return level;
}

/** XP total acumulado para alcanzar el nivel indicado (umbral inferior del nivel) */
function xpThresholdForLevel(targetLevel: number): number {
  let total = 0;
  for (let l = 1; l < targetLevel; l++) {
    total += Math.round(100 * Math.pow(l, 1.5));
  }
  return total;
}

function getTimeUntilNextReset(): string {
  const now = new Date();
  const nextReset = new Date(now);
  nextReset.setHours(24, 0, 0, 0);

  const diffMs = Math.max(0, nextReset.getTime() - now.getTime());
  const totalSeconds = Math.floor(diffMs / 1000);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':');
}

export const HomeScreen = () => {
  const { user, token, updateUser, isNewRegistration, clearNewRegistration } = useAuth();
  const { colors, isDark } = useTheme();
  const { showAlert } = useAlert();
  const navigation = useNavigation<any>();
  const hasBackendSession = isBackendSessionToken(token);

  const [avatar, setAvatar] = useState<string | null>(null);
  const [profileModalVisible, setProfileModalVisible] = useState(false);
  const [challenges, setChallenges] = useState<DailyChallengeDto[]>([]);
  const [loadingChallenges, setLoadingChallenges] = useState(true);
  const [completingId, setCompletingId] = useState<number | null>(null);
  const [resetCountdown, setResetCountdown] = useState(getTimeUntilNextReset());

  // Dias completados esta semana
  const [weekCompletedDates, setWeekCompletedDates] = useState<string[]>([]);

  // Noticias
  const [news, setNews] = useState<NewsArticle[]>([]);
  const [loadingNews, setLoadingNews] = useState(true);
  const [teachers, setTeachers] = useState<BulletinTeacher[]>([]);
  const [loadingTeachers, setLoadingTeachers] = useState(true);
  const [popularTabs, setPopularTabs] = useState<PdfDocument[]>([]);
  const [loadingPopular, setLoadingPopular] = useState(true);

  const avatarKey = user ? `wavii_avatar_${user.id}` : null;

  // Flujo post-registro: navegar a suscripciones en modo onboarding
  useEffect(() => {
    if (isNewRegistration) {
      clearNewRegistration();
      navigation.navigate('Subscription', { isOnboarding: true });
    }
  }, [isNewRegistration]);

  useEffect(() => {
    if (!avatarKey) return;
    AsyncStorage.getItem(avatarKey).then((v) => { if (v) setAvatar(v); }).catch(() => { });
  }, [avatarKey]);

  const loadChallenges = useCallback(async () => {
    if (!hasBackendSession || !token) {
      setChallenges([]);
      setLoadingChallenges(false);
      return;
    }
    setLoadingChallenges(true);
    try {
      const data = await apiFetchTodayChallenges();
      setChallenges(data);

      // Calculamos dias completados esta semana a partir de las completions
      // Para simplicidad, marcamos hoy si hay al menos uno completado hoy
      const todayIso = new Date().toISOString().slice(0, 10);
      const anyCompletedToday = data.some((c) => c.completedByMe);
      if (anyCompletedToday) {
        setWeekCompletedDates((prev) =>
          prev.includes(todayIso) ? prev : [...prev, todayIso],
        );
      }
    } catch (e) {
      console.warn('Error cargando desafios:', e);
    } finally {
      setLoadingChallenges(false);
    }
  }, [hasBackendSession, token]);

  useFocusEffect(
    useCallback(() => {
      loadChallenges();
    }, [loadChallenges])
  );

  useEffect(() => {
    setResetCountdown(getTimeUntilNextReset());
    const timer = setInterval(() => {
      setResetCountdown(getTimeUntilNextReset());
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    apiFetchNews({ size: 4 })
      .then(setNews)
      .catch(() => { })
      .finally(() => setLoadingNews(false));
  }, []);

  useEffect(() => {
    if (!token) {
      setLoadingTeachers(false);
      return;
    }
    setLoadingTeachers(true);
    apiFetchBulletin(token)
      .then((response) => setTeachers(response.teachers.slice(0, 6)))
      .catch(() => setTeachers([]))
      .finally(() => setLoadingTeachers(false));
  }, [token]);

  useEffect(() => {
    setLoadingPopular(true);
    apiFetchPublicPdfs({ sort: 'MOST_LIKED' }, token ?? undefined)
      .then((tabs) => setPopularTabs(tabs.slice(0, 3)))
      .catch(() => setPopularTabs([]))
      .finally(() => setLoadingPopular(false));
  }, [token]);

  const handleCompleteChallenge = async (challengeId: number) => {
    if (!token || completingId !== null) return;
    setCompletingId(challengeId);
    try {
      const result = await apiCompleteChallenge(challengeId);
      // Actualizar estado local del usuario
      updateUser({ xp: result.totalXp, streak: result.streak, bestStreak: result.bestStreak });
      // Refrescar lista de desafios
      await loadChallenges();
      if (result.leveledUp) {
        showAlert({ title: '¡Subiste de nivel!', message: `Ahora eres nivel ${result.newLevel}. Sigue practicando.` });
      }
    } catch (e) {
      showAlert({ title: 'Error', message: 'No se pudo completar el desafio. Intentalo de nuevo.' });
    } finally {
      setCompletingId(null);
    }
  };

  const completedChallenges = challenges.filter((c) => c.completedByMe).length;
  const pendingChallenges = challenges.length - completedChallenges;
  const weekDates = getCurrentWeekDates();
  const completedDays = weekDates.filter((d) => weekCompletedDates.includes(d)).length;

  // Calcular nivel y XP para la barra
  const currentXp = user?.xp ?? 0;
  const currentLevel = calculateLevelFromXp(currentXp);
  const xpForCurrentLevel = xpThresholdForLevel(currentLevel);
  const xpForNextLevel = xpThresholdForLevel(currentLevel + 1);
  const xpProgress = xpForNextLevel > xpForCurrentLevel
    ? (currentXp - xpForCurrentLevel) / (xpForNextLevel - xpForCurrentLevel)
    : 1;

  const firstName = user?.name?.split(' ')[0] ?? 'Alex';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={[styles.scroll, { paddingBottom: 16 }]}
      >

        {/* ── Cabecera ── */}
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => user && navigation.navigate('UserProfile', { userId: user.id })}
            activeOpacity={0.8}
          >
            {avatar ? (
              <Image source={{ uri: avatar }} style={styles.avatar} />
            ) : (
              <View style={[styles.avatarFallback, { backgroundColor: Colors.primaryLight }]}>
                <Text style={styles.avatarText}>
                  {user?.name?.charAt(0).toUpperCase() ?? 'U'}
                </Text>
              </View>
            )}
          </TouchableOpacity>

          <View style={styles.headerCenter}>
            <Text style={[styles.greeting, { color: colors.text }]}>
              ¡Hola, {firstName}!
            </Text>
            <Text style={[styles.levelLabel, { color: Colors.primary }]}>
              {user?.level ? user.level.charAt(0).toUpperCase() + user.level.slice(1).toLowerCase() : 'Principiante'}
            </Text>
          </View>

          <View style={styles.headerAside}>
            <View style={[styles.streakPill, { backgroundColor: colors.surface, borderColor: colors.border }]}>
              <Ionicons name="flame" size={18} color={Colors.streakOrange} />
              <Text style={[styles.streakValue, { color: colors.text }]}>{user?.streak ?? 5}</Text>
            </View>
            <NotificationBell size="sm" />
          </View>
        </View>

        {/* ── Banner motivación Wavii (Daily Objectives) ── */}
        <View style={[styles.motivationBanner, { backgroundColor: isDark ? Colors.primaryDark : '#FFF3E0', marginTop: 16, marginBottom: Spacing.sm }]}>
          <Image source={WAVII_IMAGE} style={styles.motImage} resizeMode="contain" />
          <View style={{ flex: 1, marginLeft: Spacing.sm }}>
            <Text style={[styles.motTitle, { color: colors.text }]}>¡Sigue así!</Text>
            <Text style={[styles.motSubtitle, { color: colors.textSecondary }]}>
              {pendingChallenges > 0
                ? `Te quedan ${pendingChallenges} desafío${pendingChallenges > 1 ? 's' : ''} diarios por completar.`
                : '¡Has completado todos tus desafíos de hoy!'}
            </Text>
          </View>
          <TouchableOpacity
            style={styles.motBtn}
            disabled={pendingChallenges === 0}
            onPress={() => navigation.navigate('Challenges')}
          >
            <Text style={styles.motBtnText}>{pendingChallenges > 0 ? 'Ver' : '¡Genial!'}</Text>
          </TouchableOpacity>
        </View>

        {/* ── Barra XP ── */}
        <View style={styles.xpBarRow}>
          <View style={[styles.xpBarTrack, { backgroundColor: colors.border }]}>
            <View style={[styles.xpBarFill, { width: `${xpProgress * 100}%` }]} />
          </View>
          <Text style={[styles.xpBarMeta, { color: colors.textSecondary }]}>
            {currentXp.toLocaleString()} / {xpForNextLevel.toLocaleString()} XP · Nv.{currentLevel}
          </Text>
        </View>

        {/* ── Progreso Semanal ── */}
        <View style={[styles.sectionHeader, { marginTop: 8 }]}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>Tu progreso semanal</Text>
          <TouchableOpacity onPress={() => navigation.navigate('Stats')}>
            <Text style={styles.linkText}>Ver estadisticas</Text>
          </TouchableOpacity>
        </View>

        <View style={[styles.weeklyCard, { backgroundColor: colors.surface }]}>
          <View style={styles.weekRow}>
            {WEEKLY_LABELS.map((day, i) => {
              const isDone = weekCompletedDates.includes(weekDates[i]);
              return (
                <View key={day} style={styles.dayColumn}>
                  <Text style={[styles.dayLabel, { color: colors.textSecondary }]}>{day}</Text>
                  {isDone ? (
                    <Ionicons name="checkmark-circle" size={24} color={Colors.success} />
                  ) : (
                    <Ionicons name="ellipse-outline" size={24} color={Colors.border} />
                  )}
                </View>
              );
            })}
          </View>
          <Text style={[styles.weekSummary, { color: colors.textSecondary }]}>
            <Text style={{ color: colors.text, fontFamily: FontFamily.bold }}>
              {completedDays}/7
            </Text>{' '}
            dias completados esta semana
          </Text>
        </View>

        {/* ── Desafíos Diarios (solo 2) ── */}
        <View style={styles.sectionHeader}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>Desafios diarios</Text>
          <View style={styles.resetBadge}>
            <Ionicons name="calendar-outline" size={14} color={Colors.primary} />
            <Text style={[styles.resetText, { color: Colors.primary }]}>Se reinician en {resetCountdown}</Text>
          </View>
        </View>

        {loadingChallenges ? (
          <ActivityIndicator color={Colors.primary} style={{ marginVertical: Spacing.base }} />
        ) : challenges.length === 0 ? (
          <View style={[styles.challengeCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.challengeDesc, { color: colors.textSecondary, textAlign: 'center', flex: 1 }]}>
              No hay desafios disponibles hoy. Vuelve pronto.
            </Text>
          </View>
        ) : (
          challenges.slice(0, 2).map((challenge) => (
            <ChallengeCard
              key={challenge.id}
              challenge={challenge}
              colors={colors}
              completing={completingId === challenge.id}
              onOpen={() => navigation.navigate('PdfViewer', {
                pdfId: challenge.tabId,
                title: challenge.tabTitle ?? 'Tablatura',
              })}
              onComplete={() => handleCompleteChallenge(challenge.id)}
            />
          ))
        )}

        {challenges.length > 2 && (
          <TouchableOpacity
            style={[styles.viewAllBtn, { backgroundColor: colors.surface }]}
            onPress={() => navigation.navigate('Challenges')}
          >
            <Ionicons name="trophy-outline" size={20} color={colors.text} />
            <Text style={[styles.viewAllText, { color: colors.text }]}>
              Ver los {challenges.length} desafios
            </Text>
            <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
          </TouchableOpacity>
        )}

        {/* ── Últimas Tablaturas ── */}
        <View style={styles.sectionHeader}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>Tablaturas populares</Text>
          <TouchableOpacity onPress={() => navigation.navigate('Tabs')}>
            <Text style={styles.linkText}>Ver más</Text>
          </TouchableOpacity>
        </View>

        {loadingPopular ? (
          <ActivityIndicator color={Colors.primary} style={{ marginVertical: Spacing.base }} />
        ) : popularTabs.length === 0 ? (
          <View style={[styles.tabCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.tabCardArtist, { color: colors.textSecondary }]}>Aun no hay tablaturas populares.</Text>
          </View>
        ) : (
          popularTabs.map((tab) => (
            <TabCard
              key={tab.id}
              tab={tab}
              colors={colors}
              onPress={() => navigation.navigate('PdfViewer', { pdfId: tab.id, title: tab.songTitle ?? tab.originalName })}
            />
          ))
        )}

        {/* ── Profesores cerca de ti ── */}
        <View style={styles.sectionHeader}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>Profesores cerca de ti</Text>
          <TouchableOpacity onPress={() => navigation.navigate('BulletinBoard')}>
            <Text style={styles.linkText}>Ver todos</Text>
          </TouchableOpacity>
        </View>

        {loadingTeachers ? (
          <ActivityIndicator color={Colors.primary} style={{ marginVertical: Spacing.base }} />
        ) : (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.carouselContainer}
            contentContainerStyle={styles.teachersScroll}
          >
            {teachers.map((t) => (
              <TeacherCard
                key={t.id}
                teacher={t}
                colors={colors}
                onPress={() => navigation.navigate('TeacherProfile', { teacherId: t.id })}
              />
            ))}
          </ScrollView>
        )}

        {/* ── Actualidad musical ── */}
        <View style={styles.sectionHeader}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>Actualidad musical</Text>
          <TouchableOpacity onPress={() => navigation.navigate('News')}>
            <Text style={styles.linkText}>Ver más</Text>
          </TouchableOpacity>
        </View>

        {loadingNews ? (
          <ActivityIndicator color={Colors.primary} style={{ marginVertical: Spacing.base }} />
        ) : news.length === 0 ? null : (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            style={styles.carouselContainer}
            contentContainerStyle={styles.newsScroll}
          >
            {news.map((article) => (
              <NewsCard key={article.articleId} article={article} colors={colors} isDark={isDark} />
            ))}
          </ScrollView>
        )}


      </ScrollView>

      {/* ── Modal perfil público ── */}
      <Modal
        visible={profileModalVisible}
        transparent
        animationType="fade"
        onRequestClose={() => setProfileModalVisible(false)}
      >
        <TouchableOpacity
          style={styles.profileOverlay}
          activeOpacity={1}
          onPress={() => setProfileModalVisible(false)}
        >
          <View
            style={[styles.profileModal, { backgroundColor: colors.surface }]}
            onStartShouldSetResponder={() => true}
          >
            <TouchableOpacity style={styles.profileModalClose} onPress={() => setProfileModalVisible(false)}>
              <Ionicons name="close" size={22} color={colors.textSecondary} />
            </TouchableOpacity>

            {avatar ? (
              <Image source={{ uri: avatar }} style={styles.profileModalAvatar} />
            ) : (
              <View style={[styles.profileModalAvatarFallback, { backgroundColor: Colors.primaryLight }]}>
                <Text style={styles.profileModalAvatarText}>
                  {user?.name?.charAt(0).toUpperCase() ?? 'U'}
                </Text>
              </View>
            )}

            <Text style={[styles.profileModalName, { color: colors.text }]}>{user?.name}</Text>
            <Text style={[styles.profileModalLevel, { color: Colors.primary }]}>
              {user?.level ?? 'Principiante'}
            </Text>
            <Text style={[styles.profileModalRole, { color: colors.textSecondary }]}>
              {user?.role === 'profesor_certificado'
                ? 'Profesor Certificado'
                : user?.role === 'profesor_particular'
                  ? 'Profesor Particular'
                  : 'Estudiante'}
            </Text>

            <View style={styles.profileModalStats}>
              <View style={styles.profileModalStat}>
                <Ionicons name="flame" size={22} color={Colors.streakOrange} />
                <Text style={[styles.profileModalStatValue, { color: colors.text }]}>
                  {user?.streak ?? 0}
                </Text>
                <Text style={[styles.profileModalStatLabel, { color: colors.textSecondary }]}>Racha</Text>
              </View>
              <View style={[styles.profileModalDivider, { backgroundColor: colors.border }]} />
              <View style={styles.profileModalStat}>
                <Ionicons name="star" size={22} color={Colors.warning} />
                <Text style={[styles.profileModalStatValue, { color: colors.text }]}>
                  {user?.xp?.toLocaleString() ?? '0'}
                </Text>
                <Text style={[styles.profileModalStatLabel, { color: colors.textSecondary }]}>XP</Text>
              </View>
            </View>
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
};

// ── Subcomponentes ──

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
  const { iconName, iconColor, iconBg, color } = iconForDifficulty(challenge.difficulty);
  const difficultyLabel = challenge.difficulty === 'PRINCIPIANTE'
    ? 'Principiante' : challenge.difficulty === 'INTERMEDIO' ? 'Intermedio' : 'Avanzado';

  return (
    <TouchableOpacity
      style={[styles.challengeCard, { backgroundColor: colors.surface }]}
      activeOpacity={0.85}
      onPress={onOpen}
    >
      {challenge.tabCoverImageUrl ? (
        <Image source={{ uri: challenge.tabCoverImageUrl }} style={[styles.challengeIcon, { borderRadius: BorderRadius.md }]} resizeMode="cover" />
      ) : (
        <View style={[styles.challengeIcon, { backgroundColor: iconBg }]}>
          <Ionicons name={iconName} size={22} color={iconColor} />
        </View>
      )}
      <View style={styles.challengeBody}>
        <Text style={[styles.challengeTitle, { color: colors.text }]} numberOfLines={1}>
          {challenge.tabTitle ?? 'Tablatura sin titulo'}
        </Text>
        <Text style={[styles.challengeDesc, { color: colors.textSecondary }]}>
          {difficultyLabel} · por {challenge.tabOwnerName ?? 'comunidad'}
        </Text>
      </View>
      <View style={styles.challengeRight}>
        {challenge.completedByMe ? (
          <Ionicons name="checkmark-circle" size={26} color={Colors.success} />
        ) : completing ? (
          <ActivityIndicator size="small" color={Colors.primary} />
        ) : (
          <TouchableOpacity
            onPress={onComplete}
            style={[styles.completeBtn, { backgroundColor: color }]}
            activeOpacity={0.8}
          >
            <Text style={styles.completeBtnText}>Hecho</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.xpText}>+{challenge.xpReward} XP</Text>
      </View>
    </TouchableOpacity>
  );
};

const TabCard = ({
  tab,
  colors,
  onPress,
}: {
  tab: PdfDocument;
  colors: any;
  onPress: () => void;
}) => (
  <TouchableOpacity style={[styles.tabCard, { backgroundColor: colors.surface }]} activeOpacity={0.8} onPress={onPress}>
    {tab.coverImageUrl ? (
      <Image source={{ uri: tab.coverImageUrl }} style={styles.tabCardCover} resizeMode="cover" />
    ) : (
      <View style={[styles.tabCardIcon, { backgroundColor: Colors.primaryOpacity10 }]}>
        <Ionicons name="musical-note" size={24} color={Colors.primary} />
      </View>
    )}
    <View style={styles.tabCardBody}>
      <Text style={[styles.tabCardTitle, { color: colors.text }]} numberOfLines={1}>
        {tab.songTitle ?? 'Tablatura'}
      </Text>
      <Text style={[styles.tabCardArtist, { color: colors.textSecondary }]} numberOfLines={1}>
        {tab.ownerName ?? 'Comunidad'}
      </Text>
    </View>
    <Ionicons name="chevron-forward" size={18} color={colors.textSecondary} />
  </TouchableOpacity>
);

const TeacherCard = ({
  teacher,
  colors,
  onPress,
}: {
  teacher: BulletinTeacher;
  colors: any;
  onPress: () => void;
}) => (
  <TouchableOpacity style={[styles.teacherCard, { backgroundColor: colors.surface }]} onPress={onPress} activeOpacity={0.85}>
    <View style={[styles.teacherAvatar, { backgroundColor: Colors.primaryLight }]}>
      <Text style={styles.teacherAvatarText}>{teacher.name.charAt(0)}</Text>
    </View>
    <Text style={[styles.teacherName, { color: colors.text }]} numberOfLines={1}>{teacher.name}</Text>
    <Text style={[styles.teacherInstrument, { color: colors.textSecondary }]}>{teacher.instrument}</Text>
    <View style={styles.teacherBadge}>
      {teacher.role === 'profesor_certificado' ? (
        <>
          <Ionicons name="shield-checkmark" size={12} color={Colors.primary} />
          <Text style={[styles.teacherBadgeText, { color: Colors.primary }]}>Certificado</Text>
        </>
      ) : (
        <>
          <Ionicons name="person" size={12} color={Colors.grayLight} />
          <Text style={[styles.teacherBadgeText, { color: Colors.grayLight }]}>Particular</Text>
        </>
      )}
    </View>
    <Text style={[styles.teacherPrice, { color: colors.text }]}>{teacher.pricePerHour != null ? `${teacher.pricePerHour}€/h` : 'Gratis'}</Text>
    <View style={styles.contactBtn}>
      <Text style={styles.contactBtnText}>Ver perfil</Text>
    </View>
  </TouchableOpacity>
);

const NewsCard = ({
  article,
  colors,
  isDark,
}: {
  article: NewsArticle;
  colors: any;
  isDark: boolean;
}) => {
  const handlePress = () => {
    if (article.url) {
      require('react-native').Linking.openURL(article.url).catch(() => { });
    }
  };

  return (
    <TouchableOpacity
      style={[styles.newsCard, { backgroundColor: colors.surface }]}
      activeOpacity={0.8}
      onPress={handlePress}
    >
      {article.imageUrl ? (
        <Image source={{ uri: article.imageUrl }} style={styles.newsImg} resizeMode="cover" />
      ) : (
        <View style={[styles.newsImg, { backgroundColor: isDark ? '#2a2a2a' : Colors.grayLight, alignItems: 'center', justifyContent: 'center' }]}>
          <Ionicons name="musical-notes" size={28} color={Colors.primary} />
        </View>
      )}
      <View style={styles.newsBody}>
        <Text style={[styles.newsTitle, { color: colors.text }]} numberOfLines={2}>
          {article.title}
        </Text>
        <Text style={[styles.newsDate, { color: colors.textSecondary }]}>
          {article.sourceName ?? ''}{article.sourceName && article.publishedAt ? '  ·  ' : ''}
          {formatNewsDate(article.publishedAt)}
        </Text>
      </View>
    </TouchableOpacity>
  );
};

// ── Estilos ──

const styles = StyleSheet.create({
  safe: { flex: 1 },
  scroll: { paddingTop: Spacing.sm },

  // Header
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.xs,
    marginBottom: Spacing.xs,
    gap: Spacing.sm,
  },
  avatar: { width: 64, height: 64, borderRadius: 32 },
  avatarFallback: {
    width: 64, height: 64, borderRadius: 32,
    alignItems: 'center', justifyContent: 'center',
  },
  avatarText: { fontFamily: FontFamily.black, fontSize: FontSize.xl, color: Colors.textLight },
  headerWavii: { width: 72, height: 72 },
  headerCenter: { flex: 1 },
  greeting: { fontFamily: FontFamily.extraBold, fontSize: FontSize['2xl'] },
  levelLabel: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },
  motivText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },

  statsCol: { gap: 4 },
  statChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
    paddingHorizontal: Spacing.xs,
    paddingVertical: 3,
    borderRadius: BorderRadius.sm,
  },
  statValue: { fontFamily: FontFamily.black, fontSize: FontSize.xs },
  headerAside: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  streakPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    borderWidth: 1,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    height: 34,
  },
  streakValue: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.sm,
  },

  // XP Bar
  xpBarRow: {
    paddingHorizontal: Spacing.base,
    marginBottom: Spacing.base,
    gap: 4,
  },
  xpBarTrack: { height: 6, borderRadius: 3, overflow: 'hidden' },
  xpBarFill: { height: '100%', backgroundColor: Colors.primary, borderRadius: 3 },
  xpBarMeta: { fontFamily: FontFamily.medium, fontSize: FontSize.xs, textAlign: 'right' },

  // Section headers
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    marginBottom: Spacing.sm,
    marginTop: Spacing.xl,
  },
  sectionTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  linkText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, color: Colors.primary },
  resetBadge: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  resetText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },

  // Weekly card
  weeklyCard: {
    marginHorizontal: Spacing.base,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  weekRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  dayColumn: { alignItems: 'center', gap: 4 },
  dayLabel: { fontFamily: FontFamily.bold, fontSize: FontSize.xs },
  weekSummary: { fontFamily: FontFamily.regular, fontSize: FontSize.sm },

  // Challenge cards
  challengeCard: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: Spacing.base,
    marginBottom: Spacing.sm,
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    gap: Spacing.sm,
  },
  challengeIcon: {
    width: 52, height: 52,
    borderRadius: BorderRadius.md,
    alignItems: 'center', justifyContent: 'center',
  },
  challengeBody: { flex: 1 },
  challengeTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  challengeDesc: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginBottom: 4 },
  progressTrack: { height: 6, borderRadius: 3, overflow: 'hidden', marginBottom: 2 },
  progressFill: { height: '100%', borderRadius: 3 },
  progressLabel: { fontFamily: FontFamily.regular, fontSize: 10, textAlign: 'right' },
  challengeRight: { alignItems: 'center', gap: 4, minWidth: 64 },
  xpText: { fontFamily: FontFamily.extraBold, fontSize: FontSize.sm, color: Colors.primary },
  completeBtn: {
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 5,
  },
  completeBtnText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs, color: Colors.white },

  // View all button
  viewAllBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginHorizontal: Spacing.base,
    marginTop: Spacing.xs,
    borderRadius: BorderRadius.lg,
    padding: Spacing.md,
    gap: Spacing.sm,
  },
  viewAllText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, flex: 1, textAlign: 'center' },

  // Tab cards
  tabCard: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: Spacing.base,
    marginBottom: Spacing.sm,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  tabCardIcon: {
    width: 48, height: 48,
    borderRadius: BorderRadius.md,
    alignItems: 'center', justifyContent: 'center',
  },
  tabCardCover: {
    width: 48,
    height: 48,
    borderRadius: BorderRadius.md,
  },
  tabCardBody: { flex: 1, gap: 4 },
  tabCardTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  tabCardArtist: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },

  // Teacher cards
  carouselContainer: { marginHorizontal: Spacing.base },
  teachersScroll: { gap: Spacing.sm, paddingBottom: 4 },
  teacherCard: {
    width: 140,
    borderRadius: BorderRadius.xl,
    padding: Spacing.sm,
    alignItems: 'center',
    gap: 4,
  },
  teacherAvatar: {
    width: 52, height: 52, borderRadius: 26,
    alignItems: 'center', justifyContent: 'center',
    marginBottom: 2,
  },
  teacherAvatarText: { fontFamily: FontFamily.black, fontSize: FontSize.lg, color: Colors.textLight },
  teacherName: { fontFamily: FontFamily.bold, fontSize: FontSize.xs, textAlign: 'center' },
  teacherInstrument: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  teacherBadge: { flexDirection: 'row', alignItems: 'center', gap: 3 },
  teacherBadgeText: { fontFamily: FontFamily.semiBold, fontSize: 10 },
  teacherPrice: { fontFamily: FontFamily.extraBold, fontSize: FontSize.sm, marginTop: 2 },
  contactBtn: {
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 4,
    marginTop: 2,
  },
  contactBtnText: { fontFamily: FontFamily.bold, fontSize: 11, color: Colors.white },

  // News scroll
  newsScroll: { gap: Spacing.sm, paddingBottom: 4 },
  newsCard: {
    width: 220,
    borderRadius: BorderRadius.xl,
    overflow: 'hidden',
  },
  newsImg: {
    width: '100%',
    height: 110,
    backgroundColor: Colors.grayLight,
  },
  newsBody: {
    padding: Spacing.sm,
    gap: 4,
  },
  newsTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, lineHeight: 18 },
  newsDate: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },

  // Motivation banner
  motivationBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: Spacing.base,
    marginTop: Spacing.xl,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  motImage: { width: 84, height: 72 },
  motTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  motSubtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  motBtn: {
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.full,
    paddingHorizontal: 20,
    paddingVertical: Spacing.xs,
  },
  motBtnText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs, color: Colors.textLight },

  // Public profile modal
  profileOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
  },
  profileModal: {
    width: '100%',
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    alignItems: 'center',
    gap: Spacing.xs,
  },
  profileModalClose: {
    position: 'absolute',
    top: Spacing.sm,
    right: Spacing.sm,
    padding: Spacing.xs,
  },
  profileModalAvatar: { width: 96, height: 96, borderRadius: 48, marginBottom: Spacing.sm },
  profileModalAvatarFallback: {
    width: 96, height: 96, borderRadius: 48,
    alignItems: 'center', justifyContent: 'center',
    marginBottom: Spacing.sm,
  },
  profileModalAvatarText: { fontFamily: FontFamily.black, fontSize: 40, color: Colors.textLight },
  profileModalName: { fontFamily: FontFamily.extraBold, fontSize: FontSize.xl },
  profileModalLevel: { fontFamily: FontFamily.semiBold, fontSize: FontSize.base },
  profileModalRole: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, marginBottom: Spacing.sm },
  profileModalStats: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xl,
    marginTop: Spacing.sm,
  },
  profileModalStat: { alignItems: 'center', gap: 2 },
  profileModalStatValue: { fontFamily: FontFamily.black, fontSize: FontSize.lg },
  profileModalStatLabel: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  profileModalDivider: { width: 1, height: 48 },
});
