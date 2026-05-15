import React, { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, Image, Pressable, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { ForumCategory, ForumSummary, apiGetMyForums } from '../../api/forumApi';

type Navigation = NativeStackNavigationProp<AppStackParamList>;

const CATEGORY_COLORS: Record<ForumCategory, string> = {
  FANDOM: '#8B5CF6',
  COMUNIDAD_MUSICAL: '#3B82F6',
  TEORIA: '#14B8A6',
  INSTRUMENTOS: Colors.primary,
  BANDAS: '#EC4899',
  ARTISTAS: '#F59E0B',
  GENERAL: '#6B7280',
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

export const MyForumsScreen = () => {
  const { colors } = useTheme();
  const { token } = useAuth();
  const navigation = useNavigation<Navigation>();
  const [items, setItems] = useState<ForumSummary[]>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    if (!token) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      setItems(await apiGetMyForums(token));
    } finally {
      setLoading(false);
    }
  }, [token]);

  useFocusEffect(useCallback(() => {
    load();
  }, [load]));

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Mis comunidades</Text>
        <View style={{ width: 26 }} />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          ListEmptyComponent={
            <View style={styles.center}>
              <Ionicons name="people-outline" size={44} color={colors.textSecondary} />
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>Todavia no te has unido a ninguna comunidad.</Text>
            </View>
          }
          renderItem={({ item }) => {
            const color = CATEGORY_COLORS[item.category] ?? Colors.primary;
            const icon = CATEGORY_ICONS[item.category] ?? 'chatbubbles';
            return (
              <Pressable
                style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}
                onPress={() => navigation.navigate('ForumDetail', { forumId: item.id })}
              >
                {item.coverImageUrl ? (
                  <Image source={{ uri: item.coverImageUrl }} style={styles.cardImage} resizeMode="cover" />
                ) : (
                  <View style={[styles.cardIcon, { backgroundColor: `${color}22` }]}>
                    <Ionicons name={icon} size={24} color={color} />
                  </View>
                )}
                <View style={styles.cardBody}>
                  <Text style={[styles.cardName, { color: colors.text }]} numberOfLines={1}>{item.name}</Text>
                  <Text style={[styles.cardDescription, { color: colors.textSecondary }]} numberOfLines={1}>
                    {item.description ?? 'Comunidad Wavii'}
                  </Text>
                  <Text style={[styles.meta, { color: colors.textSecondary }]}>
                    {item.memberCount} miembros · {item.likeCount} me gusta
                  </Text>
                </View>
                <Ionicons name="chevron-forward" size={18} color={colors.textSecondary} />
              </Pressable>
            );
          }}
        />
      )}
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
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: Spacing.sm, padding: Spacing.xl },
  emptyText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center' },
  list: { padding: Spacing.base, paddingBottom: Spacing['2xl'], flexGrow: 1 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  cardIcon: {
    width: 48,
    height: 48,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardImage: { width: 48, height: 48, borderRadius: BorderRadius.md },
  cardBody: { flex: 1, gap: 2 },
  cardName: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  cardDescription: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  meta: { fontFamily: FontFamily.regular, fontSize: 11 },
});
