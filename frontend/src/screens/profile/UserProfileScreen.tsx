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
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as ImagePicker from 'expo-image-picker';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
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

const DIFFICULTY_LABELS: Record<number, string> = {
  1: 'Principiante',
  2: 'Intermedio',
  3: 'Avanzado',
};

const DIFFICULTY_COLORS: Record<number, string> = {
  1: '#22C55E',
  2: '#F59E0B',
  3: '#EF4444',
};

function StatBlock({
  ionIcon,
  iconColor,
  value,
  label,
}: {
  ionIcon: React.ComponentProps<typeof Ionicons>['name'];
  iconColor: string;
  value: number | string;
  label: string;
}) {
  const { colors } = useTheme();
  return (
    <View style={styles.statBlock}>
      <Ionicons name={ionIcon} size={20} color={iconColor} />
      <Text style={[styles.statValue, { color: colors.text }]}>{value}</Text>
      <Text style={[styles.statLabel, { color: colors.textSecondary }]}>{label}</Text>
    </View>
  );
}

function TabCard({ item, onPress }: { item: PdfDocument; onPress: () => void }) {
  const { colors } = useTheme();
  const diffColor = DIFFICULTY_COLORS[item.difficulty] ?? Colors.primary;
  return (
    <Pressable style={[styles.tabCard, { backgroundColor: colors.surface, borderColor: colors.border }]} onPress={onPress}>
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
          <View style={[styles.diffDot, { backgroundColor: diffColor }]} />
          <Text style={[styles.tabMetaText, { color: colors.textSecondary }]}>{DIFFICULTY_LABELS[item.difficulty] ?? ''}</Text>
          <Ionicons name="heart" size={12} color={colors.textSecondary} style={{ marginLeft: 8 }} />
          <Text style={[styles.tabMetaText, { color: colors.textSecondary }]}>{item.likeCount}</Text>
        </View>
      </View>
    </Pressable>
  );
}

