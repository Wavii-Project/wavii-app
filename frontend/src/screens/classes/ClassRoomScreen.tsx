import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
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
import { API_BASE_URL } from '../../api/config';
import { apiFetchClassMessages, apiSendClassMessage, ClassMessage } from '../../api/classesApi';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';

type Route = RouteProp<AppStackParamList, 'ClassRoom'>;
type Nav = NativeStackNavigationProp<AppStackParamList, 'ClassRoom'>;

type SocketPayload = {
  content: string;
};

function formatTime(iso: string): string {
  const utc = iso.endsWith('Z') || iso.includes('+') ? iso : `${iso}Z`;
  const d = new Date(utc);
  const h = d.getHours().toString().padStart(2, '0');
  const m = d.getMinutes().toString().padStart(2, '0');
  return `${h}:${m}`;
}

function MessageBubble({
  item,
  isMe,
  colors,
}: {
  item: ClassMessage;
  isMe: boolean;
  colors: ReturnType<typeof useTheme>['colors'];
}) {
  return (
    <View style={[styles.messageRow, isMe && styles.messageRowMe]}>
      {!isMe && (
        <View style={[styles.avatar, { backgroundColor: `${Colors.primary}22` }]}>
          <Text style={[styles.avatarText, { color: Colors.primary }]}>
            {item.senderName.charAt(0).toUpperCase()}
          </Text>
        </View>
      )}

      <View
        style={[
          styles.bubble,
          isMe
            ? styles.bubbleMe
            : { backgroundColor: colors.surface, borderColor: colors.border },
        ]}
      >
        {!isMe && (
          <Text style={[styles.senderName, { color: Colors.primary }]}>{item.senderName}</Text>
        )}
        <Text style={[styles.content, { color: isMe ? '#FFFFFF' : colors.text }]}>
          {item.content}
        </Text>
        <Text
          style={[
            styles.timestamp,
            { color: isMe ? 'rgba(255,255,255,0.6)' : colors.textSecondary },
          ]}
        >
          {formatTime(item.createdAt)}
        </Text>
      </View>
    </View>
  );
}

