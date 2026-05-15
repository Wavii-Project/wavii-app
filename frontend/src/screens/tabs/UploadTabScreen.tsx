import React, { useState } from 'react';
import {
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import * as DocumentPicker from 'expo-document-picker';
import * as ImagePicker from 'expo-image-picker';
import { WaviiButton } from '../../components/common/WaviiButton';
import { WaviiInput } from '../../components/common/WaviiInput';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { apiUploadPdf } from '../../api/pdfApi';

const DIFFICULTY_OPTIONS = [
  { value: 1, label: 'Principiante', color: '#22C55E' },
  { value: 2, label: 'Intermedio', color: '#F59E0B' },
  { value: 3, label: 'Avanzado', color: '#EF4444' },
];

export const UploadTabScreen = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation();

  const [songTitle, setSongTitle] = useState('');
  const [description, setDescription] = useState('');
  const [difficulty, setDifficulty] = useState<number>(1);
  const [file, setFile] = useState<{ uri: string; name: string } | null>(null);
  const [coverImage, setCoverImage] = useState<{ uri: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const pickFile = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/pdf',
        copyToCacheDirectory: true,
      });
      if (!result.canceled && result.assets.length > 0) {
        const asset = result.assets[0];
        setFile({ uri: asset.uri, name: asset.name });
      }
    } catch {
      setError('No se pudo seleccionar el archivo.');
    }
  };

  const pickCover = async () => {
    try {
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: true,
        aspect: [4, 3],
        quality: 0.8,
      });
      if (!result.canceled && result.assets[0]) {
        setCoverImage({ uri: result.assets[0].uri });
      }
    } catch {
      setError('No se pudo seleccionar la portada.');
    }
  };

  const canSubmit = songTitle.trim().length > 0 && file !== null;

  const handleUpload = async () => {
    if (!token || !canSubmit || loading || !file) return;
    setError(null);
    setLoading(true);
    try {
      await apiUploadPdf(file.uri, file.name, songTitle.trim(), description.trim(), coverImage?.uri ?? null, difficulty, token);
      navigation.goBack();
    } catch (e: any) {
      setError(e?.message ?? 'No se pudo subir la tablatura.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <KeyboardAvoidingView style={styles.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={[styles.header, { borderBottomColor: colors.border }]}>
          <Pressable onPress={() => navigation.goBack()} style={styles.backBtn}>
            <Ionicons name="chevron-back" size={26} color={colors.text} />
          </Pressable>
          <Text style={[styles.headerTitle, { color: colors.text }]}>Subir tablatura</Text>
          <View style={styles.headerSpacer} />
        </View>

        <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>
          <View style={[styles.heroCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
            <Text style={[styles.heroTitle, { color: colors.text }]}>Comparte una tablatura que apetezca abrir</Text>
            <Text style={[styles.heroText, { color: colors.textSecondary }]}>
              Añade una descripción breve y una portada opcional para que destaque en Home, desafíos y tablaturas populares.
            </Text>
          </View>

          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Título de la canción *</Text>
            <WaviiInput
              placeholder="Ej: Stairway to Heaven - Led Zeppelin"
              value={songTitle}
              onChangeText={setSongTitle}
              maxLength={120}
              containerStyle={styles.inputBlock}
            />
          </View>

          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Descripción</Text>
            <WaviiInput
              placeholder="Cuenta qué contiene la tablatura"
              value={description}
              onChangeText={setDescription}
              maxLength={800}
              containerStyle={styles.inputBlock}
            />
          </View>

          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Portada opcional</Text>
            <Pressable style={[styles.coverPicker, { backgroundColor: colors.surface, borderColor: colors.border }]} onPress={pickCover}>
              {coverImage ? (
                <Image source={{ uri: coverImage.uri }} style={styles.coverPreview} />
              ) : (
                <View style={styles.coverEmpty}>
                  <Ionicons name="image-outline" size={26} color={colors.textSecondary} />
                  <Text style={[styles.coverText, { color: colors.textSecondary }]}>Toca para elegir una imagen</Text>
                </View>
              )}
            </Pressable>
            {coverImage ? (
              <Pressable onPress={() => setCoverImage(null)} style={styles.clearLink}>
                <Text style={styles.clearLinkText}>Quitar portada</Text>
              </Pressable>
            ) : null}
          </View>

          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Nivel de dificultad *</Text>
            <View style={styles.diffRow}>
              {DIFFICULTY_OPTIONS.map((opt) => {
                const selected = difficulty === opt.value;
                return (
                  <Pressable
                    key={opt.value}
                    style={[
                      styles.diffCard,
                      {
                        backgroundColor: selected ? opt.color + '18' : colors.surface,
                        borderColor: selected ? opt.color : colors.border,
                      },
                    ]}
                    onPress={() => setDifficulty(opt.value)}
                  >
                    <View style={[styles.diffDot, { backgroundColor: opt.color }]} />
                    <Text style={[styles.diffLabel, { color: selected ? opt.color : colors.text }]}>{opt.label}</Text>
                    <Text style={[styles.diffNum, { color: colors.textSecondary }]}>Nivel {opt.value}</Text>
                  </Pressable>
                );
              })}
            </View>
          </View>

          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Archivo PDF *</Text>
            <Pressable
              style={[styles.filePicker, { backgroundColor: colors.surface, borderColor: file ? Colors.primary : colors.border }]}
              onPress={pickFile}
            >
              <Ionicons
                name={file ? 'document-text' : 'cloud-upload-outline'}
                size={28}
                color={file ? Colors.primary : colors.textSecondary}
              />
              <Text style={[styles.filePickerText, { color: file ? colors.text : colors.textSecondary }]}>
                {file ? file.name : 'Toca para seleccionar un PDF'}
              </Text>
              <Text style={[styles.filePickerHint, { color: colors.textSecondary }]}>
                El PDF se usará como tablatura principal del contenido.
              </Text>
              {file && (
                <Pressable onPress={() => setFile(null)} style={styles.clearFile} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
                  <Ionicons name="close-circle" size={18} color={colors.textSecondary} />
                </Pressable>
              )}
            </Pressable>
          </View>

          {error ? <Text style={styles.error}>{error}</Text> : null}

          <WaviiButton title="Subir tablatura" onPress={handleUpload} loading={loading} disabled={!canSubmit} />

          <View style={styles.bottomSpace} />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: 12,
    borderBottomWidth: 1,
  },
  backBtn: { padding: 4 },
  headerTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  headerSpacer: { width: 28 },
  body: { padding: Spacing.base, gap: Spacing.lg },
  heroCard: { borderWidth: 1, borderRadius: BorderRadius.xl, padding: Spacing.base, gap: Spacing.sm },
  heroBadge: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: Colors.primaryOpacity10,
    alignItems: 'center',
    justifyContent: 'center',
  },
  heroTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  heroText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  section: { gap: 8 },
  sectionTitle: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  inputBlock: { marginBottom: 0 },
  diffRow: { flexDirection: 'row', gap: Spacing.sm },
  diffCard: {
    flex: 1,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    gap: 4,
    alignItems: 'center',
  },
  diffDot: { width: 8, height: 8, borderRadius: 4 },
  diffLabel: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  diffNum: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  coverPicker: {
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    borderStyle: 'dashed',
    minHeight: 168,
    overflow: 'hidden',
  },
  coverPreview: { width: '100%', height: 168 },
  coverEmpty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    padding: Spacing.base,
  },
  coverText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm, textAlign: 'center' },
  clearLink: { alignSelf: 'flex-end' },
  clearLinkText: { fontFamily: FontFamily.bold, color: Colors.primary, fontSize: FontSize.xs },
  filePicker: {
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    borderStyle: 'dashed',
    padding: Spacing.base,
    alignItems: 'center',
    gap: Spacing.xs,
    minHeight: 132,
    justifyContent: 'center',
  },
  filePickerText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm, textAlign: 'center', maxWidth: '82%' },
  filePickerHint: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, textAlign: 'center' },
  clearFile: { position: 'absolute', top: 8, right: 8 },
  error: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, color: Colors.error, textAlign: 'center' },
  bottomSpace: { height: Spacing.xl },
});
