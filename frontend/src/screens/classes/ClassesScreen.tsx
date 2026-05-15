import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useFocusEffect, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { MainTabParamList } from '../../navigation/MainNavigator';
import { apiFetchClasses, apiFetchStudentClassPosts, ClassEnrollment, ClassPost } from '../../api/classesApi';
import { NotificationBell } from '../../components/common/NotificationBell';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { isBackendSessionToken } from '../../auth/session';
import { GuestBlockedView } from '../../components/common/GuestBlockedView';

type Navigation = NativeStackNavigationProp<AppStackParamList, 'Classes'>;
type TabRoute = RouteProp<MainTabParamList, 'Clases'>;

const STATUS_LABELS: Record<string, string> = {
  pending: 'Pendiente',
  accepted: 'Aceptada',
  paid: 'Activa',
  scheduled: 'Agendada',
  completed: 'Completada',
  rejected: 'Rechazada',
  cancelled: 'Cancelada',
  refund_requested: 'Reembolso pendiente',
  refunded: 'Reembolsada',
};

const MODALITY_LABELS: Record<string, string> = {
  ONLINE: 'Online',
  PRESENCIAL: 'Presencial',
  AMBAS: 'Ambas',
};

const mergeClasses = (current: ClassEnrollment[], incoming: ClassEnrollment[], optimistic?: ClassEnrollment) => {
  const map = new Map<string, ClassEnrollment>();
  [...current, ...incoming].forEach((item) => map.set(item.id, item));
  if (optimistic) {
    map.set(optimistic.id, optimistic);
  }
  return Array.from(map.values()).sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
};

