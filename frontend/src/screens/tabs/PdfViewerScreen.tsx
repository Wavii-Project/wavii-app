import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  Platform,
  Linking,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { WebView } from 'react-native-webview';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { useAlert } from '../../context/AlertContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { AppStackParamList } from '../../navigation/AppNavigator';
import {
  PdfDocument,
  apiFetchPdfById,
  apiLikePdf,
  apiReportPdf,
  apiUnlikePdf,
  pdfDownloadUrl,
} from '../../api/pdfApi';
import { http } from '../../api/http';

type Route = RouteProp<AppStackParamList, 'PdfViewer'>;

function buildAndroidHtml(base64: string): string {
  return `<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width,initial-scale=1.0,maximum-scale=3.0,user-scalable=yes"/>
<style>
  *{margin:0;padding:0;box-sizing:border-box}
  body{background:#FFF7ED;display:flex;flex-direction:column;align-items:center;padding:10px;gap:14px}
  canvas{max-width:100%;height:auto;box-shadow:0 4px 12px rgba(0,0,0,.12);border-radius:4px}
</style>
<script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js"></script>
</head>
<body><div id="c"></div>
<script>
pdfjsLib.GlobalWorkerOptions.workerSrc='https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
try {
  const raw=atob('${base64}');
  const buf=new Uint8Array(raw.length);
  for(let i=0;i<raw.length;i++)buf[i]=raw.charCodeAt(i);
  pdfjsLib.getDocument({data:buf}).promise.then(pdf=>{
    const el=document.getElementById('c');
    let p=Promise.resolve();
    for(let n=1;n<=pdf.numPages;n++){
      const pg=n;
      p=p.then(()=>pdf.getPage(pg)).then(page=>{
        const vp=page.getViewport({scale:window.devicePixelRatio||1.5});
        const cv=document.createElement('canvas');
        cv.height=vp.height;cv.width=vp.width;cv.style.width='100%';
        page.render({canvasContext:cv.getContext('2d'),viewport:vp});
        el.appendChild(cv);
      });
    }
  }).catch(()=>window.ReactNativeWebView&&window.ReactNativeWebView.postMessage('error'));
} catch(e){window.ReactNativeWebView&&window.ReactNativeWebView.postMessage('error');}
</script>
</body>
</html>`;
}

async function fetchPdfAsBase64(uri: string): Promise<string> {
  const res = await http.get<ArrayBuffer>(uri, { responseType: 'arraybuffer' });
  const bytes = new Uint8Array(res.data);
  const chunkSize = 8192;
  const chunks: string[] = [];
  for (let i = 0; i < bytes.byteLength; i += chunkSize) {
    chunks.push(String.fromCharCode.apply(null, Array.from(bytes.subarray(i, i + chunkSize))));
  }
  return btoa(chunks.join(''));
}

const ReportModal = ({
  visible,
  onClose,
  onReason,
}: {
  visible: boolean;
  onClose: () => void;
  onReason: (reason: string) => void;
}) => {
  const { colors } = useTheme();
  const reasons = [
    { id: 'COPYRIGHT', label: 'Copyright' },
    { id: 'NOT_A_TAB', label: 'No es tablatura' },
    { id: 'WRONG_CONTENT', label: 'Contenido incorrecto' },
  ];

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={onClose}>
        <View style={[styles.reportCard, { backgroundColor: colors.surface, borderColor: colors.border }]} onStartShouldSetResponder={() => true}>
          <Text style={[styles.reportTitle, { color: colors.text }]}>Reportar tablatura</Text>
          <Text style={[styles.reportSub, { color: colors.textSecondary }]}>Elige el motivo del reporte.</Text>
          {reasons.map((item) => (
            <TouchableOpacity key={item.id} style={[styles.reasonBtn, { borderColor: colors.border }]} onPress={() => onReason(item.id)}>
              <Text style={[styles.reasonText, { color: colors.text }]}>{item.label}</Text>
              <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
            </TouchableOpacity>
          ))}
          <TouchableOpacity style={styles.cancelReportBtn} onPress={onClose}>
            <Text style={[styles.cancelReportText, { color: colors.textSecondary }]}>Cancelar</Text>
          </TouchableOpacity>
        </View>
      </TouchableOpacity>
    </Modal>
  );
};

