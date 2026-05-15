import React, { useCallback, useEffect, useRef, useState } from 'react';
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
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { AppStackParamList } from '../../navigation/AppNavigator';
import {
  ForumCategory,
  ForumSortOption,
  ForumSummary,
  apiGetForums,
  apiGetMyForums,
  apiJoinForum,
  apiLikeForum,
  apiUnlikeForum,
} from '../../api/forumApi';

type Navigation = NativeStackNavigationProp<AppStackParamList>;

const CATEGORY_LABELS: Record<ForumCategory, string> = {
  FANDOM: 'Fandom',
  COMUNIDAD_MUSICAL: 'Comunidad musical',
  TEORIA: 'Teoria musical',
  INSTRUMENTOS: 'Instrumentos',
  BANDAS: 'Bandas',
  ARTISTAS: 'Artistas',
  GENERAL: 'General',
};

const CATEGORY_COLORS: Record<ForumCategory, string> = {
  FANDOM: Colors.accentPurple,
  COMUNIDAD_MUSICAL: Colors.accentBlue,
  TEORIA: Colors.accentTeal,
  INSTRUMENTOS: Colors.primary,
  BANDAS: Colors.accentPink,
  ARTISTAS: Colors.warning,
  GENERAL: Colors.freeTier,
};

const CATEGORY_ICONS: Record<ForumCategory, React.ComponentProps<typeof Ionicons>['name']> = {
  FANDOM: 'star',
  COMUNIDAD_MUSICAL: 'musical-notes',
  TEORIA: 'library',
  INSTRUMENTOS: 'disc',
  BANDAS: 'mic',
  ARTISTAS: 'person-circle',
  GENERAL: 'chatbubbles',
};

const SORT_LABELS: Record<ForumSortOption, string> = {
  NEWEST: 'Mas recientes',
  OLDEST: 'Menos recientes',
  MOST_LIKED: 'Mas populares',
  LEAST_LIKED: 'Menos populares',
};

function formatMembers(count: number): string {
  if (count >= 1000) {
    return `${(count / 1000).toFixed(1)}k`;
  }
  return String(count);
}

interface ForumCardProps {
  forum: ForumSummary;
  onPress: () => void;
  onJoin: () => void;
  onLike: () => void;
  joining: boolean;
}

function ForumCard({ forum, onPress, onJoin, onLike, joining }: ForumCardProps) {
  const { colors } = useTheme();
  const color = CATEGORY_COLORS[forum.category] ?? Colors.primary;
  const icon = CATEGORY_ICONS[forum.category] ?? 'chatbubbles';
  const label = CATEGORY_LABELS[forum.category] ?? forum.category;

  return (
    <Pressable
      style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}
      onPress={onPress}
    >
      {forum.coverImageUrl ? (
        <Image source={{ uri: forum.coverImageUrl }} style={styles.cardImage} resizeMode="cover" />
      ) : (
        <View style={[styles.cardIcon, { backgroundColor: `${color}22` }]}>
          <Ionicons name={icon} size={26} color={color} />
        </View>
      )}

      <View style={styles.cardBody}>
        <Text style={[styles.cardName, { color: colors.text }]} numberOfLines={1}>
          {forum.name}
        </Text>

        {forum.description ? (
          <Text style={[styles.cardDescription, { color: colors.textSecondary }]} numberOfLines={1}>
            {forum.description}
          </Text>
        ) : null}

        <View style={styles.cardMeta}>
          <View style={[styles.categoryBadge, { backgroundColor: `${color}22` }]}>
            <Text style={[styles.categoryBadgeText, { color }]}>{label}</Text>
          </View>

          {forum.city ? (
            <View style={styles.cityBadge}>
              <Ionicons name="location-outline" size={10} color={colors.textSecondary} />
              <Text style={[styles.cityText, { color: colors.textSecondary }]}>{forum.city}</Text>
            </View>
          ) : null}

          <Ionicons name="people-outline" size={12} color={colors.textSecondary} />
          <Text style={[styles.memberCount, { color: colors.textSecondary }]}>
            {formatMembers(forum.memberCount)}
          </Text>
          <Pressable style={styles.likeMeta} onPress={onLike} hitSlop={8}>
            <Ionicons name={forum.likedByMe ? 'heart' : 'heart-outline'} size={13} color={forum.likedByMe ? Colors.error : colors.textSecondary} />
            <Text style={[styles.memberCount, { color: forum.likedByMe ? Colors.error : colors.textSecondary }]}>
              {formatMembers(forum.likeCount)}
            </Text>
          </Pressable>
        </View>
      </View>

      <Pressable
        style={[
          styles.joinButton,
          forum.joined ? styles.joinButtonJoined : { backgroundColor: Colors.primary },
        ]}
        onPress={onJoin}
        disabled={joining || forum.joined}
      >
        {joining ? (
          <ActivityIndicator size={12} color={forum.joined ? Colors.primary : Colors.white} />
        ) : (
          <Text style={[styles.joinButtonText, forum.joined && styles.joinButtonTextJoined]}>
            {forum.joined ? 'Unido' : 'Unirse'}
          </Text>
        )}
      </Pressable>
    </Pressable>
  );
}

