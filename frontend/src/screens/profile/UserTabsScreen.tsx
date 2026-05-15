import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { PdfDocument, PdfSortOption } from '../../api/pdfApi';
import { apiFetchUserTabs } from '../../api/userApi';

type Nav = NativeStackNavigationProp<AppStackParamList>;
type RouteT = RouteProp<AppStackParamList, 'UserTabs'>;

const DIFFICULTY_LABELS: Record<number, string> = {
  1: 'Principiante',
  2: 'Intermedio',
  3: 'Avanzado',
};

const DIFFICULTY_COLORS: Record<number, string> = {
  1: '#22C55E',
  2: '#F59E0B',
  3: '#EF4444',
};

const SORT_LABELS: Record<PdfSortOption, string> = {
  NEWEST: 'Más recientes',
  OLDEST: 'Más antiguos',
  MOST_LIKED: 'Más populares',
  LEAST_LIKED: 'Menos populares',
};

function timeAgo(iso: string): string {
  const d = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);
  if (d === 0) return 'Hoy';
  if (d === 1) return 'Ayer';
  return `Hace ${d} días`;
}

function TabCard({ item, onOpen }: { item: PdfDocument; onOpen: () => void }) {
  const { colors } = useTheme();
  const diffColor = DIFFICULTY_COLORS[item.difficulty] ?? Colors.primary;

  return (
    <TouchableOpacity
      style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}
      activeOpacity={0.85}
      onPress={onOpen}
    >
      <View style={styles.cardVisual}>
        {item.coverImageUrl ? (
          <Image source={{ uri: item.coverImageUrl }} style={styles.cardImage} resizeMode="cover" />
        ) : (
          <View style={[styles.cardImage, { backgroundColor: Colors.primaryOpacity10, alignItems: 'center', justifyContent: 'center' }]}>
            <Ionicons name="musical-notes" size={30} color={Colors.primary} />
          </View>
        )}
        <View style={styles.cardOverlay}>
          <View style={[styles.diffBadge, { backgroundColor: diffColor }]}>
            <Text style={styles.diffBadgeText}>{DIFFICULTY_LABELS[item.difficulty]}</Text>
          </View>
          <Text style={styles.timeAgo}>{timeAgo(item.uploadedAt)}</Text>
        </View>
      </View>

      <View style={styles.cardBody}>
        <Text style={[styles.songTitle, { color: colors.text }]} numberOfLines={2}>
          {item.songTitle || 'Tablatura sin título'}
        </Text>
        {item.description ? (
          <Text style={[styles.description, { color: colors.textSecondary }]} numberOfLines={2}>
            {item.description}
          </Text>
        ) : null}

        <View style={styles.metaRow}>
          <Ionicons name="heart" size={12} color={colors.textSecondary} />
          <Text style={[styles.metaText, { color: colors.textSecondary }]}>{item.likeCount}</Text>
          {item.pageCount > 0 ? (
            <>
              <Ionicons name="document-outline" size={12} color={colors.textSecondary} />
              <Text style={[styles.metaText, { color: colors.textSecondary }]}>{item.pageCount} págs.</Text>
            </>
          ) : null}
        </View>
      </View>
    </TouchableOpacity>
  );
}

