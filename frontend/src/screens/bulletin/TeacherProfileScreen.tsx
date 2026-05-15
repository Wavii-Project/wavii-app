import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ActivityIndicator, Image, Linking, Modal, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { apiRequestClass, ClassEnrollment } from '../../api/classesApi';
import { apiFetchTeacherProfile, apiReportTeacher, BulletinTeacher } from '../../api/bulletinApi';
import { NotificationBell } from '../../components/common/NotificationBell';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';

type Route = RouteProp<AppStackParamList, 'TeacherProfile'>;
type Navigation = NativeStackNavigationProp<AppStackParamList>;

const AVAILABILITY_LABELS: Record<string, string> = {
  MORNING: 'Mañanas',
  AFTERNOON: 'Tardes',
  ANYTIME: 'Horario flexible',
  CUSTOM: 'Horario personalizado',
};

const REQUEST_MODALITIES = ['PRESENCIAL', 'ONLINE', 'AMBAS'] as const;

export const TeacherProfileScreen = () => {
  const route = useRoute<Route>();
  const navigation = useNavigation<Navigation>();
  const { colors } = useTheme();
  const { token, user } = useAuth();
  const { showAlert } = useAlert();

  const [teacher, setTeacher] = useState<BulletinTeacher | null>(null);
  const [loading, setLoading] = useState(true);
  const [requestModalVisible, setRequestModalVisible] = useState(false);
  const [requesting, setRequesting] = useState(false);
  const [requestMessage, setRequestMessage] = useState('');
  const [requestAvailability, setRequestAvailability] = useState('');
  const [requestModality, setRequestModality] = useState<(typeof REQUEST_MODALITIES)[number]>('AMBAS');
  const [reportModal, setReportModal] = useState(false);
  const [reportReason, setReportReason] = useState('');
  const [reportDetails, setReportDetails] = useState('');
  const [sendingReport, setSendingReport] = useState(false);
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const hasLoadedRef = useRef(false);

  const isOwnProfile = user?.id === route.params.teacherId;

  const load = useCallback(async () => {
    if (!token) return;
    if (!hasLoadedRef.current) {
      setLoading(true);
    }
    try {
      const profile = await apiFetchTeacherProfile(token, route.params.teacherId);
      setTeacher(profile);
      hasLoadedRef.current = true;
    } catch (err: any) {
      showAlert({ title: 'Error', message: err?.response?.data?.message ?? 'No se pudo cargar el perfil.' });
      navigation.goBack();
    } finally {
      setLoading(false);
    }
  }, [navigation, route.params.teacherId, showAlert, token]);

  useEffect(() => {
    load();
  }, [load]);

  const handleContact = async (kind: 'phone' | 'email') => {
    if (!teacher) return;
    const target = kind === 'phone' ? teacher.contactPhone : teacher.contactEmail;
    if (!target) return;
    const url = kind === 'phone' ? `tel:${target}` : `mailto:${target}`;
    await Linking.openURL(url).catch(() => {
      showAlert({ title: 'Error', message: 'No se pudo abrir la app de contacto.' });
    });
  };

  const openExternalUrl = async (url?: string | null) => {
    if (!url) return;
    const normalized = /^https?:\/\//i.test(url) ? url : `https://${url}`;
    await Linking.openURL(normalized).catch(() => {
      showAlert({ title: 'Error', message: 'No se pudo abrir el enlace.' });
    });
  };

  const openRequestModal = () => {
    if (!teacher || isOwnProfile) return;
    setRequestMessage('');
    setRequestAvailability('');
    setRequestModality((teacher.classModality as (typeof REQUEST_MODALITIES)[number]) || 'AMBAS');
    setRequestModalVisible(true);
  };

  const handleRequestClass = async () => {
    if (!token || !teacher || isOwnProfile) return;
    setRequesting(true);
    try {
      const requested: ClassEnrollment = await apiRequestClass(token, teacher.id, {
        message: requestMessage.trim() || undefined,
        availability: requestAvailability.trim() || undefined,
        requestedModality: requestModality,
      });
      setRequestModalVisible(false);
      showAlert({
        title: 'Solicitud enviada',
        message: 'El profesor ya puede verla y responder desde la app.',
        buttons: [
          {
            text: 'Ir a Clases',
            onPress: () =>
              navigation.navigate('MainTabs', {
                screen: 'Clases',
                params: {
                  refreshKey: `${Date.now()}`,
                  justRequestedClass: requested,
                },
              }),
          },
        ],
      });
    } catch (err: any) {
      showAlert({ title: 'Error', message: err?.response?.data?.message ?? 'No se pudo enviar la solicitud.' });
    } finally {
      setRequesting(false);
    }
  };

  const handleReport = async () => {
    if (!teacher || !reportReason.trim()) {
      showAlert({ title: 'Falta el motivo', message: 'Indica al menos el motivo del reporte.' });
      return;
    }
    setSendingReport(true);
    try {
      await apiReportTeacher(teacher.id, {
        reason: reportReason.trim(),
        details: reportDetails.trim() || undefined,
      });
      setReportModal(false);
      setReportReason('');
      setReportDetails('');
      showAlert({ title: 'Reporte enviado', message: 'Gracias. Revisaremos este caso desde Wavii.' });
    } catch (err: any) {
      showAlert({ title: 'Error', message: err?.response?.data?.message ?? 'No se pudo enviar el reporte.' });
    } finally {
      setSendingReport(false);
    }
  };

  const availabilityLabel = teacher?.availabilityPreference ? AVAILABILITY_LABELS[teacher.availabilityPreference] ?? teacher.availabilityPreference : 'No indicado';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Perfil del profesor</Text>
        <NotificationBell size="sm" />
      </View>

      {loading ? (
        <View style={styles.centered}>
          <ActivityIndicator size="large" color={Colors.primary} />
        </View>
      ) : teacher ? (
        <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
          <View style={[styles.heroShell, { backgroundColor: colors.surface, borderColor: colors.border }]}>
            {teacher.bannerImageUrl ? (
              <Image source={{ uri: teacher.bannerImageUrl }} style={styles.banner} resizeMode="cover" />
            ) : (
              <View style={[styles.bannerFallback, { backgroundColor: Colors.primaryOpacity10 }]} />
            )}

            <View style={styles.heroCard}>
              <View style={styles.nameRow}>
                <View style={[styles.avatar, { backgroundColor: Colors.primary }]}>
                  <Text style={styles.avatarText}>{teacher.name.charAt(0).toUpperCase()}</Text>
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.name, { color: colors.text }]}>{teacher.name}</Text>
                  <Text style={[styles.meta, { color: colors.textSecondary }]}>
                    {teacher.instrument ?? 'Profesor en Wavii'} · {teacher.pricePerHour != null ? `${teacher.pricePerHour}€/h` : 'Gratis'}
                  </Text>
                </View>
              </View>

              <View style={styles.chipsRow}>
                <InfoChip icon="swap-horizontal-outline" label={teacher.classModality === 'ONLINE' ? 'Online' : teacher.classModality === 'AMBAS' ? 'Presencial y online' : 'Presencial'} colors={colors} />
                <InfoChip icon="location-outline" label={teacher.city || 'Ciudad no indicada'} colors={colors} />
                <InfoChip icon="time-outline" label={availabilityLabel} colors={colors} />
              </View>

              <Text style={[styles.bio, { color: colors.textSecondary }]}>{teacher.bio ?? 'Sin descripcion todavia.'}</Text>

              <View style={[styles.infoPanel, { backgroundColor: colors.background }]}>
                <InfoRow label="Provincia" value={teacher.province || 'No indicada'} colors={colors} />
                <InfoRow label="Ciudad" value={teacher.city || 'No indicada'} colors={colors} />
                {teacher.address ? <InfoRow label="Direccion" value={teacher.address} colors={colors} /> : null}
                <InfoRow label="Telefono" value={teacher.contactPhone || 'No indicado'} colors={colors} />
                <InfoRow label="Correo" value={teacher.contactEmail || 'No indicado'} colors={colors} />
                <InfoRow label="Disponibilidad" value={availabilityLabel} colors={colors} />
                {teacher.availabilityNotes ? <InfoRow label="Detalle horario" value={teacher.availabilityNotes} colors={colors} /> : null}
              </View>

              <View style={styles.contactActions}>
                {teacher.contactPhone ? (
                  <TouchableOpacity style={[styles.contactBtn, { borderColor: colors.border }]} onPress={() => handleContact('phone')}>
                    <Ionicons name="call-outline" size={16} color={Colors.primary} />
                    <Text style={[styles.contactBtnText, { color: colors.text }]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.82}>Llamar</Text>
                  </TouchableOpacity>
                ) : null}
                {teacher.contactEmail ? (
                  <TouchableOpacity style={[styles.contactBtn, { borderColor: colors.border }]} onPress={() => handleContact('email')}>
                    <Ionicons name="mail-outline" size={16} color={Colors.primary} />
                    <Text style={[styles.contactBtnText, { color: colors.text }]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.82}>Correo</Text>
                  </TouchableOpacity>
                ) : null}
                {!isOwnProfile ? (
                  <TouchableOpacity style={[styles.contactBtn, styles.reportBtn, { borderColor: colors.border }]} onPress={() => setReportModal(true)}>
                    <Ionicons name="flag-outline" size={16} color={Colors.error} />
                    <Text style={[styles.contactBtnText, styles.reportBtnText, { color: colors.text }]} numberOfLines={1} adjustsFontSizeToFit minimumFontScale={0.75}>Reportar</Text>
                  </TouchableOpacity>
                ) : null}
              </View>

              {teacher.instagramUrl || teacher.tiktokUrl || teacher.youtubeUrl || teacher.facebookUrl ? (
                <View style={styles.socialActions}>
                  {teacher.instagramUrl ? (
                    <TouchableOpacity style={[styles.socialBtn, { borderColor: colors.border }]} onPress={() => openExternalUrl(teacher.instagramUrl)}>
                      <Ionicons name="logo-instagram" size={15} color={Colors.primary} />
                      <Text style={[styles.socialBtnText, { color: colors.text }]} numberOfLines={1}>Instagram</Text>
                    </TouchableOpacity>
                  ) : null}
                  {teacher.tiktokUrl ? (
                    <TouchableOpacity style={[styles.socialBtn, { borderColor: colors.border }]} onPress={() => openExternalUrl(teacher.tiktokUrl)}>
                      <Ionicons name="logo-tiktok" size={15} color={Colors.primary} />
                      <Text style={[styles.socialBtnText, { color: colors.text }]} numberOfLines={1}>TikTok</Text>
                    </TouchableOpacity>
                  ) : null}
                  {teacher.youtubeUrl ? (
                    <TouchableOpacity style={[styles.socialBtn, { borderColor: colors.border }]} onPress={() => openExternalUrl(teacher.youtubeUrl)}>
                      <Ionicons name="logo-youtube" size={15} color={Colors.primary} />
                      <Text style={[styles.socialBtnText, { color: colors.text }]} numberOfLines={1}>YouTube</Text>
                    </TouchableOpacity>
                  ) : null}
                  {teacher.facebookUrl ? (
                    <TouchableOpacity style={[styles.socialBtn, { borderColor: colors.border }]} onPress={() => openExternalUrl(teacher.facebookUrl)}>
                      <Ionicons name="logo-facebook" size={15} color={Colors.primary} />
                      <Text style={[styles.socialBtnText, { color: colors.text }]} numberOfLines={1}>Facebook</Text>
                    </TouchableOpacity>
                  ) : null}
                </View>
              ) : null}

              {!isOwnProfile ? (
                <WaviiButton
                  title="Solicitar clase"
                  onPress={openRequestModal}
                />
              ) : (
                <View style={[styles.ownBanner, { backgroundColor: colors.background }]}>
                  <Ionicons name="information-circle-outline" size={18} color={Colors.primary} />
                  <Text style={[styles.ownBannerText, { color: colors.textSecondary }]}>
                    Este es tu propio perfil docente. La gestion de tus alumnos esta en Clases y Menu.
                  </Text>
                </View>
              )}
            </View>

            {teacher.placeImageUrls?.length ? (
              <View style={[styles.gallery, { borderTopColor: colors.border }]}>
                <Text style={[styles.sectionTitle, { color: colors.text }]}>Fotos del lugar</Text>
                <View style={styles.galleryGrid}>
                  {teacher.placeImageUrls.map((url) => (
                    <TouchableOpacity key={url} style={styles.galleryItem} activeOpacity={0.86} onPress={() => setSelectedImageUrl(url)}>
                      <Image source={{ uri: url }} style={styles.galleryImage} resizeMode="cover" />
                    </TouchableOpacity>
                  ))}
                </View>
              </View>
            ) : null}
          </View>
        </ScrollView>
      ) : null}

      <Modal visible={reportModal} transparent animationType="slide" onRequestClose={() => setReportModal(false)}>
        <View style={styles.overlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Reportar profesor</Text>
            <WaviiInput label="Motivo" placeholder="Cobro incorrecto, conducta inapropiada..." value={reportReason} onChangeText={setReportReason} />
            <WaviiInput label="Detalles" placeholder="Cuéntanos un poco mas" value={reportDetails} onChangeText={setReportDetails} multiline numberOfLines={5} />
            <View style={styles.modalActions}>
              <WaviiButton title="Cancelar" variant="outline" onPress={() => setReportModal(false)} style={styles.modalBtn} />
              <WaviiButton title="Enviar" onPress={handleReport} loading={sendingReport} style={styles.modalBtn} />
            </View>
          </View>
        </View>
      </Modal>

      <Modal visible={requestModalVisible} transparent animationType="slide" onRequestClose={() => setRequestModalVisible(false)}>
        <View style={styles.overlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Solicitar clase</Text>
            <Text style={[styles.requestHint, { color: colors.textSecondary }]}>
              Cuéntale al profesor cuándo sueles poder conectarte y qué modalidad te encaja mejor.
            </Text>
            <WaviiInput
              label="Disponibilidad"
              placeholder="Martes tarde, jueves a partir de las 18:00..."
              value={requestAvailability}
              onChangeText={setRequestAvailability}
              multiline
              numberOfLines={3}
            />
            <Text style={[styles.fieldLabel, { color: colors.text }]}>Modalidad preferida</Text>
            <View style={styles.toggleRow}>
              {REQUEST_MODALITIES.map((option) => (
                <TouchableOpacity
                  key={option}
                  style={[
                    styles.toggleChip,
                    requestModality === option && { backgroundColor: Colors.primary },
                  ]}
                  onPress={() => setRequestModality(option)}
                  activeOpacity={0.8}
                >
                  <Text
                    style={[
                      styles.toggleChipText,
                      {
                        color: requestModality === option ? Colors.white : colors.textSecondary,
                      },
                    ]}
                  >
                    {option === 'PRESENCIAL' ? 'Presencial' : option === 'ONLINE' ? 'Online' : 'Ambas'}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
            <WaviiInput
              label="Mensaje al profesor"
              placeholder="Cuéntale qué quieres trabajar o qué te gustaría mejorar"
              value={requestMessage}
              onChangeText={setRequestMessage}
              multiline
              numberOfLines={5}
            />
            <View style={styles.modalActions}>
              <WaviiButton
                title="Cancelar"
                variant="outline"
                onPress={() => setRequestModalVisible(false)}
                style={styles.modalBtn}
              />
              <WaviiButton
                title="Enviar solicitud"
                onPress={handleRequestClass}
                loading={requesting}
                style={styles.modalBtn}
              />
            </View>
          </View>
        </View>
      </Modal>

      <Modal visible={selectedImageUrl !== null} transparent animationType="fade" onRequestClose={() => setSelectedImageUrl(null)}>
        <View style={styles.imageOverlay}>
          <TouchableOpacity style={styles.imageCloseBtn} onPress={() => setSelectedImageUrl(null)}>
            <Ionicons name="close" size={22} color={Colors.white} />
          </TouchableOpacity>
          {selectedImageUrl ? (
            <Image source={{ uri: selectedImageUrl }} style={styles.imagePreview} resizeMode="contain" />
          ) : null}
        </View>
      </Modal>
    </SafeAreaView>
  );
};

const InfoChip = ({
  icon,
  label,
  colors,
}: {
  icon: React.ComponentProps<typeof Ionicons>['name'];
  label: string;
  colors: ReturnType<typeof useTheme>['colors'];
}) => (
  <View style={[styles.infoChip, { backgroundColor: colors.background }]}>
    <Ionicons name={icon} size={13} color={Colors.primary} />
    <Text style={[styles.infoChipText, { color: colors.textSecondary }]} numberOfLines={1}>
      {label}
    </Text>
  </View>
);

const InfoRow = ({
  label,
  value,
  colors,
}: {
  label: string;
  value: string;
  colors: ReturnType<typeof useTheme>['colors'];
}) => (
  <View style={styles.infoRow}>
    <Text style={[styles.infoLabel, { color: colors.textSecondary }]}>{label}</Text>
    <Text style={[styles.infoValue, { color: colors.text }]}>{value}</Text>
  </View>
);

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: Spacing.base, paddingVertical: Spacing.sm, borderBottomWidth: 1 },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  content: { padding: Spacing.base, gap: Spacing.base, paddingBottom: Spacing.xl },
  heroShell: { borderWidth: 1, borderRadius: BorderRadius.xl, overflow: 'hidden' },
  banner: { width: '100%', height: 220 },
  bannerFallback: { width: '100%', height: 220 },
  heroCard: { padding: Spacing.base, gap: Spacing.base },
  nameRow: { flexDirection: 'row', gap: Spacing.sm, alignItems: 'center' },
  avatar: { width: 64, height: 64, borderRadius: 32, alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontFamily: FontFamily.black, fontSize: FontSize.xl, color: Colors.white },
  name: { fontFamily: FontFamily.bold, fontSize: FontSize.xl },
  meta: { fontFamily: FontFamily.regular, fontSize: FontSize.sm },
  chipsRow: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs },
  infoChip: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: Spacing.sm, paddingVertical: 6, borderRadius: BorderRadius.full },
  infoChipText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs, flexShrink: 1, maxWidth: 140 },
  bio: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  infoPanel: { borderRadius: BorderRadius.lg, padding: Spacing.base, gap: Spacing.sm },
  infoRow: { gap: 2 },
  infoLabel: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  infoValue: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  contactActions: { flexDirection: 'row', gap: Spacing.xs },
  contactBtn: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 4, borderWidth: 1, borderRadius: BorderRadius.md, paddingHorizontal: Spacing.xs, paddingVertical: 10, minWidth: 0 },
  reportBtn: { flex: 1.2 },
  contactBtnText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs, flexShrink: 1 },
  reportBtnText: { fontSize: FontSize.xs },
  socialActions: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs },
  socialBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 8,
  },
  socialBtnText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  ownBanner: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm, borderRadius: BorderRadius.lg, padding: Spacing.base },
  ownBannerText: { flex: 1, fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 18 },
  gallery: { gap: Spacing.sm, padding: Spacing.base, borderTopWidth: 1 },
  sectionTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.base },
  galleryGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs },
  galleryItem: { flexBasis: '48%', flexGrow: 1, minWidth: 140 },
  galleryImage: { width: '100%', aspectRatio: 1.25, borderRadius: BorderRadius.lg },
  imageOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.86)', justifyContent: 'center', alignItems: 'center', padding: Spacing.base },
  imagePreview: { width: '100%', height: '82%' },
  imageCloseBtn: { position: 'absolute', top: Spacing.xl, right: Spacing.base, width: 40, height: 40, borderRadius: 20, backgroundColor: 'rgba(255,255,255,0.14)', alignItems: 'center', justifyContent: 'center', zIndex: 10 },
  overlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modalCard: { borderTopLeftRadius: BorderRadius.xl, borderTopRightRadius: BorderRadius.xl, padding: Spacing.xl },
  modalTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg, marginBottom: Spacing.base },
  modalActions: { flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.sm },
  modalBtn: { flex: 1 },
  requestHint: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20, marginBottom: Spacing.sm },
  fieldLabel: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, marginBottom: Spacing.xs, marginTop: Spacing.sm },
  toggleRow: { flexDirection: 'row', gap: Spacing.xs, marginBottom: Spacing.sm },
  toggleChip: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: BorderRadius.full,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingVertical: Spacing.xs,
  },
  toggleChipText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
});
