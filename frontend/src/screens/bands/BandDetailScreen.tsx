import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, ScrollView, Pressable,
  StyleSheet, ActivityIndicator, Alert, Clipboard, Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { AppStackParamList } from '../../navigation/AppNavigator';
import {
  BandListing, MusicalGenre, MusicianRole,
  apiGetBandListing, apiDeleteBandListing,
} from '../../api/bandApi';

type Nav = NativeStackNavigationProp<AppStackParamList>;
type RouteT = RouteProp<AppStackParamList, 'BandDetail'>;

const GENRE_LABELS: Record<MusicalGenre, string> = {
  ROCK: 'Rock', METAL: 'Metal', POP: 'Pop', JAZZ: 'Jazz', BLUES: 'Blues',
  CLASICA: 'Clásica', ELECTRONICA: 'Electrónica', REGGAETON: 'Reggaetón',
  SALSA: 'Salsa', CUMBIA: 'Cumbia', BACHATA: 'Bachata', HIP_HOP: 'Hip-Hop',
  REGGAE: 'Reggae', FOLK: 'Folk', INDIE: 'Indie', PUNK: 'Punk', FUNK: 'Funk',
  R_AND_B: 'R&B', LATIN: 'Latin', OTRO: 'Otro',
};

const ROLE_LABELS: Record<MusicianRole, string> = {
  VOCALISTA: 'Vocalista', GUITARRISTA: 'Guitarrista', BAJISTA: 'Bajista',
  BATERISTA: 'Baterista', PERCUSIONISTA: 'Percusionista', PIANISTA: 'Pianista',
  TECLADISTA: 'Tecladista', PRODUCTOR: 'Productor', DJ: 'DJ',
  VIOLINISTA: 'Violinista', TROMPETISTA: 'Trompetista',
  SAXOFONISTA: 'Saxofonista', OTRO: 'Otro',
};

const ROLE_ICONS: Record<MusicianRole, React.ComponentProps<typeof Ionicons>['name']> = {
  VOCALISTA: 'mic', GUITARRISTA: 'musical-note', BAJISTA: 'musical-note',
  BATERISTA: 'radio', PERCUSIONISTA: 'radio', PIANISTA: 'musical-notes',
  TECLADISTA: 'musical-notes', PRODUCTOR: 'headset', DJ: 'disc',
  VIOLINISTA: 'musical-note', TROMPETISTA: 'musical-note',
  SAXOFONISTA: 'musical-note', OTRO: 'ellipsis-horizontal',
};

const TYPE_COLOR: Record<string, string> = {
  BANDA_BUSCA_MUSICOS: '#8B5CF6',
  MUSICO_BUSCA_BANDA:  '#3B82F6',
};
const TYPE_LABEL: Record<string, string> = {
  BANDA_BUSCA_MUSICOS: 'Banda busca músicos',
  MUSICO_BUSCA_BANDA:  'Músico busca banda',
};

function InfoRow({ icon, text, onPress }: { icon: React.ComponentProps<typeof Ionicons>['name']; text: string; onPress?: () => void }) {
  const { colors } = useTheme();
  if (onPress) {
    return (
      <Pressable style={styles.infoRow} onPress={onPress}>
        <Ionicons name={icon} size={16} color={Colors.primary} />
        <Text style={[styles.infoText, { color: Colors.primary }]}>{text}</Text>
        <Ionicons name="chevron-forward" size={14} color={Colors.primary} />
      </Pressable>
    );
  }
  return (
    <View style={styles.infoRow}>
      <Ionicons name={icon} size={16} color={colors.textSecondary} />
      <Text style={[styles.infoText, { color: colors.textSecondary }]}>{text}</Text>
    </View>
  );
}