export const UserTabsScreen: React.FC = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation<Nav>();
  const route = useRoute<RouteT>();
  const { userId, userName } = route.params;

  const [allTabs, setAllTabs] = useState<PdfDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState<number | null>(null);
  const [sort, setSort] = useState<PdfSortOption>('MOST_LIKED');
  const [diffModalVisible, setDiffModalVisible] = useState(false);
  const [sortModalVisible, setSortModalVisible] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetchUserTabs(userId, token ?? undefined);
      setAllTabs(data);
    } catch {
      navigation.goBack();
    } finally {
      setLoading(false);
    }
  }, [userId, token]);

  useEffect(() => { load(); }, [load]);

  // Filtrado y ordenación en cliente
  const filtered = useMemo(() => {
    let result = [...allTabs];

    if (search.trim()) {
      const q = search.trim().toLowerCase();
      result = result.filter((t) =>
        (t.songTitle ?? '').toLowerCase().includes(q) ||
        (t.description ?? '').toLowerCase().includes(q)
      );
    }

    if (difficulty !== null) {
      result = result.filter((t) => t.difficulty === difficulty);
    }

    switch (sort) {
      case 'MOST_LIKED':
        result.sort((a, b) => b.likeCount - a.likeCount);
        break;
      case 'LEAST_LIKED':
        result.sort((a, b) => a.likeCount - b.likeCount);
        break;
      case 'NEWEST':
        result.sort((a, b) => new Date(b.uploadedAt).getTime() - new Date(a.uploadedAt).getTime());
        break;
      case 'OLDEST':
        result.sort((a, b) => new Date(a.uploadedAt).getTime() - new Date(b.uploadedAt).getTime());
        break;
    }

    return result;
  }, [allTabs, search, difficulty, sort]);

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Pressable onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.text }]} numberOfLines={1}>
          Tablaturas de {userName}
        </Text>
        <View style={{ width: 30 }} />
      </View>

      {/* Búsqueda */}
      <View style={[styles.searchWrap, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <Ionicons name="search-outline" size={16} color={colors.textSecondary} />
        <TextInput
          style={[styles.searchInput, { color: colors.text }]}
          placeholder="Buscar por título..."
          placeholderTextColor={colors.textSecondary}
          value={search}
          onChangeText={setSearch}
        />
        {search.length > 0 ? (
          <Pressable onPress={() => setSearch('')}>
            <Ionicons name="close-circle" size={16} color={colors.textSecondary} />
          </Pressable>
        ) : null}
      </View>

      {/* Filtros */}
      <View style={styles.filterWrap}>
        <TouchableOpacity
          style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]}
          activeOpacity={0.75}
          onPress={() => setDiffModalVisible(true)}
        >
          <Text style={[styles.dropdownBtnText, { color: difficulty === null ? colors.textSecondary : Colors.primary }]}>
            {difficulty === null ? 'Dificultad' : DIFFICULTY_LABELS[difficulty]}
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]}
          activeOpacity={0.75}
          onPress={() => setSortModalVisible(true)}
        >
          <Text style={[styles.dropdownBtnText, { color: Colors.primary }]}>
            {SORT_LABELS[sort]}
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>
      </View>

      <View style={[styles.divider, { backgroundColor: colors.border }]} />

      {loading ? (
        <View style={styles.center}><ActivityIndicator color={Colors.primary} /></View>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={styles.center}>
              <Ionicons name="document-text-outline" size={48} color={colors.textSecondary} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                {search || difficulty !== null ? 'No hay tablaturas con estos filtros.' : 'No ha publicado tablaturas.'}
              </Text>
            </View>
          }
          renderItem={({ item }) => (
            <TabCard
              item={item}
              onOpen={() => navigation.navigate('PdfViewer', { pdfId: item.id, title: item.songTitle ?? item.originalName })}
            />
          )}
        />
      )}

      {/* Modal dificultad */}
      <Modal visible={diffModalVisible} transparent animationType="fade" onRequestClose={() => setDiffModalVisible(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setDiffModalVisible(false)}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface, borderColor: colors.border }]} onStartShouldSetResponder={() => true}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Filtrar por nivel</Text>
            {['Todos', 'Principiante', 'Intermedio', 'Avanzado'].map((option) => {
              const val = option === 'Todos' ? null : option === 'Principiante' ? 1 : option === 'Intermedio' ? 2 : 3;
              const active = val === difficulty;
              return (
                <TouchableOpacity
                  key={option}
                  style={[styles.modalOption, active && { backgroundColor: Colors.primaryOpacity10 }]}
                  onPress={() => { setDifficulty(val); setDiffModalVisible(false); }}
                >
                  <Text style={[styles.modalOptionText, { color: active ? Colors.primary : colors.text }]}>{option}</Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </TouchableOpacity>
      </Modal>

      {/* Modal ordenación */}
      <Modal visible={sortModalVisible} transparent animationType="fade" onRequestClose={() => setSortModalVisible(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setSortModalVisible(false)}>
          <View style={[styles.modalCard, { backgroundColor: colors.surface, borderColor: colors.border }]} onStartShouldSetResponder={() => true}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Ordenar por</Text>
            {(Object.keys(SORT_LABELS) as PdfSortOption[]).map((option) => (
              <TouchableOpacity
                key={option}
                style={[styles.modalOption, option === sort && { backgroundColor: Colors.primaryOpacity10 }]}
                onPress={() => { setSort(option); setSortModalVisible(false); }}
              >
                <Text style={[styles.modalOptionText, { color: option === sort ? Colors.primary : colors.text }]}>
                  {SORT_LABELS[option]}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60, gap: 12 },

  header: {
    flexDirection: 'row', alignItems: 'center', gap: Spacing.sm,
    paddingHorizontal: Spacing.base, paddingVertical: 12, borderBottomWidth: 1,
  },
  backBtn: { padding: 4 },
  headerTitle: { flex: 1, fontFamily: FontFamily.bold, fontSize: FontSize.base },

  searchWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: Spacing.base,
    marginTop: Spacing.sm,
    marginBottom: 8,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 9,
    borderRadius: 12,
    borderWidth: 1,
  },
  searchInput: { flex: 1, fontFamily: FontFamily.regular, fontSize: FontSize.sm, padding: 0 },

  filterWrap: { paddingHorizontal: Spacing.base, marginBottom: 4, flexDirection: 'row', gap: Spacing.sm },
  dropdownBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: Spacing.md,
    paddingVertical: 10,
  },
  dropdownBtnText: { flex: 1, fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },

  divider: { height: 1, marginHorizontal: Spacing.base, marginVertical: 8 },

  list: { paddingHorizontal: Spacing.base, paddingBottom: Spacing['2xl'] },
  emptyText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center', lineHeight: 22 },

  card: {
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    overflow: 'hidden',
    marginBottom: 12,
  },
  cardVisual: { height: 140, position: 'relative' },
  cardImage: { width: '100%', height: '100%' },
  cardOverlay: {
    position: 'absolute',
    left: 0, right: 0, top: 0, bottom: 0,
    padding: Spacing.sm,
    justifyContent: 'space-between',
    backgroundColor: 'rgba(0,0,0,0.14)',
  },
  diffBadge: { borderRadius: 12, paddingHorizontal: 8, paddingVertical: 3, alignSelf: 'flex-start' },
  diffBadgeText: { fontFamily: FontFamily.bold, fontSize: 10, color: Colors.white },
  timeAgo: { fontFamily: FontFamily.semiBold, fontSize: 10, color: Colors.white, alignSelf: 'flex-end' },

  cardBody: { padding: Spacing.base, gap: 6 },
  songTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.lg, lineHeight: 22 },
  description: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 4, flexWrap: 'wrap' },
  metaText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginRight: 4 },

  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.34)', justifyContent: 'center', alignItems: 'center', padding: Spacing.xl },
  modalCard: {
    width: '85%',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.xl,
  },
  modalTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg, marginBottom: Spacing.md },
  modalOption: { paddingVertical: Spacing.base, paddingHorizontal: Spacing.sm, borderRadius: BorderRadius.sm },
  modalOptionText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.base },
});
