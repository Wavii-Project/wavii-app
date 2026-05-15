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
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
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
import { ChatSocketState, getPollingInterval, getReconnectDelay, getSocketStatusLabel, sanitizeWsUrl } from '../../utils/chatRealtime';

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
        <View style={[styles.avatar, { backgroundColor: Colors.primaryOpacity20 }]}>
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
        <Text style={[styles.content, { color: isMe ? Colors.white : colors.text }]}>
          {item.content}
        </Text>
        <Text
          style={[
            styles.timestamp,
            { color: isMe ? Colors.whiteOpacity60 : colors.textSecondary },
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
  const insets = useSafeAreaInsets();
  const { showAlert } = useAlert();
  const isFocused = useIsFocused();

  const { enrollmentId, teacherName, teacherId, studentId, studentName } = route.params;

  const [messages, setMessages] = useState<ClassMessage[]>([]);
  const [draft, setDraft] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [socketState, setSocketState] = useState<ChatSocketState>('connecting');
  const [connectVersion, setConnectVersion] = useState(0);
  const socketRef = useRef<WebSocket | null>(null);
  const hasLoadedRef = useRef(false);
  const listRef = useRef<FlatList<ClassMessage>>(null);
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptRef = useRef(0);
  const manualCloseRef = useRef(false);

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
      // Keep newest first so FlatList inverted renders newest at bottom.
      return Array.from(map.values()).sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
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

  const scheduleReconnect = useCallback(() => {
    if (!isFocused || !token || reconnectRef.current) return;
    const delay = getReconnectDelay(reconnectAttemptRef.current);
    reconnectAttemptRef.current += 1;
    reconnectRef.current = setTimeout(() => {
      reconnectRef.current = null;
      setSocketState('connecting');
      setConnectVersion((current) => current + 1);
    }, delay);
  }, [isFocused, token]);

  // Initial load
  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!token || !isFocused) return;
    let cancelled = false;
    manualCloseRef.current = false;
    setSocketState('connecting');
    const socket = new WebSocket(wsUrl);
    socketRef.current = socket;

    if (__DEV__) {
      console.log('[ClassRoomScreen] WS connect', sanitizeWsUrl(wsUrl));
    }

    socket.onopen = () => {
      reconnectAttemptRef.current = 0;
      setSocketState('open');
      void load();
      if (__DEV__) {
        console.log('[ClassRoomScreen] WS open');
      }
    };

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as ClassMessage;
        mergeMessages([payload]);
        listRef.current?.scrollToOffset({ offset: 0, animated: true });
      } catch {
        // ignore malformed frames
      }
    };

    socket.onerror = () => {
      if (cancelled) return;
      setSocketState('error');
      scheduleReconnect();
      try {
        socket.close();
      } catch {
        // ignore
      }
      if (__DEV__) {
        console.log('[ClassRoomScreen] WS error');
      }
    };

    socket.onclose = (event) => {
      socketRef.current = null;
      if (cancelled || manualCloseRef.current) {
        return;
      }
      setSocketState(event.wasClean ? 'closed' : 'error');
      scheduleReconnect();
      if (__DEV__) {
        console.log('[ClassRoomScreen] WS close', event.code);
      }
    };

    return () => {
      cancelled = true;
      manualCloseRef.current = true;
      if (reconnectRef.current) {
        clearTimeout(reconnectRef.current);
        reconnectRef.current = null;
      }
      socket.close();
      socketRef.current = null;
      setSocketState('closed');
    };
  }, [connectVersion, isFocused, load, mergeMessages, scheduleReconnect, token, wsUrl]);

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
    }, getPollingInterval(socketState));
    return () => clearInterval(interval);
  }, [enrollmentId, isFocused, mergeMessages, socketState, token]);

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
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
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
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            {getSocketStatusLabel(socketState) ?? headerSubtitle}
          </Text>
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
            inverted
            renderItem={({ item }) => (
              <MessageBubble
                item={item}
                isMe={item.senderId === user?.id}
                colors={colors}
              />
            )}
            ListEmptyComponent={
              <Text style={[styles.empty, { color: colors.textSecondary }]}>
                Todavia no hay mensajes. Escribe el primero.
              </Text>
            }
          />

          <View style={[styles.composer, { backgroundColor: colors.surface, borderTopColor: colors.border, paddingBottom: Math.max(insets.bottom, Spacing.sm) }]}>
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
    paddingVertical: 12,
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
