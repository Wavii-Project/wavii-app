import React, { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Modal,
  Platform,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { RouteProp, useFocusEffect, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import {
  apiCreateClassPost,
  apiCreateClassSession,
  apiFetchManageClasses,
  apiUpdateClassStatus,
  apiUpdateClassSession,
  ClassEnrollment,
  ClassManageResponse,
  ClassSession,
} from '../../api/classesApi';
import {
  apiFetchTeacherProfile,
  apiPostBulletin,
  apiUploadBulletinImage,
  BulletinTeacher,
} from '../../api/bulletinApi';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { hasScholarAccess } from '../../utils/subscription';

type Route = RouteProp<AppStackParamList, 'ManageClasses'>;

const AVAILABILITY_CHOICES = [
  { value: 'ANYTIME', label: 'Flexible' },
  { value: 'MORNING', label: 'Mañanas' },
  { value: 'AFTERNOON', label: 'Tardes' },
  { value: 'CUSTOM', label: 'Personalizado' },
] as const;

const INSTRUMENT_OPTIONS = ['Guitarra', 'Piano', 'Batería', 'Bajo', 'Violín', 'Flauta', 'Saxofón'] as const;

type PickerAsset = {
  uri: string;
  name: string;
  type: string;
};

type AnnouncementErrors = {
  instrument?: string;
  province?: string;
  email?: string;
  phone?: string;
  city?: string;
  bio?: string;
};

const AVAILABILITY_OPTIONS = [
  { value: 'ANYTIME', label: 'Flexible' },
  { value: 'MORNING', label: 'Mañanas' },
  { value: 'AFTERNOON', label: 'Tardes' },
  { value: 'CUSTOM', label: 'Personalizado' },
] as const;

type StatusIntent = 'accepted' | 'rejected';

const STATUS_LABELS: Record<string, string> = {
  pending: 'Pendiente',
  accepted: 'Aceptada',
  paid: 'Activa',
  scheduled: 'Agendada',
  completed: 'Completada',
  rejected: 'Rechazada',
  cancelled: 'Cancelada',
};

export const ManageClassesScreen = () => {
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();
  const route = useRoute<Route>();
  const { user, token } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const scholarAccess = hasScholarAccess(user?.subscription);
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<ClassManageResponse | null>(null);
  const [teacherBulletin, setTeacherBulletin] = useState<BulletinTeacher | null>(null);
  const [sessionModal, setSessionModal] = useState<ClassEnrollment | null>(null);
  const [postModal, setPostModal] = useState(false);
  const [editorModal, setEditorModal] = useState(false);
  const [instrumentModalVisible, setInstrumentModalVisible] = useState(false);
  const [selectedScheduleDate, setSelectedScheduleDate] = useState<Date | null>(null);
  const [selectedScheduleHour, setSelectedScheduleHour] = useState<number | null>(null);
  const [selectedScheduleMinute, setSelectedScheduleMinute] = useState<number | null>(null);
  const [meetingUrl, setMeetingUrl] = useState('');
  const [notes, setNotes] = useState('');
  const [statusIntent, setStatusIntent] = useState<{ item: ClassEnrollment; status: StatusIntent } | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [postTitle, setPostTitle] = useState('');
  const [postContent, setPostContent] = useState('');
  const [saving, setSaving] = useState(false);
  const [announcementSaving, setAnnouncementSaving] = useState(false);
  const [agendaSearch, setAgendaSearch] = useState('');
  const [agendaShowAll, setAgendaShowAll] = useState(false);

  const [instrument, setInstrument] = useState('');
  const [price, setPrice] = useState('');
  const [bio, setBio] = useState('');
  const [province, setProvince] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [contactPhone, setContactPhone] = useState('');
  const [instagramUrl, setInstagramUrl] = useState('');
  const [tiktokUrl, setTiktokUrl] = useState('');
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [facebookUrl, setFacebookUrl] = useState('');
  const [city, setCity] = useState('');
  const [address, setAddress] = useState('');
  const [modality, setModality] = useState<'PRESENCIAL' | 'ONLINE' | 'AMBAS'>('PRESENCIAL');
  const [availability, setAvailability] =
    useState<(typeof AVAILABILITY_CHOICES)[number]['value']>('ANYTIME');
  const [availabilityNotes, setAvailabilityNotes] = useState('');
  const [bannerAsset, setBannerAsset] = useState<PickerAsset | null>(null);
  const [placeAssetSlots, setPlaceAssetSlots] = useState<(PickerAsset | null)[]>([null, null, null]);
  const [announcementErrors, setAnnouncementErrors] = useState<AnnouncementErrors>({});
  const [announcementError, setAnnouncementError] = useState<string | undefined>();

  const load = useCallback(async () => {
    if (!token || !user || !scholarAccess) {
      setLoading(false);
      setData(null);
      return;
    }
    setLoading(true);
    try {
      const [classesResponse, bulletin] = await Promise.all([
        apiFetchManageClasses(token),
        apiFetchTeacherProfile(token, user.id).catch(() => null),
      ]);
      setData(classesResponse);
      setTeacherBulletin(bulletin);
      if (bulletin) {
        hydrateAnnouncement(bulletin);
      }
    } catch (err: any) {
      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo cargar la gestión.',
      });
    } finally {
      setLoading(false);
    }
  }, [scholarAccess, showAlert, token, user]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  const nextEnrollmentId = route.params?.focusEnrollmentId ?? null;

  const focusedClass = useMemo(() => {
    if (!nextEnrollmentId || !data?.classes) return null;
    return data.classes.find((entry) => entry.id === nextEnrollmentId) ?? null;
  }, [data?.classes, nextEnrollmentId]);

  const pendingRequests = useMemo(
    () => (data?.classes ?? []).filter((entry) => (entry.paymentStatus ?? '').toLowerCase() === 'pending'),
    [data?.classes]
  );

  const activeStudents = useMemo(
    () =>
      (data?.classes ?? []).filter((entry) =>
        ['accepted', 'paid', 'scheduled', 'completed', 'refund_requested'].includes(
          (entry.paymentStatus ?? '').toLowerCase()
        )
      ),
    [data?.classes]
  );

  function hydrateAnnouncement(profile: BulletinTeacher) {
    setInstrument(profile.instrument ?? '');
    setPrice(
      profile.pricePerHour != null && profile.pricePerHour > 0
        ? String(profile.pricePerHour)
        : ''
    );
    setBio(profile.bio ?? '');
    setProvince(profile.province ?? '');
    setContactEmail(profile.contactEmail ?? '');
    setContactPhone(profile.contactPhone ?? '');
    setInstagramUrl(profile.instagramUrl ?? '');
    setTiktokUrl(profile.tiktokUrl ?? '');
    setYoutubeUrl(profile.youtubeUrl ?? '');
    setFacebookUrl(profile.facebookUrl ?? '');
    setCity(profile.city ?? '');
    setAddress(profile.address ?? '');
    setModality(
      profile.classModality === 'ONLINE' || profile.classModality === 'AMBAS'
        ? profile.classModality
        : 'PRESENCIAL'
    );
    setAvailability(
      (profile.availabilityPreference as (typeof AVAILABILITY_CHOICES)[number]['value']) ??
        'ANYTIME'
    );
    setAvailabilityNotes(profile.availabilityNotes ?? '');
    setBannerAsset(null);
    setPlaceAssetSlots([null, null, null]);
    setAnnouncementErrors({});
    setAnnouncementError(undefined);
  }

  const pickImage = async (multiple: boolean) => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      showAlert({
        title: 'Permiso denegado',
        message: 'Necesitamos acceso a tu galería para seleccionar imágenes.',
      });
      return [];
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: multiple,
      selectionLimit: multiple ? 3 : 1,
      quality: 0.85,
    });

    if (result.canceled) return [];

    return result.assets.map((asset, index) => ({
      uri: asset.uri,
      name: asset.fileName ?? `image_${Date.now()}_${index}.jpg`,
      type: asset.mimeType ?? 'image/jpeg',
    }));
  };

  const uploadAsset = async (asset: PickerAsset, kind: 'banner' | 'place') => {
    if (!token) throw new Error('Sesión no válida');
    const response = await apiUploadBulletinImage(asset, token, kind);
    return response.url;
  };

  const formatScheduleDate = (value: Date | null) => {
    if (!value) return 'Selecciona una fecha';
    const day = String(value.getDate()).padStart(2, '0');
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const year = value.getFullYear();
    return `${day}/${month}/${year}`;
  };

  const formatScheduleTime = (hour: number | null, minute: number | null) => {
    if (hour == null || minute == null) return 'Selecciona una hora';
    return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
  };

  const openNativeDatePicker = () => {
    const now = new Date();
    now.setHours(0, 0, 0, 0);
    const initial = selectedScheduleDate ?? now;
    DateTimePickerAndroid.open({
      value: initial,
      mode: 'date',
      design: Platform.OS === 'android' ? 'material' : undefined,
      display: Platform.OS === 'android' ? 'calendar' : 'default',
      is24Hour: true,
      minimumDate: now,
      positiveButton: Platform.OS === 'android' ? { textColor: Colors.primary } : undefined,
      negativeButton: Platform.OS === 'android' ? { textColor: colors.textSecondary } : undefined,
      onChange: (_, date) => {
        if (!date) return;
        setSelectedScheduleDate(new Date(date.getFullYear(), date.getMonth(), date.getDate()));
      },
    });
  };

  const openNativeTimePicker = () => {
    const now = new Date();
    const initial = new Date();
    initial.setHours(selectedScheduleHour ?? now.getHours(), selectedScheduleMinute ?? now.getMinutes(), 0, 0);
    DateTimePickerAndroid.open({
      value: initial,
      mode: 'time',
      design: Platform.OS === 'android' ? 'material' : undefined,
      initialInputMode: Platform.OS === 'android' ? 'keyboard' : undefined,
      display: Platform.OS === 'android' ? 'clock' : 'default',
      is24Hour: true,
      positiveButton: Platform.OS === 'android' ? { textColor: Colors.primary } : undefined,
      negativeButton: Platform.OS === 'android' ? { textColor: colors.textSecondary } : undefined,
      onChange: (_, date) => {
        if (!date) return;
        setSelectedScheduleHour(date.getHours());
        setSelectedScheduleMinute(date.getMinutes());
      },
    });
  };

  const buildScheduledAt = () => {
    if (!selectedScheduleDate || selectedScheduleHour == null || selectedScheduleMinute == null) return null;

    const scheduled = new Date(selectedScheduleDate);
    scheduled.setHours(selectedScheduleHour, selectedScheduleMinute, 0, 0);
    if (scheduled.getTime() < Date.now()) return null;

    const finalYear = scheduled.getFullYear();
    const finalMonth = String(scheduled.getMonth() + 1).padStart(2, '0');
    const dayFormatted = String(scheduled.getDate()).padStart(2, '0');
    const hours = String(scheduled.getHours()).padStart(2, '0');
    const minutes = String(scheduled.getMinutes()).padStart(2, '0');
    return `${finalYear}-${finalMonth}-${dayFormatted}T${hours}:${minutes}:00`;
  };

  const openSessionModal = (item: ClassEnrollment) => {
    const now = new Date(Date.now() + 10 * 60 * 1000);
    setSessionModal(item);
    setSelectedScheduleDate(new Date(now.getFullYear(), now.getMonth(), now.getDate()));
    setSelectedScheduleHour(now.getHours());
    setSelectedScheduleMinute(now.getMinutes());
    setMeetingUrl('');
    setNotes('');
  };

  const scheduleClass = async () => {
    const scheduledAt = buildScheduledAt();
    if (!token || !sessionModal || !scheduledAt) {
      showAlert({
        title: 'Faltan datos',
        message: 'Selecciona fecha y hora para agendar la clase.',
      });
      return;
    }
    if (!['accepted', 'paid', 'scheduled'].includes(sessionModal.paymentStatus?.toLowerCase())) {
      showAlert({
        title: 'Solicitud pendiente',
        message: 'Primero acepta la solicitud antes de agendar una sesión.',
      });
      return;
    }
    setSaving(true);
    try {
      await apiCreateClassSession(token, sessionModal.id, {
        scheduledAt,
        meetingUrl: meetingUrl.trim() || undefined,
        notes: notes.trim() || undefined,
      });
      setSessionModal(null);
      setSelectedScheduleDate(null);
      setSelectedScheduleHour(null);
      setSelectedScheduleMinute(null);
      setMeetingUrl('');
      setNotes('');
      await load();
      showAlert({
        title: 'Clase agendada',
        message: 'El alumno ya tiene la sesión en su calendario y notificación.',
      });
    } catch (err: any) {
      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo agendar la clase.',
      });
    } finally {
      setSaving(false);
    }
  };

  const changeRequestStatus = async (item: ClassEnrollment, status: 'accepted' | 'rejected' | 'cancelled', reason?: string) => {
    if (!token) return;
    setSaving(true);
    try {
      const updated = await apiUpdateClassStatus(token, item.id, {
        status,
        reason: reason?.trim() || undefined,
      });
      setData((current) =>
        current
          ? {
              ...current,
              classes: current.classes
                .map((entry) => (entry.id === updated.id ? updated : entry))
                .filter((entry) => !['rejected', 'cancelled', 'refunded'].includes((entry.paymentStatus ?? '').toLowerCase())),
            }
          : current
      );
      showAlert({
        title:
          status === 'accepted' ? 'Solicitud aceptada' : status === 'rejected' ? 'Solicitud rechazada' : 'Solicitud cancelada',
        message:
          status === 'accepted'
            ? 'Ya puedes abrir el chat y organizar la clase.'
            : 'La solicitud quedó actualizada.',
      });
    } catch (err: any) {
      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo actualizar la solicitud.',
      });
    } finally {
      setSaving(false);
    }
  };

  const openStatusIntent = (item: ClassEnrollment, status: StatusIntent) => {
    setRejectReason('');
    setStatusIntent({ item, status });
  };

  const confirmStatusIntent = async () => {
    if (!statusIntent) return;
    await changeRequestStatus(statusIntent.item, statusIntent.status, rejectReason);
    setStatusIntent(null);
    setRejectReason('');
  };

  const doCompleteSession = async (session: ClassSession) => {
    if (!token) return;
    try {
      await apiUpdateClassSession(token, session.id, { status: 'completed' });
      await load();
    } catch (err: any) {
      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo completar la sesión.',
      });
    }
  };

  const completeSession = (session: ClassSession) => {
    showAlert({
      title: 'Marcar como completada',
      message: '¿Seguro que quieres marcar esta sesión como completada? Esta acción no se puede deshacer.',
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        { text: 'Confirmar', style: 'destructive', delaySeconds: 5, onPress: () => doCompleteSession(session) },
      ],
    });
  };

  const publishPost = async () => {
    if (!token || !user || !postTitle.trim() || !postContent.trim()) return;
    setSaving(true);
    try {
      await apiCreateClassPost(token, user.id, {
        title: postTitle.trim(),
        content: postContent.trim(),
      });
      setPostModal(false);
      setPostTitle('');
      setPostContent('');
      await load();
    } catch (err: any) {
      showAlert({
        title: 'Error',
        message: err?.response?.data?.message ?? 'No se pudo publicar la noticia.',
      });
    } finally {
      setSaving(false);
    }
  };

  const handleSaveAnnouncement = async () => {
    if (!token) return;

    const nextErrors: AnnouncementErrors = {};
    if (!instrument.trim()) nextErrors.instrument = 'El instrumento es obligatorio.';
    if (!province.trim()) nextErrors.province = 'La provincia es obligatoria.';
    if (!contactEmail.trim()) nextErrors.email = 'El correo es obligatorio.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contactEmail.trim())) {
      nextErrors.email = 'Introduce un correo válido.';
    }
    if (!contactPhone.trim()) nextErrors.phone = 'El teléfono es obligatorio.';
    else if (!/^\+?[0-9\s-]{7,20}$/.test(contactPhone.trim())) {
      nextErrors.phone = 'Introduce un teléfono válido.';
    }
    if (!bio.trim()) nextErrors.bio = 'La descripción es obligatoria.';
    if ((modality === 'PRESENCIAL' || modality === 'AMBAS') && !city.trim()) {
      nextErrors.city = 'La ciudad es obligatoria en presencial o ambas.';
    }

    setAnnouncementErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setAnnouncementError(undefined);
      return;
    }

    const numericPrice = price.trim() === '' ? null : Number(price);
    if (numericPrice !== null && (!Number.isFinite(numericPrice) || numericPrice < 0)) {
      setAnnouncementError('El precio debe ser cero o un valor positivo.');
      return;
    }

    setAnnouncementSaving(true);
    setAnnouncementError(undefined);
    try {
      const bannerImageUrl = bannerAsset
        ? await uploadAsset(bannerAsset, 'banner')
        : teacherBulletin?.bannerImageUrl ?? null;

      let placeImageUrls = teacherBulletin?.placeImageUrls ?? [];
      const selectedPlaceAssets = placeAssetSlots.filter((asset): asset is PickerAsset => asset !== null);
      if (selectedPlaceAssets.length > 0) {
        placeImageUrls = [];
        for (const asset of selectedPlaceAssets) {
          placeImageUrls.push(await uploadAsset(asset, 'place'));
        }
      }

      const saved = await apiPostBulletin(
        {
          instrument: instrument.trim(),
          pricePerHour: numericPrice,
          bio: bio.trim(),
          city: city.trim() || null,
          address: address.trim() || null,
          province: province.trim(),
          contactEmail: contactEmail.trim(),
          contactPhone: contactPhone.trim(),
          instagramUrl: instagramUrl.trim() || null,
          tiktokUrl: tiktokUrl.trim() || null,
          youtubeUrl: youtubeUrl.trim() || null,
          facebookUrl: facebookUrl.trim() || null,
          bannerImageUrl,
          placeImageUrls,
          availabilityPreference: availability,
          availabilityNotes: availabilityNotes.trim() || null,
          classModality: modality,
        },
        token
      );

      setTeacherBulletin(saved);
      hydrateAnnouncement(saved);
      setEditorModal(false);
      await load();
      showAlert({
        title: 'Anuncio actualizado',
        message: 'Tus cambios ya se reflejan en el tablón y en tu perfil.',
      });
    } catch (err: any) {
      setAnnouncementError(
        err?.response?.data?.message ?? 'No se pudo actualizar tu anuncio.'
      );
    } finally {
      setAnnouncementSaving(false);
    }
  };

  const openEditor = () => {
    if (teacherBulletin) {
      hydrateAnnouncement(teacherBulletin);
    }
    setEditorModal(true);
  };

  return (
    <SafeAreaView
      style={[styles.safe, { backgroundColor: colors.background }]}
      edges={['top', 'bottom']}
    >
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Mis clases</Text>
        <TouchableOpacity
          style={[styles.megaphoneBtn, { backgroundColor: Colors.primary }]}
          onPress={() => setPostModal(true)}
        >
          <Ionicons name="megaphone-outline" size={18} color={Colors.white} />
        </TouchableOpacity>
      </View>

      {!scholarAccess ? (
        <View style={styles.gateWrap}>
          <View style={[styles.gateCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
            <View style={[styles.gateIcon, { backgroundColor: Colors.educationTier + '20' }]}>
              <Ionicons name="lock-closed-outline" size={28} color={Colors.educationTier} />
            </View>
            <Text style={[styles.gateTitle, { color: colors.text }]}>Debes activar Scholar para usar Mis clases</Text>
            <Text style={[styles.gateText, { color: colors.textSecondary }]}>
              Sin Scholar no puedes publicar tu anuncio, recibir solicitudes ni abrir chats con tus alumnos.
            </Text>
            <WaviiButton
              title="Ver Scholar"
              onPress={() => navigation.navigate('Subscription')}
              style={styles.gatePrimaryBtn}
            />
            <WaviiButton
              title="Volver"
              variant="outline"
              onPress={() => navigation.goBack()}
              style={styles.gateSecondaryBtn}
            />
          </View>
        </View>
      ) : loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : (
        <ScrollView contentContainerStyle={styles.content}>
          {focusedClass ? (
            <View
              style={[
                styles.highlightCard,
                { backgroundColor: colors.surface, borderColor: Colors.primary },
              ]}
            >
              <Text style={[styles.cardTitle, { color: colors.text }]}>Solicitud destacada</Text>
              <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
                {focusedClass.studentName} · {focusedClass.instrument ?? 'Clase'}
              </Text>
              <View style={styles.cardActions}>
                {focusedClass.paymentStatus?.toLowerCase() === 'pending' ? (
                  <>
                    <RequestActionButton
                      label="Aceptar"
                      icon="checkmark-outline"
                      primary
                      onPress={() => openStatusIntent(focusedClass, 'accepted')}
                    />
                    <RequestActionButton
                      label="Rechazar"
                      icon="close-outline"
                      onPress={() => openStatusIntent(focusedClass, 'rejected')}
                    />
                  </>
                ) : (
                  <RequestActionButton
                    label="Agendar"
                    icon="calendar-outline"
                    primary
                    onPress={() => openSessionModal(focusedClass)}
                  />
                )}
              </View>
            </View>
          ) : null}

          <Text style={[styles.sectionTitle, { color: colors.text }]}>Mi anuncio</Text>
          {teacherBulletin ? (
            <View
              style={[
                styles.announcementCard,
                { backgroundColor: colors.surface, borderColor: colors.border },
              ]}
            >
              {teacherBulletin.bannerImageUrl ? (
                <Image
                  source={{ uri: teacherBulletin.bannerImageUrl }}
                  style={styles.announcementBanner}
                  resizeMode="cover"
                />
              ) : null}
              <View style={styles.announcementHead}>
                <View style={{ flex: 1 }}>
                  <Text style={[styles.cardTitle, { color: colors.text }]}>
                    {teacherBulletin.instrument ?? 'Tu anuncio'}
                  </Text>
                  <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
                    {teacherBulletin.pricePerHour != null
                      ? `${teacherBulletin.pricePerHour}€/h`
                      : 'Gratis'}{' '}
                    · {teacherBulletin.classModality ?? 'Modalidad sin definir'}
                  </Text>
                </View>
                <TouchableOpacity
                  style={[styles.editChip, { backgroundColor: colors.background }]}
                  onPress={openEditor}
                  activeOpacity={0.8}
                >
                  <Ionicons name="create-outline" size={15} color={Colors.primary} />
                  <Text style={styles.editChipText}>Editar</Text>
                </TouchableOpacity>
                {!!user?.id ? (
                  <TouchableOpacity
                    style={[styles.editChip, { backgroundColor: colors.background }]}
                    onPress={() => navigation.navigate('TeacherProfile', { teacherId: user.id })}
                    activeOpacity={0.8}
                  >
                    <Ionicons name="person-outline" size={15} color={Colors.primary} />
                    <Text style={styles.editChipText}>Perfil</Text>
                  </TouchableOpacity>
                ) : null}
              </View>
              {teacherBulletin.bio ? (
                <Text style={[styles.bioText, { color: colors.textSecondary }]}>
                  {teacherBulletin.bio}
                </Text>
              ) : null}
              <View style={styles.announcementMetaWrap}>
                <MetaChip
                  icon="location-outline"
                  label={
                    [teacherBulletin.city, teacherBulletin.province]
                      .filter(Boolean)
                      .join(', ') || 'Ubicación pendiente'
                  }
                  colors={colors}
                />
                <MetaChip
                  icon="time-outline"
                  label={teacherBulletin.availabilityNotes || 'Disponibilidad guardada'}
                  colors={colors}
                />
              </View>
            </View>
          ) : (
            <View
              style={[
                styles.announcementCard,
                { backgroundColor: colors.surface, borderColor: colors.border },
              ]}
            >
              <Text style={[styles.cardTitle, { color: colors.text }]}>
                Aún no tienes anuncio publicado
              </Text>
              <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
                Crea y edita tu anuncio desde aquí para que aparezca en el tablón.
              </Text>
              <WaviiButton title="Crear anuncio" variant="outline" onPress={openEditor} />
            </View>
          )}

          <View style={styles.sectionHeadRow}>
            <Text style={[styles.sectionTitle, { color: colors.text }]}>Solicitudes</Text>
            <TouchableOpacity onPress={() => navigation.navigate('TeacherClassStudents', { mode: 'requests' })}>
              <Text style={[styles.sectionLink, { color: Colors.primary }]}>Ver todas</Text>
            </TouchableOpacity>
          </View>
          {pendingRequests.length ? (
            pendingRequests.slice(0, 3).map((item) => (
              <View
                key={`request-${item.id}`}
                style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}
              >
                <View style={styles.cardHead}>
                  <View>
                    <Text style={[styles.cardTitle, { color: colors.text }]}>{item.studentName}</Text>
                    <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
                      {[item.instrument, (item.requestedModality || item.modality)?.charAt(0).toUpperCase() + (item.requestedModality || item.modality)?.slice(1).toLowerCase()].filter(Boolean).join(' · ')}
                    </Text>
                  </View>
                  <View style={[styles.statusChip, { backgroundColor: colors.background }]}>
                    <Text style={[styles.statusChipText, { color: colors.textSecondary }]}>Pendiente</Text>
                  </View>
                </View>
                {item.requestAvailability || item.requestMessage ? (
                  <View style={[styles.requestCard, { backgroundColor: colors.background }]}>
                    {item.requestAvailability ? (
                      <Text style={[styles.requestMeta, { color: colors.textSecondary }]}>
                        Disponibilidad: {item.requestAvailability}
                      </Text>
                    ) : null}
                    {item.requestMessage ? (
                      <Text style={[styles.requestMessage, { color: colors.text }]}>{item.requestMessage}</Text>
                    ) : null}
                  </View>
                ) : null}
                <View style={styles.cardActions}>
                  <RequestActionButton label="Aceptar" icon="checkmark-outline" primary onPress={() => openStatusIntent(item, 'accepted')} />
                  <RequestActionButton label="Rechazar" icon="close-outline" onPress={() => openStatusIntent(item, 'rejected')} />
                </View>
              </View>
            ))
          ) : (
            <Text style={[styles.emptyText, { color: colors.textSecondary }]}>Aún no tienes solicitudes pendientes.</Text>
          )}

          <View style={styles.sectionHeadRow}>
            <Text style={[styles.sectionTitle, { color: colors.text }]}>Alumnos</Text>
            <TouchableOpacity onPress={() => navigation.navigate('TeacherClassStudents', { mode: 'students' })}>
              <Text style={[styles.sectionLink, { color: Colors.primary }]}>Ver todos</Text>
            </TouchableOpacity>
          </View>
          {activeStudents.length ? (
            activeStudents.slice(0, 3).map((item) => (
              <View
                key={`student-${item.id}`}
                style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}
              >
                <View style={styles.cardHead}>
                  <View>
                    <Text style={[styles.cardTitle, { color: colors.text }]}>{item.studentName}</Text>
                    <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
                      {[
                        item.instrument,
                        (item.requestedModality || item.modality)?.replace(/\w+/g, (w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()),
                        item.city,
                      ].filter(Boolean).join(' · ') || 'Sin información'}
                    </Text>
                  </View>
                  <View style={[styles.statusChip, { backgroundColor: colors.background }]}>
                    <Text style={[styles.statusChipText, { color: colors.textSecondary }]}>
                      {STATUS_LABELS[item.paymentStatus?.toLowerCase()] ?? item.paymentStatus}
                    </Text>
                  </View>
                </View>
                <View style={styles.cardActions}>
                  <RequestActionButton label="Agendar" icon="calendar-outline" onPress={() => openSessionModal(item)} />
                  <RequestActionButton
                    label="Ver chat"
                    icon="chatbubble-ellipses-outline"
                    primary
                    onPress={() =>
                      navigation.navigate('ClassRoom', {
                        enrollmentId: item.id,
                        teacherName: item.teacherName,
                        teacherId: item.teacherId,
                        studentId: item.studentId,
                        studentName: item.studentName,
                      })
                    }
                  />
                </View>
              </View>
            ))
          ) : (
            <Text style={[styles.emptyText, { color: colors.textSecondary }]}>Aún no tienes alumnos aceptados.</Text>
          )}

          <View style={styles.sectionHeadRow}>
            <Text style={[styles.sectionTitle, { color: colors.text }]}>Agenda</Text>
            {(data?.sessions?.length ?? 0) > 3 && !agendaShowAll ? (
              <TouchableOpacity onPress={() => setAgendaShowAll(true)}>
                <Text style={[styles.sectionLink, { color: Colors.primary }]}>Ver todas</Text>
              </TouchableOpacity>
            ) : null}
          </View>
          {(data?.sessions?.length ?? 0) > 0 ? (
            <>
              <View style={[styles.searchBar, { backgroundColor: colors.surface, borderColor: colors.border }]}>
                <Ionicons name="search-outline" size={16} color={colors.textSecondary} />
                <TextInput
                  style={[styles.searchInput, { color: colors.text }]}
                  placeholder="Buscar sesión..."
                  placeholderTextColor={colors.textSecondary}
                  value={agendaSearch}
                  onChangeText={setAgendaSearch}
                />
                {agendaSearch.length > 0 ? (
                  <TouchableOpacity onPress={() => setAgendaSearch('')}>
                    <Ionicons name="close-circle" size={16} color={colors.textSecondary} />
                  </TouchableOpacity>
                ) : null}
              </View>
              {(() => {
                const sorted = [...(data?.sessions ?? [])].sort((a, b) => {
                  if (a.status === 'completed' && b.status !== 'completed') return 1;
                  if (a.status !== 'completed' && b.status === 'completed') return -1;
                  return 0;
                });
                const filtered = agendaSearch.trim()
                  ? sorted.filter((s) => s.studentName?.toLowerCase().includes(agendaSearch.trim().toLowerCase()))
                  : sorted;
                const visible = agendaShowAll ? filtered : filtered.slice(0, 3);
                return visible.length ? visible.map((session) => (
                  <View
                    key={session.id}
                    style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}
                  >
                    <Text style={[styles.cardTitle, { color: colors.text }]}>{session.studentName}</Text>
                    <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
                      {session.scheduledAt.replace('T', ' ').slice(0, 16)} ·{' '}
                      {session.durationMinutes} min · {STATUS_LABELS[session.status] ?? session.status}
                    </Text>
                    {session.meetingUrl ? (
                      <Text style={[styles.linkText, { color: Colors.primary }]}>{session.meetingUrl}</Text>
                    ) : null}
                    {session.status !== 'completed' ? (
                      <View style={styles.cardActions}>
                        <WaviiButton title="Marcar completada" size="sm" onPress={() => completeSession(session)} />
                      </View>
                    ) : null}
                  </View>
                )) : (
                  <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                    No hay sesiones que coincidan con la búsqueda.
                  </Text>
                );
              })()}
            </>
          ) : (
            <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
              Aún no hay sesiones agendadas.
            </Text>
          )}
        </ScrollView>
      )}

      <Modal
        visible={sessionModal !== null}
        transparent
        animationType="slide"
        onRequestClose={() => {
          setSessionModal(null);
        }}
      >
        <View style={styles.overlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Agendar clase</Text>
            <Text style={[styles.fieldLabel, { color: colors.text }]}>Fecha *</Text>
            <TouchableOpacity
              style={[styles.dateField, { backgroundColor: colors.surface, borderColor: colors.border }]}
              onPress={openNativeDatePicker}
              activeOpacity={0.85}
            >
              <Ionicons name="calendar-outline" size={17} color={Colors.primary} />
              <Text style={[styles.dateFieldText, { color: colors.text }]}>
                {formatScheduleDate(selectedScheduleDate)}
              </Text>
              <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
            </TouchableOpacity>

            <Text style={[styles.fieldLabel, { color: colors.text }]}>Hora *</Text>
            <TouchableOpacity
              style={[styles.dateField, { backgroundColor: colors.surface, borderColor: colors.border }]}
              onPress={openNativeTimePicker}
              activeOpacity={0.85}
            >
              <Ionicons name="time-outline" size={17} color={Colors.primary} />
              <Text style={[styles.dateFieldText, { color: colors.text }]}>
                {formatScheduleTime(selectedScheduleHour, selectedScheduleMinute)}
              </Text>
              <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
            </TouchableOpacity>
            <WaviiInput
              label="Enlace"
              placeholder="https://..."
              value={meetingUrl}
              onChangeText={setMeetingUrl}
            />
            <WaviiInput
              label="Notas"
              placeholder="Detalles para el alumno"
              value={notes}
              onChangeText={setNotes}
              multiline
              numberOfLines={4}
            />
            <View style={styles.modalActions}>
              <WaviiButton
                title="Cancelar"
                variant="outline"
                onPress={() => {
                  setSessionModal(null);
                }}
                style={styles.actionBtn}
              />
              <WaviiButton
                title="Guardar"
                onPress={scheduleClass}
                loading={saving}
                style={styles.actionBtn}
              />
            </View>
          </View>
        </View>
      </Modal>

      <Modal
        visible={postModal}
        transparent
        animationType="slide"
        onRequestClose={() => setPostModal(false)}
      >
        <View style={styles.overlay}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>
              Noticia para alumnos
            </Text>
            <WaviiInput label="Título *" value={postTitle} onChangeText={setPostTitle} />
            <WaviiInput
              label="Contenido *"
              value={postContent}
              onChangeText={setPostContent}
              multiline
              numberOfLines={5}
            />
            <View style={styles.modalActions}>
              <WaviiButton
                title="Cancelar"
                variant="outline"
                onPress={() => setPostModal(false)}
                style={styles.actionBtn}
              />
              <WaviiButton
                title="Publicar"
                onPress={publishPost}
                loading={saving}
                style={styles.actionBtn}
              />
            </View>
          </View>
        </View>
      </Modal>

      <Modal
        visible={statusIntent !== null}
        transparent
        animationType="fade"
        onRequestClose={() => setStatusIntent(null)}
      >
        <View style={styles.confirmOverlay}>
          <View style={[styles.confirmCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
            <View style={[styles.confirmIcon, { backgroundColor: Colors.primaryOpacity10 }]}>
              <Ionicons
                name={statusIntent?.status === 'accepted' ? 'checkmark-outline' : 'alert-circle-outline'}
                size={24}
                color={statusIntent?.status === 'accepted' ? Colors.primary : Colors.error}
              />
            </View>
            <Text style={[styles.confirmTitle, { color: colors.text }]}>
              {statusIntent?.status === 'accepted' ? 'Aceptar solicitud' : 'Rechazar solicitud'}
            </Text>
            <Text style={[styles.confirmText, { color: colors.textSecondary }]}>
              {statusIntent?.status === 'accepted'
                ? 'El alumno recibira una notificacion y el chat quedara abierto para cerrar la clase.'
                : 'El alumno recibira una notificacion. Puedes explicar el motivo si lo ves necesario.'}
            </Text>
            {statusIntent?.status === 'rejected' ? (
              <WaviiInput
                label="Motivo del rechazo"
                placeholder="Opcional"
                value={rejectReason}
                onChangeText={setRejectReason}
                multiline
                numberOfLines={4}
              />
            ) : null}
            <View style={styles.modalActions}>
              <WaviiButton
                title="Cancelar"
                variant="outline"
                onPress={() => setStatusIntent(null)}
                style={styles.actionBtn}
              />
              <WaviiButton
                title={statusIntent?.status === 'accepted' ? 'Aceptar' : 'Rechazar'}
                onPress={confirmStatusIntent}
                loading={saving}
                style={styles.actionBtn}
              />
            </View>
          </View>
        </View>
      </Modal>

      <Modal
        visible={editorModal}
        transparent
        animationType="slide"
        onRequestClose={() => setEditorModal(false)}
      >
        <KeyboardAvoidingView
          style={{ flex: 1 }}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
          <View style={styles.overlay}>
            <View style={[styles.modalCardLarge, { backgroundColor: colors.surface }]}>
              <ScrollView
                showsVerticalScrollIndicator={false}
                keyboardShouldPersistTaps="handled"
              >
                <Text style={[styles.modalTitle, { color: colors.text }]}>
                  Editar mi anuncio
                </Text>

                <Text style={[styles.fieldLabel, { color: colors.text }]}>Instrumento *</Text>
                <TouchableOpacity
                  style={[
                    styles.selectField,
                    {
                      backgroundColor: colors.surface,
                      borderColor: announcementErrors.instrument ? Colors.error : colors.border,
                    },
                  ]}
                  onPress={() => setInstrumentModalVisible(true)}
                  activeOpacity={0.8}
                >
                  <Text
                    style={[
                      styles.selectFieldText,
                      { color: instrument ? colors.text : colors.textSecondary },
                    ]}
                  >
                    {instrument || 'Selecciona un instrumento'}
                  </Text>
                  <Ionicons name="chevron-down" size={18} color={colors.textSecondary} />
                </TouchableOpacity>
                {announcementErrors.instrument ? (
                  <Text style={styles.fieldError}>{announcementErrors.instrument}</Text>
                ) : null}
                <View style={styles.priceHeader}>
                  <Text style={[styles.fieldLabel, { color: colors.text, marginTop: 0, marginBottom: 0 }]}>Precio por hora (€)</Text>
                  <TouchableOpacity
                    onPress={() =>
                      showAlert({
                        title: 'Precio por hora',
                        message: 'Si lo dejas vacío, se mostrará como Gratis.',
                      })
                    }
                    hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                  >
                    <Ionicons name="information-circle-outline" size={18} color={Colors.primary} />
                  </TouchableOpacity>
                </View>
                <WaviiInput
                  value={price}
                  onChangeText={(text) => setPrice(text.replace(/[^0-9.]/g, ''))}
                  keyboardType="decimal-pad"
                />
                <WaviiInput
                  label="Provincia *"
                  value={province}
                  onChangeText={(text) => {
                    setProvince(text);
                    setAnnouncementErrors((current) => ({
                      ...current,
                      province: undefined,
                    }));
                  }}
                  error={announcementErrors.province}
                />
                <WaviiInput
                  label="Correo de contacto *"
                  value={contactEmail}
                  onChangeText={(text) => {
                    setContactEmail(text);
                    setAnnouncementErrors((current) => ({
                      ...current,
                      email: undefined,
                    }));
                  }}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  error={announcementErrors.email}
                />
                <WaviiInput
                  label="Teléfono de contacto *"
                  value={contactPhone}
                  onChangeText={(text) => {
                    setContactPhone(text.replace(/[^0-9+\s-]/g, ''));
                    setAnnouncementErrors((current) => ({
                      ...current,
                      phone: undefined,
                    }));
                  }}
                  keyboardType="phone-pad"
                  error={announcementErrors.phone}
                />
                <WaviiInput
                  label="Instagram"
                  value={instagramUrl}
                  onChangeText={setInstagramUrl}
                  placeholder="https://instagram.com/tuusuario"
                  autoCapitalize="none"
                />
                <WaviiInput
                  label="TikTok"
                  value={tiktokUrl}
                  onChangeText={setTiktokUrl}
                  placeholder="https://tiktok.com/@tuusuario"
                  autoCapitalize="none"
                />
                <WaviiInput
                  label="YouTube"
                  value={youtubeUrl}
                  onChangeText={setYoutubeUrl}
                  placeholder="https://youtube.com/@tuusuario"
                  autoCapitalize="none"
                />
                <WaviiInput
                  label="Facebook"
                  value={facebookUrl}
                  onChangeText={setFacebookUrl}
                  placeholder="https://facebook.com/tuusuario"
                  autoCapitalize="none"
                />

                <Text style={[styles.fieldLabel, { color: colors.text }]}>
                  Modalidad de clases
                </Text>
                <View style={styles.toggleRow}>
                  {(['PRESENCIAL', 'ONLINE', 'AMBAS'] as const).map((option) => (
                    <TouchableOpacity
                      key={option}
                      style={[
                        styles.toggleChip,
                        modality === option && { backgroundColor: Colors.primary },
                      ]}
                      onPress={() => setModality(option)}
                      activeOpacity={0.8}
                    >
                      <Text
                        style={[
                          styles.toggleChipText,
                          {
                            color:
                              modality === option ? Colors.white : colors.textSecondary,
                          },
                        ]}
                      >
                        {option === 'PRESENCIAL'
                          ? 'Presencial'
                          : option === 'ONLINE'
                            ? 'Online'
                            : 'Ambas'}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>

                <WaviiInput
                  label="Ciudad *"
                  value={city}
                  onChangeText={(text) => {
                    setCity(text);
                    setAnnouncementErrors((current) => ({
                      ...current,
                      city: undefined,
                    }));
                  }}
                  hint="Opcional en online. Obligatoria en presencial o ambas."
                  error={announcementErrors.city}
                />
                <WaviiInput
                  label="Dirección"
                  value={address}
                  onChangeText={setAddress}
                />

                <Text style={[styles.fieldLabel, { color: colors.text }]}>Disponibilidad</Text>
                <View style={styles.availabilityRow}>
                  {AVAILABILITY_CHOICES.map((option) => (
                    <TouchableOpacity
                      key={option.value}
                      style={[
                        styles.availabilityChip,
                        availability === option.value && {
                          backgroundColor: Colors.primary,
                        },
                      ]}
                      onPress={() => setAvailability(option.value)}
                      activeOpacity={0.8}
                    >
                      <Text
                        style={[
                          styles.availabilityChipText,
                          {
                            color:
                              availability === option.value
                                ? Colors.white
                                : colors.textSecondary,
                          },
                        ]}
                      >
                        {option.label}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>

                <WaviiInput
                  label="Notas de horario"
                  value={availabilityNotes}
                  onChangeText={setAvailabilityNotes}
                  multiline
                  numberOfLines={3}
                />

                <View style={styles.imageActions}>
                  <WaviiButton
                    title={bannerAsset ? 'Banner listo' : 'Cambiar banner'}
                    variant="outline"
                    onPress={async () => {
                      const picked = await pickImage(false);
                      setBannerAsset(picked[0] ?? null);
                    }}
                  />
                  {[0, 1, 2].map((slotIndex) => (
                    <WaviiButton
                      key={`slot-${slotIndex}`}
                      title={
                        placeAssetSlots[slotIndex]
                          ? `Foto del lugar ${slotIndex + 1} lista`
                          : `Subir foto del lugar ${slotIndex + 1}`
                      }
                      variant="outline"
                      onPress={async () => {
                        const picked = await pickImage(false);
                        const next = [...placeAssetSlots];
                        next[slotIndex] = picked[0] ?? null;
                        setPlaceAssetSlots(next);
                      }}
                    />
                  ))}
                </View>
                {teacherBulletin?.placeImageUrls?.length ? (
                  <Text style={[styles.helperText, { color: colors.textSecondary }]}>
                    Mantendremos tus fotos actuales si no seleccionas nuevas.
                  </Text>
                ) : null}

                <WaviiInput
                  label="Descripción *"
                  value={bio}
                  onChangeText={(text) => {
                    setBio(text);
                    setAnnouncementErrors((current) => ({
                      ...current,
                      bio: undefined,
                    }));
                  }}
                  multiline
                  numberOfLines={7}
                  error={announcementErrors.bio ?? announcementError}
                />

                <View style={styles.modalActions}>
                  <WaviiButton
                    title="Cancelar"
                    variant="outline"
                    onPress={() => setEditorModal(false)}
                    style={styles.actionBtn}
                  />
                  <WaviiButton
                    title="Guardar"
                    onPress={handleSaveAnnouncement}
                    loading={announcementSaving}
                    style={styles.actionBtn}
                  />
                </View>
              </ScrollView>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      <SelectionModal
        visible={instrumentModalVisible}
        title="Selecciona un instrumento"
        options={[...INSTRUMENT_OPTIONS]}
        selected={instrument}
        onClose={() => setInstrumentModalVisible(false)}
        onSelect={(value) => {
          setInstrument(value);
          setAnnouncementErrors((current) => ({
            ...current,
            instrument: undefined,
          }));
          setInstrumentModalVisible(false);
        }}
      />
    </SafeAreaView>
  );
};

const MetaChip = ({
  icon,
  label,
  colors,
}: {
  icon: React.ComponentProps<typeof Ionicons>['name'];
  label: string;
  colors: ReturnType<typeof useTheme>['colors'];
}) => (
  <View style={[styles.metaChip, { backgroundColor: colors.background }]}>
    <Ionicons name={icon} size={14} color={Colors.primary} />
    <Text style={[styles.metaChipText, { color: colors.textSecondary }]} numberOfLines={1}>
      {label}
    </Text>
  </View>
);

const SelectionModal = ({
  visible,
  title,
  options,
  selected,
  onClose,
  onSelect,
}: {
  visible: boolean;
  title: string;
  options: string[];
  selected: string;
  onClose: () => void;
  onSelect: (value: string) => void;
}) => {
  const { colors } = useTheme();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <TouchableOpacity style={styles.modalOverlayCenter} activeOpacity={1} onPress={onClose}>
        <View style={[styles.dropdownModalCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <Text style={[styles.modalTitle, { color: colors.text, marginBottom: Spacing.md }]}>{title}</Text>
          {options.map((option) => (
            <TouchableOpacity
              key={option}
              style={[styles.dropdownOption, selected === option && { backgroundColor: Colors.primaryOpacity10 }]}
              onPress={() => onSelect(option)}
            >
              <Text style={[styles.dropdownOptionText, { color: selected === option ? Colors.primary : colors.text }]}>
                {option}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </TouchableOpacity>
    </Modal>
  );
};

const RequestActionButton = ({
  label,
  icon,
  primary,
  onPress,
}: {
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  primary?: boolean;
  onPress: () => void;
}) => {
  const { colors } = useTheme();

  return (
    <TouchableOpacity
      style={[
        styles.requestActionBtn,
        { borderColor: primary ? Colors.primary : colors.border, backgroundColor: primary ? Colors.primary : colors.surface },
      ]}
      onPress={onPress}
      activeOpacity={0.85}
    >
      <Ionicons name={icon} size={14} color={primary ? Colors.white : Colors.primary} />
      <Text style={[styles.requestActionText, { color: primary ? Colors.white : colors.text }]} numberOfLines={1}>
        {label}
      </Text>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  megaphoneBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    alignItems: 'center',
    justifyContent: 'center',
  },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  gateWrap: {
    flex: 1,
    padding: Spacing.base,
    justifyContent: 'center',
  },
  gateCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    alignItems: 'center',
    gap: Spacing.sm,
  },
  gateIcon: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  gateTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    textAlign: 'center',
  },
  gateText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
    textAlign: 'center',
  },
  gatePrimaryBtn: {
    width: '100%',
  },
  gateSecondaryBtn: {
    width: '100%',
  },
  content: { padding: Spacing.base, gap: Spacing.sm, paddingBottom: Spacing.xl },
  highlightCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  sectionTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.base,
    marginTop: Spacing.sm,
  },
  sectionHeadRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  sectionLink: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    minHeight: 38,
  },
  searchInput: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    paddingVertical: 0,
  },
  announcementCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  announcementBanner: {
    width: '100%',
    height: 140,
    borderRadius: BorderRadius.lg,
  },
  announcementHead: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  editChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    height: 34,
  },
  editChipText: {
    color: Colors.primary,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  bioText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  announcementMetaWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.xs,
  },
  metaChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: Spacing.sm,
    height: 34,
    borderRadius: BorderRadius.full,
    maxWidth: '100%',
  },
  metaChipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    flexShrink: 1,
  },
  card: {
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  cardHead: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: Spacing.sm,
  },
  cardTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  cardMeta: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 18,
    marginTop: 2,
  },
  linkText: {
    fontFamily: FontFamily.medium,
    fontSize: FontSize.xs,
    lineHeight: 18,
  },
  cardActions: { flexDirection: 'row', gap: Spacing.sm },
  actionBtn: { flex: 1 },
  statusChip: {
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 6,
  },
  statusChipText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  requestCard: {
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    gap: 4,
  },
  requestMeta: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  requestMessage: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  emptyText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'flex-end',
  },
  modalCard: {
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    padding: Spacing.xl,
  },
  modalCardLarge: {
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    padding: Spacing.xl,
    maxHeight: '92%',
  },
  modalTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    marginBottom: Spacing.base,
  },
  modalActions: { flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.sm },
  confirmOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    padding: Spacing.base,
  },
  confirmCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    gap: Spacing.sm,
  },
  confirmIcon: {
    width: 52,
    height: 52,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  confirmTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  confirmText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  fieldLabel: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
    marginBottom: Spacing.xs,
    marginTop: Spacing.sm,
  },
  dateField: {
    minHeight: 52,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  dateFieldText: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
  },
  schedulePanel: {
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    marginTop: Spacing.xs,
    marginBottom: Spacing.sm,
  },
  calendarHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.sm,
  },
  calendarNavBtn: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  calendarMonth: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
    textTransform: 'capitalize',
  },
  calendarWeekRow: {
    flexDirection: 'row',
    marginBottom: Spacing.xs,
  },
  calendarWeekDay: {
    flex: 1,
    textAlign: 'center',
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  calendarGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  calendarDay: {
    width: '14.285%',
    aspectRatio: 1,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: BorderRadius.sm,
  },
  calendarDayText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },
  timePanelTitle: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    marginBottom: Spacing.xs,
  },
  timeColumns: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  timeColumn: {
    flex: 1,
  },
  timeColumnLabel: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    marginBottom: Spacing.xs,
  },
  timeColumnList: {
    maxHeight: 176,
  },
  timeColumnItem: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: BorderRadius.md,
    paddingVertical: Spacing.xs,
    alignItems: 'center',
    marginBottom: Spacing.xs,
  },
  timeColumnItemText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  pickerDoneBtn: {
    alignSelf: 'flex-end',
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.xs,
    borderRadius: BorderRadius.full,
    backgroundColor: Colors.primaryOpacity10,
    marginBottom: Spacing.xs,
  },
  pickerDoneText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    color: Colors.primary,
  },
  priceHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: Spacing.xs,
  },
  selectField: {
    minHeight: 52,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.md,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: Spacing.sm,
    marginBottom: Spacing.xs,
  },
  selectFieldText: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
  },
  fieldError: {
    fontFamily: FontFamily.medium,
    fontSize: FontSize.xs,
    color: Colors.error,
    marginTop: 4,
    marginBottom: Spacing.xs,
  },
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
  toggleChipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  availabilityRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.xs,
    marginBottom: Spacing.sm,
  },
  availabilityChip: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 8,
  },
  availabilityChipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  imageActions: {
    gap: Spacing.sm,
    marginTop: Spacing.xs,
    marginBottom: Spacing.xs,
  },
  helperText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginBottom: Spacing.xs,
  },
  dateGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.xs,
    marginTop: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  dateOption: {
    width: '48%',
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    gap: 2,
  },
  dateOptionWeekday: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textTransform: 'capitalize',
  },
  dateOptionDay: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  timePickerWrap: {
    gap: Spacing.sm,
    marginTop: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  timeSection: {
    gap: Spacing.xs,
  },
  timeGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.xs,
  },
  timeOption: {
    width: '22%',
    minWidth: 56,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.sm,
  },
  timeOptionText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  modalOverlayCenter: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  dropdownModalCard: {
    width: '85%',
    maxHeight: '70%',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.xl,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 8,
  },
  dropdownOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.base,
    paddingHorizontal: Spacing.sm,
    borderRadius: BorderRadius.sm,
  },
  dropdownOptionText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
  },
  requestActionBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    minHeight: 36,
    flex: 1,
  },
  requestActionText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    flexShrink: 1,
  },
});
