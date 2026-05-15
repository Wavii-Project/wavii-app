import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
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
import * as ImagePicker from 'expo-image-picker';
import * as Location from 'expo-location';
import { Ionicons } from '@expo/vector-icons';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import {
  apiFetchBulletin,
  apiPostBulletin,
  apiUploadBulletinImage,
  BulletinBoardResponse,
  BulletinTeacher,
} from '../../api/bulletinApi';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { useAlert } from '../../context/AlertContext';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';
import { NotificationBell } from '../../components/common/NotificationBell';

type Props = {
  navigation: NativeStackNavigationProp<AppStackParamList, 'BulletinBoard'>;
};

const INSTRUMENTS = ['Todos', 'Guitarra', 'Piano', 'Batería', 'Bajo', 'Violín', 'Flauta', 'Saxofón'];
const ROLE_FILTERS = ['Todos', 'Certificados', 'Particulares'] as const;
const MODALITIES = ['Todos', 'PRESENCIAL', 'ONLINE', 'AMBAS'] as const;
const MODALITY_LABEL: Record<string, string> = {
  Todos: 'Modalidad',
  PRESENCIAL: 'Presencial',
  ONLINE: 'Online',
  AMBAS: 'Ambas',
};
const ROLE_LABEL: Record<string, string> = {
  Todos: 'Rol',
  Certificados: 'Certificados',
  Particulares: 'Particulares',
};

type PickerAsset = {
  uri: string;
  name: string;
  type: string;
};

