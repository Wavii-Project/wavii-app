import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Image,
  ActivityIndicator,
  RefreshControl,
  TextInput,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { apiFetchNews, formatNewsDate, NewsArticle } from '../../api/newsApi';

export const NewsScreen = () => {
  const { colors, isDark } = useTheme();
  const navigation = useNavigation<any>();

  const [articles,   setArticles]   = useState<NewsArticle[]>([]);
  const [loading,    setLoading]    = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error,      setError]      = useState(false);
  const [query,      setQuery]      = useState('');
  const [activeQuery, setActiveQuery] = useState('music');

  // Debounce: espera 600ms tras dejar de escribir antes de buscar
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const load = useCallback(async (q: string, isRefresh = false) => {
    if (isRefresh) setRefreshing(true);
    else setLoading(true);
    setError(false);
    try {
      const data = await apiFetchNews({ q: q || 'music', size: 10 });
      setArticles(data);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  // Carga inicial
  useEffect(() => { load('music'); }, [load]);

  // Debounce al escribir en el buscador
  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      const q = query.trim() || 'music';
      if (q !== activeQuery) {
        setActiveQuery(q);
        load(q);
      }
    }, 600);
    return () => { if (debounceRef.current) clearTimeout(debounceRef.current); };
  }, [query]);

  const openArticle = (article: NewsArticle) => {
    navigation.navigate('Article', {
      url: article.url,
      title: article.title,
      sourceName: article.sourceName ?? undefined,
    });
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>

      {/* ── Barra superior ── */}
      <View style={[styles.topBar, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.topBarTitle, { color: colors.text }]}>Actualidad musical</Text>
        <View style={{ width: 26 }} />
      </View>

      {/* ── Barra de búsqueda ── */}
      <View style={[styles.searchRow, { backgroundColor: colors.background }]}>
        <View style={[styles.searchBox, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <Ionicons name="search" size={18} color={colors.textSecondary} />
          <TextInput
            style={[styles.searchInput, { color: colors.text }]}
            placeholder="Buscar noticias, artista, instrumento..."
            placeholderTextColor={colors.textSecondary}
            value={query}
            onChangeText={setQuery}
            returnKeyType="search"
            onSubmitEditing={() => {
              if (debounceRef.current) clearTimeout(debounceRef.current);
              const q = query.trim() || 'music';
              setActiveQuery(q);
              load(q);
            }}
            clearButtonMode="while-editing"
          />
          {query.length > 0 && (
            <TouchableOpacity onPress={() => setQuery('')} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
              <Ionicons name="close-circle" size={18} color={colors.textSecondary} />
            </TouchableOpacity>
          )}
        </View>
      </View>

      {/* ── Contenido ── */}
      {loading ? (
        <ActivityIndicator color={Colors.primary} size="large" style={styles.centered} />
      ) : error ? (
        <View style={styles.emptyState}>
          <Ionicons name="cloud-offline-outline" size={64} color={colors.border} />
          <Text style={[styles.emptyTitle, { color: colors.text }]}>Sin conexion</Text>
          <Text style={[styles.emptySub, { color: colors.textSecondary }]}>
            Comprueba tu conexion e intentalo de nuevo
          </Text>
          <TouchableOpacity
            style={[styles.retryBtn, { backgroundColor: Colors.primary }]}
            onPress={() => load(activeQuery)}
          >
            <Text style={styles.retryBtnText}>Reintentar</Text>
          </TouchableOpacity>
        </View>
      ) : articles.length === 0 ? (
        <View style={styles.emptyState}>
          <Ionicons name="newspaper-outline" size={64} color={colors.border} />
          <Text style={[styles.emptyTitle, { color: colors.text }]}>Sin resultados</Text>
          <Text style={[styles.emptySub, { color: colors.textSecondary }]}>
            Prueba con otro termino de busqueda
          </Text>
        </View>
      ) : (
        <FlatList
          data={articles}
          keyExtractor={(item) => item.articleId}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => load(activeQuery, true)}
              tintColor={Colors.primary}
            />
          }
          renderItem={({ item }) => (
            <ArticleCard
              article={item}
              colors={colors}
              isDark={isDark}
              onPress={() => openArticle(item)}
            />
          )}
        />
      )}
    </SafeAreaView>
  );
};

