import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { WebView } from 'react-native-webview';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { AppStackParamList } from '../../navigation/AppNavigator';

type ArticleRouteProp = RouteProp<AppStackParamList, 'Article'>;

export const ArticleScreen = () => {
  const { colors } = useTheme();
  const navigation  = useNavigation<any>();
  const route       = useRoute<ArticleRouteProp>();

  const { url, title, sourceName } = route.params;

  const [loading,  setLoading]  = useState(true);
  const [hasError, setHasError] = useState(false);
  const [canGoBack, setCanGoBack] = useState(false);
  const webViewRef = React.useRef<WebView>(null);

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>

      {/* ── Barra superior ── */}
      <View style={[styles.topBar, { borderBottomColor: colors.border, backgroundColor: colors.surface }]}>
        <TouchableOpacity
          onPress={() => {
            if (canGoBack) webViewRef.current?.goBack();
            else navigation.goBack();
          }}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
          style={styles.topBarBtn}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>

        <View style={styles.topBarCenter}>
          <Text style={[styles.topBarTitle, { color: colors.text }]} numberOfLines={1}>
            {title}
          </Text>
          {sourceName ? (
            <Text style={[styles.topBarSource, { color: colors.textSecondary }]} numberOfLines={1}>
              {sourceName}
            </Text>
          ) : null}
        </View>

        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
          style={styles.topBarBtn}
        >
          <Ionicons name="close" size={22} color={colors.textSecondary} />
        </TouchableOpacity>
      </View>

      {/* ── WebView ── */}
      {hasError ? (
        <View style={styles.errorState}>
          <Ionicons name="cloud-offline-outline" size={64} color={colors.border} />
          <Text style={[styles.errorTitle, { color: colors.text }]}>
            No se pudo cargar el articulo
          </Text>
          <Text style={[styles.errorSub, { color: colors.textSecondary }]}>
            Comprueba tu conexion e intentalo de nuevo
          </Text>
          <TouchableOpacity
            style={[styles.retryBtn, { backgroundColor: Colors.primary }]}
            onPress={() => { setHasError(false); webViewRef.current?.reload(); }}
          >
            <Text style={styles.retryBtnText}>Reintentar</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <>
          {loading && (
            <View style={styles.loadingBar}>
              <ActivityIndicator color={Colors.primary} size="small" />
              <Text style={[styles.loadingText, { color: colors.textSecondary }]}>
                Cargando articulo...
              </Text>
            </View>
          )}
          <WebView
            ref={webViewRef}
            source={{ uri: url }}
            style={[styles.webview, { backgroundColor: colors.background }]}
            onLoadStart={() => setLoading(true)}
            onLoadEnd={() => setLoading(false)}
            onError={() => { setLoading(false); setHasError(true); }}
            onNavigationStateChange={(state) => setCanGoBack(state.canGoBack)}
            allowsBackForwardNavigationGestures
            startInLoadingState={false}
            javaScriptEnabled
            domStorageEnabled
          />
        </>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe:    { flex: 1 },
  webview: { flex: 1 },

  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.xs,
    paddingVertical: Spacing.xs,
    borderBottomWidth: 1,
    gap: Spacing.xs,
  },
  topBarBtn: {
    padding: Spacing.xs,
    width: 38,
    alignItems: 'center',
  },
  topBarCenter: {
    flex: 1,
    alignItems: 'center',
  },
  topBarTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  topBarSource: {
    fontFamily: FontFamily.regular,
    fontSize: 11,
  },

  loadingBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.xs,
    paddingVertical: Spacing.sm,
  },
  loadingText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },

  errorState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.sm,
    padding: Spacing.xl,
  },
  errorTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.lg,
    textAlign: 'center',
  },
  errorSub: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
  },
  retryBtn: {
    marginTop: Spacing.sm,
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.full,
  },
  retryBtnText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
    color: '#fff',
  },
});
