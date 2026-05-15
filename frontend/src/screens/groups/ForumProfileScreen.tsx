import React, { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  Image,
  Modal,
  Platform,
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
import * as ImagePicker from 'expo-image-picker';
import { RouteProp, useFocusEffect, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import {
  ForumCategory,
  ForumDetail,
  ForumMember,
  apiGetForum,
  apiLeaveForum,
  apiRemoveForumMember,
  apiUpdateForum,
  apiUpdateForumMemberRole,
  apiUploadForumImage,
} from '../../api/forumApi';

const CATEGORIES: { value: ForumCategory; label: string }[] = [
  { value: 'FANDOM', label: 'Fandom' },
  { value: 'COMUNIDAD_MUSICAL', label: 'Comunidad musical' },
  { value: 'TEORIA', label: 'Teoría musical' },
  { value: 'INSTRUMENTOS', label: 'Instrumentos' },
  { value: 'BANDAS', label: 'Bandas' },
  { value: 'ARTISTAS', label: 'Artistas' },
  { value: 'GENERAL', label: 'General' },
];

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

  // Edit modal state
  const [editVisible, setEditVisible] = useState(false);
  const [editDescription, setEditDescription] = useState('');
  const [editCategory, setEditCategory] = useState<ForumCategory>('GENERAL');
  const [editCoverAsset, setEditCoverAsset] = useState<{ uri: string; name: string; type: string } | null>(null);
  const [editSaving, setEditSaving] = useState(false);

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

  const openEditModal = () => {
    if (!forum) return;
    setEditDescription(forum.description ?? '');
    setEditCategory(forum.category as ForumCategory);
    setEditCoverAsset(null);
    setEditVisible(true);
  };

  const pickEditCover = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      showAlert({ title: 'Permiso denegado', message: 'Necesitamos acceso a tu galería para elegir una foto.' });
      return;
    }
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ['images'],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.82,
    });
    if (result.canceled || !result.assets[0]) return;
    const asset = result.assets[0];
    setEditCoverAsset({
      uri: asset.uri,
      name: asset.fileName ?? `forum_${Date.now()}.jpg`,
      type: asset.mimeType ?? 'image/jpeg',
    });
  };

  const handleSaveEdit = async () => {
    if (!token || !forum || editSaving) return;
    setEditSaving(true);
    try {
      const coverImageUrl = editCoverAsset
        ? (await apiUploadForumImage(editCoverAsset, token)).url
        : undefined;
      const updated = await apiUpdateForum(
        forum.id,
        {
          description: editDescription.trim(),
          category: editCategory,
          ...(coverImageUrl ? { coverImageUrl } : {}),
        },
        token,
      );
      setForum(updated);
      setEditVisible(false);
    } catch {
      showAlert({ title: 'Error', message: 'No se pudo guardar los cambios.' });
    } finally {
      setEditSaving(false);
    }
  };

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
          delaySeconds: 5,
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

  const changeRole = (member: ForumMember, nextRole: 'ADMIN' | 'MEMBER') => {
    if (!token || !forum) return;
    const isPromoting = nextRole === 'ADMIN';
    showAlert({
      title: isPromoting ? 'Hacer administrador' : 'Quitar administrador',
      message: isPromoting
        ? `¿Seguro que quieres hacer administrador a ${member.name}? Tendrá permisos para gestionar miembros.`
        : `¿Seguro que quieres quitar el rol de administrador a ${member.name}?`,
      buttons: [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Confirmar',
          style: 'destructive',
          onPress: async () => {
            setSavingUserId(member.userId);
            try {
              setForum(await apiUpdateForumMemberRole(forum.id, member.userId, nextRole, token));
            } catch {
              showAlert({ title: 'Error', message: 'No se pudo actualizar el rol.' });
            } finally {
              setSavingUserId(null);
            }
          },
        },
      ],
    });
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
        <TouchableOpacity
          style={styles.memberInfo}
          onPress={() => navigation.navigate('UserProfile', { userId: item.userId })}
          activeOpacity={0.7}
        >
          {item.avatarUrl ? (
            <Image source={{ uri: item.avatarUrl }} style={styles.avatar} />
          ) : (
            <View style={[styles.avatarFallback, { backgroundColor: Colors.primaryOpacity10 }]}>
              <Text style={styles.avatarText}>{item.name.charAt(0).toUpperCase()}</Text>
            </View>
          )}
          <View style={styles.memberBody}>
            <Text style={[styles.memberName, { color: colors.text }]} numberOfLines={1}>
              {item.name}{isSelf ? ' (tú)' : ''}
            </Text>
            <Text style={[styles.memberRole, { color: colors.textSecondary }]}>{ROLE_LABELS[item.role] ?? item.role}</Text>
          </View>
        </TouchableOpacity>
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

  const coverPreviewUri = editCoverAsset?.uri ?? forum.coverImageUrl ?? null;

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
            {canManage ? (
              <TouchableOpacity style={[styles.editBtn, { borderColor: colors.border }]} onPress={openEditModal}>
                <Ionicons name="pencil-outline" size={15} color={colors.text} />
                <Text style={[styles.editBtnText, { color: colors.text }]}>Editar grupo</Text>
              </TouchableOpacity>
            ) : null}
            <Text style={[styles.sectionTitle, { color: colors.textSecondary }]}>Miembros</Text>
          </View>
        }
        renderItem={renderMember}
      />

      {/* Edit modal */}
      <Modal visible={editVisible} transparent animationType="slide" onRequestClose={() => setEditVisible(false)}>
        <Pressable style={styles.modalOverlay} onPress={() => setEditVisible(false)} />
        <View style={[styles.modalSheet, { backgroundColor: colors.background }]}>
          <View style={[styles.modalHeader, { borderBottomColor: colors.border }]}>
            <TouchableOpacity onPress={() => setEditVisible(false)} hitSlop={12}>
              <Ionicons name="close" size={22} color={colors.text} />
            </TouchableOpacity>
            <Text style={[styles.modalTitle, { color: colors.text }]}>Editar grupo</Text>
            <TouchableOpacity onPress={handleSaveEdit} disabled={editSaving} hitSlop={12}>
              {editSaving
                ? <ActivityIndicator size="small" color={Colors.primary} />
                : <Text style={styles.saveBtn}>Guardar</Text>}
            </TouchableOpacity>
          </View>

          <ScrollView contentContainerStyle={styles.modalBody} keyboardShouldPersistTaps="handled">
            {/* Cover photo */}
            <Text style={[styles.fieldLabel, { color: colors.textSecondary }]}>Foto del grupo</Text>
            <Pressable style={[styles.coverPicker, { backgroundColor: colors.surface, borderColor: colors.border }]} onPress={pickEditCover}>
              {coverPreviewUri ? (
                <Image source={{ uri: coverPreviewUri }} style={styles.coverThumb} resizeMode="cover" />
              ) : (
                <View style={[styles.coverThumb, styles.coverThumbPlaceholder]}>
                  <Ionicons name="camera-outline" size={22} color={Colors.primary} />
                </View>
              )}
              <View style={{ flex: 1 }}>
                <Text style={[styles.coverPickerTitle, { color: colors.text }]}>
                  {editCoverAsset ? 'Foto seleccionada' : 'Cambiar foto'}
                </Text>
                <Text style={[styles.coverPickerHint, { color: colors.textSecondary }]}>
                  Se verá como imagen de perfil del grupo.
                </Text>
              </View>
              <Ionicons name="chevron-forward" size={16} color={colors.textSecondary} />
            </Pressable>

            {/* Description */}
            <Text style={[styles.fieldLabel, { color: colors.textSecondary }]}>Descripción</Text>
            <View style={[styles.textAreaWrap, { backgroundColor: colors.surface, borderColor: colors.border }]}>
              <TextInput
                style={[styles.textArea, { color: colors.text }]}
                placeholder="¿De qué trata esta comunidad?"
                placeholderTextColor={colors.textSecondary}
                value={editDescription}
                onChangeText={setEditDescription}
                multiline
                numberOfLines={4}
                maxLength={400}
                textAlignVertical="top"
              />
            </View>
            <Text style={[styles.counter, { color: colors.textSecondary }]}>{editDescription.length}/400</Text>

            {/* Category */}
            <Text style={[styles.fieldLabel, { color: colors.textSecondary }]}>Temática</Text>
            <View style={styles.categoryGrid}>
              {CATEGORIES.map((cat) => {
                const active = editCategory === cat.value;
                return (
                  <TouchableOpacity
                    key={cat.value}
                    style={[
                      styles.categoryChip,
                      { borderColor: active ? Colors.primary : colors.border, backgroundColor: active ? Colors.primaryOpacity10 : colors.surface },
                    ]}
                    onPress={() => setEditCategory(cat.value)}
                    activeOpacity={0.75}
                  >
                    <Text style={[styles.categoryChipText, { color: active ? Colors.primary : colors.text }]}>
                      {cat.label}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          </ScrollView>
        </View>
      </Modal>
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
  editBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    borderWidth: 1,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.md,
    paddingVertical: 6,
    marginTop: Spacing.xs,
  },
  editBtnText: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs },
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
  memberInfo: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
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

  // Modal
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.45)',
  },
  modalSheet: {
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    maxHeight: '75%',
    paddingBottom: Platform.OS === 'ios' ? 34 : 16,
  },
  modalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  modalTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.base, flex: 1, textAlign: 'center' },
  saveBtn: { fontFamily: FontFamily.bold, fontSize: FontSize.sm, color: Colors.primary },
  modalBody: { padding: Spacing.base, gap: Spacing.sm },
  fieldLabel: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  coverPicker: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.sm,
  },
  coverThumb: { width: 52, height: 52, borderRadius: 26 },
  coverThumbPlaceholder: { alignItems: 'center', justifyContent: 'center', backgroundColor: Colors.primaryOpacity10 },
  coverPickerTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.sm },
  coverPickerHint: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, lineHeight: 17 },
  textAreaWrap: {
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  textArea: {
    minHeight: 90,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    padding: 0,
  },
  counter: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    textAlign: 'right',
  },
  categoryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  categoryChip: {
    borderWidth: 1,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.sm,
    paddingVertical: 7,
  },
  categoryChipText: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
  },
});