export const PdfViewerScreen = () => {
  const { colors } = useTheme();
  const { token, user } = useAuth();
  const { showAlert } = useAlert();
  const navigation = useNavigation<any>();
  const route = useRoute<Route>();
  const { pdfId, title } = route.params;

  const [pdfBase64, setPdfBase64] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(true);
  const [loadError, setLoadError] = useState(false);
  const [webLoading, setWebLoading] = useState(true);
  const [pdfMeta, setPdfMeta] = useState<PdfDocument | null>(null);
  const [liking, setLiking] = useState(false);
  const [reportModalVisible, setReportModalVisible] = useState(false);
  const [paywallVisible, setPaywallVisible] = useState(false);

  const uri = pdfDownloadUrl(pdfId);
  const isAndroid = Platform.OS === 'android';

  useEffect(() => {
    apiFetchPdfById(pdfId, token ?? undefined)
      .then(setPdfMeta)
      .catch(() => {
        // not critical
      });
  }, [pdfId, token]);

  const download = useCallback(() => {
    setLoadError(false);
    setPdfBase64(null);
    setDownloading(true);
    fetchPdfAsBase64(uri)
      .then(setPdfBase64)
      .catch(() => setLoadError(true))
      .finally(() => setDownloading(false));
  }, [uri]);

  useEffect(() => {
    if (isAndroid) {
      download();
    } else {
      setDownloading(false);
    }
  }, [download, isAndroid]);

  const handleLike = async () => {
    if (!token || !pdfMeta || liking) return;
    setLiking(true);
    try {
      const updated = pdfMeta.likedByMe
        ? await apiUnlikePdf(pdfMeta.id, token)
        : await apiLikePdf(pdfMeta.id, token);
      setPdfMeta(updated);
    } catch {
      // ignore
    } finally {
      setLiking(false);
    }
  };

  const handleReport = async (reason: string) => {
    if (!token || !pdfMeta) return;
    try {
      await apiReportPdf(pdfMeta.id, { reason }, token);
      showAlert({
        title: 'Reporte enviado',
        message: 'Gracias por avisar. Revisaremos esta tablatura.',
      });
    } catch {
      showAlert({
        title: 'Error',
        message: 'No se pudo enviar el reporte.',
      });
    }
  };

  const handleOpenOwnerProfile = () => {
    if (!pdfMeta?.ownerId) return;
    navigation.navigate('UserProfile', { userId: String(pdfMeta.ownerId) });
  };

  const handleDownload = async () => {
    const subscription = String(user?.subscription ?? '').toLowerCase();
    if (!subscription || subscription === 'free') {
      setPaywallVisible(true);
      return;
    }
    try {
      await Linking.openURL(uri);
    } catch {
      showAlert({
        title: 'Error',
        message: 'No se pudo abrir la descarga.',
      });
    }
  };

  const showOverlay = isAndroid ? downloading : webLoading;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Pressable onPress={() => navigation.goBack()} style={styles.backBtn} hitSlop={12}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.text }]} numberOfLines={1}>
          {title}
        </Text>
        <View style={styles.headerSpacer} />
      </View>

      {loadError ? (
        <View style={styles.center}>
          <Ionicons name="document-text-outline" size={52} color={colors.textSecondary} />
          <Text style={[styles.errorText, { color: colors.textSecondary }]}>
            No se pudo cargar el PDF.{'\n'}Comprueba tu conexion e intentalo de nuevo.
          </Text>
          <Pressable style={[styles.retryBtn, { backgroundColor: Colors.primary }]} onPress={download}>
            <Text style={styles.retryBtnText}>Reintentar</Text>
          </Pressable>
        </View>
      ) : (
        <View style={styles.webContainer}>
          {(!isAndroid || pdfBase64 !== null) && (
            <WebView
              style={styles.webview}
              source={
                isAndroid && pdfBase64
                  ? { html: buildAndroidHtml(pdfBase64) }
                  : { uri, headers: { 'ngrok-skip-browser-warning': 'true' } }
              }
              onMessage={(event) => {
                if (event.nativeEvent.data === 'error') {
                  setWebLoading(false);
                  setLoadError(true);
                }
              }}
              onLoadStart={() => {
                if (!isAndroid) setWebLoading(true);
              }}
              onLoadEnd={() => setWebLoading(false)}
              onError={() => {
                setWebLoading(false);
                setLoadError(true);
              }}
              onHttpError={(event) => {
                if (event.nativeEvent.statusCode >= 400) {
                  setWebLoading(false);
                  setLoadError(true);
                }
              }}
              startInLoadingState={false}
              javaScriptEnabled
              domStorageEnabled
              allowFileAccess
              originWhitelist={['*']}
            />
          )}
          {showOverlay && (
            <View style={styles.loadingOverlay}>
              <ActivityIndicator size="large" color={Colors.primary} />
              <Text style={[styles.loadingText, { color: colors.textSecondary }]}>
                {downloading ? 'Descargando partitura...' : 'Cargando partitura...'}
              </Text>
            </View>
          )}
        </View>
      )}

      <View style={[styles.actionsBar, { borderTopColor: colors.border, backgroundColor: colors.surface }]}>
        <Pressable style={styles.actionPill} onPress={handleLike} disabled={!pdfMeta || liking || !token}>
          <Ionicons
            name={pdfMeta?.likedByMe ? 'heart' : 'heart-outline'}
            size={18}
            color={pdfMeta?.likedByMe ? Colors.error : colors.textSecondary}
          />
          <Text style={[styles.actionText, { color: colors.text }]}>
            {pdfMeta?.likeCount ?? 0}
          </Text>
        </Pressable>

        <Pressable style={styles.actionPill} onPress={() => setReportModalVisible(true)} disabled={!pdfMeta || !token}>
          <Ionicons name="flag-outline" size={18} color={Colors.error} />
        </Pressable>

        <Pressable style={styles.actionPill} onPress={handleOpenOwnerProfile} disabled={!pdfMeta?.ownerId}>
          <Ionicons name="person-outline" size={18} color={colors.textSecondary} />
        </Pressable>

        <Pressable style={[styles.downloadBtn, { backgroundColor: Colors.primary }]} onPress={handleDownload}>
          <Ionicons name="download-outline" size={16} color={Colors.white} />
          <Text style={styles.downloadBtnText}>Descargar</Text>
        </Pressable>
      </View>

      <ReportModal
        visible={reportModalVisible}
        onClose={() => setReportModalVisible(false)}
        onReason={async (reason) => {
          setReportModalVisible(false);
          await handleReport(reason);
        }}
      />

      <Modal visible={paywallVisible} transparent animationType="fade" onRequestClose={() => setPaywallVisible(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setPaywallVisible(false)}>
          <View style={[styles.paywallCard, { backgroundColor: colors.surface }]} onStartShouldSetResponder={() => true}>
            <Image source={require('../../../assets/wavii/wavii_bienvenida.png')} style={styles.paywallWavii} resizeMode="contain" />
            <Text style={[styles.paywallTitle, { color: colors.text }]}>Funcion Premium</Text>
            <Text style={[styles.paywallSub, { color: colors.textSecondary }]}>
              Descarga tablaturas sin limite con Wavii Plus o Education.
            </Text>
            <Pressable
              style={[styles.paywallBtn, { backgroundColor: Colors.primary }]}
              onPress={() => {
                setPaywallVisible(false);
                navigation.navigate('Subscription');
              }}
            >
              <Text style={styles.paywallBtnText}>Ver suscripciones</Text>
            </Pressable>
            <Pressable onPress={() => setPaywallVisible(false)}>
              <Text style={[styles.paywallCancel, { color: colors.textSecondary }]}>Cancelar</Text>
            </Pressable>
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 10,
    borderBottomWidth: 1,
    gap: Spacing.xs,
  },
  backBtn: { padding: Spacing.xs },
  headerTitle: {
    flex: 1,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    textAlign: 'center',
  },
  headerSpacer: { width: 34 },
  webContainer: { flex: 1 },
  webview: { flex: 1 },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  loadingText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 14,
    paddingHorizontal: Spacing.xl,
  },
  errorText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 22,
  },
  retryBtn: {
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.sm,
    borderRadius: 12,
  },
  retryBtnText: {
    color: Colors.white,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  actionsBar: {
    borderTopWidth: 1,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  actionPill: {
    minHeight: 34,
    minWidth: 38,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 4,
  },
  actionText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  downloadBtn: {
    marginLeft: 'auto',
    minHeight: 34,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.md,
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    gap: 4,
  },
  downloadBtnText: {
    color: Colors.white,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.34)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
  },
  reportCard: {
    width: '100%',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.xl,
    gap: 10,
  },
  reportTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  reportSub: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  reasonBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
  },
  reasonText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },
  cancelReportBtn: { alignItems: 'center', paddingTop: 4 },
  cancelReportText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  paywallCard: {
    width: '100%',
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    alignItems: 'center',
    gap: Spacing.sm,
  },
  paywallWavii: {
    width: 88,
    height: 88,
  },
  paywallTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  paywallSub: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 20,
  },
  paywallBtn: {
    width: '100%',
    minHeight: 42,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  paywallBtnText: {
    color: Colors.white,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  paywallCancel: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },
});
