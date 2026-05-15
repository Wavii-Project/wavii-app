import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Image,
  Pressable,
  ScrollView,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Modal,
  TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import * as ImagePicker from 'expo-image-picker';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';
import { ScreenHeader } from '../../components/common/ScreenHeader';
import { StepProgress } from '../../components/common/StepProgress';
import { WaviiHintBubble } from '../../components/common/WaviiHintBubble';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import {
  ListingType,
  MusicalGenre,
  MusicianRole,
  apiCreateBandListing,
  apiUploadBandImage,
} from '../../api/bandApi';

type Nav = NativeStackNavigationProp<AppStackParamList>;

const LISTING_TYPES: { value: ListingType; label: string; desc: string; color: string }[] = [
  {
    value: 'BANDA_BUSCA_MUSICOS',
    label: 'Banda busca músicos',
    desc: 'Tengo una banda y busco integrantes',
    color: '#8B5CF6',
  },
  {
    value: 'MUSICO_BUSCA_BANDA',
    label: 'Músico busca banda',
    desc: 'Soy músico y quiero unirme a una banda',
    color: '#3B82F6',
  },
];

const GENRES: { value: MusicalGenre; label: string }[] = [
  { value: 'ROCK', label: 'Rock' },
  { value: 'METAL', label: 'Metal' },
  { value: 'POP', label: 'Pop' },
  { value: 'JAZZ', label: 'Jazz' },
  { value: 'BLUES', label: 'Blues' },
  { value: 'CLASICA', label: 'Clásica' },
  { value: 'ELECTRONICA', label: 'Electrónica' },
  { value: 'REGGAETON', label: 'Reggaeton' },
  { value: 'SALSA', label: 'Salsa' },
  { value: 'CUMBIA', label: 'Cumbia' },
  { value: 'BACHATA', label: 'Bachata' },
  { value: 'HIP_HOP', label: 'Hip-Hop' },
  { value: 'REGGAE', label: 'Reggae' },
  { value: 'FOLK', label: 'Folk' },
  { value: 'INDIE', label: 'Indie' },
  { value: 'PUNK', label: 'Punk' },
  { value: 'FUNK', label: 'Funk' },
  { value: 'R_AND_B', label: 'R&B' },
  { value: 'LATIN', label: 'Latin' },
  { value: 'OTRO', label: 'Otro' },
];

const ROLES: { value: MusicianRole; label: string; icon: React.ComponentProps<typeof Ionicons>['name'] }[] = [
  { value: 'VOCALISTA', label: 'Vocalista', icon: 'mic' },
  { value: 'GUITARRISTA', label: 'Guitarrista', icon: 'musical-note' },
  { value: 'BAJISTA', label: 'Bajista', icon: 'musical-note' },
  { value: 'BATERISTA', label: 'Baterista', icon: 'radio' },
  { value: 'PERCUSIONISTA', label: 'Percusionista', icon: 'radio' },
  { value: 'PIANISTA', label: 'Pianista', icon: 'musical-notes' },
  { value: 'TECLADISTA', label: 'Tecladista', icon: 'musical-notes' },
  { value: 'PRODUCTOR', label: 'Productor', icon: 'headset' },
  { value: 'DJ', label: 'DJ', icon: 'disc' },
  { value: 'VIOLINISTA', label: 'Violinista', icon: 'musical-note' },
  { value: 'TROMPETISTA', label: 'Trompetista', icon: 'musical-note' },
  { value: 'SAXOFONISTA', label: 'Saxofonista', icon: 'musical-note' },
  { value: 'OTRO', label: 'Otro', icon: 'ellipsis-horizontal' },
];

function SectionTitle({ text }: { text: string }) {
  const { colors } = useTheme();
  return <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>{text}</Text>;
}