export const ClassesScreen = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation<Navigation>();
  const route = useRoute<TabRoute>();

  const [classes, setClasses] = useState<ClassEnrollment[]>([]);
  const [posts, setPosts] = useState<ClassPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const hasLoadedRef = useRef(false);

  const refreshKey = route.params?.refreshKey;
  const justRequestedClass = route.params?.justRequestedClass;

  const load = useCallback(
    async (optimisticClass?: ClassEnrollment) => {
      if (!token) return;
      if (!hasLoadedRef.current) {
        setLoading(true);
      }
      setError(null);
      try {
        const data = await apiFetchClasses(token);
        setClasses((current) => mergeClasses(current, data, optimisticClass));

        const nextPosts = await apiFetchStudentClassPosts(token).catch(() => []);
        setPosts(nextPosts);
      } catch (err: any) {
        setPosts([]);
        setError(err?.response?.data?.message ?? 'No se pudieron cargar tus clases.');
      } finally {
        hasLoadedRef.current = true;
        setLoading(false);
      }
    },
    [token]
  );

  useEffect(() => {
    if (justRequestedClass) {
      setClasses((current) => mergeClasses(current, [], justRequestedClass));
    }
  }, [justRequestedClass]);

  useFocusEffect(
    useCallback(() => {
      load(justRequestedClass);
    }, [justRequestedClass, load, refreshKey])
  );

  const sortedPosts = useMemo(
    () => [...posts].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [posts]
  );

  const openChat = (item: ClassEnrollment) => {
    navigation.navigate('ClassRoom', {
      enrollmentId: item.id,
      teacherName: item.teacherName,
      teacherId: item.teacherId,
      studentId: item.studentId,
      studentName: item.studentName,
    });
  };

  const latestPost = sortedPosts[0] ?? null;

  const renderClassCard = ({ item }: { item: ClassEnrollment }) => {
    const statusLabel = STATUS_LABELS[item.paymentStatus?.toLowerCase()] ?? item.paymentStatus ?? 'Pendiente';
    const modalityLabel = MODALITY_LABELS[item.requestedModality || item.modality] ?? item.requestedModality ?? item.modality ?? 'Sin definir';
    const detailLine = [item.instrument ?? 'Clase musical', modalityLabel, item.city].filter(Boolean).join(' · ');
    const requestSummary = item.requestAvailability?.trim() || 'Disponibilidad pendiente';
    const requestMessage = item.requestMessage?.trim();
    const canOpenChat = item.canChat;

    return (
      <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <View style={styles.cardTopRow}>
          <View style={[styles.avatar, { backgroundColor: Colors.primaryOpacity10 }]}>
            <Text style={[styles.avatarText, { color: Colors.primary }]}>{item.teacherName.charAt(0).toUpperCase()}</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={[styles.cardTitle, { color: colors.text }]} numberOfLines={1}>
              {item.teacherName}
            </Text>
            <Text style={[styles.cardSubtitle, { color: colors.textSecondary }]} numberOfLines={2}>
              {detailLine}
            </Text>
          </View>
          <View style={[styles.statusBadge, { backgroundColor: Colors.primaryOpacity10 }]}>
            <Text style={[styles.statusText, { color: Colors.primary }]}>{statusLabel}</Text>
          </View>
        </View>

        <View style={styles.metaRow}>
          <MetaPill label={modalityLabel} icon="swap-horizontal-outline" colors={colors} />
          <MetaPill label={requestSummary} icon="time-outline" colors={colors} />
        </View>

        {requestMessage ? (
          <View style={[styles.requestBox, { backgroundColor: colors.background }]}>
            <Text style={[styles.requestLabel, { color: colors.textSecondary }]}>Mensaje</Text>
            <Text style={[styles.requestText, { color: colors.text }]}>{requestMessage}</Text>
          </View>
        ) : null}

        {item.nextSession ? (
          <View style={[styles.sessionRow, { backgroundColor: colors.background, borderColor: colors.border }]}>
            <Ionicons name="calendar-outline" size={15} color={Colors.primary} />
            <Text style={[styles.sessionText, { color: colors.text }]} numberOfLines={1}>
              Próxima sesión: {item.nextSession.scheduledAt.replace('T', ' ').slice(0, 16)}
            </Text>
          </View>
        ) : null}

        <View style={styles.actionRow}>
          <ActionChip
            icon="person-outline"
            label="Perfil"
            onPress={() => navigation.navigate('TeacherProfile', { teacherId: item.teacherId })}
            colors={colors}
          />
          {canOpenChat ? (
            <ActionChip
              icon="chatbubble-ellipses-outline"
              label="Chat"
              primary
              onPress={() => openChat(item)}
              colors={colors}
            />
          ) : (
            <View style={[styles.waitingChip, { backgroundColor: colors.surface, borderColor: colors.border }]}>
              <Ionicons name="time-outline" size={14} color={colors.textSecondary} />
              <Text style={[styles.waitingChipText, { color: colors.textSecondary }]}>Esperando respuesta</Text>
            </View>
          )}
        </View>
      </View>
    );
  };

  const hasAccount = isBackendSessionToken(token);
  if (!hasAccount) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
        <View style={styles.header}>
          <View style={{ flex: 1 }}>
            <Text style={[styles.title, { color: colors.text }]}>Clases</Text>
            <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Tus solicitudes, chat y clases activas</Text>
          </View>
        </View>
        <GuestBlockedView feature="para solicitar y gestionar tus clases" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.title, { color: colors.text }]}>Clases</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Tus solicitudes, chat y clases activas</Text>
        </View>
        <NotificationBell size="sm" />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={classes}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          renderItem={renderClassCard}
          ListHeaderComponent={
            <View style={styles.headerContent}>
              {error ? (
                <View style={[styles.errorBanner, { backgroundColor: colors.surface, borderColor: Colors.error }]}>
                  <Ionicons name="alert-circle-outline" size={18} color={Colors.error} />
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.errorTitle, { color: colors.text }]}>No pudimos cargar las clases</Text>
                    <Text style={[styles.errorText, { color: colors.textSecondary }]}>{error}</Text>
                  </View>
                  <TouchableOpacity onPress={() => load(justRequestedClass)}>
                    <Text style={[styles.retryText, { color: Colors.primary }]}>Reintentar</Text>
                  </TouchableOpacity>
                </View>
              ) : null}

              {latestPost ? (
                <TouchableOpacity
                  style={[styles.postsSummaryCard, { backgroundColor: colors.surface, borderColor: colors.border }]}
                  activeOpacity={0.86}
                  onPress={() => navigation.navigate('ClassPosts')}
                >
                  <View style={styles.postsSummaryHead}>
                    <View>
                      <Text style={[styles.postsSummaryTitle, { color: colors.text }]}>Novedades para tus clases</Text>
                      <Text style={[styles.postsSummaryMeta, { color: colors.textSecondary }]}>
                        {latestPost.teacherName} · {latestPost.createdAt.replace('T', ' ').slice(0, 16)}
                      </Text>
                    </View>
                    <View style={[styles.postsSummaryCount, { backgroundColor: Colors.primaryOpacity10 }]}>
                      <Text style={styles.postsSummaryCountText}>{sortedPosts.length}</Text>
                    </View>
                  </View>
                  <Text style={[styles.postTitle, { color: colors.text }]} numberOfLines={1}>
                    {latestPost.title}
                  </Text>
                  <Text style={[styles.postContent, { color: colors.textSecondary }]} numberOfLines={2}>
                    {latestPost.content}
                  </Text>
                  <View style={styles.postsSummaryFooter}>
                    <Text style={[styles.postsSummaryLink, { color: Colors.primary }]}>Ver todas</Text>
                    <Ionicons name="chevron-forward" size={16} color={Colors.primary} />
                  </View>
                </TouchableOpacity>
              ) : null}
            </View>
          }
          ListEmptyComponent={
            <View style={styles.center}>
              <View style={[styles.emptyIconWrap, { backgroundColor: Colors.primaryOpacity10 }]}>
                <Ionicons name="school-outline" size={24} color={Colors.primary} />
              </View>
              <Text style={[styles.emptyTitle, { color: colors.text }]}>Aún no tienes solicitudes activas</Text>
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                Cuando pidas una clase o te respondan, aparecerá aquí.
              </Text>
            </View>
          }
        />
      )}
    </SafeAreaView>
  );
};

