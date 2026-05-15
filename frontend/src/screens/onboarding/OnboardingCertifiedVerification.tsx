import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
  Image,
} from 'react-native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as DocumentPicker from 'expo-document-picker';
import { AuthStackParamList } from '../../navigation/AuthNavigator';
import { WaviiButton } from '../../components/common/WaviiButton';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { apiUploadDocument } from '../../api/verificationApi';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { useAlert } from '../../context/AlertContext';

type Props = {
  navigation: NativeStackNavigationProp<AuthStackParamList, 'OnboardingCertifiedVerification'>;
};

const TOTAL_STEPS = 7;
const CURRENT_STEP = 6;

const WAVII_IMAGE = require('../../../assets/wavii/wavii_bienvenida.png');

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

export const OnboardingCertifiedVerification: React.FC<Props> = ({ navigation }) => {
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const { pendingToken, confirmEmailVerified } = useAuth();

  const [pickedFile, setPickedFile] = useState<{ uri: string; name: string; mimeType: string } | null>(null);
  const [uploaded, setUploaded] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [continuing, setContinuing] = useState(false);

  const handlePickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ['application/pdf', 'image/*'],
        copyToCacheDirectory: true,
      });

      if (result.canceled) return;

      const asset = result.assets[0];
      setPickedFile({
        uri: asset.uri,
        name: asset.name,
        mimeType: asset.mimeType ?? 'application/octet-stream',
      });
      setUploaded(false);
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo seleccionar el archivo.' });
    }
  };

  const handleUpload = async () => {
    if (!pickedFile) {
      showAlert({ title: 'Documento requerido', message: 'Selecciona un PDF o imagen de tu titulación.' });
      return;
    }
    setUploading(true);
    try {
      await apiUploadDocument(pickedFile.uri, pickedFile.name, pickedFile.mimeType, pendingToken ?? '');
      setUploaded(true);
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo enviar el documento. Inténtalo de nuevo.' });
    } finally {
      setUploading(false);
    }
  };

  const handleContinue = async () => {
    if (!uploaded) {
      showAlert({
        title: 'Sin insignia verificada',
        message: 'Si continúas sin subir el documento no obtendrás la insignia de "Profesor Certificado". Puedes conseguirla más tarde desde tu perfil.',
        buttons: [
          { text: 'Subir ahora', style: 'cancel' },
          {
            text: 'Continuar sin verificar',
            style: 'destructive',
            onPress: async () => {
              setContinuing(true);
              try { await confirmEmailVerified(); } finally { setContinuing(false); }
            },
          },
        ],
      });
      return;
    }
    setContinuing(true);
    try {
      await confirmEmailVerified();
    } finally {
      setContinuing(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>

      <ProgressDots />

      <ScrollView
        contentContainerStyle={styles.container}
        showsVerticalScrollIndicator={false}
      >
        <Image source={WAVII_IMAGE} style={styles.mascot} resizeMode="contain" />

        <Text style={[styles.title, { color: colors.text }]}>Verifica tus credenciales</Text>
        <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
          Ya puedes usar Scholar. Si quieres la insignia de profesor certificado, sube ahora tu título, diploma o certificación para que Odoo revise tu perfil.
        </Text>

        {/* Área de selección / estado de subida */}
        {uploaded ? (
          <View style={[styles.statusBox, { backgroundColor: colors.surface, borderColor: Colors.success }]}>
            <Ionicons name="checkmark-circle" size={28} color={Colors.success} />
            <View style={styles.statusTexts}>
              <Text style={[styles.statusFileName, { color: colors.text }]} numberOfLines={1}>
                {pickedFile?.name}
              </Text>
              <Text style={[styles.statusLabel, { color: Colors.success }]}>Enviado para revisión</Text>
            </View>
          </View>
        ) : (
          <TouchableOpacity
            style={[styles.uploadArea, { borderColor: pickedFile ? Colors.primary : colors.border, backgroundColor: colors.surface }]}
            onPress={handlePickDocument}
            activeOpacity={0.75}
          >
            <Ionicons
              name={pickedFile ? 'document-text-outline' : 'document-attach-outline'}
              size={40}
              color={pickedFile ? Colors.primary : colors.textSecondary}
            />
            <Text style={[styles.uploadTitle, { color: pickedFile ? Colors.primary : colors.text }]}>
              {pickedFile ? pickedFile.name : 'Seleccionar documento'}
            </Text>
            <Text style={[styles.uploadNote, { color: colors.textSecondary }]}>
              {pickedFile ? 'Toca para cambiar' : 'PDF o imagen — Toca para seleccionar'}
            </Text>
          </TouchableOpacity>
        )}

        {/* Info box */}
        <View style={[styles.infoBox, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <Ionicons name="information-circle-outline" size={20} color={Colors.gray} />
          <Text style={[styles.infoText, { color: colors.textSecondary }]}>
            Te notificaremos por email en 24–48 h. Mientras tanto tienes acceso completo a Scholar y puedes completar este paso más tarde desde Ajustes.
          </Text>
        </View>
      </ScrollView>

      {/* Botones fijos abajo */}
      <View style={styles.footer}>
        {!uploaded && (
          <WaviiButton
            title="Enviar documento"
            onPress={handleUpload}
            loading={uploading}
            disabled={!pickedFile || uploading}
          />
        )}
        <WaviiButton
          title="Continuar a la app"
          onPress={handleContinue}
          loading={continuing}
          variant={uploaded ? 'primary' : 'outline'}
        />
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  dotsRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
    paddingTop: Spacing.base,
    paddingBottom: Spacing.sm,
  },
  dot: { height: 8, borderRadius: 4 },
  dotActive: { width: 24, backgroundColor: Colors.primary },
  dotInactive: { width: 8, backgroundColor: Colors.border },
  container: {
    flexGrow: 1,
    paddingHorizontal: Spacing.xl,
    paddingTop: Spacing.lg,
    paddingBottom: Spacing.base,
    alignItems: 'center',
    gap: Spacing.sm,
  },
  mascot: { width: 180, height: 144, marginBottom: 0 },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 20,
  },
  uploadArea: {
    width: '100%',
    borderWidth: 2,
    borderStyle: 'dashed',
    borderRadius: BorderRadius.lg,
    paddingVertical: Spacing.xl,
    alignItems: 'center',
    gap: Spacing.xs,
  },
  uploadTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    textAlign: 'center',
    paddingHorizontal: Spacing.sm,
  },
  uploadNote: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },
  statusBox: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
  },
  statusTexts: { flex: 1 },
  statusFileName: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },
  statusLabel: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginTop: 2,
  },
  infoBox: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.sm,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
  },
  infoText: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 18,
  },
  footer: {
    paddingHorizontal: Spacing.xl,
    paddingBottom: Spacing.lg,
    paddingTop: Spacing.sm,
    gap: Spacing.xs,
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
});
