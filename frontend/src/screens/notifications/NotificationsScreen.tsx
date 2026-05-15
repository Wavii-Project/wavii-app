import React, { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  apiClearNotifications,
  apiFetchNotifications,
  apiMarkAllNotificationsRead,
  apiMarkNotificationRead,
  AppNotification,
} from '../../api/notificationsApi';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';

export const NotificationsScreen = () => {
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();
  const { colors } = useTheme();
  const { token } = useAuth();
  const { showAlert } = useAlert();
  const [loading, setLoading] = useState(true);
  const [items, setItems] = useState<AppNotification[]>([]);

  const load = useCallback(async () => {
    if (!token) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const response = await apiFetchNotifications(token);
      setItems(response.items);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  const handleOpen = async (item: AppNotification) => {
    if (!item.read && token) {
      try {
        await apiMarkNotificationRead(item.id, token);
        setItems((current) => current.map((entry) => (entry.id === item.id ? { ...entry, read: true } : entry)));
      } catch {}
    }

    const enrollmentId = typeof item.data?.enrollmentId === 'string' ? item.data.enrollmentId : null;
    const teacherName = typeof item.data?.teacherName === 'string' ? item.data.teacherName : '';
    const teacherId = typeof item.data?.teacherId === 'string' ? item.data.teacherId : '';
    const studentId = typeof item.data?.studentId === 'string' ? item.data.studentId : '';
    const studentName = typeof item.data?.studentName === 'string' ? item.data.studentName : '';
    if (enrollmentId) {
      navigation.navigate('ClassRoom', {
        enrollmentId,
        teacherName,
        teacherId,
        studentId,
        studentName,
      });
    }
  };

  const handleClear = async () => {
    if (!token || items.length === 0) return;
    showAlert({
      title: 'Limpiar notificaciones',
      message: 'Esta accion eliminara todas tus notificaciones de forma permanente.',
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Limpiar',
          style: 'destructive',
          onPress: async () => {
            try {
              const result = await apiClearNotifications(token);
              setItems([]);
              showAlert({
                title: 'Notificaciones eliminadas',
                message:
                  result.removed > 0
                    ? `Se eliminaron ${result.removed} notificaciones.`
                    : 'No habia notificaciones para eliminar.',
              });
            } catch {
              showAlert({
                title: 'Error',
                message: 'No se pudieron limpiar las notificaciones. Intentalo de nuevo.',
              });
            }
          },
        },
      ],
    });
  };

  const handleMarkAllRead = async () => {
    if (!token || items.length === 0) return;
    try {
      await apiMarkAllNotificationsRead(token);
      setItems((current) => current.map((entry) => ({ ...entry, read: true })));
    } catch {}
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Notificaciones</Text>
        <View style={styles.headerActions}>
          <TouchableOpacity
            style={[styles.clearBtn, { borderColor: colors.border, backgroundColor: colors.surface }]}
            onPress={handleMarkAllRead}
            disabled={items.length === 0}
            activeOpacity={0.85}
          >
            <Ionicons
              name="checkmark-done-outline"
              size={16}
              color={items.length === 0 ? colors.textSecondary : Colors.primary}
            />
            <Text style={[styles.clearBtnText, { color: items.length === 0 ? colors.textSecondary : colors.text }]}>
              Leídas
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.clearBtn, { borderColor: colors.border, backgroundColor: colors.surface }]}
            onPress={handleClear}
            disabled={items.length === 0}
            activeOpacity={0.85}
          >
            <Ionicons name="trash-outline" size={16} color={items.length === 0 ? colors.textSecondary : Colors.primary} />
            <Text style={[styles.clearBtnText, { color: items.length === 0 ? colors.textSecondary : colors.text }]}>
              Limpiar
            </Text>
          </TouchableOpacity>
        </View>
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
              <Ionicons name="notifications-off-outline" size={44} color={colors.textSecondary} />
              <Text style={[styles.empty, { color: colors.textSecondary }]}>No tienes notificaciones.</Text>
            </View>
          }
          renderItem={({ item }) => (
            <TouchableOpacity
              style={[
                styles.card,
                {
                  backgroundColor: colors.surface,
                  borderColor: item.read ? colors.border : Colors.primary,
                },
              ]}
              onPress={() => handleOpen(item)}
              activeOpacity={0.8}
            >
              <View style={[styles.iconWrap, { backgroundColor: item.read ? colors.background : Colors.primaryOpacity10 }]}>
                <Ionicons name="notifications-outline" size={18} color={item.read ? colors.textSecondary : Colors.primary} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={[styles.cardTitle, { color: colors.text }]}>{item.title}</Text>
                <Text style={[styles.cardBody, { color: colors.textSecondary }]}>{item.body}</Text>
              </View>
            </TouchableOpacity>
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
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: Spacing.sm },
  list: { padding: Spacing.base, gap: Spacing.sm, flexGrow: 1 },
  empty: { fontFamily: FontFamily.regular, fontSize: FontSize.sm },
  card: {
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.base,
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  iconWrap: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cardTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, marginBottom: 2 },
  cardBody: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 18 },
  clearBtn: {
    minHeight: 30,
    borderWidth: 1,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  clearBtnText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
  },
  headerActions: {
    flexDirection: 'row',
    gap: Spacing.xs,
  },
});
