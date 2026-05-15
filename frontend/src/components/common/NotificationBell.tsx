import React, { useCallback, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  apiFetchNotifications,
  apiMarkNotificationRead,
  AppNotification,
} from '../../api/notificationsApi';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';

type Navigation = NativeStackNavigationProp<AppStackParamList>;

type NotificationBellProps = {
  size?: 'sm' | 'md';
};

const BELL_CONFIG = {
  sm: { button: 34, icon: 17, badgeMinWidth: 16, badgeHeight: 16, badgeFont: FontSize.xs },
  md: { button: 38, icon: 18, badgeMinWidth: 18, badgeHeight: 18, badgeFont: FontSize.xs },
} as const;

export const NotificationBell = ({ size = 'sm' }: NotificationBellProps) => {
  const navigation = useNavigation<Navigation>();
  const { colors } = useTheme();
  const { token } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<AppNotification[]>([]);
  const config = BELL_CONFIG[size];

  const syncUnread = useCallback(async () => {
    // No llamar al backend si no hay sesión activa
    if (!token) {
      setUnreadCount(0);
      return [];
    }
    try {
      const response = await apiFetchNotifications(token);
      setUnreadCount(response.summary.unreadCount ?? 0);
      return response.items;
    } catch {
      setUnreadCount(0);
      return [];
    }
  }, [token]);

  const loadModal = useCallback(async () => {
    setLoading(true);
    const nextItems = await syncUnread();
    setItems(nextItems);
    setLoading(false);
  }, [syncUnread]);

  useFocusEffect(
    useCallback(() => {
      syncUnread();
    }, [syncUnread])
  );

  const openModal = useCallback(() => {
    setVisible(true);
    loadModal();
  }, [loadModal]);

  const closeModal = useCallback(() => {
    setVisible(false);
  }, []);

  const modalTitle = useMemo(
    () => (unreadCount > 0 ? `Notificaciones (${unreadCount})` : 'Notificaciones'),
    [unreadCount]
  );

  const handleOpenItem = useCallback(
    async (item: AppNotification) => {
      if (!item.read && token) {
        try {
          await apiMarkNotificationRead(item.id, token);
          setItems((current) =>
            current.map((entry) => (entry.id === item.id ? { ...entry, read: true } : entry))
          );
          setUnreadCount((current) => Math.max(0, current - 1));
        } catch {
          // ignored on purpose
        }
      }

      closeModal();
      const enrollmentId =
        typeof item.data?.enrollmentId === 'string' ? item.data.enrollmentId : undefined;
      const teacherId = typeof item.data?.teacherId === 'string' ? item.data.teacherId : undefined;

      if (enrollmentId) {
        navigation.navigate('MainTabs', {
          screen: 'Clases',
          params: { refreshKey: `${Date.now()}` },
        });
        return;
      }

      if (teacherId) {
        navigation.navigate('TeacherProfile', { teacherId });
      }
    },
    [closeModal, navigation]
  );

  return (
    <>
      <TouchableOpacity
        style={[
          styles.button,
          {
            width: config.button,
            height: config.button,
            borderColor: colors.border,
            backgroundColor: colors.surface,
          },
        ]}
        onPress={openModal}
        activeOpacity={0.82}
      >
        <Ionicons name="notifications-outline" size={config.icon} color={colors.text} />
        {unreadCount > 0 ? (
          <View
            style={[
              styles.badge,
              {
                minWidth: config.badgeMinWidth,
                height: config.badgeHeight,
                borderRadius: config.badgeHeight / 2,
              },
            ]}
          >
            <Text style={[styles.badgeText, { fontSize: config.badgeFont }]}>
              {unreadCount > 9 ? '9+' : String(unreadCount)}
            </Text>
          </View>
        ) : null}
      </TouchableOpacity>

      <Modal visible={visible} transparent animationType="fade" onRequestClose={closeModal}>
        <Pressable style={styles.overlay} onPress={closeModal}>
          <Pressable
            style={[styles.modalCard, { backgroundColor: colors.surface, borderColor: colors.border }]}
            onPress={(event) => event.stopPropagation()}
          >
            <View style={styles.modalHeader}>
              <View style={{ flex: 1 }}>
                <Text style={[styles.modalTitle, { color: colors.text }]}>{modalTitle}</Text>
                <Text style={[styles.modalSubtitle, { color: colors.textSecondary }]}>
                  Tus avisos recientes de clases y agenda.
                </Text>
              </View>
              <TouchableOpacity
                style={[styles.closeButton, { borderColor: colors.border }]}
                onPress={closeModal}
                activeOpacity={0.8}
              >
                <Ionicons name="close" size={16} color={colors.textSecondary} />
              </TouchableOpacity>
            </View>

            {loading ? (
              <View style={styles.center}>
                <ActivityIndicator color={Colors.primary} />
              </View>
            ) : (
              <FlatList
                data={items}
                keyExtractor={(item) => item.id}
                style={styles.modalList}
                contentContainerStyle={[
                  styles.modalListContent,
                  items.length === 0 ? styles.modalListContentEmpty : null,
                ]}
                ListEmptyComponent={
                  <View style={styles.center}>
                    <View
                      style={[
                        styles.emptyIconWrap,
                        { backgroundColor: Colors.primaryOpacity10 },
                      ]}
                    >
                      <Ionicons name="notifications-off-outline" size={20} color={Colors.primary} />
                    </View>
                    <Text style={[styles.emptyTitle, { color: colors.text }]}>
                      Todo al dia
                    </Text>
                    <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                      Cuando haya novedades de clases o agenda, apareceran aqui.
                    </Text>
                  </View>
                }
                renderItem={({ item }) => (
                  <TouchableOpacity
                    style={[
                      styles.itemCard,
                      {
                        backgroundColor: item.read ? colors.background : Colors.primaryOpacity10,
                        borderColor: item.read ? colors.border : Colors.primaryOpacity20,
                      },
                    ]}
                    onPress={() => handleOpenItem(item)}
                    activeOpacity={0.82}
                  >
                    <View
                      style={[
                        styles.itemIconWrap,
                        { backgroundColor: item.read ? colors.surface : Colors.white },
                      ]}
                    >
                      <Ionicons
                        name="notifications-outline"
                        size={16}
                        color={item.read ? colors.textSecondary : Colors.primary}
                      />
                    </View>
                    <View style={{ flex: 1 }}>
                      <Text style={[styles.itemTitle, { color: colors.text }]}>{item.title}</Text>
                      <Text style={[styles.itemBody, { color: colors.textSecondary }]}>
                        {item.body}
                      </Text>
                    </View>
                  </TouchableOpacity>
                )}
              />
            )}
          </Pressable>
        </Pressable>
      </Modal>
    </>
  );
};