function MyForumChip({ forum, onPress }: { forum: ForumSummary; onPress: () => void }) {
  const { colors } = useTheme();
  const color = CATEGORY_COLORS[forum.category] ?? Colors.primary;
  const icon = CATEGORY_ICONS[forum.category] ?? 'chatbubbles';

  return (
    <Pressable
      style={[styles.chip, { backgroundColor: colors.surface, borderColor: `${color}55` }]}
      onPress={onPress}
    >
      <Ionicons name={icon} size={14} color={color} />
      <Text style={[styles.chipText, { color: colors.text }]} numberOfLines={1}>
        {forum.name}
      </Text>
    </Pressable>
  );
}

export const GroupsScreen = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation<Navigation>();

  const [forums, setForums] = useState<ForumSummary[]>([]);
  const [myForums, setMyForums] = useState<ForumSummary[]>([]);
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState<ForumCategory | null>(null);
  const [sort, setSort] = useState<ForumSortOption>('MOST_LIKED');
  const [loading, setLoading] = useState(true);
  const [joiningId, setJoiningId] = useState<string | null>(null);
  const [categoryModalVisible, setCategoryModalVisible] = useState(false);
  const [sortModalVisible, setSortModalVisible] = useState(false);
  const [forumsError, setForumsError] = useState<string | null>(null);
  const searchRef = useRef('');
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const loadRequestIdRef = useRef(0);

  const loadForums = useCallback(
    async (query?: string, options?: { showLoading?: boolean }) => {
      if (!token) {
        setLoading(false);
        setForumsError('Necesitas iniciar sesion para ver comunidades.');
        return;
      }

      const requestId = ++loadRequestIdRef.current;
      const showLoading = options?.showLoading ?? true;
      if (showLoading) {
        setLoading(true);
      }
      setForumsError(null);

      const [allForumsResult, joinedForumsResult] = await Promise.allSettled([
        apiGetForums({ search: query, category: category ?? undefined, sort }, token),
        apiGetMyForums(token),
      ]);

      if (requestId !== loadRequestIdRef.current) {
        return;
      }

      if (allForumsResult.status === 'fulfilled') {
        setForums(allForumsResult.value);
      } else {
        if (showLoading) {
          setForums([]);
        }
        setForumsError('No se pudieron cargar las comunidades. Intentalo de nuevo.');
      }

      if (joinedForumsResult.status === 'fulfilled') {
        setMyForums(joinedForumsResult.value);
      } else {
        setMyForums([]);
      }

      if (showLoading && requestId === loadRequestIdRef.current) {
        setLoading(false);
      }
    },
    [category, sort, token],
  );

  useFocusEffect(
    useCallback(() => {
      void loadForums(searchRef.current || undefined, { showLoading: true });
      const interval = setInterval(() => {
        void loadForums(searchRef.current || undefined, { showLoading: false });
      }, 20000);
      return () => clearInterval(interval);
    }, [loadForums]),
  );

  useEffect(() => () => {
    if (searchTimer.current) {
      clearTimeout(searchTimer.current);
    }
  }, []);

  const handleSearch = (text: string) => {
    setSearch(text);
    searchRef.current = text;
    if (searchTimer.current) {
      clearTimeout(searchTimer.current);
    }
    searchTimer.current = setTimeout(() => {
      loadForums(text, { showLoading: true });
    }, 400);
  };

  const handleJoin = async (forum: ForumSummary) => {
    if (!token || joiningId) {
      return;
    }

    setJoiningId(forum.id);
    try {
      if (forum.joined) return;
      await apiJoinForum(forum.id, token);
      await loadForums(search || undefined);
      navigation.navigate('ForumDetail', { forumId: forum.id });
    } finally {
      setJoiningId(null);
    }
  };

  const handleLike = async (forum: ForumSummary) => {
    if (!token) return;
    const previousForums = forums;
    const previousMyForums = myForums;
    const optimistic = (entry: ForumSummary) =>
      entry.id === forum.id
        ? {
            ...entry,
            likedByMe: !entry.likedByMe,
            likeCount: Math.max(0, entry.likeCount + (entry.likedByMe ? -1 : 1)),
          }
        : entry;
    setForums((current) => current.map(optimistic));
    setMyForums((current) => current.map(optimistic));
    try {
      await (forum.likedByMe ? apiUnlikeForum(forum.id, token) : apiLikeForum(forum.id, token));
    } catch {
      setForums(previousForums);
      setMyForums(previousMyForums);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <View style={styles.headerCenter}>
          <Text style={[styles.headerTitle, { color: colors.text }]}>Comunidades</Text>
        </View>
        <TouchableOpacity
          style={styles.createButton}
          onPress={() => navigation.navigate('CreateForum')}
          activeOpacity={0.8}
        >
          <Ionicons name="add" size={18} color={Colors.white} />
          <Text style={styles.createButtonText}>Crear</Text>
        </TouchableOpacity>
      </View>

      <View
        style={[
          styles.searchWrapper,
          { backgroundColor: colors.surface, borderColor: colors.border },
        ]}
      >
        <Ionicons name="search-outline" size={16} color={colors.textSecondary} />
        <TextInput
          style={[styles.searchInput, { color: colors.text }]}
          placeholder="Buscar comunidades..."
          placeholderTextColor={colors.textSecondary}
          value={search}
          onChangeText={handleSearch}
          returnKeyType="search"
        />
        {search.length > 0 ? (
          <Pressable onPress={() => handleSearch('')}>
            <Ionicons name="close-circle" size={16} color={colors.textSecondary} />
          </Pressable>
        ) : null}
      </View>

      {myForums.length > 0 ? (
        <View style={styles.mySection}>
          <Pressable style={styles.sectionTitleRow} onPress={() => navigation.navigate('MyForums')}>
            <Text style={[styles.sectionLabel, { color: colors.textSecondary, paddingHorizontal: 0, marginBottom: 0 }]}>Mis comunidades</Text>
            <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
          </Pressable>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chipRow}>
            <View style={{ width: Spacing.base }} />
            {myForums.map((forum) => (
              <MyForumChip
                key={forum.id}
                forum={forum}
                onPress={() => navigation.navigate('ForumDetail', { forumId: forum.id })}
              />
            ))}
            <View style={{ width: Spacing.base }} />
          </ScrollView>
        </View>
      ) : null}

      <View style={styles.filterRow}>
        <TouchableOpacity style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]} onPress={() => setCategoryModalVisible(true)}>
          <Text style={[styles.dropdownText, { color: category ? Colors.primary : colors.textSecondary }]} numberOfLines={1}>
            {category ? CATEGORY_LABELS[category] : 'Categoria'}
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>
        <TouchableOpacity style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border }]} onPress={() => setSortModalVisible(true)}>
          <Text style={[styles.dropdownText, { color: Colors.primary }]} numberOfLines={1}>{SORT_LABELS[sort]}</Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : forumsError ? (
        <View style={styles.emptyState}>
          <Ionicons name="alert-circle-outline" size={48} color={Colors.error} />
          <Text style={[styles.emptyTitle, { color: colors.text }]}>Error al cargar</Text>
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>{forumsError}</Text>
          <TouchableOpacity
            style={styles.retryButton}
            onPress={() => {
              void loadForums(searchRef.current || undefined);
            }}
            activeOpacity={0.8}
          >
            <Text style={styles.retryButtonText}>Reintentar</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <FlatList
          data={forums}
          keyExtractor={(forum) => forum.id}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListHeaderComponent={
            forums.length > 0 ? (
              <Text style={[styles.sectionLabel, { color: colors.textSecondary, marginBottom: Spacing.sm, paddingHorizontal: 0 }]}>
                {search ? `Resultados para "${search}"` : 'Comunidades'}
              </Text>
            ) : null
          }
          ListEmptyComponent={
            <View style={styles.emptyState}>
              <Ionicons name="people-outline" size={48} color={colors.textSecondary} />
              <Text style={[styles.emptyTitle, { color: colors.text }]}>
                {search ? 'Sin resultados' : 'Aún no hay comunidades'}
              </Text>
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                {search
                  ? `No encontramos comunidades que coincidan con "${search}".`
                  : 'Sé el primero en crear una comunidad musical.'}
              </Text>
            </View>
          }
          renderItem={({ item }) => (
            <ForumCard
              forum={item}
              onPress={() => navigation.navigate('ForumDetail', { forumId: item.id })}
              onJoin={() => handleJoin(item)}
              onLike={() => handleLike(item)}
              joining={joiningId === item.id}
            />
          )}
        />
      )}

      <SelectionModal
        visible={categoryModalVisible}
        title="Filtrar por categoria"
        options={['Todas', ...Object.values(CATEGORY_LABELS)]}
        selected={category ? CATEGORY_LABELS[category] : 'Todas'}
        onClose={() => setCategoryModalVisible(false)}
        onSelect={(label) => {
          const next = Object.entries(CATEGORY_LABELS).find(([, value]) => value === label)?.[0] as ForumCategory | undefined;
          setCategory(next ?? null);
          setCategoryModalVisible(false);
        }}
      />

      <SelectionModal
        visible={sortModalVisible}
        title="Ordenar por"
        options={Object.values(SORT_LABELS)}
        selected={SORT_LABELS[sort]}
        onClose={() => setSortModalVisible(false)}
        onSelect={(label) => {
          const next = Object.entries(SORT_LABELS).find(([, value]) => value === label)?.[0] as ForumSortOption | undefined;
          setSort(next ?? 'MOST_LIKED');
          setSortModalVisible(false);
        }}
      />
    </SafeAreaView>
  );
};