export const BandDetailScreen = () => {
  const { colors } = useTheme();
  const { token, user } = useAuth();
  const navigation = useNavigation<Nav>();
  const route = useRoute<RouteT>();
  const { listingId } = route.params;

  const [listing, setListing] = useState<BandListing | null>(null);
  const [loading, setLoading] = useState(true);
  const [deleting, setDeleting] = useState(false);
  const [copyToastVisible, setCopyToastVisible] = useState(false);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      setListing(await apiGetBandListing(listingId, token));
    } catch {
      navigation.goBack();
    } finally {
      setLoading(false);
    }
  }, [listingId, token]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = () => {
    Alert.alert(
      'Eliminar anuncio',
      '¿Estás seguro de que quieres eliminar este anuncio?',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Eliminar',
          style: 'destructive',
          onPress: async () => {
            if (!token) return;
            setDeleting(true);
            try {
              await apiDeleteBandListing(listingId, token);
              navigation.goBack();
            } catch {
              setDeleting(false);
            }
          },
        },
      ],
    );
  };

  const handleCopyContact = () => {
    if (listing?.contactInfo) {
      Clipboard.setString(listing.contactInfo);
      setCopyToastVisible(true);
      setTimeout(() => setCopyToastVisible(false), 1800);
    }
  };

  if (loading) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
        <View style={styles.center}><ActivityIndicator color={Colors.primary} /></View>
      </SafeAreaView>
    );
  }

  if (!listing) return null;

  const typeColor = TYPE_COLOR[listing.type] ?? Colors.primary;
  const isOwner = user?.id === listing.creatorId;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <Pressable onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </Pressable>
        <Text style={[styles.headerTitle, { color: colors.text }]} numberOfLines={1}>
          Detalle del anuncio
        </Text>
        {isOwner ? (
          <Pressable onPress={handleDelete} disabled={deleting} style={styles.headerRight}>
            {deleting
              ? <ActivityIndicator size={18} color={Colors.error} />
              : <Ionicons name="trash-outline" size={20} color={Colors.error} />}
          </Pressable>
        ) : (
          <View style={styles.headerRight} />
        )}
      </View>

      <ScrollView contentContainerStyle={styles.body} showsVerticalScrollIndicator={false}>
        <View style={[styles.hero, { backgroundColor: typeColor + '15' }]}>
          {listing.coverImageUrl ? (
            <Image source={{ uri: listing.coverImageUrl }} style={styles.heroImage} resizeMode="cover" />
          ) : (
            <View style={[styles.heroIconBg, { backgroundColor: typeColor + '22' }]}>
              <Ionicons name="musical-notes" size={36} color={typeColor} />
            </View>
          )}
          <View style={[styles.typeBadge, { backgroundColor: typeColor + '22' }]}>
            <Text style={[styles.typeBadgeText, { color: typeColor }]}>
              {TYPE_LABEL[listing.type]}
            </Text>
          </View>
          <Text style={[styles.title, { color: colors.text }]}>{listing.title}</Text>
        </View>

        {listing.imageUrls?.length ? (
          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Galeria</Text>
            <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.galleryRow}>
              {listing.imageUrls.map((url) => (
                <Image key={url} source={{ uri: url }} style={styles.galleryImage} resizeMode="cover" />
              ))}
            </ScrollView>
          </View>
        ) : null}

        {/* Meta */}
        <View style={[styles.metaCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <InfoRow icon="musical-notes-outline" text={GENRE_LABELS[listing.genre]} />
          <View style={[styles.metaDivider, { backgroundColor: colors.border }]} />
          <InfoRow icon="location-outline" text={listing.city} />
          <View style={[styles.metaDivider, { backgroundColor: colors.border }]} />
          <InfoRow
            icon="person-outline"
            text={`Publicado por ${listing.creatorName}`}
            onPress={!isOwner ? () => navigation.navigate('UserProfile', { userId: listing.creatorId }) : undefined}
          />
        </View>

        {/* Description */}
        {listing.description ? (
          <View style={styles.section}>
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Descripción</Text>
            <Text style={[styles.description, { color: colors.text }]}>{listing.description}</Text>
          </View>
        ) : null}

        {/* Roles */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>
            {listing.type === 'BANDA_BUSCA_MUSICOS' ? 'Buscamos' : 'Soy'}
          </Text>
          <View style={styles.roleGrid}>
            {listing.roles.map(r => (
              <View
                key={r}
                style={[styles.roleChip, { backgroundColor: colors.surface, borderColor: Colors.primary + '55' }]}
              >
                <Ionicons name={ROLE_ICONS[r]} size={15} color={Colors.primary} />
                <Text style={[styles.roleText, { color: colors.text }]}>{ROLE_LABELS[r]}</Text>
              </View>
            ))}
          </View>
        </View>

        {/* Contact */}
        {listing.contactInfo ? (
          <Pressable
            style={[styles.contactBtn, { backgroundColor: Colors.primary }]}
            onPress={handleCopyContact}
          >
            <Ionicons name="copy-outline" size={18} color={Colors.white} />
            <View style={styles.contactText}>
              <Text style={[styles.contactLabel, { color: Colors.white }]}>
                Contacto
              </Text>
              <Text style={[styles.contactValue, { color: Colors.white }]}>
                {listing.contactInfo}
              </Text>
            </View>
            <Ionicons name="chevron-forward" size={16} color={Colors.white} />
          </Pressable>
        ) : (
          <View style={[styles.noContact, { borderColor: colors.border }]}>
            <Text style={[styles.noContactText, { color: colors.textSecondary }]}>
              Sin información de contacto. Busca al creador en la app.
            </Text>
          </View>
        )}
      </ScrollView>
      {copyToastVisible ? (
        <View style={[styles.copyToast, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <View style={styles.copyIcon}>
            <Ionicons name="checkmark" size={16} color={Colors.white} />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={[styles.copyTitle, { color: colors.text }]}>Contacto copiado</Text>
            <Text style={[styles.copyText, { color: colors.textSecondary }]}>Ya esta en el portapapeles.</Text>
          </View>
        </View>
      ) : null}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  header: {
    flexDirection: 'row', alignItems: 'center', gap: Spacing.sm,
    paddingHorizontal: Spacing.base, paddingVertical: 12, borderBottomWidth: 1,
  },
  backBtn: { padding: 4 },
  headerRight: { width: 28, alignItems: 'flex-end' },
  headerTitle: { flex: 1, fontFamily: FontFamily.bold, fontSize: FontSize.base, textAlign: 'center' },

  body: { padding: Spacing.base, gap: Spacing.base },

  hero: {
    borderRadius: 16,
    padding: Spacing.base,
    gap: Spacing.sm,
    alignItems: 'center',
  },
  heroImage: {
    width: '100%',
    aspectRatio: 16 / 9,
    borderRadius: 14,
  },
  heroIconBg: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  typeBadge: { alignSelf: 'center', borderRadius: 8, paddingHorizontal: 10, paddingVertical: 4 },
  typeBadgeText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs },

  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize['2xl'], lineHeight: 30, textAlign: 'center' },

  metaCard: {
    borderRadius: 14, borderWidth: 1,
    paddingHorizontal: Spacing.base, paddingVertical: Spacing.sm,
  },
  infoRow: { flexDirection: 'row', alignItems: 'center', gap: 8, paddingVertical: 10 },
  infoText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, flex: 1 },
  metaDivider: { height: 1, marginHorizontal: 4 },

  section: { gap: 8 },
  sectionTitle: {
    fontFamily: FontFamily.semiBold, fontSize: FontSize.xs,
    textTransform: 'uppercase', letterSpacing: 0.8,
  },
  description: { fontFamily: FontFamily.regular, fontSize: FontSize.base, lineHeight: 22 },
  galleryRow: { gap: Spacing.sm, paddingRight: Spacing.base },
  galleryImage: {
    width: 132,
    height: 98,
    borderRadius: 14,
  },

  roleGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  roleChip: {
    flexDirection: 'row', alignItems: 'center', gap: 6,
    borderWidth: 1, borderRadius: 10,
    paddingHorizontal: 10, paddingVertical: 7,
  },
  roleText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },

  contactBtn: {
    flexDirection: 'row', alignItems: 'center', gap: Spacing.sm,
    borderRadius: 14, padding: Spacing.sm,
  },
  contactText: { flex: 1 },
  contactLabel: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  contactValue: { fontFamily: FontFamily.bold, fontSize: FontSize.base },

  noContact: {
    borderWidth: 1, borderRadius: 14, borderStyle: 'dashed',
    padding: Spacing.sm, alignItems: 'center',
  },
  noContactText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center' },
  copyToast: {
    position: 'absolute',
    left: Spacing.base,
    right: Spacing.base,
    bottom: Spacing.xl,
    borderWidth: 1,
    borderRadius: 16,
    padding: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15,
    shadowRadius: 12,
    elevation: 6,
  },
  copyIcon: {
    width: 30,
    height: 30,
    borderRadius: 15,
    backgroundColor: Colors.success,
    alignItems: 'center',
    justifyContent: 'center',
  },
  copyTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  copyText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
});
