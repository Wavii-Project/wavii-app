import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  ScrollView,
  Pressable,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Modal,
  TouchableOpacity,
  FlatList,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { AppStackParamList } from '../../navigation/AppNavigator';
import {
  PublicUserProfile,
  apiFetchPublicProfile,
  apiFetchUserTabs,
  apiReportUser,
  apiBlockUser,
  apiUnblockUser,
} from '../../api/userApi';
import { PdfDocument } from '../../api/pdfApi';

type Nav = NativeStackNavigationProp<AppStackParamList>;
type RouteT = RouteProp<AppStackParamList, 'UserProfile'>;

const LEVEL_LABELS: Record<string, string> = {
  PRINCIPIANTE: 'Principiante',
  INTERMEDIO: 'Intermedio',
  AVANZADO: 'Avanzado',
};

const ROLE_LABELS: Record<string, string> = {
  USUARIO: 'Usuario',
  PROFESOR_PARTICULAR: 'Profesor',
  PROFESOR_CERTIFICADO: 'Prof. Certificado',
};

const LEVEL_COLORS: Record<string, string> = {
  PRINCIPIANTE: '#22C55E',
  INTERMEDIO: '#F59E0B',
  AVANZADO: Colors.primary,
};

const REPORT_REASONS = [
  { key: 'SPAM', label: 'Spam o publicidad' },
  { key: 'HARASSMENT', label: 'Acoso o abuso' },
  { key: 'FAKE', label: 'Perfil falso' },
  { key: 'OTHER', label: 'Otro motivo' },
];

function StatBlock({ icon, value, label }: { icon: string; value: number | string; label: string }) {
  const { colors } = useTheme();
  return (
    <View style={styles.statBlock}>
      <Text style={styles.statIcon}>{icon}</Text>
      <Text style={[styles.statValue, { color: colors.text }]}>{value}</Text>
      <Text style={[styles.statLabel, { color: colors.textSecondary }]}>{label}</Text>
    </View>
  );
}

function TabCard({ item, onPress }: { item: PdfDocument; onPress: () => void }) {
  const { colors } = useTheme();
  return (
    <Pressable style={[styles.tabCard, { backgroundColor: colors.surface }]} onPress={onPress}>
      {item.coverImageUrl ? (
        <Image source={{ uri: item.coverImageUrl }} style={styles.tabCover} resizeMode="cover" />
      ) : (
        <View style={[styles.tabCoverPlaceholder, { backgroundColor: Colors.primaryOpacity10 }]}>
          <Ionicons name="musical-notes" size={22} color={Colors.primary} />
        </View>
      )}
      <View style={styles.tabInfo}>
        <Text style={[styles.tabTitle, { color: colors.text }]} numberOfLines={2}>{item.songTitle || item.originalName}</Text>
        <View style={styles.tabMeta}>
          <Ionicons name="heart" size={12} color={colors.textSecondary} />
          <Text style={[styles.tabMetaText, { color: colors.textSecondary }]}>{item.likeCount}</Text>
        </View>
      </View>
    </Pressable>
  );
}