export const UserProfileScreen: React.FC = () => {
  const { colors } = useTheme();
  const { token, user: me } = useAuth();
  const { showAlert } = useAlert();
  const navigation = useNavigation<Nav>();
  const route = useRoute<RouteT>();
  const { userId } = route.params;

  const [profile, setProfile] = useState<PublicUserProfile | null>(null);
  const [tabs, setTabs] = useState<PdfDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [blocked, setBlocked] = useState(false);
  const [reportModalVisible, setReportModalVisible] = useState(false);
  const [avatar, setAvatar] = useState<string | null>(null);

  const isOwnProfile = me?.id === userId;
  const avatarKey = `wavii_avatar_${userId}`;

  // Cargar avatar propio desde AsyncStorage
  useEffect(() => {
    if (!isOwnProfile) return;
    AsyncStorage.getItem(avatarKey).then((v) => { if (v) setAvatar(v); }).catch(() => {});
  }, [isOwnProfile, avatarKey]);

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

  const handlePickAvatar = useCallback(async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      showAlert({ title: 'Permiso denegado', message: 'Necesitamos acceso a tu galería para cambiar la foto.' });
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.7,
    });
    if (result.canceled || !result.assets[0]) return;
    const uri = result.assets[0].uri;
    setAvatar(uri);
    await AsyncStorage.setItem(avatarKey, uri);
  }, [avatarKey, showAlert]);

  const handleBlock = async () => {
    if (!token) return;
    showAlert({
      title: blocked ? 'Desbloquear usuario' : 'Bloquear usuario',
      message: blocked
        ? `¿Desbloquear a ${profile?.name}?`
        : `¿Bloquear a ${profile?.name}? Ya no podrá contactarte.`,
      buttons: [
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
              showAlert({ title: 'Error', message: 'No se pudo completar la acción.' });
            }
          },
        },
      ],
    });
  };

  const handleReport = async (reason: string) => {
    if (!token) return;
    setReportModalVisible(false);
    try {
      await apiReportUser(userId, reason, token);
      showAlert({ title: 'Reporte enviado', message: 'Gracias por ayudarnos a mantener Wavii seguro.' });
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo enviar el reporte.' });
    }
  };

  const handleDirectMessage = () => {
    if (!profile) return;
    if (!profile.acceptsMessages) {
      showAlert({
        title: 'Mensajes desactivados',
        message: `${profile.name} ha desactivado los mensajes directos.`,
      });
      return;
    }
    navigation.navigate('DirectMessage', { userId, userName: profile.name });
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

  // Top 5 tabs por popularidad
  const topTabs = [...tabs].sort((a, b) => b.likeCount - a.likeCount).slice(0, 5);
  const hasMoreTabs = tabs.length > 5;

  const initial = profile.name.charAt(0).toUpperCase();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Pressable onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.text }]} numberOfLines={1}>
          {isOwnProfile ? 'Mi perfil' : 'Perfil de usuario'}
        </Text>
        <View style={{ width: 30 }} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        {/* Avatar + Nombre + Badges */}
        <View style={styles.heroSection}>
          {isOwnProfile ? (
            <TouchableOpacity style={styles.avatarWrapper} onPress={handlePickAvatar} activeOpacity={0.8}>
              {avatar ? (
                <Image source={{ uri: avatar }} style={styles.avatarImg} />
              ) : (
                <View style={[styles.avatar, { backgroundColor: levelColor + '22' }]}>
                  <Text style={[styles.avatarText, { color: levelColor }]}>{initial}</Text>
                </View>
              )}
              <View style={[styles.cameraBtn, { backgroundColor: colors.surface, borderColor: colors.border }]}>
                <Ionicons name="camera" size={14} color={Colors.primary} />
              </View>
            </TouchableOpacity>
          ) : (
            <View style={[styles.avatar, { backgroundColor: levelColor + '22' }]}>
              <Text style={[styles.avatarText, { color: levelColor }]}>{initial}</Text>
            </View>
          )}

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
          <StatBlock ionIcon="flame" iconColor={Colors.streakOrange} value={profile.streak} label="Racha" />
          <View style={[styles.statDivider, { backgroundColor: colors.border }]} />
          <StatBlock ionIcon="trophy" iconColor={Colors.warning} value={profile.bestStreak} label="Mejor racha" />
          <View style={[styles.statDivider, { backgroundColor: colors.border }]} />
          <StatBlock ionIcon="star" iconColor={Colors.primary} value={profile.xp.toLocaleString()} label="XP" />
        </View>

        {profile.memberSince && (
          <Text style={[styles.memberSince, { color: colors.textSecondary }]}>
            Miembro desde {(() => {
              const parts = profile.memberSince.split('-');
              if (parts.length === 3) {
                return new Date(profile.memberSince).toLocaleDateString('es-ES', { day: 'numeric', month: 'long', year: 'numeric' });
              }
              return new Date(profile.memberSince + '-01').toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });
            })()}
          </Text>
        )}

        {/* Botón a ajustes si es tu propio perfil */}
        {isOwnProfile && (
          <TouchableOpacity
            style={[styles.ownProfileBtn, { backgroundColor: colors.surface, borderColor: colors.border }]}
            onPress={() => navigation.navigate('Profile')}
            activeOpacity={0.75}
          >
            <Ionicons name="settings-outline" size={18} color={Colors.primary} />
            <Text style={[styles.ownProfileBtnText, { color: Colors.primary }]}>Ir a ajustes de perfil</Text>
            <Ionicons name="chevron-forward" size={16} color={Colors.primary} />
          </TouchableOpacity>
        )}

        {/* Botones de acción (solo si no es el propio perfil) */}
        {!isOwnProfile && token && (
          <View style={styles.actionsRow}>
            {/* Bloquear */}
            <Pressable
              style={[styles.actionBtn, { backgroundColor: colors.surface, borderColor: Colors.error + '80' }]}
              onPress={handleBlock}
            >
              <Ionicons name={blocked ? 'lock-open-outline' : 'ban-outline'} size={18} color={Colors.error} />
              <Text style={[styles.actionBtnText, { color: Colors.error }]}>
                {blocked ? 'Desbloquear' : 'Bloquear'}
              </Text>
            </Pressable>

            {/* Reportar */}
            <Pressable
              style={[styles.actionBtn, { backgroundColor: colors.surface, borderColor: Colors.warning + '80' }]}
              onPress={() => setReportModalVisible(true)}
            >
              <Ionicons name="flag-outline" size={18} color={Colors.warning} />
              <Text style={[styles.actionBtnText, { color: Colors.warning }]}>Reportar</Text>
            </Pressable>

            {/* Mensaje directo */}
            <Pressable
              style={[
                styles.actionBtn,
                {
                  backgroundColor: colors.surface,
                  borderColor: profile.acceptsMessages ? Colors.primary + '80' : colors.border,
                  opacity: profile.acceptsMessages ? 1 : 0.5,
                },
              ]}
              onPress={handleDirectMessage}
            >
              <Ionicons name="chatbubble-outline" size={18} color={profile.acceptsMessages ? Colors.primary : colors.textSecondary} />
              <Text style={[styles.actionBtnText, { color: profile.acceptsMessages ? Colors.primary : colors.textSecondary }]}>
                Mensaje
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
            <>
              {topTabs.map((tab) => (
                <TabCard
                  key={tab.id}
                  item={tab}
                  onPress={() => navigation.navigate('PdfViewer', { pdfId: tab.id, title: tab.songTitle || tab.originalName })}
                />
              ))}
              {hasMoreTabs && (
                <Pressable
                  style={[styles.seeAllBtn, { borderColor: colors.border }]}
                  onPress={() => navigation.navigate('UserTabs', { userId, userName: profile.name })}
                >
                  <Text style={[styles.seeAllText, { color: Colors.primary }]}>
                    Ver todas las tablaturas ({tabs.length})
                  </Text>
                  <Ionicons name="arrow-forward" size={16} color={Colors.primary} />
                </Pressable>
              )}
            </>
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

  // Avatar normal (otros usuarios)
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

  // Avatar propio (con cámara)
  avatarWrapper: {
    position: 'relative',
    marginBottom: 4,
  },
  avatarImg: {
    width: 80,
    height: 80,
    borderRadius: 40,
  },
  cameraBtn: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    width: 26,
    height: 26,
    borderRadius: 13,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
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

  // Botón "Ir a ajustes" (perfil propio)
  ownProfileBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingVertical: 12,
    paddingHorizontal: Spacing.base,
  },
  ownProfileBtnText: {
    flex: 1,
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },

  actionsRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  actionBtn: {
    flex: 1,
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingVertical: 12,
    paddingHorizontal: 6,
  },
  actionBtnText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textAlign: 'center',
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
    borderWidth: 1,
    overflow: 'hidden',
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
    gap: 6,
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
  diffDot: {
    width: 7,
    height: 7,
    borderRadius: 4,
  },
  tabMetaText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },

  seeAllBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: BorderRadius.lg,
    paddingVertical: 12,
  },
  seeAllText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
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
