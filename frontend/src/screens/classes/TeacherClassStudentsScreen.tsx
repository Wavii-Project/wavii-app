import React, { useCallback, useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useFocusEffect, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { apiFetchManageClasses, apiUpdateClassStatus, ClassEnrollment, ClassManageResponse } from '../../api/classesApi';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';

type Nav = NativeStackNavigationProp<AppStackParamList>;
type Route = RouteProp<AppStackParamList, 'TeacherClassStudents'>;

const STUDENT_STATUSES = new Set(['accepted', 'paid', 'scheduled', 'completed', 'refund_requested']);
const REQUEST_STATUSES = new Set(['pending']);

export const TeacherClassStudentsScreen = () => {
  const navigation = useNavigation<Nav>();
  const route = useRoute<Route>();
  const { token } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();

  const mode = route.params?.mode ?? 'students';
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<ClassManageResponse | null>(null);
  const [query, setQuery] = useState('');
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    try {
      const response = await apiFetchManageClasses(token);
      setData(response);
    } catch (err: any) {
      showAlert({ title: 'Error', message: err?.response?.data?.message ?? 'No se pudo cargar la lista.' });
    } finally {
      setLoading(false);
    }
  }, [showAlert, token]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  const filtered = useMemo(() => {
    const source = data?.classes ?? [];
    const base = source.filter((entry) => {
      const status = (entry.paymentStatus ?? '').toLowerCase();
      return mode === 'requests' ? REQUEST_STATUSES.has(status) : STUDENT_STATUSES.has(status);
    });
    const text = query.trim().toLowerCase();
    if (!text) return base;
    return base.filter((entry) => {
      const haystack = `${entry.studentName} ${entry.instrument ?? ''} ${entry.city ?? ''} ${entry.requestedModality ?? entry.modality ?? ''}`.toLowerCase();
      return haystack.includes(text);
    });
  }, [data?.classes, mode, query]);

  const updateStatus = async (item: ClassEnrollment, status: 'accepted' | 'rejected') => {
    if (!token) return;
    setSaving(true);
    try {
      const updated = await apiUpdateClassStatus(token, item.id, { status });
      setData((current) =>
        current
          ? {
              ...current,
              classes: current.classes
                .map((entry) => (entry.id === updated.id ? updated : entry))
                .filter((entry) => (entry.paymentStatus ?? '').toLowerCase() !== 'rejected'),
            }
          : current
      );
    } catch (err: any) {
      showAlert({ title: 'Error', message: err?.response?.data?.message ?? 'No se pudo actualizar la solicitud.' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>{mode === 'requests' ? 'Solicitudes' : 'Alumnos'}</Text>
        <View style={{ width: 26 }} />
      </View>

      <View style={[styles.searchWrap, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <Ionicons name="search-outline" size={16} color={colors.textSecondary} />
        <TextInput
          style={[styles.searchInput, { color: colors.text }]}
          value={query}
          onChangeText={setQuery}
          placeholder={mode === 'requests' ? 'Buscar solicitud' : 'Buscar alumno'}
          placeholderTextColor={colors.textSecondary}
        />
        {query ? (
          <TouchableOpacity onPress={() => setQuery('')}>
            <Ionicons name="close-circle" size={16} color={colors.textSecondary} />
          </TouchableOpacity>
        ) : null}
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
              <Text style={[styles.cardTitle, { color: colors.text }]}>{item.studentName}</Text>
              <Text style={[styles.cardMeta, { color: colors.textSecondary }]}>
                {[
                  item.instrument,
                  (item.requestedModality || item.modality)?.replace(/\w+/g, (w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()),
                  item.city,
                ].filter(Boolean).join(' · ') || 'Sin información'}
              </Text>
              <View style={styles.actions}>
                {mode === 'requests' ? (
                  <>
                    <MiniAction label="Aceptar" icon="checkmark-outline" primary onPress={() => updateStatus(item, 'accepted')} />
                    <MiniAction label="Rechazar" icon="close-outline" onPress={() => updateStatus(item, 'rejected')} />
                  </>
                ) : (
                  <>
                    <MiniAction
                      label="Agendar"
                      icon="calendar-outline"
                      onPress={() => navigation.navigate('ManageClasses', { focusEnrollmentId: item.id })}
                    />
                    <MiniAction
                      label="Chat"
                      icon="chatbubble-ellipses-outline"
                      primary
                      onPress={() =>
                        navigation.navigate('ClassRoom', {
                          enrollmentId: item.id,
                          teacherName: item.teacherName,
                          teacherId: item.teacherId,
                          studentId: item.studentId,
                          studentName: item.studentName,
                        })
                      }
                    />
                  </>
                )}
              </View>
            </View>
          )}
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={[styles.emptyTitle, { color: colors.text }]}>
                {mode === 'requests' ? 'Aún no tienes solicitudes pendientes' : 'Aún no tienes alumnos aceptados'}
              </Text>
            </View>
          }
        />
      )}

      {saving ? <View style={styles.savingMask}><ActivityIndicator color={Colors.primary} /></View> : null}
    </SafeAreaView>
  );
};

const MiniAction = ({
  label,
  icon,
  primary,
  onPress,
}: {
  label: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  primary?: boolean;
  onPress: () => void;
}) => {
  const { colors } = useTheme();
  return (
    <TouchableOpacity
      style={[
        styles.actionBtn,
        {
          borderColor: primary ? Colors.primary : colors.border,
          backgroundColor: primary ? Colors.primary : colors.surface,
        },
      ]}
      onPress={onPress}
      activeOpacity={0.85}
    >
      <Ionicons name={icon} size={14} color={primary ? Colors.white : Colors.primary} />
      <Text style={[styles.actionText, { color: primary ? Colors.white : colors.text }]}>{label}</Text>
    </TouchableOpacity>
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
  searchWrap: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.xs,
    margin: Spacing.base,
    paddingHorizontal: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    minHeight: 40,
  },
  searchInput: { flex: 1, fontFamily: FontFamily.regular, fontSize: FontSize.sm, paddingVertical: 0 },
  list: { paddingHorizontal: Spacing.base, paddingBottom: Spacing.xl, gap: Spacing.sm },
  card: { borderWidth: 1, borderRadius: BorderRadius.lg, padding: Spacing.base, gap: Spacing.sm },
  cardTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  cardMeta: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 18 },
  actions: { flexDirection: 'row', gap: Spacing.xs },
  actionBtn: {
    flex: 1,
    minHeight: 36,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    paddingHorizontal: Spacing.sm,
  },
  actionText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: Spacing.xl },
  emptyTitle: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm, textAlign: 'center' },
  savingMask: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255,255,255,0.45)',
    alignItems: 'center',
    justifyContent: 'center',
  },
});
