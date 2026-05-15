import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useIsFocused, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { API_BASE_URL } from '../../api/config';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { AppStackParamList } from '../../navigation/AppNavigator';
import {
  ForumCategory,
  ForumDetail,
  Post,
  apiCreatePost,
  apiGetForum,
  apiGetPosts,
  apiJoinForum,
} from '../../api/forumApi';

type Navigation = NativeStackNavigationProp<AppStackParamList>;
type ForumDetailRoute = RouteProp<AppStackParamList, 'ForumDetail'>;

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
  FANDOM: '#8B5CF6',
  COMUNIDAD_MUSICAL: '#3B82F6',
  TEORIA: '#14B8A6',
  INSTRUMENTOS: Colors.primary,
  BANDAS: '#EC4899',
  ARTISTAS: '#F59E0B',
  GENERAL: '#6B7280',
};

function timeAgo(isoString: string): string {
  // El backend devuelve LocalDateTime sin 'Z'. Sin sufijo de timezone
  // JS lo interpreta como hora local; añadimos 'Z' para forzar UTC.
  const utc = isoString.endsWith('Z') || isoString.includes('+') ? isoString : `${isoString}Z`;
  const diff = Date.now() - new Date(utc).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) {
    return 'ahora';
  }
  if (mins < 60) {
    return `${mins}m`;
  }
  const hours = Math.floor(mins / 60);
  if (hours < 24) {
    return `${hours}h`;
  }
  const days = Math.floor(hours / 24);
  return `${days}d`;
}

function initials(name: string): string {
  return name
    .split(' ')
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase() ?? '')
    .join('');
}

function PostCard({ post, currentUserId }: { post: Post; currentUserId: string }) {
  const { colors } = useTheme();
  const isMe = post.authorId === currentUserId;
  const accentColor = Colors.primary;

  return (
    <View style={[styles.postCard, isMe && styles.postCardMe]}>
      {!isMe ? (
        <View style={[styles.avatar, { backgroundColor: `${accentColor}33` }]}>
          <Text style={[styles.avatarText, { color: accentColor }]}>{initials(post.authorName)}</Text>
        </View>
      ) : null}

      <View
        style={[
          styles.bubble,
          isMe ? styles.bubbleMe : { backgroundColor: colors.surface, borderColor: colors.border },
        ]}
      >
        {!isMe ? (
          <Text style={[styles.postAuthor, { color: Colors.primary }]}>{post.authorName}</Text>
        ) : null}
        <Text style={[styles.postContent, { color: isMe ? '#FFFFFF' : colors.text }]}>{post.content}</Text>
        <Text
          style={[
            styles.postTime,
            { color: isMe ? 'rgba(255,255,255,0.6)' : colors.textSecondary },
          ]}
        >
          {timeAgo(post.createdAt)}
        </Text>
      </View>
    </View>
  );
}

