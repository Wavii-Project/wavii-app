import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
  Modal,
  TextInput,
  Platform,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as ImagePicker from 'expo-image-picker';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { apiUpdateName, apiScheduleDeletion, apiCancelDeletion } from '../../api/userApi';
import { LinearGradient } from 'expo-linear-gradient';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { useAlert } from '../../context/AlertContext';
import { useTutorial } from '../../context/TutorialContext';
import { WaviiPromoBanner } from '../../components/common/WaviiPromoBanner';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

const SUBSCRIPTION_LABELS: Record<string, { label: string; colors: readonly [string, string]; color: string; bg: string }> = {
  free:      { label: 'Free',      colors: ['#9CA3AF', '#6B7280'], color: Colors.freeTier,      bg: 'rgba(107,114,128,0.12)' },
  plus:      { label: 'Plus',      colors: ['#FF8A00', '#FF5A00'], color: Colors.plusTier,      bg: Colors.primaryOpacity10  },
  education: { label: 'Scholar',   colors: ['#A78BFA', '#7C3AED'], color: Colors.educationTier, bg: 'rgba(124,58,237,0.12)'  },
};

interface RowProps {
  icon: IoniconsName;
  label: string;
  onPress?: () => void;
  right?: React.ReactNode;
  color?: string;
}

const SettingRow: React.FC<RowProps> = ({ icon, label, onPress, right, color }) => {
  const { colors } = useTheme();
  const labelColor = color ?? colors.text;
  return (
    <TouchableOpacity
      style={styles.row}
      onPress={onPress}
      activeOpacity={onPress ? 0.65 : 1}
      disabled={!onPress && !right}
    >
      <Ionicons name={icon} size={20} color={labelColor} style={styles.rowIcon} />
      <Text style={[styles.rowLabel, { color: labelColor }]}>{label}</Text>
      {right ?? (
        onPress ? <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} /> : null
      )}
    </TouchableOpacity>
  );
};

