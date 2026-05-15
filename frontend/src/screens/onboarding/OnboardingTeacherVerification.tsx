import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  KeyboardAvoidingView,
  ScrollView,
  Platform,
  Image,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { useAlert } from '../../context/AlertContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingTeacherVerification'>;
  route: RouteProp<AuthStackParamList, 'OnboardingTeacherVerification'>;
};

const TOTAL_STEPS = 7;
const CURRENT_STEP = 6;

const ProgressDots = () => (
  <View style={styles.dotsRow}>
    {Array.from({ length: TOTAL_STEPS }).map((_, i) => (
      <View
        key={i}
        style={[styles.dot, i + 1 === CURRENT_STEP ? styles.dotActive : styles.dotInactive]}
      />
    ))}
  </View>
);

export const OnboardingTeacherVerification: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const [imageUri, setImageUri] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handlePickImage = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      showAlert({ title: 'Permiso denegado', message: 'Necesitamos acceso a tu galería para seleccionar el documento.' });
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: 'images',
      quality: 0.8,
    });
    if (!result.canceled && result.assets[0]) {
      setImageUri(result.assets[0].uri);
    }
  };

  const handleSendForReview = async () => {
    if (!imageUri) {
      showAlert({ title: 'Documento requerido', message: 'Por favor, sube un documento que acredite tu titulación.' });
      return;
    }
    setLoading(true);
    await new Promise((r) => setTimeout(r, 1000));
    setLoading(false);
    navigation.navigate('OnboardingTeacherWaiting');
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>

      {/* Flecha volver */}
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>
      <ProgressDots />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.flex}>
        <ScrollView keyboardShouldPersistTaps="handled" contentContainerStyle={styles.container} showsVerticalScrollIndicator={false}>
          <View style={styles.headerSection}>
            <Text style={[styles.title, { color: colors.text }]}>Sube tu documentación</Text>
            <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
              Necesitamos verificar tu titulación para activar tu insignia de Profesor Certificado
            </Text>
          </View>

          <TouchableOpacity
            style={[
              styles.uploadArea,
              {
                borderColor: imageUri ? Colors.success : colors.border,
                backgroundColor: imageUri ? Colors.successLight : colors.surface,
              },
            ]}
            onPress={handlePickImage}
            activeOpacity={0.8}
          >
            {imageUri ? (
              <Image source={{ uri: imageUri }} style={styles.previewImage} resizeMode="cover" />
            ) : (
              <>
                <Ionicons name="document-attach-outline" size={40} color={colors.textSecondary} />
                <Text style={[styles.uploadTitle, { color: colors.text }]}>Seleccionar documento</Text>
                <Text style={[styles.uploadNote, { color: colors.textSecondary }]}>JPG, PNG — Toca para abrir galería</Text>
              </>
            )}
            {imageUri && (
              <View style={styles.changeOverlay}>
                <Text style={styles.changeText}>Cambiar</Text>
              </View>
            )}
          </TouchableOpacity>

          <View style={styles.btnSection}>
            <WaviiButton
              title="Enviar para revisión"
              onPress={handleSendForReview}
              loading={loading}
            />
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,

  },
  flex: { flex: 1 },
  dotsRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
    paddingTop: Spacing.base,
    paddingBottom: Spacing.sm,
  },
  dot: {
    height: 8,
    borderRadius: 4,
  },
  dotActive: {
    width: 24,
    backgroundColor: Colors.primary,
  },
  dotInactive: {
    width: 8,
    backgroundColor: Colors.border,
  },
  container: {
    flexGrow: 1,
    paddingHorizontal: Spacing.xl,
    paddingBottom: Spacing.lg,
    justifyContent: 'center',
  },
  headerSection: {
    paddingTop: Spacing.base,
    gap: Spacing.sm,
    marginBottom: Spacing.base,
    alignItems: 'center',
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
    textAlign: 'center',
  },
  uploadArea: {
    borderWidth: 2,
    borderStyle: 'dashed',
    borderRadius: BorderRadius.lg,
    height: 160,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.xs,
    marginBottom: Spacing.base,
    overflow: 'hidden',
  },
  previewImage: {
    width: '100%',
    height: '100%',
  },
  uploadTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  uploadNote: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },
  changeOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.35)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  changeText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    color: Colors.white,
  },
  btnSection: {
    paddingBottom: Spacing.lg,
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});
