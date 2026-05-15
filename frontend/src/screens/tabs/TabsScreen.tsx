import React, { useCallback, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import {
  PdfDocument,
  PdfSortOption,
  apiDeletePdf,
  apiFetchPublicPdfs,
  apiLikePdf,
  apiReportPdf,
  apiUnlikePdf,
} from '../../api/pdfApi';

type Nav = NativeStackNavigationProp<AppStackParamList>;

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
  NEWEST: 'Mas recientes',
  OLDEST: 'Mas antiguos',
  MOST_LIKED: 'Mas populares',
  LEAST_LIKED: 'Menos populares',
};

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function timeAgo(iso: string): string {
  const d = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);
  if (d === 0) return 'Hoy';
  if (d === 1) return 'Ayer';
  return `Hace ${d} días`;
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

function TabCard({
  item,
  isOwner,
  onLike,
  onDelete,
  onReport,
  onOpen,
}: {
  item: PdfDocument;
  isOwner: boolean;
  onLike: () => void;
  onDelete: () => void;
  onReport: () => void;
  onOpen: () => void;
}) {
  const { colors } = useTheme();
  const diffColor = DIFFICULTY_COLORS[item.difficulty] ?? Colors.primary;

  return (
    <TouchableOpacity style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]} activeOpacity={0.85} onPress={onOpen}>
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
          {item.songTitle || 'Tablatura de la comunidad'}
        </Text>
        <Text style={[styles.description, { color: colors.textSecondary }]} numberOfLines={2}>
          {item.description ?? `Compartida por ${item.ownerName ?? 'la comunidad Wavii'}.`}
        </Text>

        <View style={styles.metaRow}>
          <Ionicons name="person-outline" size={12} color={colors.textSecondary} />
          <Text style={[styles.metaText, { color: colors.textSecondary }]}>{item.ownerName ?? 'Comunidad'}</Text>
          <Ionicons name="document-outline" size={12} color={colors.textSecondary} />
          <Text style={[styles.metaText, { color: colors.textSecondary }]}>{item.pageCount > 0 ? `${item.pageCount} págs.` : formatSize(item.fileSize)}</Text>
        </View>

        <View style={styles.actions}>
          <Pressable style={styles.likeBtn} onPress={onLike}>
            <Ionicons name={item.likedByMe ? 'heart' : 'heart-outline'} size={18} color={item.likedByMe ? Colors.error : colors.textSecondary} />
            <Text style={[styles.likeCount, { color: item.likedByMe ? Colors.error : colors.textSecondary }]}>{item.likeCount}</Text>
          </Pressable>

          <Pressable style={[styles.openBtn, { backgroundColor: Colors.primary }]} onPress={onOpen}>
            <Ionicons name="open-outline" size={14} color={Colors.white} />
            <Text style={styles.openBtnText}>Ver</Text>
          </Pressable>

          {!isOwner ? (
            <Pressable onPress={onReport} style={styles.iconBtn}>
              <Ionicons name="flag-outline" size={16} color={Colors.error} />
            </Pressable>
          ) : null}

          {isOwner ? (
            <Pressable onPress={onDelete} style={styles.iconBtn}>
              <Ionicons name="trash-outline" size={17} color={Colors.error} />
            </Pressable>
          ) : null}
        </View>
      </View>
    </TouchableOpacity>
  );
}