export const ProfileScreen: React.FC = () => {
  const { user, token, logout, updateUser } = useAuth();
  const { colors, themeMode, setThemeMode } = useTheme();
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();
  const { showAlert } = useAlert();
  const { startTutorial } = useTutorial();

  const [avatar, setAvatar] = useState<string | null>(null);
  const [nameModalVisible, setNameModalVisible] = useState(false);
  const [nameInput, setNameInput] = useState('');
  const [nameLoading, setNameLoading] = useState(false);
  const [deleteLoading, setDeleteLoading] = useState(false);
  
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteCountdown, setDeleteCountdown] = useState(10);

  const avatarKey = user ? `wavii_avatar_${user.id}` : null;

  useEffect(() => {
    if (!avatarKey) return;
    AsyncStorage.getItem(avatarKey).then((v) => { if (v) setAvatar(v); }).catch(() => {});
  }, [avatarKey]);

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
    if (avatarKey) await AsyncStorage.setItem(avatarKey, uri);
  }, [avatarKey]);

  const openChangeName = () => {
    setNameInput(user?.name ?? '');
    if (Platform.OS === 'ios') {
      Alert.prompt(
        'Cambiar nombre',
        'Introduce tu nuevo nombre',
        async (text) => {
          if (!text || text.trim().length < 3) return;
          await submitNameChange(text.trim());
        },
        'plain-text',
        user?.name ?? '',
      );
    } else {
      setNameModalVisible(true);
    }
  };

  const submitNameChange = async (name: string) => {
    if (!token) return;
    if (name.trim().length < 3) {
      showAlert({ title: 'Nombre no valido', message: 'El nombre debe tener al menos 3 caracteres.' });
      return;
    }
    setNameLoading(true);
    try {
      const response = await apiUpdateName(name.trim(), token);
      updateUser({ name: response.name });
    } catch (err) {
      showAlert({ title: 'Error', message: 'No se pudo actualizar el nombre. Inténtalo de nuevo.' });
    } finally {
      setNameLoading(false);
      setNameModalVisible(false);
    }
  };

  useEffect(() => {
    let timer: NodeJS.Timeout;
    if (deleteModalVisible && deleteCountdown > 0) {
      timer = setTimeout(() => setDeleteCountdown(deleteCountdown - 1), 1000);
    }
    return () => clearTimeout(timer);
  }, [deleteModalVisible, deleteCountdown]);

  const openDeleteModal = () => {
    if (user?.deletionScheduledAt) {
      // Ya hay eliminación programada: ofrecer cancelarla
      const deletionDateFormatted = (() => {
        try {
          return new Date(user.deletionScheduledAt).toLocaleDateString('es-ES', {
            day: 'numeric', month: 'long', year: 'numeric',
          });
        } catch { return user.deletionScheduledAt; }
      })();
      showAlert({
        title: 'Cancelar eliminación',
        message: `Tu cuenta está programada para eliminarse el ${deletionDateFormatted}. ¿Quieres cancelar la eliminación y seguir usándola con normalidad?`,
        buttons: [
          { text: 'No, mantener eliminación', style: 'cancel' },
          {
            text: 'Sí, cancelar eliminación',
            style: 'default',
            onPress: async () => {
              if (!token) return;
              try {
                await apiCancelDeletion(token);
                updateUser({ deletionScheduledAt: undefined, subscriptionCancelAtPeriodEnd: false });
                showAlert({ title: 'Cancelada', message: 'La eliminación de tu cuenta ha sido cancelada. Tu suscripción también se ha reactivado.' });
              } catch (err: any) {
                const msg = err?.response?.data?.message ?? 'No se pudo cancelar la eliminación.';
                showAlert({ title: 'Error', message: msg });
              }
            },
          },
        ],
      });
      return;
    }
    setDeleteCountdown(10);
    setDeleteModalVisible(true);
  };

  const handleConfirmDelete = async () => {
    if (!token) return;
    setDeleteLoading(true);
    try {
      // Llama al backend: cancela suscripción Stripe al final del periodo
      // y programa la eliminación de la cuenta (15 días o hasta que expire la suscripción)
      const result = await apiScheduleDeletion(token);

      updateUser({
        deletionScheduledAt: result.deletionScheduledAt,
        subscriptionCancelAtPeriodEnd: true,
      });

      const deletionDateFormatted = (() => {
        try {
          return new Date(result.deletionScheduledAt).toLocaleDateString('es-ES', {
            day: 'numeric', month: 'long', year: 'numeric',
          });
        } catch { return result.deletionScheduledAt; }
      })();

      showAlert({
        title: 'Cuenta programada para eliminar',
        message: `Tu cuenta se eliminará el ${deletionDateFormatted}. Hasta entonces conservas todos tus beneficios actuales. Si cambias de opinión, puedes cancelar la eliminación desde tu perfil.`,
      });
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'No se pudo programar la eliminación. Inténtalo de nuevo.';
      showAlert({ title: 'Error', message: msg });
    } finally {
      setDeleteLoading(false);
      setDeleteModalVisible(false);
    }
  };

  const handleLogout = () => {
    showAlert({
      title: 'Cerrar sesión',
      message: '¿Estás seguro de que quieres salir?',
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Salir', style: 'destructive', onPress: logout },
      ],
    });
  };

  const initial = (user?.name ?? 'U')[0].toUpperCase();
  const subInfo = SUBSCRIPTION_LABELS[(user?.subscription ?? 'free').toLowerCase()] ?? SUBSCRIPTION_LABELS['free'];

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <ScrollView contentContainerStyle={[styles.scroll, { paddingBottom: 32 }]} showsVerticalScrollIndicator={false}>

        {/* ── Header / Avatar ── */}
        <View style={styles.header}>
          <TouchableOpacity
            style={styles.backButton}
            onPress={() => navigation.goBack()}
            hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
          >
            <Ionicons name="chevron-back" size={28} color={colors.text} />
          </TouchableOpacity>

          <TouchableOpacity style={styles.avatarWrapper} onPress={handlePickAvatar} activeOpacity={0.8}>
            {avatar ? (
              <Image source={{ uri: avatar }} style={styles.avatarImg} />
            ) : (
              <View style={[styles.avatarFallback, { backgroundColor: Colors.primary }]}>
                <Text style={styles.avatarInitial}>{initial}</Text>
              </View>
            )}
            <View style={[styles.cameraBtn, { backgroundColor: colors.surface, borderColor: colors.border }]}>
              <Ionicons name="camera" size={14} color={Colors.primary} />
            </View>
          </TouchableOpacity>

          <Text style={[styles.userName, { color: colors.text }]}>{user?.name}</Text>
          <Text style={[styles.userEmail, { color: colors.textSecondary }]}>{user?.email}</Text>

          {/* Subscription badge */}
          <LinearGradient
            colors={subInfo.colors}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 0 }}
            style={[styles.subBadge, {
              shadowColor: subInfo.colors[1],
              shadowOffset: { width: 0, height: 4 },
              shadowOpacity: 0.3,
              shadowRadius: 8,
              elevation: 5,
            }]}
          >
            <Text style={[styles.subBadgeText, { color: '#fff' }]}>{subInfo.label}</Text>
          </LinearGradient>

          {/* Racha y mejor racha */}
          <View style={styles.statsRow}>
            <View style={styles.statChip}>
              <Ionicons name="flame" size={18} color={Colors.streakOrange} />
              <Text style={[styles.statChipValue, { color: colors.text }]}>{user?.streak ?? 0}</Text>
              <Text style={[styles.statChipLabel, { color: colors.textSecondary }]}>Racha</Text>
            </View>
            <View style={[styles.statDivider, { backgroundColor: colors.border }]} />
            <View style={styles.statChip}>
              <Ionicons name="trophy" size={18} color={Colors.warning} />
              <Text style={[styles.statChipValue, { color: colors.text }]}>{user?.bestStreak ?? 0}</Text>
              <Text style={[styles.statChipLabel, { color: colors.textSecondary }]}>Mejor racha</Text>
            </View>
            <View style={[styles.statDivider, { backgroundColor: colors.border }]} />
            <View style={styles.statChip}>
              <Ionicons name="star" size={18} color={Colors.primary} />
              <Text style={[styles.statChipValue, { color: colors.text }]}>{(user?.xp ?? 0).toLocaleString()}</Text>
              <Text style={[styles.statChipLabel, { color: colors.textSecondary }]}>XP</Text>
            </View>
          </View>

          {/* Role badge */}
          {user?.role === 'profesor_certificado' && (
            <View style={styles.verifiedBadge}>
              <Ionicons name="shield-checkmark" size={14} color={Colors.success} />
              <Text style={[styles.verifiedText, { color: Colors.success }]}>Profesor Verificado</Text>
            </View>
          )}
          {user?.role === 'profesor_particular' && (
            <View style={[styles.verifyBanner, { backgroundColor: Colors.warningLight, borderColor: Colors.warning }]}>
              <Ionicons name="alert-circle-outline" size={16} color={Colors.warning} />
              <Text style={[styles.verifyBannerText, { color: Colors.warning }]}>
                Sin verificar — consigue la insignia desde Ajustes
              </Text>
            </View>
          )}
        </View>

        {user?.subscription === 'plus' ? (
          <WaviiPromoBanner
            title="Da el salto a Scholar"
            body="Si quieres enseñar dentro de Wavii, Scholar te abre el tablón de anuncios y las herramientas docentes."
            icon="school-outline"
            tone="education"
            ctaLabel="Ver Scholar"
            onPress={() => navigation.navigate('SubscriptionPlan', { planId: 'education' })}
          />
        ) : null}

        {user?.subscription === 'education' && !user?.teacherVerified ? (
          <WaviiPromoBanner
            title="Consigue tu insignia certificada"
            body="Ya tienes Scholar. Sube tu certificado en PDF para que Odoo revise tu perfil y te apruebe como profesor certificado."
            icon="shield-checkmark-outline"
            tone="education"
            ctaLabel="Enviar documento"
            onPress={() => navigation.navigate('TeacherVerification')}
          />
        ) : null}

        {/* ── Mi cuenta ── */}
        <SectionCard title="Mi cuenta" colors={colors}>
          <SettingRow icon="person-outline" label="Cambiar nombre" onPress={openChangeName} />
          <Divider colors={colors} />
          <SettingRow icon="lock-closed-outline" label="Cambiar contraseña" onPress={() => navigation.navigate('ChangePassword')} />
          <Divider colors={colors} />
          <SettingRow icon="card-outline" label="Mi suscripción" onPress={() => navigation.navigate('Subscription')} />
          {user?.subscription === 'education' || user?.role === 'profesor_particular' || user?.role === 'profesor_certificado' ? (
            <>
              <Divider colors={colors} />
              <SettingRow
                icon="shield-checkmark-outline"
                label="Verificación docente"
                onPress={() => navigation.navigate('TeacherVerification')}
                right={
                  <Text
                    style={[
                      styles.rowStatus,
                      { color: user?.teacherVerified ? Colors.success : Colors.warning },
                    ]}
                  >
                    {user?.teacherVerified ? 'Aprobada' : 'Pendiente'}
                  </Text>
                }
              />
            </>
          ) : null}
          <Divider colors={colors} />
          <View style={styles.appearanceCell}>
            <View style={[styles.row, { paddingBottom: 14, flexDirection: 'row', alignItems: 'center', flexWrap: 'nowrap' }]}>
              <Ionicons name="color-palette-outline" size={20} color={colors.text} style={styles.rowIcon} />
              <Text style={[styles.rowLabel, { color: colors.text }]} numberOfLines={1}>Apariencia</Text>
              
              {/* Appearance Row Options inline */}
              <View style={[styles.themeSegment, { backgroundColor: colors.background, borderColor: colors.border }]}>
                {(['light', 'dark', 'system'] as const).map((mode) => {
                  const active = themeMode === mode;
                  const iconName: React.ComponentProps<typeof Ionicons>['name'] =
                    mode === 'light' ? 'sunny' : mode === 'dark' ? 'moon' : 'phone-portrait';
                  return (
                    <TouchableOpacity
                      key={mode}
                      style={[styles.themeOption, active && { backgroundColor: Colors.primary }]}
                      onPress={() => setThemeMode(mode)}
                      activeOpacity={0.7}
                    >
                      <Ionicons name={iconName} size={14} color={active ? Colors.white : colors.textSecondary} />
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>
          </View>
        </SectionCard>

        {/* ── Soporte ── */}
        <SectionCard title="Soporte" colors={colors}>
          <SettingRow icon="help-circle-outline" label="Ayuda y FAQ" onPress={() => navigation.navigate('Help')} />
          <Divider colors={colors} />
          <SettingRow icon="compass-outline" label="Ver tutorial" onPress={startTutorial} />
          <Divider colors={colors} />
          <SettingRow icon="document-text-outline" label="Términos y condiciones" onPress={() => navigation.navigate('Terms')} />
        </SectionCard>

        {/* ── Peligro ── */}
        <SectionCard title="Cuenta" colors={colors}>
          <SettingRow icon="log-out-outline" label="Cerrar sesión" onPress={handleLogout} color={Colors.error} />
          <Divider colors={colors} />
          
          <TouchableOpacity
            style={styles.row}
            onPress={deleteLoading ? undefined : openDeleteModal}
            activeOpacity={0.7}
          >
            <Ionicons name="trash-outline" size={20} color={Colors.error} style={styles.rowIcon} />
            <Text style={[styles.rowLabel, { color: Colors.error }]}>
              {deleteLoading ? 'Procesando…' : user?.deletionScheduledAt ? 'Cancelar eliminación de cuenta' : 'Eliminar cuenta'}
            </Text>
          </TouchableOpacity>
        </SectionCard>

        <Text style={[styles.version, { color: colors.textSecondary }]}>Wavii v1.0.0</Text>
      </ScrollView>

      {/* ── Android name modal ── */}
      <Modal visible={nameModalVisible} transparent animationType="fade" onRequestClose={() => setNameModalVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Cambiar nombre</Text>
            <TextInput
              style={[styles.modalInput, { color: colors.text, borderColor: colors.border, backgroundColor: colors.background }]}
              value={nameInput}
              onChangeText={setNameInput}
              placeholder="Tu nombre"
              placeholderTextColor={colors.textSecondary}
              autoFocus
              maxLength={50}
            />
            <View style={styles.modalBtns}>
              <TouchableOpacity style={styles.modalBtnCancel} onPress={() => setNameModalVisible(false)}>
                <Text style={[styles.modalBtnText, { color: colors.textSecondary }]}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalBtnConfirm, { backgroundColor: Colors.primary, opacity: nameLoading ? 0.6 : 1 }]}
                onPress={() => { if (nameInput.trim().length >= 3) submitNameChange(nameInput.trim()); }}
                disabled={nameLoading || nameInput.trim().length < 3}
              >
                <Text style={styles.modalBtnConfirmText}>{nameLoading ? 'Guardando…' : 'Guardar'}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* ── Delete Account Modal ── */}
      <Modal visible={deleteModalVisible} transparent animationType="fade" onRequestClose={() => setDeleteModalVisible(false)}>
        <View style={styles.modalOverlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
            <Ionicons name="warning" size={48} color={Colors.error} style={{ alignSelf: 'center', marginBottom: 8 }} />
            <Text style={[styles.modalTitle, { color: colors.text }]}>¿Eliminar cuenta?</Text>
            <Text style={{ fontFamily: FontFamily.regular, fontSize: FontSize.sm, color: colors.text, textAlign: 'center', marginBottom: Spacing.sm }}>
              Esta acción programará tu cuenta para ser eliminada. Podrás seguir usando la app con normalidad hasta la fecha de eliminación y cancelarlo desde tu perfil.{'\n\n'}
              <Text style={{ fontFamily: FontFamily.bold, color: Colors.warning }}>⚠️ Si tienes suscripción activa, se cancelará al final de tu ciclo de facturación. Conservarás los beneficios hasta entonces.</Text>
            </Text>
            <View style={styles.modalBtns}>
              <TouchableOpacity style={[styles.modalBtnCancel, { borderWidth: 1, borderColor: colors.border }]} onPress={() => setDeleteModalVisible(false)}>
                <Text style={[styles.modalBtnText, { color: colors.text }]}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={[styles.modalBtnConfirm, { backgroundColor: Colors.error, opacity: deleteCountdown > 0 ? 0.5 : 1 }]}
                onPress={handleConfirmDelete}
                disabled={deleteCountdown > 0 || deleteLoading}
              >
                <Text style={styles.modalBtnConfirmText}>
                  {deleteCountdown > 0 ? `Espera (${deleteCountdown}s)` : 'Eliminar'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </SafeAreaView>
  );
};

// ── Local helpers ──

const SectionCard: React.FC<{ title: string; colors: any; children: React.ReactNode }> = ({ title, colors, children }) => (
  <View style={styles.section}>
    <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>{title.toUpperCase()}</Text>
    <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
      {children}
    </View>
  </View>
);

const Divider: React.FC<{ colors: any }> = ({ colors }) => (
  <View style={[styles.divider, { backgroundColor: colors.border }]} />
);

// ── Styles ──

const styles = StyleSheet.create({
  safe: { flex: 1 },
  scroll: {
    paddingHorizontal: Spacing.base,
    paddingBottom: 16,
  },

  // Header
  header: {
    alignItems: 'center',
    paddingTop: Spacing.xl,
    paddingBottom: Spacing.base,
    gap: Spacing.xs,
    position: 'relative',
  },
  backButton: {
    position: 'absolute',
    left: 0,
    top: Spacing.xl,
    zIndex: 10,
  },
  avatarWrapper: {
    marginBottom: Spacing.sm,
    position: 'relative',
  },
  avatarImg: {
    width: 104,
    height: 104,
    borderRadius: 52,
  },
  avatarFallback: {
    width: 104,
    height: 104,
    borderRadius: 52,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarInitial: {
    fontFamily: FontFamily.black,
    fontSize: FontSize['4xl'],
    color: Colors.white,
  },
  cameraBtn: {
    position: 'absolute',
    bottom: 2,
    right: 6,
    width: 28,
    height: 28,
    borderRadius: 14,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  userName: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
    marginTop: Spacing.xs,
  },
  userEmail: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    marginBottom: Spacing.xs,
  },
  subBadge: {
    paddingHorizontal: Spacing.md,
    paddingVertical: 3,
    borderRadius: BorderRadius.full,
    marginTop: Spacing.xs,
  },
  subBadgeText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  verifiedBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 2,
  },
  verifiedText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  verifyBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 6,
    marginTop: Spacing.xs,
  },
  verifyBannerText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    flex: 1,
  },

  // Sections
  section: { marginTop: Spacing.sm },
  sectionTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    letterSpacing: 0.8,
    marginBottom: Spacing.xs,
    marginLeft: Spacing.xs,
  },
  card: {
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
  },

  // Rows
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: 14,
  },
  rowIcon: { marginRight: Spacing.md },
  rowLabel: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
    flex: 1,
  },
  rowStatus: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  divider: { height: 1, marginLeft: Spacing.base + 20 + Spacing.md },

  // Stats row
  statsRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    gap: Spacing.sm,
    marginTop: Spacing.base,
    marginBottom: Spacing.xs,
  },
  statChip: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 1,
    minWidth: 0,
  },
  statChipValue: {
    fontFamily: FontFamily.black,
    fontSize: FontSize.lg,
  },
  statChipLabel: {
    fontFamily: FontFamily.regular,
    fontSize: 10,
  },
  statDivider: {
    width: 1,
    height: 30,
  },

  // Version
  version: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
    marginTop: Spacing.xl,
  },

  // Theme segment
  appearanceCell: {},
  themeSegmentWrapper: {
    paddingHorizontal: Spacing.base,
    paddingBottom: 12,
    alignItems: 'flex-end',
  },
  themeSegment: {
    flexDirection: 'row',
    borderRadius: BorderRadius.full,
    borderWidth: 1,
    overflow: 'hidden',
    flex: 0,
  },
  themeOption: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
  },

  // Modal
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
    justifyContent: 'center',
    padding: Spacing.xl,
  },
  modalCard: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    gap: Spacing.base,
  },
  modalTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    textAlign: 'center',
  },
  modalInput: {
    borderWidth: 1.5,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.base,
    paddingVertical: 12,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
  },
  modalBtns: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  modalBtnCancel: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderRadius: BorderRadius.md,
  },
  modalBtnText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
  },
  modalBtnConfirm: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    borderRadius: BorderRadius.md,
  },
  modalBtnConfirmText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
    color: Colors.white,
  },
});