// ── Modal genérico de selección única (igual al de BandsScreen) ───────────────
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
      <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={onClose}>
        <View style={[styles.modalCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <Text style={[styles.modalTitle, { color: colors.text }]}>{title}</Text>
          <ScrollView showsVerticalScrollIndicator={false} bounces={false}>
            {options.map((option) => (
              <TouchableOpacity
                key={option}
                style={[styles.modalOption, selected === option && { backgroundColor: Colors.primaryOpacity10 }]}
                onPress={() => onSelect(option)}
              >
                <Text style={[styles.modalOptionText, { color: selected === option ? Colors.primary : colors.text }]}>
                  {option}
                </Text>
                {selected === option ? <Ionicons name="checkmark" size={20} color={Colors.primary} /> : null}
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>
      </TouchableOpacity>
    </Modal>
  );
};

// ── Modal de roles multiselección ─────────────────────────────────────────────
const RolesModal = ({
  visible,
  selected,
  onClose,
  onConfirm,
}: {
  visible: boolean;
  selected: Set<MusicianRole>;
  onClose: () => void;
  onConfirm: (roles: Set<MusicianRole>) => void;
}) => {
  const { colors } = useTheme();
  const [draft, setDraft] = useState<Set<MusicianRole>>(new Set(selected));

  useEffect(() => {
    if (visible) setDraft(new Set(selected));
  }, [visible, selected]);

  const toggle = (role: MusicianRole) => {
    setDraft((prev) => {
      const next = new Set(prev);
      if (next.has(role)) next.delete(role);
      else next.add(role);
      return next;
    });
  };

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={onClose}>
        <TouchableOpacity activeOpacity={1} style={[styles.modalCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <Text style={[styles.modalTitle, { color: colors.text }]}>Seleccionar roles</Text>
          <ScrollView showsVerticalScrollIndicator={false} bounces={false}>
            {ROLES.map((item) => {
              const sel = draft.has(item.value);
              return (
                <TouchableOpacity
                  key={item.value}
                  style={[styles.modalOption, sel && { backgroundColor: Colors.primaryOpacity10 }]}
                  onPress={() => toggle(item.value)}
                >
                  <Ionicons name={item.icon} size={16} color={sel ? Colors.primary : colors.textSecondary} style={{ marginRight: 8 }} />
                  <Text style={[styles.modalOptionText, { flex: 1, color: sel ? Colors.primary : colors.text }]}>
                    {item.label}
                  </Text>
                  <Ionicons
                    name={sel ? 'checkmark-circle' : 'ellipse-outline'}
                    size={20}
                    color={sel ? Colors.primary : colors.textSecondary}
                  />
                </TouchableOpacity>
              );
            })}
          </ScrollView>
          <WaviiButton
            title={draft.size > 0 ? `Confirmar (${draft.size})` : 'Confirmar'}
            onPress={() => { onConfirm(draft); onClose(); }}
            size="md"
          />
        </TouchableOpacity>
      </TouchableOpacity>
    </Modal>
  );
};

const STEP_TITLES = ['¿Qué buscas?', 'Cuéntanos más', 'Últimos detalles'];

const HINTS: Record<number, (type: ListingType | null, title: string, description: string) => string | null> = {
  1: () => '¡Genial! Antes de empezar, dime si tienes banda o si eres tú quien busca.',
  2: (type, title) => {
    if (title.length >= 20) return 'Ese título mola. Añade ciudad y así aparecerás en búsquedas locales.';
    if (type === 'BANDA_BUSCA_MUSICOS') return 'Cuanto más específico seas con el título, más músicos de calidad te contactarán.';
    return 'Los géneros y roles bien elegidos te conectan con las bandas que realmente encajan contigo.';
  },
  3: (_, __, description) => {
    if (description.length === 0) return '¿Sin descripción? No pasa nada, pero un par de frases convence mucho más.';
    return 'Casi listo. La descripción es opcional pero marca la diferencia — cuenta tu historia.';
  },
};

export const CreateBandListingScreen = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation<Nav>();

  const [step, setStep] = useState(1);
  const [type, setType] = useState<ListingType | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [genre, setGenre] = useState<MusicalGenre | null>(null);
  const [genreModalVisible, setGenreModalVisible] = useState(false);
  const [rolesModalVisible, setRolesModalVisible] = useState(false);
  const [city, setCity] = useState('');
  const [detectedCity, setDetectedCity] = useState<string | null>(null);
  const [locLoading, setLocLoading] = useState(true);
  const [roles, setRoles] = useState<Set<MusicianRole>>(new Set());
  const [contact, setContact] = useState('');
  const [coverAsset, setCoverAsset] = useState<{ uri: string; name: string; type: string } | null>(null);
  const [galleryAssets, setGalleryAssets] = useState<{ uri: string; name: string; type: string }[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [hint, setHint] = useState<string | null>(null);
  const [hintVisible, setHintVisible] = useState(false);

  // GPS detection
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status !== 'granted' || cancelled) { setLocLoading(false); return; }
        const pos = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
        const [geo] = await Location.reverseGeocodeAsync({ latitude: pos.coords.latitude, longitude: pos.coords.longitude });
        const found = geo?.city ?? geo?.subregion ?? geo?.region ?? null;
        if (!cancelled && found) {
          setDetectedCity(found);
          setCity((prev) => (prev === '' ? found : prev));
        }
      } catch { /* ignore */ }
      finally { if (!cancelled) setLocLoading(false); }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const msg = HINTS[step]?.(type, title, description) ?? null;
    setHint(msg);
    setHintVisible(!!msg);
  }, [step, type]);

  useEffect(() => {
    if (step === 2 && title.length >= 20) {
      setHint('Ese título mola. Añade ciudad y así aparecerás en búsquedas locales.');
      setHintVisible(true);
    }
  }, [title, step]);

  const canStep1 = Boolean(type);
  const canStep2 = Boolean(title.trim() && genre && city.trim() && roles.size > 0);
  const canSubmit = canStep1 && canStep2;

  const handleNext = () => { if (step < 3) setStep((s) => s + 1); };
  const handleBack = () => { if (step === 1) navigation.goBack(); else setStep((s) => s - 1); };

  const pickBandImage = async (mode: 'cover' | 'gallery') => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      setError('Necesitamos acceso a tu galeria para elegir fotos.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsMultipleSelection: mode === 'gallery',
      selectionLimit: mode === 'gallery' ? Math.max(1, 3 - galleryAssets.length) : 1,
      allowsEditing: mode === 'cover',
      aspect: mode === 'cover' ? [16, 9] : undefined,
      quality: 0.82,
    });
    if (result.canceled) return;
    const picked = result.assets.map((asset, index) => ({
      uri: asset.uri,
      name: asset.fileName ?? `band_${Date.now()}_${index}.jpg`,
      type: asset.mimeType ?? 'image/jpeg',
    }));
    if (mode === 'cover') {
      setCoverAsset(picked[0] ?? null);
    } else {
      setGalleryAssets((current) => [...current, ...picked].slice(0, 3));
    }
  };

  const handleCreate = async () => {
    if (!token || !canSubmit || loading) return;
    setError(null);
    setLoading(true);
    try {
      const coverImageUrl = coverAsset ? (await apiUploadBandImage(coverAsset, token)).url : undefined;
      const imageUrls: string[] = [];
      for (const asset of galleryAssets.slice(0, 3)) {
        imageUrls.push((await apiUploadBandImage(asset, token)).url);
      }
      await apiCreateBandListing(
        {
          title: title.trim(),
          description: description.trim(),
          type: type!,
          genre: genre!,
          city: city.trim(),
          roles: Array.from(roles),
          contactInfo: contact.trim() || undefined,
          coverImageUrl,
          imageUrls,
        },
        token,
      );
      navigation.goBack();
    } catch (e: any) {
      const is401 = e?.response?.status === 401 || e?.message === 'SESSION_INVALID';
      if (is401) {
        setError('Tu sesión ha expirado. Cierra sesión y vuelve a entrar.');
      } else {
        setError(e?.response?.data?.message ?? 'No se pudo publicar el anuncio.');
      }
    } finally {
      setLoading(false);
    }
  };

  const selectedGenreLabel = GENRES.find((g) => g.value === genre)?.label ?? null;
  const selectedRolesLabel = roles.size > 0
    ? Array.from(roles).map((r) => ROLES.find((item) => item.value === r)?.label ?? r).join(', ')
    : null;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScreenHeader title={STEP_TITLES[step - 1]} onBack={handleBack} />

        <View style={styles.progressWrap}>
          <StepProgress totalSteps={3} currentStep={step} />
        </View>

        <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>

          {/* PASO 1 — ¿Qué buscas? */}
          {step === 1 && (
            <View style={styles.stepContainer}>
              <Text style={[styles.stepHeading, { color: colors.text }]}>
                ¿Cómo describes tu situación?
              </Text>
              <Text style={[styles.stepSubtitle, { color: colors.textSecondary }]}>
                Elige el tipo de anuncio. Esto define cómo lo verán los demás.
              </Text>
              <View style={styles.typeList}>
                {LISTING_TYPES.map((item) => {
                  const selected = type === item.value;
                  return (
                    <Pressable
                      key={item.value}
                      style={[
                        styles.typeCard,
                        {
                          backgroundColor: selected ? item.color + '18' : colors.surface,
                          borderColor: selected ? item.color : colors.border,
                        },
                      ]}
                      onPress={() => setType(item.value)}
                    >
                      <View style={[styles.typeDot, { backgroundColor: item.color }]} />
                      <View style={styles.typeTextWrap}>
                        <Text style={[styles.typeLabel, { color: selected ? item.color : colors.text }]}>
                          {item.label}
                        </Text>
                        <Text style={[styles.typeDesc, { color: colors.textSecondary }]}>{item.desc}</Text>
                      </View>
                      {selected && <Ionicons name="checkmark-circle" size={22} color={item.color} />}
                    </Pressable>
                  );
                })}
              </View>
              <WaviiButton title="Siguiente →" onPress={handleNext} disabled={!canStep1} />
            </View>
          )}

          {/* PASO 2 — Cuéntanos más */}
          {step === 2 && (
            <View style={styles.stepContainer}>
              <View style={styles.section}>
                <SectionTitle text="Título *" />
                <WaviiInput
                  placeholder={
                    type === 'BANDA_BUSCA_MUSICOS'
                      ? 'Ej: Banda de rock busca guitarrista en Madrid'
                      : 'Ej: Baterista busca banda de metal en Barcelona'
                  }
                  value={title}
                  onChangeText={setTitle}
                  maxLength={120}
                  containerStyle={styles.inputBlock}
                />
                <Text style={[styles.counter, { color: colors.textSecondary }]}>{title.length}/120</Text>
              </View>

              <View style={styles.section}>
                <SectionTitle text="Ciudad *" />
                <View
                  style={[
                    styles.cityRow,
                    {
                      backgroundColor: colors.surface,
                      borderColor: detectedCity && city === detectedCity ? Colors.primary : colors.border,
                    },
                  ]}
                >
                  <Ionicons
                    name={detectedCity && city === detectedCity ? 'location' : 'location-outline'}
                    size={16}
                    color={detectedCity && city === detectedCity ? Colors.primary : colors.textSecondary}
                  />
                  <TextInput
                    style={[styles.cityInput, { color: colors.text }]}
                    placeholder={locLoading ? 'Detectando ubicación...' : 'Ej: Madrid, Barcelona, Bogotá...'}
                    placeholderTextColor={colors.textSecondary}
                    value={city}
                    onChangeText={setCity}
                    maxLength={80}
                  />
                  {city.length > 0 && (
                    <Pressable onPress={() => setCity('')}>
                      <Ionicons name="close-circle" size={15} color={colors.textSecondary} />
                    </Pressable>
                  )}
                </View>
              </View>

              <View style={styles.section}>
                <SectionTitle text="Género musical *" />
                <TouchableOpacity
                  style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]}
                  activeOpacity={0.75}
                  onPress={() => setGenreModalVisible(true)}
                >
                  <Text style={[styles.dropdownBtnText, { color: selectedGenreLabel ? colors.text : colors.textSecondary }]}>
                    {selectedGenreLabel ?? 'Seleccionar género'}
                  </Text>
                  <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
                </TouchableOpacity>
              </View>

              <View style={styles.section}>
                <SectionTitle text={type === 'BANDA_BUSCA_MUSICOS' ? 'Roles que buscas *' : 'Tu rol o roles *'} />
                <TouchableOpacity
                  style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]}
                  activeOpacity={0.75}
                  onPress={() => setRolesModalVisible(true)}
                >
                  <Text
                    style={[styles.dropdownBtnText, { color: selectedRolesLabel ? colors.text : colors.textSecondary }]}
                    numberOfLines={1}
                  >
                    {selectedRolesLabel ?? 'Seleccionar roles'}
                  </Text>
                  <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
                </TouchableOpacity>
              </View>

              <WaviiButton title="Siguiente →" onPress={handleNext} disabled={!canStep2} />
            </View>
          )}

          {/* PASO 3 — Últimos detalles */}
          {step === 3 && (
            <View style={styles.stepContainer}>
              {/* Preview card */}
              <View style={[styles.previewCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
                <View style={styles.previewRow}>
                  <View style={[styles.previewBadge, { backgroundColor: type === 'BANDA_BUSCA_MUSICOS' ? '#8B5CF620' : '#3B82F620' }]}>
                    <Text style={[styles.previewBadgeText, { color: type === 'BANDA_BUSCA_MUSICOS' ? '#8B5CF6' : '#3B82F6' }]}>
                      {LISTING_TYPES.find((t) => t.value === type)?.label}
                    </Text>
                  </View>
                  {genre && (
                    <View style={[styles.previewBadge, { backgroundColor: Colors.primaryOpacity10 }]}>
                      <Text style={[styles.previewBadgeText, { color: Colors.primary }]}>
                        {GENRES.find((g) => g.value === genre)?.label}
                      </Text>
                    </View>
                  )}
                </View>
                <Text style={[styles.previewTitle, { color: colors.text }]} numberOfLines={2}>
                  {title || 'Sin título'}
                </Text>
                {city ? (
                  <View style={styles.previewLocation}>
                    <Ionicons name="location-outline" size={12} color={colors.textSecondary} />
                    <Text style={[styles.previewLocationText, { color: colors.textSecondary }]}>{city}</Text>
                  </View>
                ) : null}
              </View>

              <View style={styles.section}>
                <SectionTitle text="Descripción" />
                <View style={[styles.textAreaWrap, { backgroundColor: colors.surface, borderColor: colors.border }]}>
                  <TextInput
                    style={[styles.textArea, { color: colors.text }]}
                    placeholder="Cuenta un poco del proyecto, estilo, experiencia, ensayos o lo que buscas."
                    placeholderTextColor={colors.textSecondary}
                    value={description}
                    onChangeText={setDescription}
                    multiline
                    numberOfLines={5}
                    maxLength={800}
                    textAlignVertical="top"
                  />
                </View>
                <Text style={[styles.counter, { color: colors.textSecondary }]}>{description.length}/800</Text>
              </View>

              <View style={styles.section}>
                <SectionTitle text="Contacto" />
                <WaviiInput
                  placeholder="WhatsApp, Instagram o email"
                  value={contact}
                  onChangeText={setContact}
                  maxLength={200}
                  leftIcon={<Ionicons name="chatbubble-ellipses-outline" size={16} color={colors.textSecondary} />}
                  containerStyle={styles.inputBlock}
                />
              </View>

              <View style={styles.section}>
                <SectionTitle text="Foto principal" />
                <Pressable style={[styles.imagePicker, { backgroundColor: colors.surface, borderColor: colors.border }]} onPress={() => pickBandImage('cover')}>
                  {coverAsset ? (
                    <Image source={{ uri: coverAsset.uri }} style={styles.coverPreview} resizeMode="cover" />
                  ) : (
                    <View style={[styles.coverPreview, styles.imagePlaceholder]}>
                      <Ionicons name="camera-outline" size={24} color={Colors.primary} />
                    </View>
                  )}
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.imagePickerTitle, { color: colors.text }]}>
                      {coverAsset ? 'Foto principal lista' : 'Anadir foto principal'}
                    </Text>
                    <Text style={[styles.imagePickerHint, { color: colors.textSecondary }]}>
                      Opcional. Se vera en el listado y el detalle.
                    </Text>
                  </View>
                </Pressable>
              </View>

              <View style={styles.section}>
                <SectionTitle text="Galeria (max. 3)" />
                <View style={styles.galleryRow}>
                  {galleryAssets.map((asset, index) => (
                    <Pressable
                      key={`${asset.uri}-${index}`}
                      onPress={() => setGalleryAssets((current) => current.filter((_, i) => i !== index))}
                    >
                      <Image source={{ uri: asset.uri }} style={styles.galleryThumb} resizeMode="cover" />
                    </Pressable>
                  ))}
                  {galleryAssets.length < 3 ? (
                    <Pressable style={[styles.galleryAdd, { borderColor: colors.border, backgroundColor: colors.surface }]} onPress={() => pickBandImage('gallery')}>
                      <Ionicons name="add" size={22} color={Colors.primary} />
                    </Pressable>
                  ) : null}
                </View>
              </View>

              {error ? <Text style={styles.error}>{error}</Text> : null}

              <WaviiButton
                title="Publicar anuncio"
                onPress={handleCreate}
                loading={loading}
                disabled={!canSubmit}
              />
            </View>
          )}

          <View style={styles.bottomSpace} />
        </ScrollView>

        <WaviiHintBubble
          message={hint ?? ''}
          visible={hintVisible}
          onDismiss={() => setHintVisible(false)}
        />
      </KeyboardAvoidingView>

      <SelectionModal
        visible={genreModalVisible}
        title="Género musical"
        options={GENRES.map((g) => g.label)}
        selected={selectedGenreLabel ?? ''}
        onClose={() => setGenreModalVisible(false)}
        onSelect={(label) => {
          const found = GENRES.find((g) => g.label === label);
          if (found) setGenre(found.value);
          setGenreModalVisible(false);
        }}
      />

      <RolesModal
        visible={rolesModalVisible}
        selected={roles}
        onClose={() => setRolesModalVisible(false)}
        onConfirm={(newRoles) => setRoles(newRoles)}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  progressWrap: {
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.xs,
  },
  body: {
    padding: Spacing.base,
    paddingBottom: Spacing.xl * 4,
  },
  stepContainer: { gap: Spacing.lg },
  stepHeading: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.xl,
    lineHeight: 28,
  },
  stepSubtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  typeList: { gap: Spacing.sm },
  typeCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    padding: Spacing.md,
  },
  typeDot: { width: 10, height: 10, borderRadius: 5, flexShrink: 0 },
  typeTextWrap: { flex: 1, gap: 2 },
  typeLabel: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  typeDesc: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 16 },
  section: { gap: 8 },
  sectionTitle: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  inputBlock: { marginBottom: 0 },
  counter: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, textAlign: 'right' },
  cityRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 9,
  },
  cityInput: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    padding: 0,
  },
  dropdownBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: Spacing.md,
    paddingVertical: 10,
  },
  dropdownBtnText: {
    flex: 1,
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    marginRight: 4,
  },
  previewCard: {
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    padding: Spacing.md,
    gap: Spacing.xs,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  previewRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  previewBadge: { paddingHorizontal: 8, paddingVertical: 3, borderRadius: BorderRadius.full },
  previewBadgeText: { fontFamily: FontFamily.semiBold, fontSize: 11 },
  previewTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.base, lineHeight: 22 },
  previewLocation: { flexDirection: 'row', alignItems: 'center', gap: 3 },
  previewLocationText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  textAreaWrap: {
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.md,
  },
  textArea: {
    minHeight: 104,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    padding: 0,
  },
  imagePicker: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.sm,
  },
  coverPreview: {
    width: 76,
    height: 54,
    borderRadius: BorderRadius.md,
  },
  imagePlaceholder: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.primaryOpacity10,
  },
  imagePickerTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  imagePickerHint: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 17,
  },
  galleryRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.sm,
  },
  galleryThumb: {
    width: 72,
    height: 72,
    borderRadius: BorderRadius.md,
  },
  galleryAdd: {
    width: 72,
    height: 72,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  error: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, color: Colors.error, textAlign: 'center' },
  bottomSpace: { height: Spacing.xl },
  // Modales
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
    justifyContent: 'center',
    padding: Spacing.base,
  },
  modalCard: {
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.base,
    maxHeight: '80%',
    gap: Spacing.sm,
  },
  modalTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  modalOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: Spacing.sm,
    borderRadius: BorderRadius.lg,
  },
  modalOptionText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },
});