// ── Tarjeta de articulo ──

const ArticleCard = ({
  article,
  colors,
  isDark,
  onPress,
}: {
  article: NewsArticle;
  colors: any;
  isDark: boolean;
  onPress: () => void;
}) => (
  <TouchableOpacity
    style={[styles.card, { backgroundColor: colors.surface }]}
    activeOpacity={0.8}
    onPress={onPress}
  >
    {article.imageUrl ? (
      <Image
        source={{ uri: article.imageUrl }}
        style={styles.cardImage}
        resizeMode="cover"
      />
    ) : (
      <View style={[styles.cardImage, styles.cardImagePlaceholder,
        { backgroundColor: isDark ? '#2a2a2a' : Colors.grayLight }]}>
        <Ionicons name="musical-notes" size={32} color={Colors.primary} />
      </View>
    )}

    <View style={styles.cardBody}>
      <Text style={[styles.cardTitle, { color: colors.text }]} numberOfLines={3}>
        {article.title}
      </Text>
      {article.description ? (
        <Text style={[styles.cardDesc, { color: colors.textSecondary }]} numberOfLines={2}>
          {article.description}
        </Text>
      ) : null}
      <View style={styles.cardMeta}>
        {article.sourceName ? (
          <View style={styles.sourceRow}>
            <Ionicons name="globe-outline" size={11} color={colors.textSecondary} />
            <Text style={[styles.sourceName, { color: colors.textSecondary }]} numberOfLines={1}>
              {article.sourceName}
            </Text>
          </View>
        ) : null}
        {article.publishedAt ? (
          <Text style={[styles.date, { color: colors.textSecondary }]}>
            {formatNewsDate(article.publishedAt)}
          </Text>
        ) : null}
      </View>
    </View>

    <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} style={{ alignSelf: 'center' }} />
  </TouchableOpacity>
);

// ── Estilos ──

const styles = StyleSheet.create({
  safe:    { flex: 1 },
  centered: { flex: 1 },

  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  topBarTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },

  // Buscador
  searchRow: {
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
  },
  searchBox: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderRadius: BorderRadius.xl,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 10,
    gap: Spacing.xs,
  },
  searchInput: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    padding: 0,
  },

  // Lista
  list: { paddingHorizontal: Spacing.base, paddingBottom: Spacing.xl, gap: Spacing.sm },

  // Tarjeta
  card: {
    flexDirection: 'row',
    borderRadius: BorderRadius.xl,
    padding: Spacing.sm,
    alignItems: 'flex-start',
    gap: Spacing.sm,
  },
  cardImage: {
    width: 88,
    height: 88,
    borderRadius: BorderRadius.lg,
    flexShrink: 0,
  },
  cardImagePlaceholder: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardBody:  { flex: 1, gap: 4 },
  cardTitle: { fontFamily: FontFamily.bold,    fontSize: FontSize.sm, lineHeight: 20 },
  cardDesc:  { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 17 },
  cardMeta:  { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 2 },
  sourceRow: { flexDirection: 'row', alignItems: 'center', gap: 3, flex: 1 },
  sourceName: { fontFamily: FontFamily.medium, fontSize: 10, flex: 1 },
  date:       { fontFamily: FontFamily.regular, fontSize: 10, flexShrink: 0 },

  // Estados vacíos
  emptyState: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.sm,
    padding: Spacing.xl,
  },
  emptyTitle: { fontFamily: FontFamily.bold,    fontSize: FontSize.lg, textAlign: 'center' },
  emptySub:   { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center' },
  retryBtn: {
    marginTop: Spacing.sm,
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.full,
  },
  retryBtnText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, color: '#fff' },
});