export const UserProfileScreen: React.FC = () => {
  const { colors } = useTheme();
  const { token, user: me } = useAuth();
  const navigation = useNavigation<Nav>();
  const route = useRoute<RouteT>();
  const { userId } = route.params;

  const [profile, setProfile] = useState<PublicUserProfile | null>(null);
  const [tabs, setTabs] = useState<PdfDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [blocked, setBlocked] = useState(false);
  const [reportModalVisible, setReportModalVisible] = useState(false);

  const isOwnProfile = me?.id === userId;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [profileData, tabsData] = await Promise.all([
        apiFetchPublicProfile(userId),
        apiFetchUserTabs(userId, token ?? undefined),
      ]);
      setProfile(profileData);
      setTabs(tabsData);
    } catch {
      navigation.goBack();
    } finally {
      setLoading(false);
    }
  }, [userId, token]);

  useEffect(() => { load(); }, [load]);

  const handleBlock = async () => {
    if (!token) return;
    Alert.alert(
      blocked ? 'Desbloquear usuario' : 'Bloquear usuario',
      blocked
        ? `¿Desbloquear a ${profile?.name}?`
        : `¿Bloquear a ${profile?.name}? Ya no podrá contactarte.`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: blocked ? 'Desbloquear' : 'Bloquear',
          style: 'destructive',
          onPress: async () => {
            try {
              if (blocked) {
                await apiUnblockUser(userId, token);
                setBlocked(false);
              } else {
                await apiBlockUser(userId, token);
                setBlocked(true);
              }
            } catch {
              Alert.alert('Error', 'No se pudo completar la acción.');
            }
          },
        },
      ],
    );
  };

  const handleReport = async (reason: string) => {
    if (!token) return;
    setReportModalVisible(false);
    try {
      await apiReportUser(userId, reason, token);
      Alert.alert('Reporte enviado', 'Gracias por ayudarnos a mantener Wavii seguro.');
    } catch {
      Alert.alert('Error', 'No se pudo enviar el reporte.');
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
        <View style={styles.center}><ActivityIndicator color={Colors.primary} /></View>
      </SafeAreaView>
    );
  }

  if (!profile) return null;

  const levelColor = profile.level ? (LEVEL_COLORS[profile.level] ?? Colors.primary) : Colors.primary;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Pressable onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Ionicons name="arrow-back" size={22} color={colors.text} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.text }]} numberOfLines={1}>
          {isOwnProfile ? 'Mi perfil' : 'Perfil de usuario'}
        </Text>
        {!isOwnProfile ? (
          <Pressable onPress={() => setReportModalVisible(true)} hitSlop={8}>
            <Ionicons name="flag-outline" size={20} color={colors.textSecondary} />
          </Pressable>
        ) : <View style={{ width: 24 }} />}
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        {/* Avatar + Nombre + Badges */}
        <View style={styles.heroSection}>
          <View style={[styles.avatar, { backgroundColor: levelColor + '22' }]}>
            <Text style={[styles.avatarText, { color: levelColor }]}>
              {profile.name.charAt(0).toUpperCase()}
            </Text>
          </View>
          <Text style={[styles.name, { color: colors.text }]}>{profile.name}</Text>

          <View style={styles.badgeRow}>
            {profile.level && (
              <View style={[styles.badge, { backgroundColor: levelColor + '22' }]}>
                <Text style={[styles.badgeText, { color: levelColor }]}>
                  {LEVEL_LABELS[profile.level] ?? profile.level}
                </Text>
              </View>
            )}
            {profile.role && (
              <View style={[styles.badge, { backgroundColor: Colors.primaryOpacity10 }]}>
                <Text style={[styles.badgeText, { color: Colors.primary }]}>
                  {ROLE_LABELS[profile.role] ?? profile.role}
                </Text>
              </View>
            )}
          </View>
        </View>

        {/* Stats */}
        <View style={[styles.statsCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <StatBlock icon="🔥" value={profile.streak} label="Racha" />
          <View style={[styles.statDivider, { backgroundColor: colors.border }]} />
          <StatBlock icon="⭐" value={profile.bestStreak} label="Mejor racha" />
          <View style={[styles.statDivider, { backgroundColor: colors.border }]} />
          <StatBlock icon="✨" value={profile.xp.toLocaleString()} label="XP" />
        </View>

        {profile.memberSince && (
          <Text style={[styles.memberSince, { color: colors.textSecondary }]}>
            Miembro desde {new Date(profile.memberSince + '-01').toLocaleDateString('es-ES', { month: 'long', year: 'numeric' })}
          </Text>
        )}

        {/* Botones de acción (solo si no es el propio perfil) */}
        {!isOwnProfile && token && (
          <View style={styles.actionsRow}>
            <Pressable
              style={[styles.actionBtn, { backgroundColor: blocked ? colors.surface : colors.surface, borderColor: Colors.error + '80', flex: 1 }]}
              onPress={handleBlock}
            >
              <Ionicons name={blocked ? 'lock-open-outline' : 'ban-outline'} size={18} color={Colors.error} />
              <Text style={[styles.actionBtnText, { color: Colors.error }]}>
                {blocked ? 'Desbloquear' : 'Bloquear'}
              </Text>
            </Pressable>
          </View>
        )}

        {/* Tablaturas publicadas */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
            Tablaturas publicadas ({tabs.length})
          </Text>
          {tabs.length === 0 ? (
            <View style={[styles.emptyTabs, { borderColor: colors.border }]}>
              <Ionicons name="musical-notes-outline" size={32} color={colors.textSecondary} />
              <Text style={[styles.emptyTabsText, { color: colors.textSecondary }]}>
                Aún no ha publicado tablaturas
              </Text>
            </View>
          ) : (
            tabs.map((tab) => (
              <TabCard
                key={tab.id}
                item={tab}
                onPress={() => navigation.navigate('PdfViewer', { pdfId: tab.id, title: tab.songTitle || tab.originalName })}
              />
            ))
          )}
        </View>
      </ScrollView>

      {/* Modal de reporte */}
      <Modal visible={reportModalVisible} transparent animationType="fade" onRequestClose={() => setReportModalVisible(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setReportModalVisible(false)}>
          <View style={[styles.reportCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
            <Text style={[styles.reportTitle, { color: colors.text }]}>Reportar usuario</Text>
            {REPORT_REASONS.map((r) => (
              <TouchableOpacity key={r.key} style={styles.reportOption} onPress={() => handleReport(r.key)}>
                <Text style={[styles.reportOptionText, { color: colors.text }]}>{r.label}</Text>
                <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
              </TouchableOpacity>
            ))}
            <TouchableOpacity style={styles.reportCancel} onPress={() => setReportModalVisible(false)}>
              <Text style={[styles.reportCancelText, { color: Colors.primary }]}>Cancelar</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  header: {
    flexDirection: 'row', alignItems: 'center', gap: Spacing.sm,
    paddingHorizontal: Spacing.base, paddingVertical: 12, borderBottomWidth: 1,
  },
  backBtn: { padding: 4 },
  headerTitle: { flex: 1, fontFamily: FontFamily.bold, fontSize: FontSize.base },

  scroll: { padding: Spacing.base, gap: Spacing.base },

  heroSection: {
    alignItems: 'center',
    gap: Spacing.sm,
    paddingVertical: Spacing.md,
  },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarText: {
    fontFamily: FontFamily.extraBold,
    fontSize: 36,
  },
  name: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xl,
    textAlign: 'center',
  },
  badgeRow: {
    flexDirection: 'row',
    gap: 8,
    flexWrap: 'wrap',
    justifyContent: 'center',
  },
  badge: {
    borderRadius: 20,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  badgeText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },

  statsCard: {
    flexDirection: 'row',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    paddingVertical: Spacing.base,
  },
  statBlock: {
    flex: 1,
    alignItems: 'center',
    gap: 4,
  },
  statDivider: {
    width: 1,
    marginVertical: 4,
  },
  statIcon: {
    fontSize: 20,
  },
  statValue: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  statLabel: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
  },

  memberSince: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
    marginTop: -4,
  },

  actionsRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  actionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingVertical: 12,
  },
  actionBtnText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },

  section: { gap: Spacing.sm },
  sectionTitle: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },

  emptyTabs: {
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    alignItems: 'center',
    gap: 8,
  },
  emptyTabsText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
  },

  tabCard: {
    flexDirection: 'row',
    borderRadius: BorderRadius.lg,
    overflow: 'hidden',
    elevation: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.04,
    shadowRadius: 4,
    gap: Spacing.sm,
    padding: Spacing.sm,
  },
  tabCover: {
    width: 52,
    height: 52,
    borderRadius: 8,
  },
  tabCoverPlaceholder: {
    width: 52,
    height: 52,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  tabInfo: {
    flex: 1,
    justifyContent: 'center',
    gap: 4,
  },
  tabTitle: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    lineHeight: 18,
  },
  tabMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  tabMetaText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },

  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
  },
  reportCard: {
    width: '100%',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.base,
    gap: 4,
  },
  reportTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    marginBottom: Spacing.sm,
  },
  reportOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.base,
    paddingHorizontal: Spacing.sm,
    borderRadius: BorderRadius.sm,
  },
  reportOptionText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
  },
  reportCancel: {
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    marginTop: 4,
  },
  reportCancelText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
});