type FieldErrors = {
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

const INSTRUMENT_OPTIONS = ['Guitarra', 'Piano', 'Batería', 'Bajo', 'Violín', 'Flauta', 'Saxofón'] as const;
const AVAILABILITY_FILTERS = [
  { value: 'Todos', label: 'Disponibilidad' },
  { value: 'ANYTIME', label: 'Flexible' },
  { value: 'MORNING', label: 'Mañanas' },
  { value: 'AFTERNOON', label: 'Tardes' },
  { value: 'CUSTOM', label: 'Personalizado' },
] as const;

const ANNOUNCEMENT_AVAILABILITY_OPTIONS = [
  { value: 'ANYTIME', label: 'Flexible' },
  { value: 'MORNING', label: 'Mañanas' },
  { value: 'AFTERNOON', label: 'Tardes' },
  { value: 'CUSTOM', label: 'Personalizado' },
] as const;

export const BulletinBoardScreen: React.FC<Props> = ({ navigation }) => {
  const { token } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();

  const [board, setBoard] = useState<BulletinBoardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [filterInstrument, setFilterInstrument] = useState('Todos');
  const [filterRole, setFilterRole] = useState<(typeof ROLE_FILTERS)[number]>('Todos');
  const [filterModality, setFilterModality] = useState<(typeof MODALITIES)[number]>('Todos');
  const [filterAvailability, setFilterAvailability] = useState<(typeof AVAILABILITY_FILTERS)[number]['value']>('Todos');

  const [modalVisible, setModalVisible] = useState(false);
  const [instrumentModalVisible, setInstrumentModalVisible] = useState(false);
  const [roleModalVisible, setRoleModalVisible] = useState(false);
  const [modalityModalVisible, setModalityModalVisible] = useState(false);
  const [availabilityModalVisible, setAvailabilityModalVisible] = useState(false);
  const [postInstrumentModalVisible, setPostInstrumentModalVisible] = useState(false);

  const [postInstrument, setPostInstrument] = useState('');
  const [postPrice, setPostPrice] = useState('');
  const [postBio, setPostBio] = useState('');
  const [postProvince, setPostProvince] = useState('');
  const [postEmail, setPostEmail] = useState('');
  const [postPhone, setPostPhone] = useState('');
  const [postInstagram, setPostInstagram] = useState('');
  const [postTiktok, setPostTiktok] = useState('');
  const [postYoutube, setPostYoutube] = useState('');
  const [postFacebook, setPostFacebook] = useState('');
  const [postCity, setPostCity] = useState('');
  const [postAddress, setPostAddress] = useState('');
  const [postModality, setPostModality] = useState<'PRESENCIAL' | 'ONLINE' | 'AMBAS'>('PRESENCIAL');
  const [postAvailability, setPostAvailability] = useState<(typeof ANNOUNCEMENT_AVAILABILITY_OPTIONS)[number]['value']>('ANYTIME');
  const [postAvailabilityNotes, setPostAvailabilityNotes] = useState('');
  const [postLat, setPostLat] = useState<number | undefined>();
  const [postLon, setPostLon] = useState<number | undefined>();
  const [bannerAsset, setBannerAsset] = useState<PickerAsset | null>(null);
  const [placeAssetSlots, setPlaceAssetSlots] = useState<(PickerAsset | null)[]>([null, null, null]);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [formError, setFormError] = useState<string | undefined>();
  const [posting, setPosting] = useState(false);

  const loadBoard = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const data = await apiFetchBulletin(token, {
        query: search.trim() || undefined,
        instrument: filterInstrument !== 'Todos' ? filterInstrument : undefined,
        role: filterRole !== 'Todos' ? filterRole : undefined,
        modality: filterModality !== 'Todos' ? filterModality : undefined,
        availability: filterAvailability !== 'Todos' ? filterAvailability : undefined,
      });
      setBoard(data);
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'No se pudo cargar el tablón de anuncios.';
      showAlert({ title: 'Error', message: msg });
    } finally {
      setLoading(false);
    }
  }, [filterAvailability, filterInstrument, filterModality, filterRole, search, showAlert, token]);

  useEffect(() => {
    loadBoard();
  }, [loadBoard]);

  const teachers = useMemo(() => board?.teachers ?? [], [board]);
  const hasFullAccess = board?.hasFullAccess ?? false;
  const hiddenCount = board?.hiddenCount ?? 0;

  const clearFieldError = (key: keyof FieldErrors) => {
    setFieldErrors((current) => ({ ...current, [key]: undefined }));
  };

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

  const handleAutofillLocation = async () => {
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') {
        showAlert({
          title: 'Permiso denegado',
          message: 'Necesitamos permiso de ubicación para rellenar ciudad y provincia.',
        });
        return;
      }

      const position = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });
      const [geo] = await Location.reverseGeocodeAsync({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      });

      const nextCity = geo?.city ?? geo?.subregion ?? geo?.region ?? '';
      const nextProvince = geo?.region ?? geo?.subregion ?? '';
      const nextAddress = [geo?.street, geo?.streetNumber].filter(Boolean).join(' ');

      if (nextCity) setPostCity(nextCity);
      if (nextProvince) setPostProvince(nextProvince);
      if (nextAddress) setPostAddress(nextAddress);
      setPostLat(position.coords.latitude);
      setPostLon(position.coords.longitude);
      clearFieldError('city');
      clearFieldError('province');
    } catch (err: any) {
      showAlert({
        title: 'No pudimos usar tu ubicación',
        message: err?.message ?? 'Prueba otra vez en unos segundos.',
      });
    }
  };

  const handlePost = async () => {
    if (!token) return;

    const instrument = postInstrument.trim();
    const province = postProvince.trim();
    const email = postEmail.trim();
    const phone = postPhone.trim();
    const city = postCity.trim();
    const bio = postBio.trim();

    const nextErrors: FieldErrors = {};
    if (!instrument) nextErrors.instrument = 'El instrumento es obligatorio.';
    if (!province) nextErrors.province = 'La provincia es obligatoria.';
    if (!email) nextErrors.email = 'El correo de contacto es obligatorio.';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) nextErrors.email = 'Introduce un correo válido.';
    if (!phone) nextErrors.phone = 'El teléfono de contacto es obligatorio.';
    else if (!/^\+?[0-9\s-]{7,20}$/.test(phone)) nextErrors.phone = 'Introduce un teléfono válido.';
    if (!bio) nextErrors.bio = 'La descripción es obligatoria.';
    if ((postModality === 'PRESENCIAL' || postModality === 'AMBAS') && !city) {
      nextErrors.city = 'La ciudad es obligatoria en presencial o ambas.';
    }

    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      setFormError(undefined);
      return;
    }

    const price = postPrice.trim() === '' ? null : Number(postPrice);
    if (price !== null && (!Number.isFinite(price) || price <= 0)) {
      setFormError('El precio debe ser positivo o deja el campo vacío para Gratis.');
      return;
    }

    setPosting(true);
    setFormError(undefined);
    try {
      const bannerUrl = bannerAsset ? await uploadAsset(bannerAsset, 'banner') : null;
      const placeUrls: string[] = [];
      for (const asset of placeAssetSlots) {
        if (!asset) continue;
        placeUrls.push(await uploadAsset(asset, 'place'));
      }

      await apiPostBulletin(
        {
          instrument,
          pricePerHour: price,
          bio,
          city: city || null,
          latitude: postLat,
          longitude: postLon,
          address: postAddress.trim() || null,
          province,
          contactEmail: email,
          contactPhone: phone,
          instagramUrl: postInstagram.trim() || null,
          tiktokUrl: postTiktok.trim() || null,
          youtubeUrl: postYoutube.trim() || null,
          facebookUrl: postFacebook.trim() || null,
          bannerImageUrl: bannerUrl,
          placeImageUrls: placeUrls,
          availabilityPreference: postAvailability,
          availabilityNotes: postAvailabilityNotes.trim() || null,
          classModality: postModality,
        },
        token
      );

      setModalVisible(false);
      setPostInstrument('');
      setPostPrice('');
      setPostBio('');
      setPostProvince('');
      setPostEmail('');
      setPostPhone('');
      setPostInstagram('');
      setPostTiktok('');
      setPostYoutube('');
      setPostFacebook('');
      setPostCity('');
      setPostAddress('');
      setPostAvailability('ANYTIME');
      setPostAvailabilityNotes('');
      setPostLat(undefined);
      setPostLon(undefined);
      setBannerAsset(null);
      setPlaceAssetSlots([null, null, null]);
      setFieldErrors({});
      setFormError(undefined);
      await loadBoard();
      showAlert({ title: 'Publicado', message: 'Tu anuncio ya aparece en el tablón.' });
    } catch (err: any) {
      setFormError(err?.response?.data?.message ?? err?.message ?? 'No se pudo publicar el anuncio.');
    } finally {
      setPosting(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <View style={styles.headerCenter}>
          <Text style={[styles.headerTitle, { color: colors.text }]}>Tablón de anuncios</Text>
        </View>
        <View style={styles.headerRight}>
          <NotificationBell size="sm" />
        </View>
      </View>

      <View style={[styles.searchWrap, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <Ionicons name="search-outline" size={16} color={colors.textSecondary} />
        <TextInput
          style={[styles.searchInput, { color: colors.text }]}
          placeholder="Buscar profesor o ciudad"
          placeholderTextColor={colors.textSecondary}
          value={search}
          onChangeText={setSearch}
        />
        {search ? (
          <TouchableOpacity onPress={() => setSearch('')} hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}>
            <Ionicons name="close-circle" size={16} color={colors.textSecondary} />
          </TouchableOpacity>
        ) : null}
      </View>

      <View style={styles.filtersShell}>
        <FilterChip
          label={filterInstrument === 'Todos' ? 'Instrumento' : filterInstrument}
          active={filterInstrument !== 'Todos'}
          icon="musical-notes-outline"
          onPress={() => setInstrumentModalVisible(true)}
          colors={colors}
        />
        <FilterChip
          label={ROLE_LABEL[filterRole] ?? filterRole}
          active={filterRole !== 'Todos'}
          icon="people-outline"
          onPress={() => setRoleModalVisible(true)}
          colors={colors}
        />
        <FilterChip
          label={MODALITY_LABEL[filterModality] ?? 'Modalidad'}
          active={filterModality !== 'Todos'}
          icon="swap-horizontal-outline"
          onPress={() => setModalityModalVisible(true)}
          colors={colors}
        />
        <FilterChip
          label={
            filterAvailability === 'Todos'
              ? 'Disponibilidad'
              : AVAILABILITY_FILTERS.find((option) => option.value === filterAvailability)?.label ?? 'Disponibilidad'
          }
          active={filterAvailability !== 'Todos'}
          icon="time-outline"
          onPress={() => setAvailabilityModalVisible(true)}
          colors={colors}
        />
      </View>

      {loading ? (
        <View style={styles.centered}>
          <ActivityIndicator color={Colors.primary} size="large" />
        </View>
      ) : teachers.length === 0 ? (
        <View style={styles.centered}>
          <Ionicons name="search-outline" size={64} color={colors.textSecondary} />
          <Text style={[styles.emptyTitle, { color: colors.text }]}>No se encontraron profesores</Text>
          <Text style={[styles.emptySubtitle, { color: colors.textSecondary }]}>
            Prueba con otro filtro o término de búsqueda
          </Text>
        </View>
      ) : (
        <FlatList
          data={teachers}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          renderItem={({ item }) => (
            <TeacherCard
              teacher={item}
              colors={colors}
              onPress={() => navigation.navigate('TeacherProfile', { teacherId: item.id })}
            />
          )}
          ListFooterComponent={
            !hasFullAccess ? (
              <View style={[styles.upgradeGate, { backgroundColor: colors.surface, borderColor: Colors.educationTier }]}>
                <View style={styles.upgradeGateIcon}>
                  <Ionicons name="lock-closed" size={28} color={Colors.educationTier} />
                </View>
                <Text style={[styles.upgradeGateTitle, { color: colors.text }]}>
                  {hiddenCount > 0 ? `+${hiddenCount} profesores más disponibles` : 'Acceso completo al tablón'}
                </Text>
                <Text style={[styles.upgradeGateSubtitle, { color: colors.textSecondary }]}>
                  El tablón completo y la publicación de anuncios están conectados a Scholar.
                </Text>
                <TouchableOpacity
                  style={[styles.upgradeGateBtn, { backgroundColor: Colors.educationTier }]}
                  onPress={() => navigation.navigate('Subscription')}
                  activeOpacity={0.8}
                >
                  <Text style={styles.upgradeGateBtnText}>Ver Scholar</Text>
                  <Ionicons name="arrow-forward" size={16} color={Colors.white} />
                </TouchableOpacity>
              </View>
            ) : null
          }
        />
      )}

      <Modal visible={modalVisible} transparent animationType="slide" onRequestClose={() => setModalVisible(false)}>
        <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
          <View style={styles.modalOverlay}>
            <View style={[styles.modalCard, { backgroundColor: colors.surface }]}>
              <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
                <Text style={[styles.modalTitle, { color: colors.text }]}>Publicar anuncio</Text>

                <Text style={[styles.sectionLabel, { color: colors.text }]}>Instrumento *</Text>
                <TouchableOpacity
                  style={[
                    styles.selectField,
                    { backgroundColor: colors.surface, borderColor: fieldErrors.instrument ? Colors.error : colors.border },
                  ]}
                  onPress={() => setPostInstrumentModalVisible(true)}
                  activeOpacity={0.8}
                >
                  <Text style={[styles.selectFieldText, { color: postInstrument ? colors.text : colors.textSecondary }]}>
                    {postInstrument || 'Selecciona un instrumento'}
                  </Text>
                  <Ionicons name="chevron-down" size={18} color={colors.textSecondary} />
                </TouchableOpacity>
                {fieldErrors.instrument ? <Text style={styles.fieldError}>{fieldErrors.instrument}</Text> : null}
                <View style={styles.priceHeader}>
                  <Text style={[styles.sectionLabel, { color: colors.text, marginTop: 0, marginBottom: 0 }]}>Precio por hora (€)</Text>
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
                  value={postPrice}
                  onChangeText={(text) => setPostPrice(text.replace(/[^0-9.]/g, ''))}
                  placeholder="20"
                  keyboardType="decimal-pad"
                />
                <WaviiInput
                  label="Provincia *"
                  value={postProvince}
                  onChangeText={(text) => {
                    setPostProvince(text);
                    clearFieldError('province');
                  }}
                  placeholder="Madrid, Barcelona..."
                  error={fieldErrors.province}
                />
                <WaviiInput
                  label="Correo de contacto *"
                  value={postEmail}
                  onChangeText={(text) => {
                    setPostEmail(text);
                    clearFieldError('email');
                  }}
                  placeholder="profesor@wavii.com"
                  keyboardType="email-address"
                  autoCapitalize="none"
                  error={fieldErrors.email}
                />
                <WaviiInput
                  label="Teléfono de contacto *"
                  value={postPhone}
                  onChangeText={(text) => {
                    setPostPhone(text.replace(/[^0-9+\s-]/g, ''));
                    clearFieldError('phone');
                  }}
                  placeholder="+34 600 000 000"
                  keyboardType="phone-pad"
                  error={fieldErrors.phone}
                />
                <WaviiInput
                  label="Instagram"
                  value={postInstagram}
                  onChangeText={setPostInstagram}
                  placeholder="https://instagram.com/tuusuario"
                  autoCapitalize="none"
                />
                <WaviiInput
                  label="TikTok"
                  value={postTiktok}
                  onChangeText={setPostTiktok}
                  placeholder="https://tiktok.com/@tuusuario"
                  autoCapitalize="none"
                />
                <WaviiInput
                  label="YouTube"
                  value={postYoutube}
                  onChangeText={setPostYoutube}
                  placeholder="https://youtube.com/@tuusuario"
                  autoCapitalize="none"
                />
                <WaviiInput
                  label="Facebook"
                  value={postFacebook}
                  onChangeText={setPostFacebook}
                  placeholder="https://facebook.com/tuusuario"
                  autoCapitalize="none"
                />

                <Text style={[styles.sectionLabel, { color: colors.text }]}>Modalidad de clases</Text>
                <View style={styles.modalityRow}>
                  {(['PRESENCIAL', 'ONLINE', 'AMBAS'] as const).map((m) => (
                    <TouchableOpacity
                      key={m}
                      style={[styles.modalityBtn, postModality === m && { backgroundColor: Colors.primary }]}
                      onPress={() => setPostModality(m)}
                      activeOpacity={0.8}
                    >
                      <Ionicons
                        name={m === 'ONLINE' ? 'videocam-outline' : m === 'PRESENCIAL' ? 'location-outline' : 'swap-horizontal-outline'}
                        size={14}
                        color={postModality === m ? Colors.white : colors.textSecondary}
                      />
                      <Text style={[styles.modalityBtnText, { color: postModality === m ? Colors.white : colors.textSecondary }]}>
                        {m.charAt(0) + m.slice(1).toLowerCase()}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>

                <WaviiInput
                  label="Ciudad *"
                  value={postCity}
                  onChangeText={(text) => {
                    setPostCity(text);
                    clearFieldError('city');
                  }}
                  placeholder="Madrid..."
                  hint="Opcional en online. Obligatoria en presencial o ambas."
                  error={fieldErrors.city}
                />

                <TouchableOpacity
                  style={[styles.locationBtn, { borderColor: colors.border, backgroundColor: colors.background }]}
                  onPress={handleAutofillLocation}
                  activeOpacity={0.8}
                >
                  <Ionicons name="locate-outline" size={16} color={Colors.primary} />
                  <Text style={[styles.locationBtnText, { color: Colors.primary }]}>Rellenar con GPS</Text>
                </TouchableOpacity>

                <WaviiInput
                  label="Dirección"
                  value={postAddress}
                  onChangeText={setPostAddress}
                  placeholder="Calle Mayor 10..."
                />

                <Text style={[styles.sectionLabel, { color: colors.text }]}>Disponibilidad</Text>
                <View style={styles.availabilityRow}>
                  {ANNOUNCEMENT_AVAILABILITY_OPTIONS.map((option) => (
                    <TouchableOpacity
                      key={option.value}
                      style={[styles.availabilityBtn, postAvailability === option.value && { backgroundColor: Colors.primary }]}
                      onPress={() => setPostAvailability(option.value)}
                      activeOpacity={0.8}
                    >
                      <Text style={[styles.availabilityBtnText, { color: postAvailability === option.value ? Colors.white : colors.textSecondary }]}>
                        {option.label}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>
                <WaviiInput
                  label="Notas de horario"
                  value={postAvailabilityNotes}
                  onChangeText={setPostAvailabilityNotes}
                  placeholder="Ejemplo: lunes y miercoles desde las 18:00"
                  multiline
                  numberOfLines={3}
                />

                <View style={styles.imageActions}>
                  <WaviiButton
                    title={bannerAsset ? 'Banner seleccionado' : 'Subir banner'}
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

                {bannerAsset ? <Text style={[styles.assetHint, { color: colors.textSecondary }]}>Banner listo para subir.</Text> : null}
                {placeAssetSlots.filter(Boolean).length > 0 ? <Text style={[styles.assetHint, { color: colors.textSecondary }]}>{placeAssetSlots.filter(Boolean).length} fotos del lugar seleccionadas.</Text> : null}

                <WaviiInput
                  label="Descripción *"
                  value={postBio}
                  onChangeText={(text) => {
                    setPostBio(text);
                    clearFieldError('bio');
                  }}
                  placeholder="Cuéntales algo sobre ti"
                  multiline
                  numberOfLines={7}
                  maxLength={500}
                  error={fieldErrors.bio ?? formError}
                />

                <View style={styles.modalBtns}>
                  <WaviiButton
                    title="Cancelar"
                    variant="outline"
                    onPress={() => setModalVisible(false)}
                    style={styles.modalAction}
                  />
                  <WaviiButton
                    title="Publicar"
                    onPress={handlePost}
                    loading={posting}
                    style={styles.modalAction}
                  />
                </View>
              </ScrollView>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      <SelectionModal
        visible={instrumentModalVisible}
        title="Seleccionar instrumento"
        options={INSTRUMENTS}
        selected={filterInstrument}
        onClose={() => setInstrumentModalVisible(false)}
        onSelect={(value) => {
          setFilterInstrument(value);
          setInstrumentModalVisible(false);
        }}
      />

      <SelectionModal
        visible={roleModalVisible}
        title="Filtrar por rol"
        options={[...ROLE_FILTERS]}
        selected={filterRole}
        onClose={() => setRoleModalVisible(false)}
        onSelect={(value) => {
          setFilterRole(value as (typeof ROLE_FILTERS)[number]);
          setRoleModalVisible(false);
        }}
      />

      <SelectionModal
        visible={modalityModalVisible}
        title="Filtrar por modalidad"
        options={[...MODALITIES].map((m) => MODALITY_LABEL[m] ?? m)}
        selected={MODALITY_LABEL[filterModality] ?? filterModality}
        onClose={() => setModalityModalVisible(false)}
        onSelect={(value) => {
          const mapped = Object.entries(MODALITY_LABEL).find(([, label]) => label === value)?.[0] ?? value;
          setFilterModality(mapped as (typeof MODALITIES)[number]);
          setModalityModalVisible(false);
        }}
      />

      <SelectionModal
        visible={availabilityModalVisible}
        title="Filtrar por disponibilidad"
        options={[...AVAILABILITY_FILTERS].map((option) => option.label)}
        selected={filterAvailability === 'Todos'
          ? 'Disponibilidad'
          : AVAILABILITY_FILTERS.find((option) => option.value === filterAvailability)?.label ?? 'Disponibilidad'}
        onClose={() => setAvailabilityModalVisible(false)}
        onSelect={(value) => {
          const mapped = AVAILABILITY_FILTERS.find((option) => option.label === value)?.value ?? 'Todos';
          setFilterAvailability(mapped);
          setAvailabilityModalVisible(false);
        }}
      />

      <SelectionModal
        visible={postInstrumentModalVisible}
        title="Selecciona un instrumento"
        options={[...INSTRUMENT_OPTIONS]}
        selected={postInstrument}
        onClose={() => setPostInstrumentModalVisible(false)}
        onSelect={(value) => {
          setPostInstrument(value);
          clearFieldError('instrument');
          setPostInstrumentModalVisible(false);
        }}
      />
    </SafeAreaView>
  );
};

const FilterChip = ({
  label,
  active,
  icon,
  onPress,
  colors,
}: {
  label: string;
  active: boolean;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  onPress: () => void;
  colors: ReturnType<typeof useTheme>['colors'];
}) => (
  <TouchableOpacity
    style={[
      styles.filterChip,
      { backgroundColor: colors.surface, borderColor: colors.border },
      active && { borderColor: Colors.primary, backgroundColor: Colors.primaryOpacity10 },
    ]}
    onPress={onPress}
    activeOpacity={0.75}
  >
    <Ionicons name={icon} size={14} color={active ? Colors.primary : colors.textSecondary} />
    <Text style={[styles.filterChipText, { color: active ? Colors.primary : colors.textSecondary }]} numberOfLines={1}>
      {label}
    </Text>
    <Ionicons name="chevron-down" size={14} color={active ? Colors.primary : colors.textSecondary} />
  </TouchableOpacity>
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
              {selected === option ? <Ionicons name="checkmark" size={20} color={Colors.primary} /> : null}
            </TouchableOpacity>
          ))}
        </View>
      </TouchableOpacity>
    </Modal>
  );
};

const TeacherCard = ({
  teacher,
  colors,
  onPress,
}: {
  teacher: BulletinTeacher;
  colors: ReturnType<typeof useTheme>['colors'];
  onPress: () => void;
}) => {
  const isCertified = teacher.role === 'profesor_certificado';
  const instrumentChips = (teacher.instrument ?? '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);

  const modalityLabel =
    teacher.classModality === 'ONLINE'
      ? 'Online'
      : teacher.classModality === 'AMBAS'
      ? 'Presencial y Online'
      : teacher.classModality === 'PRESENCIAL'
      ? 'Presencial'
      : null;

  const modalityIcon: React.ComponentProps<typeof Ionicons>['name'] =
    teacher.classModality === 'ONLINE'
      ? 'videocam-outline'
      : teacher.classModality === 'AMBAS'
      ? 'swap-horizontal-outline'
      : 'location-outline';

  return (
    <TouchableOpacity style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]} activeOpacity={0.85} onPress={onPress}>
      <View style={styles.cardHeader}>
        <View style={[styles.avatar, { backgroundColor: Colors.primary }]}>
          <Text style={styles.avatarText}>{teacher.name.charAt(0).toUpperCase()}</Text>
        </View>
        <View style={styles.cardHeaderInfo}>
          <Text style={[styles.cardName, { color: colors.text }]} numberOfLines={1}>
            {teacher.name}
          </Text>
          <View style={styles.badgeRow}>
            <Ionicons
              name={isCertified ? 'shield-checkmark' : 'person-outline'}
              size={14}
              color={isCertified ? Colors.success : colors.textSecondary}
            />
            <Text style={[styles.badgeText, { color: isCertified ? Colors.success : colors.textSecondary }]}>
              {isCertified ? 'Verificado' : 'Particular'}
            </Text>
            {modalityLabel ? (
              <>
                <Text style={{ color: colors.border, fontSize: 12 }}>·</Text>
                <Ionicons name={modalityIcon} size={13} color={colors.textSecondary} />
                <Text style={[styles.badgeText, { color: colors.textSecondary }]}>{modalityLabel}</Text>
              </>
            ) : null}
          </View>
        </View>
      </View>

      {teacher.bannerImageUrl ? <Image source={{ uri: teacher.bannerImageUrl }} style={styles.bannerThumb} resizeMode="cover" /> : null}

      {instrumentChips.length > 0 ? (
        <View style={styles.chipRow}>
          {instrumentChips.map((chip) => (
            <View key={chip} style={[styles.instrChip, { backgroundColor: Colors.primaryOpacity10 }]}>
              <Text style={[styles.instrChipText, { color: Colors.primary }]}>{chip}</Text>
            </View>
          ))}
        </View>
      ) : null}

      {teacher.bio ? <Text style={[styles.bio, { color: colors.textSecondary }]} numberOfLines={2}>{teacher.bio}</Text> : null}

      {teacher.city || teacher.address ? (
        <View style={styles.locationRow}>
          <Ionicons name="location-outline" size={13} color={colors.textSecondary} />
          <Text style={[styles.locationText, { color: colors.textSecondary }]} numberOfLines={1}>
            {[teacher.address, teacher.city, teacher.province].filter(Boolean).join(', ')}
          </Text>
        </View>
      ) : null}

      <View style={styles.cardFooter}>
        <View style={styles.priceChip}>
          <Ionicons name="card-outline" size={13} color={Colors.success} />
          <Text style={[styles.priceText, { color: Colors.success }]}>
            {teacher.pricePerHour != null ? `${teacher.pricePerHour}€/h` : 'Gratis'}
          </Text>
        </View>
        <Text style={[styles.viewProfileBtn, { color: Colors.primary }]}>Ver perfil</Text>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
    gap: Spacing.sm,
  },
  headerCenter: { flex: 1, justifyContent: 'center', alignItems: 'center', minWidth: 0 },
  headerRight: { flexDirection: 'row', alignItems: 'center', gap: Spacing.xs },
  headerTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg, flexShrink: 1 },
  publishBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    height: 34,
  },
  publishBtnText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs, color: Colors.white },
  searchWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    marginHorizontal: Spacing.base,
    marginTop: Spacing.sm,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 9,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
  },
  searchInput: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    padding: 0,
  },
  filtersShell: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignItems: 'center',
    gap: Spacing.xs,
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.sm,
  },
  filtersRail: {
    alignItems: 'center',
    gap: Spacing.xs,
    flex: 1,
  },
  filterChip: {
    flexBasis: '48%',
    flexGrow: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: Spacing.xs,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    minHeight: 42,
    paddingHorizontal: Spacing.sm,
    paddingVertical: Spacing.sm,
    minWidth: 0,
  },
  filterChipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    flex: 1,
    lineHeight: 16,
  },
  centered: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: Spacing.sm },
  emptyTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base, textAlign: 'center' },
  emptySubtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center', paddingHorizontal: Spacing.xl },
  list: { paddingHorizontal: Spacing.base, paddingBottom: Spacing.xl },
  card: { borderRadius: BorderRadius.lg, borderWidth: 1, padding: Spacing.base, marginBottom: Spacing.base, gap: Spacing.sm },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: Spacing.sm },
  avatar: { width: 56, height: 56, borderRadius: 28, alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  avatarText: { fontFamily: FontFamily.black, fontSize: FontSize.xl, color: Colors.white },
  cardHeaderInfo: { flex: 1, gap: 3 },
  cardName: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  badgeRow: { flexDirection: 'row', alignItems: 'center', gap: 4, flexWrap: 'wrap' },
  badgeText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  bannerThumb: { width: '100%', height: 120, borderRadius: BorderRadius.md },
  chipRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  instrChip: { paddingHorizontal: Spacing.sm, paddingVertical: 3, borderRadius: BorderRadius.full },
  instrChipText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  bio: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  locationRow: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  locationText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, flex: 1 },
  cardFooter: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  priceChip: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  priceText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  viewProfileBtn: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  upgradeGate: { marginTop: 4, borderRadius: BorderRadius.xl, borderWidth: 1.5, padding: Spacing.xl, alignItems: 'center', gap: Spacing.sm },
  upgradeGateIcon: { width: 56, height: 56, borderRadius: 28, backgroundColor: 'rgba(167,139,250,0.12)', alignItems: 'center', justifyContent: 'center', marginBottom: Spacing.xs },
  upgradeGateTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.base, textAlign: 'center' },
  upgradeGateSubtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center', lineHeight: 20 },
  upgradeGateBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: Spacing.xl, paddingVertical: 12, borderRadius: BorderRadius.full, marginTop: Spacing.xs },
  upgradeGateBtnText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, color: Colors.white },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.45)', justifyContent: 'flex-end' },
  modalCard: { borderTopLeftRadius: BorderRadius.xl, borderTopRightRadius: BorderRadius.xl, padding: Spacing.xl, maxHeight: '92%' },
  modalTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg, marginBottom: Spacing.base },
  modalBtns: { flexDirection: 'row', gap: Spacing.sm, marginTop: Spacing.sm },
  modalAction: { flex: 1 },
  modalOverlayCenter: { flex: 1, backgroundColor: 'rgba(0,0,0,0.3)', justifyContent: 'center', alignItems: 'center' },
  dropdownModalCard: { width: '85%', maxHeight: '70%', borderRadius: BorderRadius.xl, borderWidth: 1, padding: Spacing.xl, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.15, shadowRadius: 12, elevation: 8 },
  dropdownOption: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingVertical: Spacing.base, paddingHorizontal: Spacing.sm, borderRadius: BorderRadius.sm },
  dropdownOptionText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.base },
  sectionLabel: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, marginBottom: Spacing.xs, marginTop: Spacing.sm },
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
  modalityRow: { flexDirection: 'row', gap: Spacing.xs, marginBottom: Spacing.sm },
  modalityBtn: { flex: 1, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 4, paddingVertical: Spacing.xs, borderRadius: BorderRadius.full, borderWidth: 1, borderColor: Colors.border },
  modalityBtnText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  availabilityRow: { flexDirection: 'row', flexWrap: 'wrap', gap: Spacing.xs, marginBottom: Spacing.sm },
  availabilityBtn: { borderWidth: 1, borderColor: Colors.border, borderRadius: BorderRadius.full, paddingHorizontal: Spacing.sm, paddingVertical: 8 },
  availabilityBtnText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  imageActions: { gap: Spacing.sm, marginTop: Spacing.xs, marginBottom: Spacing.xs },
  locationBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    borderWidth: 1,
    borderRadius: BorderRadius.full,
    paddingVertical: 10,
    marginBottom: Spacing.base,
  },
  locationBtnText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  assetHint: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginBottom: Spacing.xs },
});
