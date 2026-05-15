import React, { useEffect, useState } from 'react';
import {
  KeyboardAvoidingView,
  Image,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as Location from 'expo-location';
import * as ImagePicker from 'expo-image-picker';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { ForumCategory, apiCreateForum, apiUploadForumImage } from '../../api/forumApi';
import { ScreenHeader } from '../../components/common/ScreenHeader';
import { StepProgress } from '../../components/common/StepProgress';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiHintBubble } from '../../components/common/WaviiHintBubble';
import { WaviiInput } from '../../components/common/WaviiInput';

type Navigation = NativeStackNavigationProp<AppStackParamList>;

interface CategoryOption {
  value: ForumCategory;
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  color: string;
}

const CATEGORIES: CategoryOption[] = [
  { value: 'FANDOM', label: 'Fandom', icon: 'star', color: '#8B5CF6' },
  { value: 'COMUNIDAD_MUSICAL', label: 'Comunidad musical', icon: 'musical-notes', color: '#3B82F6' },
  { value: 'TEORIA', label: 'Teoría musical', icon: 'library', color: '#14B8A6' },
  { value: 'INSTRUMENTOS', label: 'Instrumentos', icon: 'disc', color: Colors.primary },
  { value: 'BANDAS', label: 'Bandas', icon: 'mic', color: '#EC4899' },
  { value: 'ARTISTAS', label: 'Artistas', icon: 'person-circle', color: '#F59E0B' },
  { value: 'GENERAL', label: 'General', icon: 'chatbubbles', color: '#6B7280' },
];

const STEP_TITLES = ['Elige categoría', 'Ponle nombre', 'Revisa y crea'];

type HintFn = (category: ForumCategory | null, name: string) => string | null;
const HINTS: Record<number, HintFn> = {
  1: () => 'Crea un espacio donde los músicos puedan encontrarse. Primero elige el tipo de comunidad.',
  2: (category, name) => {
    if (name.length >= 10) return 'Perfecto. Una buena descripción hace que la gente quiera entrar.';
    if (category === 'FANDOM') return 'Los fandoms más activos tienen el nombre del artista o banda directamente en el título.';
    return null;
  },
  3: () => 'Esta será tu comunidad. Revísalo y cuando estés listo, ¡publícala!',
};

