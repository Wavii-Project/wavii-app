import React, { useCallback, useState } from 'react';
import { ActivityIndicator, FlatList, Image, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useFocusEffect, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import {
  ForumDetail,
  ForumMember,
  apiGetForum,
  apiLeaveForum,
  apiRemoveForumMember,
  apiUpdateForumMemberRole,
} from '../../api/forumApi';

type Route = RouteProp<AppStackParamList, 'ForumProfile'>;
type Navigation = NativeStackNavigationProp<AppStackParamList>;

const ROLE_LABELS: Record<string, string> = {
  OWNER: 'Creador',
  ADMIN: 'Admin',
  MEMBER: 'Miembro',
};

export const ForumProfileScreen = () => {
  const { colors } = useTheme();
  const { token, user } = useAuth();
  const { showAlert } = useAlert();
  const navigation = useNavigation<Navigation>();
  const route = useRoute<Route>();
  const { forumId } = route.params;

  const [forum, setForum] = useState<ForumDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [savingUserId, setSavingUserId] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!token) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      setForum(await apiGetForum(forumId, token));
    } finally {
      setLoading(false);
    }
  }, [forumId, token]);

  useFocusEffect(useCallback(() => {
    load();
  }, [load]));

  const currentRole = forum?.currentUserRole;
  const canManage = currentRole === 'OWNER' || currentRole === 'ADMIN';
  const isOwner = currentRole === 'OWNER';

  const leaveGroup = () => {
    if (!token || !forum) return;
    showAlert({
      title: 'Salir de la comunidad',
      message: 'Dejaras de ver y enviar mensajes en este grupo.',
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Salir',
          style: 'destructive',
          onPress: async () => {
            try {
              await apiLeaveForum(forum.id, token);
              navigation.navigate('Social');
            } catch {
              showAlert({ title: 'Error', message: 'No se pudo salir de la comunidad.' });
            }
          },
        },
      ],
    });
  };

  const changeRole = async (member: ForumMember, nextRole: 'ADMIN' | 'MEMBER') => {
    if (!token || !forum) return;
    setSavingUserId(member.userId);
    try {
      setForum(await apiUpdateForumMemberRole(forum.id, member.userId, nextRole, token));
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo actualizar el rol.' });
    } finally {
      setSavingUserId(null);
    }
  };

  const removeMember = (member: ForumMember) => {
    if (!token || !forum) return;
    showAlert({
      title: 'Expulsar miembro',
      message: `Quieres expulsar a ${member.name} de esta comunidad?`,
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Expulsar',
          style: 'destructive',
          onPress: async () => {
            setSavingUserId(member.userId);
            try {
              await apiRemoveForumMember(forum.id, member.userId, token);
              setForum((current) =>
                current
                  ? {
                      ...current,
                      memberCount: Math.max(0, current.memberCount - 1),
                      members: current.members.filter((item) => item.userId !== member.userId),
                    }
                  : current
              );
            } catch {
              showAlert({ title: 'Error', message: 'No se pudo expulsar a este miembro.' });
            } finally {
              setSavingUserId(null);
            }
          },
        },
      ],
    });
  };

  const renderMember = ({ item }: { item: ForumMember }) => {
    const isSelf = item.userId === user?.id;
    const targetIsOwner = item.role === 'OWNER';
    const canPromote = isOwner && !isSelf && !targetIsOwner;
    const canKick = canManage && !isSelf && !targetIsOwner;

    return (
      <View style={[styles.memberRow, { borderBottomColor: colors.border }]}>
        {item.avatarUrl ? (
          <Image source={{ uri: item.avatarUrl }} style={styles.avatar} />
        ) : (
          <View style={[styles.avatarFallback, { backgroundColor: Colors.primaryOpacity10 }]}>
            <Text style={styles.avatarText}>{item.name.charAt(0).toUpperCase()}</Text>
          </View>
        )}
        <View style={styles.memberBody}>
          <Text style={[styles.memberName, { color: colors.text }]} numberOfLines={1}>
            {item.name}{isSelf ? ' (tu)' : ''}
          </Text>
          <Text style={[styles.memberRole, { color: colors.textSecondary }]}>{ROLE_LABELS[item.role] ?? item.role}</Text>
        </View>
        {savingUserId === item.userId ? (
          <ActivityIndicator size="small" color={Colors.primary} />
        ) : (
          <View style={styles.memberActions}>
            {canPromote ? (
              <TouchableOpacity
                style={[styles.iconAction, { borderColor: colors.border }]}
                onPress={() => changeRole(item, item.role === 'ADMIN' ? 'MEMBER' : 'ADMIN')}
              >
                <Ionicons name={item.role === 'ADMIN' ? 'remove-circle-outline' : 'shield-checkmark-outline'} size={17} color={Colors.primary} />
              </TouchableOpacity>
            ) : null}
            {canKick ? (
              <TouchableOpacity style={[styles.iconAction, { borderColor: colors.border }]} onPress={() => removeMember(item)}>
                <Ionicons name="person-remove-outline" size={17} color={Colors.error} />
              </TouchableOpacity>
            ) : null}
          </View>
        )}
      </View>
    );
  };

  if (loading) {
    return (
      <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
        <View style={styles.center}><ActivityIndicator color={Colors.primary} /></View>
      </SafeAreaView>
    );
  }

  if (!forum) return null;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.headerTitle, { color: colors.text }]}>Perfil del grupo</Text>
        <TouchableOpacity onPress={leaveGroup} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="exit-outline" size={22} color={Colors.error} />
        </TouchableOpacity>
      </View>

      <FlatList
        data={forum.members}
        keyExtractor={(item) => item.userId}
        contentContainerStyle={styles.list}
        ListHeaderComponent={
          <View style={styles.profileHead}>
            {forum.coverImageUrl ? (
              <Image source={{ uri: forum.coverImageUrl }} style={styles.cover} resizeMode="cover" />
            ) : (
              <View style={[styles.cover, styles.coverFallback]}>
                <Ionicons name="people" size={46} color={Colors.primary} />
              </View>
            )}
            <Text style={[styles.name, { color: colors.text }]}>{forum.name}</Text>
            <Text style={[styles.meta, { color: colors.textSecondary }]}>
              {forum.memberCount} miembros - {forum.likeCount} me gusta
            </Text>
            {forum.description ? (
              <Text style={[styles.description, { color: colors.textSecondary }]}>{forum.description}</Text>
            ) : null}
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Miembros</Text>
          </View>
        }
        renderItem={renderMember}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  headerTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },
  list: { padding: Spacing.base, paddingBottom: Spacing['2xl'] },
  profileHead: { alignItems: 'center', gap: Spacing.xs, marginBottom: Spacing.base },
  cover: { width: 116, height: 116, borderRadius: 58 },
  coverFallback: { alignItems: 'center', justifyContent: 'center', backgroundColor: Colors.primaryOpacity10 },
  name: { fontFamily: FontFamily.extraBold, fontSize: FontSize.xl, textAlign: 'center' },
  meta: { fontFamily: FontFamily.semiBold, fontSize: FontSize.sm },
  description: { fontFamily: FontFamily.regular, fontSize: FontSize.sm, lineHeight: 20, textAlign: 'center' },
  sectionTitle: {
    alignSelf: 'flex-start',
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
    marginTop: Spacing.base,
  },
  memberRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
    gap: Spacing.sm,
  },
  avatar: { width: 42, height: 42, borderRadius: 21 },
  avatarFallback: { width: 42, height: 42, borderRadius: 21, alignItems: 'center', justifyContent: 'center' },
  avatarText: { fontFamily: FontFamily.black, color: Colors.primary, fontSize: FontSize.base },
  memberBody: { flex: 1 },
  memberName: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  memberRole: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },
  memberActions: { flexDirection: 'row', gap: Spacing.xs },
  iconAction: {
    width: 34,
    height: 34,
    borderRadius: 17,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
