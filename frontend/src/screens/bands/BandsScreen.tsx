import React, { useState, useCallback, useRef, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  FlatList,
  Pressable,
  StyleSheet,
  ActivityIndicator,
  Image,
  ScrollView,
  Modal,
  TouchableOpacity,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import * as Location from 'expo-location';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import {
  apiGetBandListings,
  BandListing,
  MusicalGenre,
  MusicianRole,
} from '../../api/bandApi';
import { isBackendSessionToken } from '../../auth/session';
import { GuestBlockedView } from '../../components/common/GuestBlockedView';

type Nav = NativeStackNavigationProp<AppStackParamList>;

const GENRE_LABELS: Record<MusicalGenre, string> = {
  ROCK: 'Rock',
  METAL: 'Metal',
  POP: 'Pop',
  JAZZ: 'Jazz',
  BLUES: 'Blues',
  CLASICA: 'Clasica',
  ELECTRONICA: 'Electronica',
  REGGAETON: 'Reggaeton',
  SALSA: 'Salsa',
  CUMBIA: 'Cumbia',
  BACHATA: 'Bachata',
  HIP_HOP: 'Hip-Hop',
  REGGAE: 'Reggae',
  FOLK: 'Folk',
  INDIE: 'Indie',
  PUNK: 'Punk',
  FUNK: 'Funk',
  R_AND_B: 'R&B',
  LATIN: 'Latin',
  OTRO: 'Otro',
};

const ROLE_LABELS: Record<MusicianRole, string> = {
  VOCALISTA: 'Vocalista',
  GUITARRISTA: 'Guitarrista',
  BAJISTA: 'Bajista',
  BATERISTA: 'Baterista',
  PERCUSIONISTA: 'Percusionista',
  PIANISTA: 'Pianista',
  TECLADISTA: 'Tecladista',
  PRODUCTOR: 'Productor',
  DJ: 'DJ',
  VIOLINISTA: 'Violinista',
  TROMPETISTA: 'Trompetista',
  SAXOFONISTA: 'Saxofonista',
  OTRO: 'Otro',
};

const ROLE_ICONS: Record<MusicianRole, React.ComponentProps<typeof Ionicons>['name']> = {
  VOCALISTA: 'mic',
  GUITARRISTA: 'musical-note',
  BAJISTA: 'musical-note',
  BATERISTA: 'radio',
  PERCUSIONISTA: 'radio',
  PIANISTA: 'musical-notes',
  TECLADISTA: 'musical-notes',
  PRODUCTOR: 'headset',
  DJ: 'disc',
  VIOLINISTA: 'musical-note',
  TROMPETISTA: 'musical-note',
  SAXOFONISTA: 'musical-note',
  OTRO: 'ellipsis-horizontal',
};

const TYPE_COLOR: Record<string, string> = {
  BANDA_BUSCA_MUSICOS: '#8B5CF6',
  MUSICO_BUSCA_BANDA: '#3B82F6',
};

const TYPE_LABEL: Record<string, string> = {
  BANDA_BUSCA_MUSICOS: 'Banda busca musicos',
  MUSICO_BUSCA_BANDA: 'Musico busca banda',
};

const GENRES = Object.entries(GENRE_LABELS) as [MusicalGenre, string][];
const ROLES = Object.entries(ROLE_LABELS) as [MusicianRole, string][];

function timeAgo(iso: string): string {
  const days = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);
  if (days === 0) return 'Hoy';
  if (days === 1) return 'Ayer';
  return `Hace ${days} dias`;
}

