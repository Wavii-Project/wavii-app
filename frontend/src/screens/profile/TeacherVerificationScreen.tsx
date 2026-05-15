import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import * as DocumentPicker from 'expo-document-picker';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useNavigation } from '@react-navigation/native';
import { WaviiButton } from '../../components/common/WaviiButton';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { apiGetVerificationStatus, apiUploadDocument, VerificationStatusResponse } from '../../api/verificationApi';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';

const WAVII_IMAGE = require('../../../assets/wavii/wavii_bienvenida.png');

export const TeacherVerificationScreen: React.FC = () => {
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();
  const { token, updateUser } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();

  const [status, setStatus] = useState<VerificationStatusResponse | null>(null);
  const [loadingStatus, setLoadingStatus] = useState(true);
  const [pickedFile, setPickedFile] = useState<{ uri: string; name: string; mimeType: string } | null>(null);
  const [uploading, setUploading] = useState(false);

  const loadStatus = useCallback(async () => {
    if (!token) {
      setLoadingStatus(false);
      return;
    }
    setLoadingStatus(true);
    try {
      const nextStatus = await apiGetVerificationStatus(token);
      setStatus(nextStatus);
      if (nextStatus.status === 'APPROVED') {
        updateUser({ teacherVerified: true, role: 'profesor_certificado' });
      }
    } catch {
      setStatus({ status: 'NONE' });
    } finally {
      setLoadingStatus(false);
    }
  }, [token, updateUser]);

  useEffect(() => {
    loadStatus();
  }, [loadStatus]);

  const handlePickDocument = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/pdf',
        copyToCacheDirectory: true,
      });

      if (result.canceled) {
        return;
      }

      const asset = result.assets[0];
      setPickedFile({
        uri: asset.uri,
        name: asset.name,
        mimeType: asset.mimeType ?? 'application/octet-stream',
      });
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo seleccionar el documento.' });
    }
  };

  const handleUpload = async () => {
    if (!token || !pickedFile) {
      showAlert({ title: 'Documento requerido', message: 'Selecciona un PDF de tu certificado.' });
      return;
    }

    setUploading(true);
    try {
      await apiUploadDocument(pickedFile.uri, pickedFile.name, pickedFile.mimeType, token);
      await loadStatus();
      showAlert({
        title: 'Documento enviado',
        message: 'Lo hemos mandado a Odoo para su revisión. Mientras tanto seguirás como profesor particular.',
      });
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo enviar el documento. Inténtalo de nuevo.' });
    } finally {
      setUploading(false);
    }
  };

  const statusTone =
    status?.status === 'APPROVED' ? Colors.success :
    status?.status === 'REJECTED' ? Colors.error :
    Colors.warning;

  const statusLabel =
    status?.status === 'APPROVED' ? 'Insignia aprobada' :
    status?.status === 'REJECTED' ? 'Revisión rechazada' :
    status?.status === 'PENDING' ? 'Revisión en curso' :
    'Aún sin enviar';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <TouchableOpacity
        style={styles.backBtn}
        onPress={() => navigation.goBack()}
        hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
      >
        <Ionicons name="chevron-back" size={28} color={colors.text} />
      </TouchableOpacity>

      {loadingStatus ? (
        <View style={styles.centered}>
          <ActivityIndicator color={Colors.primary} size="large" />
        </View>
      ) : (
        <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
          <Image source={WAVII_IMAGE} style={styles.mascot} resizeMode="contain" />

          <Text style={[styles.title, { color: colors.text }]}>Consigue tu insignia docente</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            Sube tu título, diploma o certificado musical en PDF para que el equipo de Odoo pueda validar tu perfil.
          </Text>

          <View
            style={[
              styles.statusCard,
              {
                backgroundColor: colors.surface,
                borderColor: `${statusTone}44`,
              },
            ]}
          >
            <Ionicons
              name={
                status?.status === 'APPROVED'
                  ? 'shield-checkmark'
                  : status?.status === 'REJECTED'
                  ? 'close-circle'
                  : 'time-outline'
              }
              size={22}
              color={statusTone}
            />
            <View style={styles.statusTextWrap}>
              <Text style={[styles.statusLabel, { color: statusTone }]}>{statusLabel}</Text>
              <Text style={[styles.statusHelp, { color: colors.textSecondary }]}>
                {status?.status === 'APPROVED'
                  ? 'Tu perfil ya aparece como profesor certificado.'
                  : status?.status === 'REJECTED'
                  ? 'Puedes reenviar un documento más claro o actualizado.'
                  : status?.status === 'PENDING'
                  ? `Documento enviado: ${status.fileName ?? 'archivo recibido'}`
                  : 'Todavía no has enviado documentación para revisión.'}
              </Text>
            </View>
          </View>

          {status?.status !== 'APPROVED' ? (
            <>
              <TouchableOpacity
                style={[
                  styles.uploadArea,
                  {
                    borderColor: pickedFile ? Colors.primary : colors.border,
                    backgroundColor: colors.surface,
                  },
                ]}
                onPress={handlePickDocument}
                activeOpacity={0.78}
              >
                <Ionicons
                  name={pickedFile ? 'document-text-outline' : 'cloud-upload-outline'}
                  size={40}
                  color={pickedFile ? Colors.primary : colors.textSecondary}
                />
                <Text style={[styles.uploadTitle, { color: pickedFile ? Colors.primary : colors.text }]}>
                  {pickedFile ? pickedFile.name : 'Seleccionar documento'}
                </Text>
                <Text style={[styles.uploadNote, { color: colors.textSecondary }]}>
                  Solo se aceptan archivos PDF.
                </Text>
              </TouchableOpacity>

              <View style={[styles.infoBox, { backgroundColor: colors.surface, borderColor: colors.border }]}>
                <Ionicons name="information-circle-outline" size={18} color={Colors.gray} />
                <Text style={[styles.infoText, { color: colors.textSecondary }]}>
                  Enviaremos a Odoo tu email, el nombre del archivo y la URL pública del documento para que puedan revisarlo y aprobarte manualmente.
                </Text>
              </View>
            </>
          ) : null}
        </ScrollView>
      )}

      {!loadingStatus && status?.status !== 'APPROVED' ? (
        <View style={styles.footer}>
          <WaviiButton
            title={status?.status === 'PENDING' ? 'Volver a enviar documento' : 'Enviar documento'}
            onPress={handleUpload}
            loading={uploading}
            disabled={!pickedFile || uploading}
          />
          <WaviiButton
            title="Volver a ajustes"
            onPress={() => navigation.goBack()}
            variant="outline"
          />
        </View>
      ) : null}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  centered: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  backBtn: {
    position: 'absolute',
    top: 56,
    left: Spacing.base,
    zIndex: 10,
    padding: Spacing.xs,
  },
  content: {
    paddingTop: 96,
    paddingHorizontal: Spacing.xl,
    paddingBottom: Spacing.base,
    gap: Spacing.base,
  },
  mascot: {
    width: 170,
    height: 136,
    alignSelf: 'center',
  },
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
  statusCard: {
    flexDirection: 'row',
    gap: Spacing.sm,
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
  },
  statusTextWrap: {
    flex: 1,
    gap: 2,
  },
  statusLabel: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  statusHelp: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 18,
  },
  uploadArea: {
    borderWidth: 2,
    borderStyle: 'dashed',
    borderRadius: BorderRadius.lg,
    paddingVertical: Spacing.xl,
    paddingHorizontal: Spacing.base,
    alignItems: 'center',
    gap: Spacing.xs,
  },
  uploadTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    textAlign: 'center',
  },
  uploadNote: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
    lineHeight: 18,
  },
  infoBox: {
    flexDirection: 'row',
    gap: Spacing.sm,
    borderWidth: 1,
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
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.lg,
    gap: Spacing.xs,
  },
});