export const CreateForumScreen = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation<Navigation>();

  const [step, setStep] = useState(1);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState<ForumCategory | null>(null);
  const [city, setCity] = useState('');
  const [detectedCity, setDetectedCity] = useState<string | null>(null);
  const [locLoading, setLocLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [coverAsset, setCoverAsset] = useState<{ uri: string; name: string; type: string } | null>(null);
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
    const msg = HINTS[step]?.(category, name) ?? null;
    setHint(msg);
    setHintVisible(!!msg);
  }, [step, category]);

  useEffect(() => {
    if (step === 2 && name.length >= 10) {
      setHint('Perfecto. Una buena descripción hace que la gente quiera entrar.');
      setHintVisible(true);
    }
  }, [name, step]);

  const canStep1 = category !== null;
  const canStep2 = name.trim().length > 0;

  const handleNext = () => {
    if (step < 3) setStep((s) => s + 1);
  };

  const handleBack = () => {
    if (step === 1) {
      navigation.goBack();
    } else {
      setStep((s) => s - 1);
    }
  };

  const pickCover = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      setError('Necesitamos acceso a tu galeria para elegir una foto.');
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.82,
    });
    if (result.canceled || !result.assets[0]) return;
    const asset = result.assets[0];
    setCoverAsset({
      uri: asset.uri,
      name: asset.fileName ?? `forum_${Date.now()}.jpg`,
      type: asset.mimeType ?? 'image/jpeg',
    });
  };

  const handleCreate = async () => {
    if (!token || !canStep1 || !canStep2 || loading || !category) return;
    setError(null);
    setLoading(true);
    try {
      const coverImageUrl = coverAsset ? (await apiUploadForumImage(coverAsset, token)).url : undefined;
      const created = await apiCreateForum(
        {
          name: name.trim(),
          description: description.trim(),
          category,
          coverImageUrl,
          city: city.trim() || undefined,
        },
        token,
      );
      navigation.replace('ForumDetail', { forumId: created.id });
    } catch (err: unknown) {
      const e = err as any;
      const is401 = e?.response?.status === 401 || e?.message === 'SESSION_INVALID';
      if (is401) {
        setError('Tu sesión ha expirado. Cierra sesión y vuelve a entrar.');
      } else {
        setError(e?.response?.data?.message ?? 'No se pudo crear la comunidad.');
      }
    } finally {
      setLoading(false);
    }
  };

  const selectedCategory = CATEGORIES.find((c) => c.value === category);

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <ScreenHeader title={STEP_TITLES[step - 1]} onBack={handleBack} />

        <View style={styles.progressWrap}>
          <StepProgress totalSteps={3} currentStep={step} />
        </View>

        <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>

          {/* PASO 1 — Elige categoría */}
          {step === 1 && (
            <View style={styles.stepContainer}>
              <Text style={[styles.stepHeading, { color: colors.text }]}>
                ¿De qué irá la comunidad?
              </Text>
              <Text style={[styles.stepSubtitle, { color: colors.textSecondary }]}>
                Elige la categoría que mejor la define.
              </Text>
              <View style={styles.categoryGrid}>
                {CATEGORIES.map((item) => {
                  const selected = category === item.value;
                  return (
                    <Pressable
                      key={item.value}
                      style={[
                        styles.categoryCard,
                        {
                          backgroundColor: selected ? `${item.color}22` : colors.surface,
                          borderColor: selected ? item.color : colors.border,
                        },
                      ]}
                      onPress={() => setCategory(item.value)}
                    >
                      <View style={[styles.categoryIconWrap, { backgroundColor: `${item.color}18` }]}>
                        <Ionicons name={item.icon} size={22} color={item.color} />
                      </View>
                      <Text
                        style={[styles.categoryLabel, { color: selected ? item.color : colors.text }]}
                        numberOfLines={2}
                      >
                        {item.label}
                      </Text>
                      {selected && (
                        <Ionicons name="checkmark-circle" size={16} color={item.color} style={styles.categoryCheck} />
                      )}
                    </Pressable>
                  );
                })}
              </View>

              <WaviiButton
                title="Siguiente →"
                onPress={handleNext}
                disabled={!canStep1}
              />
            </View>
          )}

          {/* PASO 2 — Ponle nombre */}
          {step === 2 && (
            <View style={styles.stepContainer}>
              <View style={styles.section}>
                <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>Nombre *</Text>
                <WaviiInput
                  placeholder="Ej: Beatlemania España"
                  value={name}
                  onChangeText={setName}
                  maxLength={80}
                  containerStyle={styles.inputBlock}
                />
                <Text style={[styles.counter, { color: colors.textSecondary }]}>{name.length}/80</Text>
              </View>

              <View style={styles.section}>
                <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>Descripción</Text>
                <View style={[styles.textAreaWrap, { backgroundColor: colors.surface, borderColor: colors.border }]}>
                  <TextInput
                    style={[styles.textArea, { color: colors.text }]}
                    placeholder="¿De qué trata esta comunidad?"
                    placeholderTextColor={colors.textSecondary}
                    value={description}
                    onChangeText={setDescription}
                    multiline
                    numberOfLines={3}
                    maxLength={400}
                    textAlignVertical="top"
                  />
                </View>
                <Text style={[styles.counter, { color: colors.textSecondary }]}>{description.length}/400</Text>
              </View>

              <View style={styles.section}>
                <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>Ciudad</Text>
                <View
                  style={[
                    styles.inputRow,
                    {
                      backgroundColor: colors.surface,
                      borderColor: detectedCity && city === detectedCity ? Colors.primary : city ? Colors.primary : colors.border,
                    },
                  ]}
                >
                  <Ionicons
                    name={detectedCity && city === detectedCity ? 'location' : 'location-outline'}
                    size={16}
                    color={detectedCity && city === detectedCity ? Colors.primary : city ? Colors.primary : colors.textSecondary}
                  />
                  <TextInput
                    style={[styles.inputInRow, { color: colors.text }]}
                    placeholder={locLoading ? 'Detectando ubicación...' : 'Ej: Madrid, Bogotá, Buenos Aires...'}
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
                <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>Foto del grupo</Text>
                <Pressable style={[styles.coverPicker, { backgroundColor: colors.surface, borderColor: colors.border }]} onPress={pickCover}>
                  {coverAsset ? (
                    <Image source={{ uri: coverAsset.uri }} style={styles.coverPreview} resizeMode="cover" />
                  ) : (
                    <View style={[styles.coverPreview, styles.coverPlaceholder]}>
                      <Ionicons name="camera-outline" size={24} color={Colors.primary} />
                    </View>
                  )}
                  <View style={{ flex: 1 }}>
                    <Text style={[styles.coverTitle, { color: colors.text }]}>
                      {coverAsset ? 'Foto seleccionada' : 'Anadir foto'}
                    </Text>
                    <Text style={[styles.coverHint, { color: colors.textSecondary }]}>
                      Opcional. Se vera como imagen de perfil del grupo.
                    </Text>
                  </View>
                </Pressable>
              </View>

              <WaviiButton
                title="Siguiente →"
                onPress={handleNext}
                disabled={!canStep2}
              />
            </View>
          )}

          {/* PASO 3 — Revisa y crea */}
          {step === 3 && (
            <View style={styles.stepContainer}>
              {/* Preview card */}
              {selectedCategory && (
                <View style={[styles.previewCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
                  {coverAsset ? (
                    <Image source={{ uri: coverAsset.uri }} style={styles.previewCover} resizeMode="cover" />
                  ) : (
                    <View style={[styles.previewIconWrap, { backgroundColor: `${selectedCategory.color}18` }]}>
                      <Ionicons name={selectedCategory.icon} size={28} color={selectedCategory.color} />
                    </View>
                  )}
                  <Text style={[styles.previewName, { color: colors.text }]}>{name || 'Sin nombre'}</Text>
                  {description ? (
                    <Text style={[styles.previewDesc, { color: colors.textSecondary }]} numberOfLines={3}>
                      {description}
                    </Text>
                  ) : null}
                  <View style={styles.previewMeta}>
                    <View style={[styles.previewBadge, { backgroundColor: `${selectedCategory.color}18` }]}>
                      <Text style={[styles.previewBadgeText, { color: selectedCategory.color }]}>
                        {selectedCategory.label}
                      </Text>
                    </View>
                    {city ? (
                      <View style={styles.previewLocation}>
                        <Ionicons name="location-outline" size={12} color={colors.textSecondary} />
                        <Text style={[styles.previewLocationText, { color: colors.textSecondary }]}>{city}</Text>
                      </View>
                    ) : null}
                  </View>
                </View>
              )}

              {error ? <Text style={styles.errorText}>{error}</Text> : null}

              <WaviiButton
                title="Crear comunidad"
                onPress={handleCreate}
                loading={loading}
                disabled={!canStep1 || !canStep2}
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
  stepContainer: {
    gap: Spacing.lg,
  },
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
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.sm,
  },
  categoryCard: {
    width: '48%',
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    padding: Spacing.md,
    gap: Spacing.xs,
    minHeight: 90,
    position: 'relative',
  },
  categoryIconWrap: {
    width: 40,
    height: 40,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  categoryLabel: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    lineHeight: 18,
  },
  categoryCheck: {
    position: 'absolute',
    top: 8,
    right: 8,
  },
  section: { gap: 6 },
  sectionLabel: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  inputBlock: { marginBottom: 0 },
  counter: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'right',
  },
  textAreaWrap: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  textArea: {
    minHeight: 80,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    padding: 0,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    paddingHorizontal: 12,
    paddingVertical: 12,
  },
  inputInRow: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    padding: 0,
  },
  coverPicker: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.sm,
  },
  coverPreview: {
    width: 58,
    height: 58,
    borderRadius: 29,
  },
  coverPlaceholder: {
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Colors.primaryOpacity10,
  },
  coverTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  coverHint: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 17,
  },
  previewCard: {
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    padding: Spacing.md,
    gap: Spacing.sm,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06,
    shadowRadius: 4,
    elevation: 2,
  },
  previewIconWrap: {
    width: 56,
    height: 56,
    borderRadius: BorderRadius.xl,
    alignItems: 'center',
    justifyContent: 'center',
  },
  previewCover: {
    width: 74,
    height: 74,
    borderRadius: 37,
  },
  previewName: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    lineHeight: 24,
  },
  previewDesc: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  previewMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    flexWrap: 'wrap',
    marginTop: 2,
  },
  previewBadge: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: BorderRadius.full,
  },
  previewBadgeText: {
    fontFamily: FontFamily.semiBold,
    fontSize: 11,
  },
  previewLocation: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 3,
  },
  previewLocationText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },
  errorText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    color: Colors.error,
    textAlign: 'center',
  },
  bottomSpace: { height: Spacing.xl },
});