function BandCard({ item, onPress }: { item: BandListing; onPress: () => void }) {
  const { colors } = useTheme();
  const typeColor = TYPE_COLOR[item.type] ?? Colors.primary;

  return (
    <Pressable
      style={[styles.card, { backgroundColor: colors.surface }]}
      onPress={onPress}
    >
      <View style={[styles.cardTopStrip, { backgroundColor: typeColor }]} />
      {item.coverImageUrl ? (
        <Image source={{ uri: item.coverImageUrl }} style={styles.cardCover} resizeMode="cover" />
      ) : null}
      <View style={styles.cardHeader}>
        <View style={[styles.typeBadge, { backgroundColor: typeColor + '22' }]}>
          <Text style={[styles.typeBadgeText, { color: typeColor }]}>{TYPE_LABEL[item.type]}</Text>
        </View>
        <Text style={[styles.timeAgo, { color: colors.textSecondary }]}>{timeAgo(item.createdAt)}</Text>
      </View>

      <Text style={[styles.cardTitle, { color: colors.text }]} numberOfLines={2}>
        {item.title}
      </Text>

      <View style={styles.cardMeta}>
        <Ionicons name="musical-notes-outline" size={13} color={colors.textSecondary} />
        <Text style={[styles.metaText, { color: colors.textSecondary }]}>{GENRE_LABELS[item.genre]}</Text>
        <Ionicons name="location-outline" size={13} color={colors.textSecondary} />
        <Text style={[styles.metaText, { color: colors.textSecondary }]} numberOfLines={1}>
          {item.city}
        </Text>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.roleRow}>
        {item.roles.slice(0, 5).map((role) => (
          <View
            key={role}
            style={[styles.roleChip, { backgroundColor: colors.background, borderColor: typeColor + '55' }]}
          >
            <Ionicons name={ROLE_ICONS[role]} size={11} color={Colors.primary} />
            <Text style={[styles.roleChipText, { color: colors.text }]}>{ROLE_LABELS[role]}</Text>
          </View>
        ))}
        {item.roles.length > 5 ? (
          <View style={[styles.roleChip, { backgroundColor: colors.background, borderColor: typeColor + '55' }]}>
            <Text style={[styles.roleChipText, { color: colors.textSecondary }]}>+{item.roles.length - 5}</Text>
          </View>
        ) : null}
      </ScrollView>

      <Text style={[styles.cardCreator, { color: colors.textSecondary }]}>por {item.creatorName}</Text>
    </Pressable>
  );
}