const SelectionModal = ({
  visible,
  title,
  options,
  selected,
  onClose,
  onSelect,
}: {
  visible: boolean;
  title: string;
  options: string[];
  selected: string;
  onClose: () => void;
  onSelect: (value: string) => void;
}) => {
  const { colors } = useTheme();

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onClose}>
      <TouchableOpacity style={styles.modalOverlay} activeOpacity={1} onPress={onClose}>
        <View style={[styles.modalCard, { backgroundColor: colors.surface, borderColor: colors.border }]} onStartShouldSetResponder={() => true}>
          <Text style={[styles.modalTitle, { color: colors.text }]}>{title}</Text>
          <ScrollView showsVerticalScrollIndicator={false} bounces={false}>
            {options.map((option) => (
              <TouchableOpacity
                key={option}
                style={[styles.modalOption, option === selected && { backgroundColor: Colors.primaryOpacity10 }]}
                onPress={() => onSelect(option)}
              >
                <Text style={[styles.modalOptionText, { color: option === selected ? Colors.primary : colors.text }]}>
                  {option}
                </Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>
      </TouchableOpacity>
    </Modal>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  headerCenter: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  createButton: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 6,
    minWidth: 76,
    justifyContent: 'center',
  },
  createButtonText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    color: Colors.white,
  },
  searchWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    marginHorizontal: Spacing.base,
    marginTop: Spacing.sm,
    marginBottom: Spacing.sm,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 10,
    borderRadius: 12,
    borderWidth: 1,
  },
  searchInput: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    padding: 0,
  },
  mySection: {
    marginBottom: Spacing.sm,
  },
  sectionLabel: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
    paddingHorizontal: Spacing.base,
    marginBottom: 6,
  },
  chipRow: {
    gap: Spacing.sm,
    alignItems: 'center',
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: Spacing.md,
    paddingVertical: 7,
    borderRadius: 20,
    borderWidth: 1,
    maxWidth: 160,
  },
  chipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  list: {
    paddingHorizontal: Spacing.base,
    paddingBottom: Spacing['2xl'],
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyState: {
    alignItems: 'center',
    gap: Spacing.sm,
    paddingTop: Spacing['2xl'],
    paddingHorizontal: Spacing.xl,
  },
  emptyTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    textAlign: 'center',
  },
  emptyText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 20,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    padding: Spacing.sm,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    marginBottom: Spacing.sm,
  },
  cardIcon: {
    width: 48,
    height: 48,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  cardImage: {
    width: 48,
    height: 48,
    borderRadius: BorderRadius.md,
    flexShrink: 0,
  },
  cardBody: {
    flex: 1,
    gap: 3,
  },
  cardName: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  cardDescription: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },
  cardMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    flexWrap: 'wrap',
  },
  categoryBadge: {
    borderRadius: BorderRadius.sm,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  categoryBadgeText: {
    fontFamily: FontFamily.semiBold,
    fontSize: 10,
  },
  cityBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
  cityText: {
    fontFamily: FontFamily.regular,
    fontSize: 10,
  },
  memberCount: {
    fontFamily: FontFamily.regular,
    fontSize: 11,
  },
  likeMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
  },
  joinButton: {
    paddingHorizontal: 14,
    paddingVertical: 7,
    borderRadius: BorderRadius.full,
    minWidth: 70,
    alignItems: 'center',
  },
  joinButtonJoined: {
    backgroundColor: Colors.transparent,
    borderWidth: 1,
    borderColor: Colors.primary,
  },
  joinButtonText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    color: Colors.white,
  },
  joinButtonTextJoined: {
    color: Colors.primary,
  },
  retryButton: {
    marginTop: Spacing.sm,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    backgroundColor: Colors.primary,
    borderRadius: BorderRadius.full,
  },
  retryButtonText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    color: Colors.white,
  },
  sectionTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: 2,
    paddingHorizontal: Spacing.base,
    marginBottom: 6,
  },
  filterRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.base,
    marginBottom: Spacing.sm,
  },
  dropdownBtn: {
    flex: 1,
    minHeight: 40,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: Spacing.xs,
  },
  dropdownText: {
    flex: 1,
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: Colors.overlayDark34,
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
  },
  modalCard: {
    width: '85%',
    maxHeight: '70%',
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.xl,
  },
  modalTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    marginBottom: Spacing.md,
  },
  modalOption: {
    paddingVertical: Spacing.base,
    paddingHorizontal: Spacing.sm,
    borderRadius: BorderRadius.sm,
  },
  modalOptionText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
  },
});
