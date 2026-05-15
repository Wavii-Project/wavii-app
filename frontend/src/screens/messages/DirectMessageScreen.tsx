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
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { DirectMessage, apiFetchConversation, apiSendDirectMessage } from '../../api/dmApi';
import { API_BASE_URL } from '../../api/config';
import { ChatSocketState, getPollingInterval, getReconnectDelay, getSocketStatusLabel, sanitizeWsUrl } from '../../utils/chatRealtime';

type Route = RouteProp<AppStackParamList, 'DirectMessage'>;
type Nav = NativeStackNavigationProp<AppStackParamList>;

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
  item: DirectMessage;
  isMe: boolean;
  colors: ReturnType<typeof useTheme>['colors'];
}) {
  return (
    <View style={[styles.messageRow, isMe && styles.messageRowMe]}>
      <View
        style={[
          styles.bubble,
          isMe
            ? { backgroundColor: Colors.primary }
            : { backgroundColor: colors.surface, borderColor: colors.border, borderWidth: 1 },
        ]}
      >
        {!isMe && (
          <Text style={[styles.senderName, { color: Colors.primary }]}>{item.senderName}</Text>
        )}
        <Text style={[styles.bubbleText, { color: isMe ? Colors.white : colors.text }]}>
          {item.content}
        </Text>
        <Text style={[styles.bubbleTime, { color: isMe ? Colors.whiteOpacity70 : colors.textSecondary }]}>
          {formatTime(item.createdAt)}
        </Text>
      </View>
    </View>
  );
}