export const BandsScreen = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation<Nav>();
  const hasAccount = isBackendSessionToken(token);

  const [listings, setListings] = useState<BandListing[]>([]);
  const [city, setCity] = useState('');
  const [genre, setGenre] = useState<MusicalGenre | null>(null);
  const [role, setRole] = useState<MusicianRole | null>(null);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [detectedCity, setDetectedCity] = useState<string | null>(null);
  const [locLoading, setLocLoading] = useState(true);
  const [genreModalVisible, setGenreModalVisible] = useState(false);
  const [roleModalVisible, setRoleModalVisible] = useState(false);
  const cityTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const cityRef = useRef(city);
  const genreRef = useRef(genre);
  const roleRef = useRef(role);
  cityRef.current = city;
  genreRef.current = genre;
  roleRef.current = role;

  const load = useCallback(
    async (
      nextPage: number,
      reset: boolean,
      filters: { city?: string; genre?: MusicalGenre | null; role?: MusicianRole | null },
    ) => {
      if (nextPage === 0) setLoading(true);
      else setLoadingMore(true);

      try {
        const data = await apiGetBandListings(
          {
            city: filters.city || undefined,
            genre: filters.genre ?? undefined,
            role: filters.role ?? undefined,
            page: nextPage,
          },
        );
        const items = data.content ?? [];
        setListings((prev) => (reset ? items : [...prev, ...items]));
        setHasMore(!data.last);
        setPage(nextPage);
      } catch {
        if (reset) setListings([]);
        setHasMore(false);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [],
  );

  const applyFilters = useCallback(
    (nextCity: string, nextGenre: MusicalGenre | null, nextRole: MusicianRole | null) => {
      load(0, true, { city: nextCity, genre: nextGenre, role: nextRole });
    },
    [load],
  );

  useFocusEffect(
    useCallback(() => {
      load(0, true, { city: cityRef.current, genre: genreRef.current, role: roleRef.current });
    }, [load]),
  );

  const detectCity = useCallback(async () => {
    setLocLoading(true);
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;
      const position = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      const [geo] = await Location.reverseGeocodeAsync({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      });
      const found = geo?.city ?? geo?.subregion ?? geo?.region ?? null;
      if (found) {
        setDetectedCity(found);
        setCity(found);
        applyFilters(found, genreRef.current, roleRef.current);
      }
    } catch {
      // ignore
    } finally {
      setLocLoading(false);
    }
  }, [applyFilters]);

  useEffect(() => {
    detectCity();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleCityChange = (text: string) => {
    setCity(text);
    if (cityTimer.current) clearTimeout(cityTimer.current);
    cityTimer.current = setTimeout(() => applyFilters(text, genre, role), 450);
  };

  const toggleGenre = (value: MusicalGenre) => {
    const next = genre === value ? null : value;
    setGenre(next);
    applyFilters(city, next, role);
  };

  const toggleRole = (value: MusicianRole) => {
    const next = role === value ? null : value;
    setRole(next);
    applyFilters(city, genre, next);
  };

  const handleEndReached = () => {
    if (!loadingMore && hasMore) load(page + 1, false, { city, genre, role });
  };

  if (!hasAccount) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
        <View style={styles.header}>
          <View>
            <Text style={[styles.title, { color: colors.text }]}>Bandas</Text>
            <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Encuentra o forma tu banda local</Text>
          </View>
        </View>
        <GuestBlockedView feature="para buscar y publicar anuncios de bandas" />
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <View>
          <Text style={[styles.title, { color: colors.text }]}>Bandas</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Encuentra o forma tu banda local</Text>
        </View>
        <Pressable style={styles.addBtn} onPress={() => navigation.navigate('CreateBandListing')}>
          <Ionicons name="add" size={22} color={Colors.textLight} />
        </Pressable>
      </View>

      <View
        style={[
          styles.searchWrap,
          {
            backgroundColor: colors.surface,
            borderColor: detectedCity && city === detectedCity ? Colors.primary : colors.border,
          },
        ]}
      >
        <Ionicons
          name={detectedCity && city === detectedCity ? 'location' : 'location-outline'}
          size={16}
          color={detectedCity && city === detectedCity ? Colors.primary : colors.textSecondary}
        />
        <TextInput
          style={[styles.searchInput, { color: colors.text }]}
          placeholder={locLoading ? 'Detectando ubicacion...' : 'Filtrar por ciudad...'}
          placeholderTextColor={colors.textSecondary}
          value={city}
          onChangeText={handleCityChange}
        />
        {city.length > 0 ? (
          <Pressable onPress={() => handleCityChange('')}>
            <Ionicons name="close-circle" size={16} color={colors.textSecondary} />
          </Pressable>
        ) : locLoading ? (
          <ActivityIndicator size={14} color={Colors.primary} />
        ) : (
          <Pressable onPress={detectCity} hitSlop={8}>
            <Ionicons name="locate-outline" size={16} color={Colors.primary} />
          </Pressable>
        )}
      </View>

      <View style={styles.filtersRow}>
        <TouchableOpacity
          style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border, flex: 1 }]}
          activeOpacity={0.75}
          onPress={() => setGenreModalVisible(true)}
        >
          <Text
            style={[
              styles.dropdownBtnText,
              { color: genre ? Colors.primary : colors.textSecondary },
            ]}
            numberOfLines={1}
          >
            {genre ? GENRE_LABELS[genre] : 'Genero'}
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.dropdownBtn, { backgroundColor: colors.surface, borderColor: colors.border, flex: 1 }]}
          activeOpacity={0.75}
          onPress={() => setRoleModalVisible(true)}
        >
          <Text
            style={[
              styles.dropdownBtnText,
              { color: role ? Colors.primary : colors.textSecondary },
            ]}
            numberOfLines={1}
          >
            {role ? ROLE_LABELS[role] : 'Musico'}
          </Text>
          <Ionicons name="chevron-down" size={16} color={colors.textSecondary} />
        </TouchableOpacity>
      </View>

      <View style={[styles.divider, { backgroundColor: colors.border }]} />

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={listings}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          onEndReached={handleEndReached}
          onEndReachedThreshold={0.3}
          ListEmptyComponent={
            <View style={styles.center}>
              <Ionicons name="musical-notes-outline" size={48} color={colors.textSecondary} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                No hay anuncios con esos filtros.{'\n'}Se el primero en publicar.
              </Text>
            </View>
          }
          ListFooterComponent={loadingMore ? <ActivityIndicator color={Colors.primary} style={styles.footerLoader} /> : null}
          renderItem={({ item }) => (
            <BandCard
              item={item}
              onPress={() => navigation.navigate('BandDetail', { listingId: item.id })}
            />
          )}
        />
      )}

      <SelectionModal
        visible={genreModalVisible}
        title="Filtrar por genero"
        options={['Todos', ...GENRES.map(([, label]) => label)]}
        selected={genre ? GENRE_LABELS[genre] : 'Todos'}
        onClose={() => setGenreModalVisible(false)}
        onSelect={(value) => {
          setGenreModalVisible(false);
          if (value === 'Todos') {
            setGenre(null);
            applyFilters(city, null, role);
            return;
          }

          const selectedGenre = GENRES.find(([, label]) => label === value)?.[0] ?? null;
          setGenre(selectedGenre);
          applyFilters(city, selectedGenre, role);
        }}
      />

      <SelectionModal
        visible={roleModalVisible}
        title="Filtrar por musico"
        options={['Todos', ...ROLES.map(([, label]) => label)]}
        selected={role ? ROLE_LABELS[role] : 'Todos'}
        onClose={() => setRoleModalVisible(false)}
        onSelect={(value) => {
          setRoleModalVisible(false);
          if (value === 'Todos') {
            setRole(null);
            applyFilters(city, genre, null);
            return;
          }

          const selectedRole = ROLES.find(([, label]) => label === value)?.[0] ?? null;
          setRole(selectedRole);
          applyFilters(city, genre, selectedRole);
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
      <TouchableOpacity style={styles.modalOverlayCenter} activeOpacity={1} onPress={onClose}>
        <View style={[styles.dropdownModalCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <Text style={[styles.modalTitle, { color: colors.text }]}>{title}</Text>
          <ScrollView showsVerticalScrollIndicator={false} bounces={false}>
            {options.map((option) => (
              <TouchableOpacity
                key={option}
                style={[styles.dropdownOption, selected === option && { backgroundColor: Colors.primaryOpacity10 }]}
                onPress={() => onSelect(option)}
              >
                <Text style={[styles.dropdownOptionText, { color: selected === option ? Colors.primary : colors.text }]}>
                  {option}
                </Text>
                {selected === option ? <Ionicons name="checkmark" size={20} color={Colors.primary} /> : null}
              </TouchableOpacity>
            ))}
          </ScrollView>
        </View>
      </TouchableOpacity>
    </Modal>
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
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginTop: 1,
  },
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
    marginBottom: 6,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 9,
    borderRadius: 12,
    borderWidth: 1,
  },
  searchInput: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    padding: 0,
  },
  filtersRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.base,
    marginBottom: 4,
  },
  dropdownBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: Spacing.md,
    paddingVertical: 10,
  },
  dropdownBtnText: {
    flex: 1,
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },
  divider: {
    height: 1,
    marginHorizontal: Spacing.base,
    marginVertical: 8,
  },
  list: {
    paddingHorizontal: Spacing.base,
    paddingBottom: Spacing['2xl'],
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 60,
    gap: 12,
  },
  emptyText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 22,
  },
  footerLoader: { margin: 12 },
  card: {
    borderRadius: BorderRadius.xl,
    overflow: 'hidden',
    padding: Spacing.base,
    marginBottom: 10,
    gap: 6,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.06,
    shadowRadius: 6,
  },
  cardTopStrip: {
    height: 6,
    marginHorizontal: -Spacing.base,
    marginTop: -Spacing.base,
    marginBottom: Spacing.sm,
  },
  cardCover: {
    height: 132,
    marginHorizontal: -Spacing.base,
    marginTop: -Spacing.sm,
    marginBottom: Spacing.sm,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  typeBadge: {
    borderRadius: 6,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  typeBadgeText: {
    fontFamily: FontFamily.bold,
    fontSize: 10,
  },
  timeAgo: {
    fontFamily: FontFamily.regular,
    fontSize: 10,
  },
  cardTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    lineHeight: 20,
  },
  cardMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    flexWrap: 'wrap',
  },
  metaText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },
  roleRow: { marginVertical: 2 },
  roleChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginRight: 6,
  },
  roleChipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: 11,
  },
  cardCreator: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },
  modalOverlayCenter: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.3)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  dropdownModalCard: {
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
  dropdownOption: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: Spacing.base,
    paddingHorizontal: Spacing.sm,
    borderRadius: BorderRadius.sm,
  },
  dropdownOptionText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.base,
  },
});
