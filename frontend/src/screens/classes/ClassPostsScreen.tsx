import React, { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { apiFetchStudentClassPosts, ClassPost } from '../../api/classesApi';
import { NotificationBell } from '../../components/common/NotificationBell';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';

export const ClassPostsScreen = () => {
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();
  const { token } = useAuth();
  const { colors } = useTheme();
  const [posts, setPosts] = useState<ClassPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError(null);
    try {
      const data = await apiFetchStudentClassPosts(token);
      setPosts(data);
    } catch (err: any) {
      setError(err?.response?.data?.message ?? 'No se pudieron cargar las novedades.');
      setPosts([]);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <View style={{ flex: 1 }}>
          <Text style={[styles.title, { color: colors.text }]}>Novedades de clases</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>Noticias de tus profesores activos</Text>
        </View>
        <NotificationBell size="sm" />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={posts}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListHeaderComponent={
            error ? (
              <View style={[styles.errorBanner, { backgroundColor: colors.surface, borderColor: Colors.error }]}>
                <Ionicons name="alert-circle-outline" size={18} color={Colors.error} />
                <View style={{ flex: 1 }}>
                  <Text style={[styles.errorTitle, { color: colors.text }]}>No pudimos cargar las novedades</Text>
                  <Text style={[styles.errorText, { color: colors.textSecondary }]}>{error}</Text>
                </View>
                <TouchableOpacity onPress={load}>
                  <Text style={[styles.retryText, { color: Colors.primary }]}>Reintentar</Text>
                </TouchableOpacity>
              </View>
            ) : null
          }
          ListEmptyComponent={
            <View style={styles.center}>
              <View style={[styles.emptyIconWrap, { backgroundColor: Colors.primaryOpacity10 }]}>
                <Ionicons name="megaphone-outline" size={24} color={Colors.primary} />
              </View>
              <Text style={[styles.emptyTitle, { color: colors.text }]}>Aun no tienes novedades de tus profesores</Text>
              <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                Cuando publiquen avisos o noticias para sus alumnos, apareceran aqui.
              </Text>
            </View>
          }
          renderItem={({ item }) => (
            <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
              <Text style={[styles.teacherName, { color: colors.text }]}>{item.teacherName}</Text>
              <Text style={[styles.meta, { color: colors.textSecondary }]}>
                {item.createdAt.replace('T', ' ').slice(0, 16)}
              </Text>
              <Text style={[styles.postTitle, { color: colors.text }]}>{item.title}</Text>
              <Text style={[styles.postContent, { color: colors.textSecondary }]}>{item.content}</Text>
            </View>
          )}
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
    gap: Spacing.base,
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.sm,
    borderBottomWidth: 1,
  },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize.xl },
  subtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginTop: 2 },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.xl,
  },
  list: {
    padding: Spacing.base,
    gap: Spacing.sm,
    paddingBottom: Spacing.xl,
  },
  errorBanner: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.sm,
    marginBottom: Spacing.sm,
  },
  errorTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  errorText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 18, marginTop: 2 },
  retryText: { fontFamily: FontFamily.bold, fontSize: FontSize.xs },
  card: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    padding: Spacing.base,
    gap: Spacing.xs,
    marginBottom: Spacing.sm,
  },
  teacherName: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  meta: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  postTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.base, marginTop: Spacing.xs },
  postContent: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20 },
  emptyIconWrap: {
    width: 52,
    height: 52,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base, textAlign: 'center' },
  emptyText: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, textAlign: 'center', lineHeight: 20 },
});