export const DirectMessageScreen: React.FC = () => {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const { token, user: me } = useAuth();
  const { showAlert } = useAlert();
  const navigation = useNavigation<Nav>();
  const route = useRoute<Route>();
  const { userId, userName } = route.params;
  const isFocused = useIsFocused();

  const [messages, setMessages] = useState<DirectMessage[]>([]);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const [socketState, setSocketState] = useState<ChatSocketState>('connecting');
  const [connectVersion, setConnectVersion] = useState(0);
  const listRef = useRef<FlatList<DirectMessage>>(null);
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptRef = useRef(0);
  const manualCloseRef = useRef(false);

  const wsUrl = useMemo(() => {
    const base = API_BASE_URL.replace(/^http/i, 'ws');
    return `${base}/ws/direct?token=${encodeURIComponent(token ?? '')}&userId=${encodeURIComponent(userId)}`;
  }, [token, userId]);

  const mergeMessages = useCallback((incoming: DirectMessage[]) => {
    setMessages((prev) => {
      const map = new Map<string, DirectMessage>();
      [...prev, ...incoming].forEach((msg) => map.set(msg.id, msg));
      return Array.from(map.values()).sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    });
  }, []);

  const loadMessages = useCallback(async () => {
    if (!token) return;
    try {
      const data = await apiFetchConversation(userId, token);
      mergeMessages(data);
    } catch {
      // silencioso en polling
    } finally {
      setLoading(false);
    }
  }, [mergeMessages, userId, token]);

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

  useEffect(() => {
    if (!isFocused) return;
    loadMessages();
    pollRef.current = setInterval(loadMessages, getPollingInterval(socketState));
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
      pollRef.current = null;
    };
  }, [isFocused, loadMessages, socketState]);

  useEffect(() => {
    if (!token || !isFocused) return;
    let cancelled = false;
    manualCloseRef.current = false;
    setSocketState('connecting');
    const socket = new WebSocket(wsUrl);
    socketRef.current = socket;

    if (__DEV__) {
      console.log('[DirectMessageScreen] WS connect', sanitizeWsUrl(wsUrl));
    }

    socket.onopen = () => {
      reconnectAttemptRef.current = 0;
      setSocketState('open');
      void loadMessages();
      if (__DEV__) {
        console.log('[DirectMessageScreen] WS open');
      }
    };

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as DirectMessage;
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
        console.log('[DirectMessageScreen] WS error');
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
        console.log('[DirectMessageScreen] WS close', event.code);
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
  }, [connectVersion, isFocused, loadMessages, mergeMessages, scheduleReconnect, token, wsUrl]);

  const handleSend = async () => {
    if (!token || !text.trim() || sending) return;
    const content = text.trim();
    setText('');
    setSending(true);
    try {
      const socket = socketRef.current;
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify({ content }));
      } else {
        const sent = await apiSendDirectMessage(userId, content, token);
        mergeMessages([sent]);
        listRef.current?.scrollToOffset({ offset: 0, animated: true });
      }
    } catch (err: any) {
      const msg = err?.response?.data?.message ?? 'No se pudo enviar el mensaje.';
      showAlert({ title: 'Error', message: msg });
      setText(content); // restaurar el texto
    } finally {
      setSending(false);
    }
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <TouchableOpacity
          style={styles.headerInfo}
          activeOpacity={0.75}
          onPress={() => navigation.navigate('UserProfile', { userId })}
        >
          <View style={[styles.headerAvatar, { backgroundColor: Colors.primaryOpacity10 }]}>
            <Text style={[styles.headerAvatarText, { color: Colors.primary }]}>
              {userName.charAt(0).toUpperCase()}
            </Text>
          </View>
          <View style={styles.headerText}>
            <Text style={[styles.headerName, { color: colors.text }]} numberOfLines={1}>
              {userName}
            </Text>
            {getSocketStatusLabel(socketState) ? (
              <Text style={[styles.headerSub, { color: colors.textSecondary }]}>
                {getSocketStatusLabel(socketState)}
              </Text>
            ) : null}
          </View>
        </TouchableOpacity>
        <View style={{ width: 30 }} />
      </View>

      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={0}
      >
        {loading ? (
          <View style={styles.center}><ActivityIndicator color={Colors.primary} /></View>
        ) : (
          <FlatList
            ref={listRef}
            data={messages}
            keyExtractor={(item) => item.id}
            contentContainerStyle={styles.list}
            showsVerticalScrollIndicator={false}
            inverted
            ListEmptyComponent={
              <View style={styles.emptyWrap}>
                <Ionicons name="chatbubbles-outline" size={48} color={colors.textSecondary} />
                <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
                  Se el primero en escribir
                </Text>
              </View>
            }
            renderItem={({ item }) => (
              <MessageBubble
                item={item}
                isMe={item.senderId === me?.id}
                colors={colors}
              />
            )}
          />
        )}

        {/* Input */}
        <View style={[styles.inputRow, { backgroundColor: colors.surface, borderTopColor: colors.border, paddingBottom: Math.max(insets.bottom, Spacing.sm) }]}>
          <TextInput
            style={[styles.input, { color: colors.text, backgroundColor: colors.background, borderColor: colors.border }]}
            placeholder="Escribe un mensaje..."
            placeholderTextColor={colors.textSecondary}
            value={text}
            onChangeText={setText}
            multiline
            maxLength={2000}
            returnKeyType="default"
          />
          <TouchableOpacity
            style={[
              styles.sendBtn,
              { backgroundColor: text.trim() ? Colors.primary : colors.border },
            ]}
            onPress={handleSend}
            disabled={!text.trim() || sending}
            activeOpacity={0.75}
          >
            {sending
              ? <ActivityIndicator size="small" color={Colors.white} />
              : <Ionicons name="send" size={18} color={Colors.white} />
            }
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },

  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.base,
    paddingVertical: 12,
    borderBottomWidth: 1,
  },
  backBtn: { padding: 4 },
  headerInfo: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
  },
  headerText: {
    flex: 1,
  },
  headerAvatar: {
    width: 34,
    height: 34,
    borderRadius: 17,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerAvatarText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  headerName: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  headerSub: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginTop: 1,
  },

  list: {
    padding: Spacing.base,
    gap: 8,
    flexGrow: 1,
  },

  emptyWrap: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 80,
    gap: 12,
  },
  emptyText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
  },

  messageRow: {
    flexDirection: 'row',
    justifyContent: 'flex-start',
    marginBottom: 4,
  },
  messageRowMe: {
    justifyContent: 'flex-end',
  },
  bubble: {
    maxWidth: '78%',
    borderRadius: BorderRadius.xl,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    gap: 2,
  },
  senderName: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.xs,
    marginBottom: 2,
  },
  bubbleText: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  bubbleTime: {
    fontFamily: FontFamily.regular,
    fontSize: 10,
    alignSelf: 'flex-end',
    marginTop: 2,
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
    maxHeight: 120,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.base,
    paddingVertical: 10,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
  },
  sendBtn: {
    width: 42,
    height: 42,
    borderRadius: 21,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