const styles = StyleSheet.create({
  button: {
    borderRadius: BorderRadius.full,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    position: 'absolute',
    top: -4,
    right: -4,
    backgroundColor: Colors.primary,
    paddingHorizontal: 4,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: {
    color: Colors.white,
    fontFamily: FontFamily.bold,
    lineHeight: 14,
  },
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.28)',
    justifyContent: 'flex-start',
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.xl + Spacing.base,
  },
  modalCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    maxHeight: '72%',
    gap: Spacing.sm,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.sm,
  },
  modalTitle: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.base,
  },
  modalSubtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginTop: 2,
  },
  closeButton: {
    width: 30,
    height: 30,
    borderRadius: 15,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalList: {
    flexGrow: 0,
  },
  modalListContent: {
    gap: Spacing.sm,
    paddingBottom: Spacing.xs,
  },
  modalListContentEmpty: {
    flexGrow: 1,
    justifyContent: 'center',
    minHeight: 200,
  },
  itemCard: {
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    padding: Spacing.sm,
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: Spacing.sm,
  },
  itemIconWrap: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  itemTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
    marginBottom: 2,
  },
  itemBody: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    lineHeight: 18,
  },
  center: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.sm,
    paddingVertical: Spacing.xl,
  },
  emptyIconWrap: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
  },
  emptyTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  emptyText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'center',
    lineHeight: 18,
    paddingHorizontal: Spacing.base,
  },
});