export const ForumDetailScreen = () => {
  const { colors } = useTheme();
  const { token, user } = useAuth();
  const navigation = useNavigation<Navigation>();
  const route = useRoute<ForumDetailRoute>();
  const { forumId } = route.params;

  const [forum, setForum] = useState<ForumDetail | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingForum, setLoadingForum] = useState(true);
  const [loadingPosts, setLoadingPosts] = useState(false);
  const [toggling, setToggling] = useState(false);
  const [draftText, setDraftText] = useState('');
  const [sending, setSending] = useState(false);
  const listRef = useRef<FlatList<Post>>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const isFocused = useIsFocused();

  // Ref para controlar el loading sin meterlo en las deps de useCallback
  // (evita el bucle infinito loadingPosts→loadPosts→useEffect→loadPosts…)
  const loadingPostsRef = useRef(false);
  const hasMoreRef = useRef(true);

  const loadForum = useCallback(async () => {
    if (!token) {
      setLoadingForum(false);
      return;
    }
    try {
      const data = await apiGetForum(forumId, token);
      setForum(data);
    } finally {
      setLoadingForum(false);
    }
  }, [forumId, token]);

  // Carga paginada de posts — deps estables (sin loadingPosts ni hasMore en estado)
  const loadPosts = useCallback(
    async (nextPage: number, reset = false) => {
      if (!token || loadingPostsRef.current) return;
      if (!reset && !hasMoreRef.current) return;

      loadingPostsRef.current = true;
      setLoadingPosts(true);
      try {
        const data = await apiGetPosts(forumId, nextPage, token);
        const incoming = data.content ?? [];
        setPosts((prev) => (reset ? incoming : [...prev, ...incoming]));
        hasMoreRef.current = !data.last;
        setHasMore(!data.last);
        setPage(nextPage);
      } finally {
        loadingPostsRef.current = false;
        setLoadingPosts(false);
      }
    },
    [forumId, token],
  );

  const wsUrl = useMemo(() => {
    const base = API_BASE_URL.replace(/^http/i, 'ws');
    return `${base}/ws/forums?token=${encodeURIComponent(token ?? '')}&forumId=${encodeURIComponent(forumId)}`;
  }, [forumId, token]);

  useEffect(() => {
    loadForum();
  }, [loadForum]);

  // Carga inicial de posts cuando se confirma que el usuario es miembro.
  // loadPosts NO está en las deps para evitar el bucle: cambiar hasMore/loadingPosts
  // no debe volver a lanzar la carga desde cero.
  useEffect(() => {
    if (forum?.joined) {
      void loadPosts(0, true);
    } else {
      setPosts([]);
      setPage(0);
      hasMoreRef.current = true;
      setHasMore(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [forum?.joined]);

  useEffect(() => {
    if (!token || !forum?.joined) return;
    const socket = new WebSocket(wsUrl);
    socketRef.current = socket;

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as Post;
        setPosts((currentPosts) => {
          if (currentPosts.some((post) => post.id === payload.id)) return currentPosts;
          return [payload, ...currentPosts];
        });
        listRef.current?.scrollToOffset({ offset: 0, animated: true });
      } catch {
        // ignore malformed frames
      }
    };

    return () => {
      socket.close();
      socketRef.current = null;
    };
  }, [forum?.joined, token, wsUrl]);

  // Polling fallback: refresh page-0 every 5s when focused so messages received
  // while the other user's WS was not open still appear without leaving the screen.
  useEffect(() => {
    if (!isFocused || !forum?.joined) return;
    const interval = setInterval(async () => {
      if (!token || loadingPostsRef.current) return;
      try {
        const data = await apiGetPosts(forumId, 0, token);
        const incoming = data.content ?? [];
        setPosts((prev) => {
          const map = new Map<string, Post>();
          // incoming first so existing posts keep their position if merged
          [...incoming, ...prev].forEach((p) => map.set(p.id, p));
          // Keep chronological order: newest first (inverted list)
          return Array.from(map.values()).sort(
            (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
          );
        });
      } catch {
        // silent
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [forumId, isFocused, forum?.joined, token]);

  const handleToggle = async () => {
    if (!token || !forum || toggling) {
      return;
    }

    if (forum.joined) {
      navigation.navigate('ForumProfile', { forumId });
      return;
    }

    setToggling(true);
    try {
      await apiJoinForum(forumId, token);
      await loadForum();
    } finally {
      setToggling(false);
    }
  };

  const handleSend = async () => {
    if (!token || !draftText.trim() || sending) {
      return;
    }

    setSending(true);
    const text = draftText.trim();
    setDraftText('');

    try {
      const socket = socketRef.current;
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ content: text }));
      } else {
        const newPost = await apiCreatePost(forumId, text, token);
        setPosts((currentPosts) => [newPost, ...currentPosts]);
        listRef.current?.scrollToOffset({ offset: 0, animated: true });
      }
    } catch {
      setDraftText(text);
    } finally {
      setSending(false);
    }
  };

  const color = forum ? CATEGORY_COLORS[forum.category] ?? Colors.primary : Colors.primary;
  const categoryLabel = forum ? CATEGORY_LABELS[forum.category] ?? forum.category : '';

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.headerCenter}
          activeOpacity={0.8}
          disabled={!forum?.joined}
          onPress={() => forum?.joined && navigation.navigate('ForumProfile', { forumId })}
        >
          {loadingForum ? (
            <ActivityIndicator color={Colors.primary} />
          ) : (
            <>
              <Text style={[styles.headerName, { color: colors.text }]} numberOfLines={1}>
                {forum?.name ?? ''}
              </Text>
              <View style={styles.headerMeta}>
                <View style={[styles.categoryDot, { backgroundColor: color }]} />
                <Text style={[styles.headerSub, { color: colors.textSecondary }]}>
                  {categoryLabel} · {forum?.memberCount ?? 0} miembros
                </Text>
              </View>
            </>
          )}
        </TouchableOpacity>

        {forum ? (
          <TouchableOpacity
            style={[
              styles.joinButton,
              { borderColor: color, backgroundColor: forum.joined ? 'transparent' : color },
            ]}
            onPress={handleToggle}
            disabled={toggling}
            activeOpacity={0.8}
          >
            {toggling ? (
              <ActivityIndicator size={12} color={forum.joined ? color : '#FFFFFF'} />
            ) : (
              <Text style={[styles.joinButtonText, { color: forum.joined ? color : '#FFFFFF' }]}>
              {forum.joined ? 'Perfil' : 'Unirse'}
            </Text>
          )}
          </TouchableOpacity>
        ) : null}
      </View>

      {forum?.description ? (
        <View
          style={[
            styles.descriptionBanner,
            { backgroundColor: `${color}11`, borderBottomColor: `${color}33` },
          ]}
        >
          <Text style={[styles.descriptionText, { color: colors.textSecondary }]}>
            {forum.description}
          </Text>
        </View>
      ) : null}

      {!forum?.joined ? (
        <View style={styles.center}>
          <Ionicons name="lock-closed-outline" size={48} color={colors.textSecondary} />
          <Text style={[styles.lockText, { color: colors.textSecondary }]}>
            Unete para ver los mensajes
          </Text>
        </View>
      ) : (
        <KeyboardAvoidingView
          style={styles.flex}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        >
          <FlatList
            ref={listRef}
            data={posts}
            keyExtractor={(post) => post.id}
            contentContainerStyle={styles.postList}
            showsVerticalScrollIndicator={false}
            inverted
            onEndReached={() => {
              if (hasMoreRef.current && !loadingPostsRef.current) {
                void loadPosts(page + 1);
              }
            }}
            onEndReachedThreshold={0.2}
            ListEmptyComponent={
              loadingPosts ? null : (
                <View style={styles.emptyPosts}>
                  <Ionicons name="chatbubble-outline" size={40} color={colors.textSecondary} />
                  <Text style={[styles.emptyPostsText, { color: colors.textSecondary }]}>
                    Se el primero en escribir un mensaje
                  </Text>
                </View>
              )
            }
            ListFooterComponent={
              loadingPosts && posts.length > 0 ? (
                <ActivityIndicator color={Colors.primary} style={styles.footerLoader} />
              ) : null
            }
            renderItem={({ item }) => <PostCard post={item} currentUserId={user?.id ?? ''} />}
          />

          <View
            style={[
              styles.inputRow,
              { backgroundColor: colors.surface, borderTopColor: colors.border },
            ]}
          >
            <TextInput
              style={[
                styles.input,
                {
                  backgroundColor: colors.background,
                  color: colors.text,
                  borderColor: colors.border,
                },
              ]}
              placeholder="Escribe un mensaje..."
              placeholderTextColor={colors.textSecondary}
              value={draftText}
              onChangeText={setDraftText}
              multiline
              maxLength={2000}
            />
            <Pressable
              style={[
                styles.sendButton,
                { backgroundColor: draftText.trim() ? Colors.primary : colors.border },
              ]}
              onPress={handleSend}
              disabled={!draftText.trim() || sending}
            >
              {sending ? (
                <ActivityIndicator size={16} color="#FFFFFF" />
              ) : (
                <Ionicons name="send" size={16} color="#FFFFFF" />
              )}
            </Pressable>
          </View>
        </KeyboardAvoidingView>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: {
    flex: 1,
  },
  flex: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  headerCenter: {
    flex: 1,
    justifyContent: 'center',
  },
  headerName: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  headerMeta: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  categoryDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  headerSub: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
  },
  joinButton: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: BorderRadius.full,
    borderWidth: 1.5,
    alignItems: 'center',
    minWidth: 68,
  },
  joinButtonText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  descriptionBanner: {
    paddingHorizontal: Spacing.base,
    paddingVertical: 8,
    borderBottomWidth: 1,
  },
  descriptionText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.md,
  },
  lockText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },
  postList: {
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
    gap: 10,
  },
  postCard: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: Spacing.sm,
  },
  postCardMe: {
    flexDirection: 'row-reverse',
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  avatarText: {
    fontFamily: FontFamily.bold,
    fontSize: 12,
  },
  bubble: {
    maxWidth: '75%',
    padding: 10,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    gap: 2,
  },
  bubbleMe: {
    backgroundColor: Colors.primary,
    borderColor: Colors.primary,
  },
  postAuthor: {
    fontFamily: FontFamily.semiBold,
    fontSize: 11,
  },
  postContent: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },
  postTime: {
    fontFamily: FontFamily.regular,
    fontSize: 10,
    alignSelf: 'flex-end',
  },
  emptyPosts: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 60,
    gap: Spacing.md,
  },
  emptyPostsText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
  },
  footerLoader: {
    marginVertical: Spacing.md,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: Spacing.sm,
    padding: Spacing.sm,
    borderTopWidth: 1,
  },
  input: {
    flex: 1,
    borderRadius: 20,
    borderWidth: 1,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    maxHeight: 100,
  },
  sendButton: {
    width: 40,
    height: 40,
    borderRadius: BorderRadius.full,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