const MetaPill = ({
  label,
  icon,
  colors,
}: {
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  colors: ReturnType<typeof useTheme>['colors'];
}) => (
  <View style={[styles.metaPill, { backgroundColor: colors.background }]}>
    <Ionicons name={icon} size={13} color={Colors.primary} />
    <Text style={[styles.metaPillText, { color: colors.textSecondary }]} numberOfLines={1}>
      {label}
    </Text>
  </View>
);

const ActionChip = ({
  icon,
  label,
  onPress,
  colors,
  primary,
}: {
  icon: React.ComponentProps<typeof Ionicons>['name'];
  label: string;
  onPress: () => void;
  colors: ReturnType<typeof useTheme>['colors'];
  primary?: boolean;
}) => (
  <TouchableOpacity
    style={[
      styles.actionChip,
      { borderColor: primary ? Colors.primary : colors.border, backgroundColor: primary ? Colors.primary : colors.surface },
    ]}
    onPress={onPress}
    activeOpacity={0.8}
  >
    <Ionicons name={icon} size={15} color={primary ? Colors.white : Colors.primary} />
    <Text style={[styles.actionChipText, { color: primary ? Colors.white : colors.text }]}>{label}</Text>
  </TouchableOpacity>
);

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.base,
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.sm,
  },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize['2xl'] },
  subtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginTop: 2 },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.xl,
  },
  list: {
    paddingHorizontal: Spacing.base,
    paddingBottom: Spacing.xl,
    gap: Spacing.sm,
  },
  headerContent: {
    gap: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  errorBanner: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  errorTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  errorText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 18, marginTop: 2 },
  retryText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs },
  card: {
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  cardTopRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  avatar: {
    width: 46,
    height: 46,
    borderRadius: 23,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: { fontFamily: FontFamily.black, fontSize: FontSize.lg },
  cardTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  cardSubtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginTop: 2, lineHeight: 18 },
  statusBadge: { borderRadius: BorderRadius.md, paddingHorizontal: Spacing.sm, paddingVertical: 6 },
  statusText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs },
  metaRow: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs },
  metaPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 8,
    flexGrow: 1,
    minWidth: 0,
  },
  metaPillText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs, flexShrink: 1 },
  requestBox: {
    borderRadius: BorderRadius.md,
    padding: Spacing.sm,
    gap: 4,
  },
  requestLabel: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  requestText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  sessionRow: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
  },
  sessionText: { flex: 1, fontFamily: FontFamily.medium, fontSize: FontSize.xs, lineHeight: 18 },
  actionRow: { flexDirection: 'row', gap: Spacing.xs },
  actionChip: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    minHeight: 40,
  },
  actionChipText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  waitingChip: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    paddingHorizontal: Spacing.sm,
    minHeight: 40,
  },
  waitingChipText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  emptyIconWrap: {
    width: 52,
    height: 52,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base, textAlign: 'center' },
  emptyText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center', lineHeight: 20 },
  postsSummaryCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    gap: Spacing.xs,
  },
  postsSummaryHead: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: Spacing.sm,
  },
  postsSummaryTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.base,
  },
  postsSummaryMeta: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginTop: 2,
  },
  postsSummaryCount: {
    minWidth: 32,
    height: 28,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.xs,
  },
  postsSummaryCountText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    color: Colors.primary,
  },
  postsSummaryFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
    marginTop: Spacing.xs,
  },
  postsSummaryLink: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  postTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  postContent: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
});