export const ClassRoomScreen = () => {
  const route = useRoute<Route>();
  const navigation = useNavigation<Nav>();
  const { token, user } = useAuth();
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const isFocused = useIsFocused();

  const { enrollmentId, teacherName, teacherId, studentId, studentName } = route.params;

  const [messages, setMessages] = useState<ClassMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);
  const hasLoadedRef = useRef(false);
  const listRef = useRef<FlatList<ClassMessage>>(null);

  // Determine who "the other person" is for the header
  const isTeacher = user?.id !== studentId;
  const headerTitle = isTeacher ? studentName : teacherName;
  const headerSubtitle = 'Chat de clase';

  const wsUrl = useMemo(() => {
    const base = API_BASE_URL.replace(/^http/i, 'ws');
    return `${base}/ws/classes?token=${encodeURIComponent(token ?? '')}&enrollmentId=${encodeURIComponent(enrollmentId)}`;
  }, [enrollmentId, token]);

  const mergeMessages = useCallback((incoming: ClassMessage[]) => {
    setMessages((prev) => {
      const map = new Map<string, ClassMessage>();
      [...prev, ...incoming].forEach((msg) => map.set(msg.id, msg));
      return Array.from(map.values()).sort(
        (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
      );
    });
  }, []);

  const load = useCallback(async () => {
    if (!token) return;
    if (!hasLoadedRef.current) setLoading(true);
    try {
      const data = await apiFetchClassMessages(token, enrollmentId);
      mergeMessages(data);
      hasLoadedRef.current = true;
    } catch (err: any) {
      showAlert({ title: 'Error', message: err?.response?.data?.message ?? 'No se pudo cargar el chat.' });
    } finally {
      setLoading(false);
    }
  }, [enrollmentId, mergeMessages, showAlert, token]);

  // Initial load
  useEffect(() => {
    load();
  }, [load]);

  // WebSocket
  useEffect(() => {
    if (!token) return;
    const socket = new WebSocket(wsUrl);
    socketRef.current = socket;

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as ClassMessage;
        mergeMessages([payload]);
      } catch {
        // ignore malformed frames
      }
    };

    return () => {
      socket.close();
      socketRef.current = null;
    };
  }, [token, wsUrl, mergeMessages]);

  // Polling fallback: refresh every 5s when focused (covers cases where WS is not open on the other end)
  useEffect(() => {
    if (!isFocused || !hasLoadedRef.current) return;
    const interval = setInterval(async () => {
      if (!token) return;
      try {
        const data = await apiFetchClassMessages(token, enrollmentId);
        mergeMessages(data);
      } catch {
        // silent
      }
    }, 5000);
    return () => clearInterval(interval);
  }, [enrollmentId, isFocused, mergeMessages, token]);

  // Auto-scroll to end on new messages
  useEffect(() => {
    if (messages.length > 0) {
      setTimeout(() => listRef.current?.scrollToEnd({ animated: true }), 50);
    }
  }, [messages.length]);

  const send = async () => {
    if (!token || !draft.trim()) return;
    const content = draft.trim();
    setDraft('');
    setSending(true);
    try {
      const socket = socketRef.current;
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ content } satisfies SocketPayload));
      } else {
        const saved = await apiSendClassMessage(token, enrollmentId, content);
        mergeMessages([saved]);
      }
    } catch (err: any) {
      setDraft(content);
      showAlert({ title: 'Error', message: err?.response?.data?.message ?? 'No se pudo enviar el mensaje.' });
    } finally {
      setSending(false);
    }
  };


  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={() => navigation.goBack()}
          hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}
        >
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.headerCenter}
          activeOpacity={0.7}
          onPress={() => {
            if (isTeacher) {
              navigation.navigate('UserProfile', { userId: studentId });
            } else {
              navigation.navigate('TeacherProfile', { teacherId });
            }
          }}
        >
          <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>
            {headerTitle}
          </Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>{headerSubtitle}</Text>
        </TouchableOpacity>

        <View style={{ width: 26 }} />
      </View>

      {loading ? (
        <View style={styles.center}>
          <ActivityIndicator color={Colors.primary} />
        </View>
      ) : (
        <KeyboardAvoidingView
          style={styles.flex}
          behavior={Platform.OS === 'ios' ? 'padding' : undefined}
          keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 0}
        >
          <FlatList
            ref={listRef}
            data={messages}
            keyExtractor={(item) => item.id}
            contentContainerStyle={[styles.list, messages.length === 0 && styles.listEmpty]}
            showsVerticalScrollIndicator={false}
            renderItem={({ item }) => (
              <MessageBubble
                item={item}
                isMe={item.senderId === user?.id}
                colors={colors}
              />
            )}
            ListEmptyComponent={
              <Text style={[styles.empty, { color: colors.textSecondary }]}>
                Todavía no hay mensajes. Escribe el primero.
              </Text>
            }
          />

          <View style={[styles.composer, { backgroundColor: colors.surface, borderTopColor: colors.border }]}>
            <TextInput
              value={draft}
              onChangeText={setDraft}
              placeholder="Escribe un mensaje..."
              placeholderTextColor={colors.textSecondary}
              style={[styles.input, { color: colors.text, backgroundColor: colors.background, borderColor: colors.border }]}
              multiline
              maxLength={2000}
            />
            <TouchableOpacity
              style={[
                styles.sendBtn,
                { backgroundColor: draft.trim() ? Colors.primary : colors.border },
              ]}
              onPress={send}
              disabled={sending || !draft.trim()}
            >
              {sending ? (
                <ActivityIndicator color={Colors.white} size="small" />
              ) : (
                <Ionicons name="send" size={16} color={Colors.white} />
              )}
            </TouchableOpacity>
          </View>
        </KeyboardAvoidingView>
      )}
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  flex: { flex: 1 },
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
  title: { fontFamily: FontFamily.extraBold, fontSize: FontSize.base },
  subtitle: { fontFamily: FontFamily.regular, fontSize: FontSize.xs, marginTop: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  list: {
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.md,
    paddingBottom: Spacing.sm,
    gap: 8,
  },
  listEmpty: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  messageRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: Spacing.sm,
  },
  messageRowMe: {
    flexDirection: 'row-reverse',
  },
  avatar: {
    width: 30,
    height: 30,
    borderRadius: 15,
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  avatarText: {
    fontFamily: FontFamily.bold,
    fontSize: 11,
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
  senderName: {
    fontFamily: FontFamily.semiBold,
    fontSize: 11,
  },
  content: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  timestamp: {
    fontFamily: FontFamily.regular,
    fontSize: 10,
    alignSelf: 'flex-end',
    marginTop: 2,
  },
  empty: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
  },
  composer: {
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
  sendBtn: {
    width: 40,
    height: 40,
    borderRadius: BorderRadius.full,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