export const TabsScreen = () => {
  const { colors } = useTheme();
  const { token, user } = useAuth();
  const { showAlert } = useAlert();
  const navigation = useNavigation<Nav>();

  const [items, setItems] = useState<PdfDocument[]>([]);
  const [search, setSearch] = useState('');
  const [difficulty, setDifficulty] = useState<number | null>(null);
  const [sort, setSort] = useState<PdfSortOption>('NEWEST');
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(false);
  const [difficultyModalVisible, setDifficultyModalVisible] = useState(false);
  const [sortModalVisible, setSortModalVisible] = useState(false);
  const [reportTarget, setReportTarget] = useState<PdfDocument | null>(null);
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const load = useCallback(async (q: string, diff: number | null, currentSort: PdfSortOption) => {
    setLoading(true);
    setFetchError(false);
    try {
      const data = await apiFetchPublicPdfs(
        { search: q || undefined, difficulty: diff ?? undefined, sort: currentSort },
        token ?? undefined
      );
      setItems(data);
    } catch {
      setFetchError(true);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useFocusEffect(useCallback(() => {
    setSearch('');
    setDifficulty(null);
    setSort('NEWEST');
    load('', null, 'NEWEST');
  }, [load]));

  const handleSearchChange = (text: string) => {
    setSearch(text);
    if (searchTimer.current) clearTimeout(searchTimer.current);
    searchTimer.current = setTimeout(() => load(text, difficulty, sort), 400);
  };

  const handleLike = async (item: PdfDocument) => {
    if (!token) return;
    try {
      const updated = item.likedByMe ? await apiUnlikePdf(item.id, token) : await apiLikePdf(item.id, token);
      setItems((prev) => prev.map((pdf) => (pdf.id === item.id ? updated : pdf)));
    } catch {
      // no-op
    }
  };

  const handleDelete = (item: PdfDocument) => {
    showAlert({
      title: 'Eliminar tablatura',
      message: '¿Seguro que quieres borrar este archivo? Esta acción no se puede deshacer.',
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Eliminar',
          style: 'destructive',
          onPress: async () => {
            if (!token) return;
            try {
              await apiDeletePdf(item.id, token);
              setItems((prev) => prev.filter((pdf) => pdf.id !== item.id));
            } catch {
              showAlert({ title: 'Error', message: 'No se pudo eliminar la tablatura.' });
            }
          },
        },
      ],
    });
  };

  const sendReport = async (item: PdfDocument, reason: string) => {
    if (!token) return;
    try {
      await apiReportPdf(item.id, { reason }, token);
      showAlert({ title: 'Reporte enviado', message: 'Gracias por avisar. Revisaremos esta tablatura.' });
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo enviar el reporte. Inténtalo de nuevo.' });
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <View>
          <Text style={[styles.title, { color: colors.text }]}>Tablaturas</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Explora el catálogo real de la comunidad</Text>
        </View>
        <Pressable style={styles.addBtn} onPress={() => navigation.navigate('UploadTab')}>
          <Ionicons name="add" size={22} color={Colors.white} />
        </Pressable>
      </View>

      <View style={[styles.searchWrap, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <Ionicons name="search-outline" size={16} color={colors.textSecondary} />
        <TextInput
          style={[styles.searchInput, { color: colors.text }]}
          placeholder="Buscar por canción o autor..."
          placeholderTextColor={colors.textSecondary}
          value={search}
          onChangeText={handleSearchChange}
        />
        {search.length > 0 ? (
          <Pressable onPress={() => handleSearchChange('')}>
            <Ionicons name="close-circle" size={16} color={colors.textSecondary} />
          </Pressable>
        ) : null}
      </View>

      <View style={styles.filterWrap}>
        <TouchableOpacity style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]} activeOpacity={0.75} onPress={() => setDifficultyModalVisible(true)}>
          <Text style={[styles.dropdownBtnText, { color: difficulty === null ? colors.textSecondary : Colors.primary }]}>
            {difficulty === null ? 'Dificultad' : DIFFICULTY_LABELS[difficulty]}
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>
        <TouchableOpacity style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]} activeOpacity={0.75} onPress={() => setSortModalVisible(true)}>
          <Text style={[styles.dropdownBtnText, { color: Colors.primary }]}>
            {SORT_LABELS[sort]}
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>
      </View>

      <View style={[styles.divider, { backgroundColor: colors.border }]} />

      {loading ? (
        <View style={styles.center}><ActivityIndicator color={Colors.primary} /></View>
      ) : fetchError ? (
        <View style={styles.center}>
          <Ionicons name="cloud-offline-outline" size={48} color={colors.textSecondary} />
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>No se pudieron cargar las tablaturas.</Text>
          <Pressable onPress={() => load(search, difficulty, sort)} style={[styles.retryBtn, { backgroundColor: Colors.primary }]}>
            <Text style={styles.retryBtnText}>Reintentar</Text>
          </Pressable>
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={styles.center}>
              <Ionicons name="document-text-outline" size={48} color={colors.textSecondary} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>Todavía no hay tablaturas públicas.</Text>
            </View>
          }
          renderItem={({ item }) => (
            <TabCard
              item={item}
              isOwner={String(user?.id) === item.ownerId}
              onLike={() => handleLike(item)}
              onDelete={() => handleDelete(item)}
              onReport={() => setReportTarget(item)}
              onOpen={() => navigation.navigate('PdfViewer', { pdfId: item.id, title: item.songTitle ?? item.originalName })}
            />
          )}
        />
      )}

      <ReportModal
        visible={reportTarget !== null}
        onClose={() => setReportTarget(null)}
        onReason={async (reason) => {
          const target = reportTarget;
          setReportTarget(null);
          if (target) await sendReport(target, reason);
        }}
      />

      <Modal visible={difficultyModalVisible} transparent animationType="fade" onRequestClose={() => setDifficultyModalVisible(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setDifficultyModalVisible(false)}>
          <View style={[styles.dropdownModalCard, { backgroundColor: colors.surface, borderColor: colors.border }]} onStartShouldSetResponder={() => true}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Filtrar por nivel</Text>
            {['Todos', 'Principiante', 'Intermedio', 'Avanzado'].map((option) => (
              <TouchableOpacity
                key={option}
                style={[styles.dropdownOption, difficulty === null && option === 'Todos' ? { backgroundColor: Colors.primaryOpacity10 } : null]}
                onPress={() => {
                  setDifficultyModalVisible(false);
                  if (option === 'Todos') {
                    setDifficulty(null);
                    load(search, null, sort);
                    return;
                  }
                  const nextDifficulty = option === 'Principiante' ? 1 : option === 'Intermedio' ? 2 : 3;
                  setDifficulty(nextDifficulty);
                  load(search, nextDifficulty, sort);
                }}
              >
                <Text style={[styles.dropdownOptionText, { color: colors.text }]}>{option}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </TouchableOpacity>
      </Modal>

      <Modal visible={sortModalVisible} transparent animationType="fade" onRequestClose={() => setSortModalVisible(false)}>
        <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={() => setSortModalVisible(false)}>
          <View style={[styles.dropdownModalCard, { backgroundColor: colors.surface, borderColor: colors.border }]} onStartShouldSetResponder={() => true}>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Ordenar por</Text>
            {(Object.keys(SORT_LABELS) as PdfSortOption[]).map((option) => (
              <TouchableOpacity
                key={option}
                style={[styles.dropdownOption, option === sort ? { backgroundColor: Colors.primaryOpacity10 } : null]}
                onPress={() => {
                  setSortModalVisible(false);
                  setSort(option);
                  load(search, difficulty, option);
                }}
              >
                <Text style={[styles.dropdownOptionText, { color: option === sort ? Colors.primary : colors.text }]}>
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
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.sm,
    paddingBottom: 4,
  },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize['2xl'] },
  subtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginTop: 1 },
  addBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: Colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  searchWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginHorizontal: Spacing.base,
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
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingTop: 60, gap: 12 },
  emptyText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center', lineHeight: 22 },
  retryBtn: { paddingHorizontal: Spacing.xl, paddingVertical: Spacing.sm, borderRadius: BorderRadius.lg },
  retryBtnText: { color: Colors.white, fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  card: {
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    overflow: 'hidden',
    marginBottom: 12,
  },
  cardVisual: { height: 148, position: 'relative' },
  cardImage: { width: '100%', height: '100%' },
  cardOverlay: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    padding: Spacing.sm,
    justifyContent: 'space-between',
    backgroundColor: 'rgba(0,0,0,0.14)',
  },
  cardBody: { padding: Spacing.base, gap: 6 },
  diffBadge: { borderRadius: 12, paddingHorizontal: 8, paddingVertical: 3, alignSelf: 'flex-start' },
  diffBadgeText: { fontFamily: FontFamily.bold, fontSize: 10, color: Colors.white },
  timeAgo: { fontFamily: FontFamily.semiBold, fontSize: 10, color: Colors.white, alignSelf: 'flex-end' },
  songTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.lg, lineHeight: 22 },
  description: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 4, flexWrap: 'wrap' },
  metaText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginRight: 4 },
  actions: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 2 },
  likeBtn: { flexDirection: 'row', alignItems: 'center', gap: 4, padding: 4 },
  likeCount: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },
  openBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    flexGrow: 1,
    justifyContent: 'center',
    paddingVertical: 8,
    borderRadius: BorderRadius.full,
  },
  openBtnText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs, color: Colors.white },
  iconBtn: { padding: 6 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.34)', justifyContent: 'center', alignItems: 'center', padding: Spacing.xl },
  reportCard: { width: '100%', borderRadius: BorderRadius.xl, borderWidth: 1, padding: Spacing.xl, gap: 10 },
  reportTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  reportSub: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  reasonBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderWidth: 1, borderRadius: BorderRadius.lg, padding: Spacing.base },
  reasonText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },
  cancelReportBtn: { alignItems: 'center', paddingTop: 4 },
  cancelReportText: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  dropdownModalCard: {
    width: '85%',
    maxHeight: '70%',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.xl,
  },
  modalTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg, marginBottom: Spacing.md },
  dropdownOption: { paddingVertical: Spacing.base, paddingHorizontal: Spacing.sm, borderRadius: BorderRadius.sm },
  dropdownOptionText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.base },
  teacherRow: { gap: Spacing.sm, paddingRight: Spacing.base, paddingLeft: Spacing.base, paddingBottom: 4 },
});
