import React, { createContext, useContext, useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  Modal,
  TouchableOpacity,
  Animated,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from './ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../theme';

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

/* ── Tipos ── */

export interface AlertButton {
  text: string;
  style?: 'default' | 'cancel' | 'destructive';
  onPress?: () => void;
  delaySeconds?: number;
}

export interface AlertOptions {
  title: string;
  message?: string;
  buttons?: AlertButton[];
  icon?: IoniconsName;
  iconColor?: string;
}

interface AlertContextValue {
  showAlert: (options: AlertOptions) => void;
}

const AlertContext = createContext<AlertContextValue>({
  showAlert: () => {},
});

export const useAlert = () => useContext(AlertContext);

/* ── Provider ── */

const AlertButtonComponent = ({ btn, index, total, colors, handlePress }: { btn: AlertButton, index: number, total: number, colors: any, handlePress: (b?: AlertButton) => void }) => {
  const [disabled, setDisabled] = React.useState(!!btn.delaySeconds);
  const [timeLeft, setTimeLeft] = React.useState(btn.delaySeconds || 0);

  React.useEffect(() => {
    if (btn.delaySeconds) {
      setDisabled(true);
      setTimeLeft(btn.delaySeconds);
      const interval = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            clearInterval(interval);
            setDisabled(false);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      return () => clearInterval(interval);
    } else {
      setDisabled(false);
      setTimeLeft(0);
    }
  }, [btn.delaySeconds, btn.text]);

  const isDestructive = btn.style === 'destructive';
  const isCancel = btn.style === 'cancel';
  const isPrimary = !isCancel && !isDestructive && total > 1 && index === total - 1;

  return (
    <TouchableOpacity
      style={[
        styles.btn,
        total === 1 && { flex: 1 },
        isCancel && { backgroundColor: colors.background, borderWidth: 1, borderColor: colors.border },
        isDestructive && { backgroundColor: Colors.error },
        isPrimary && { backgroundColor: Colors.primary },
        !isCancel && !isDestructive && !isPrimary && total === 1 && { backgroundColor: Colors.primary },
        disabled && { opacity: 0.5 },
      ]}
      onPress={() => handlePress(btn)}
      activeOpacity={0.8}
      disabled={disabled}
    >
      <Text
        style={[
          styles.btnText,
          isCancel && { color: colors.text },
          isDestructive && { color: Colors.white },
          isPrimary && { color: Colors.white },
          !isCancel && !isDestructive && !isPrimary && total === 1 && { color: Colors.white },
        ]}
      >
        {btn.text}{disabled ? ` (${timeLeft}s)` : ''}
      </Text>
    </TouchableOpacity>
  );
};

export const AlertProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { colors } = useTheme();
  const [visible, setVisible] = useState(false);
  const [opts, setOpts] = useState<AlertOptions>({ title: '' });

  const showAlert = useCallback((options: AlertOptions) => {
    setOpts(options);
    setVisible(true);
  }, []);

  const handlePress = (btn?: AlertButton) => {
    setVisible(false);
    btn?.onPress?.();
  };

  const close = () => setVisible(false);

  /* Derive icon from title if not provided */
  const resolveIcon = (): { name: IoniconsName; color: string } => {
    if (opts.icon) return { name: opts.icon, color: opts.iconColor ?? Colors.primary };
    const t = opts.title.toLowerCase();
    if (t.includes('error') || t.includes('denegado') || t.includes('inválid'))
      return { name: 'alert-circle', color: Colors.error };
    if (t.includes('éxito') || t.includes('listo') || t.includes('publicado') || t.includes('enviado') || t.includes('cancelado') || t.includes('actualizado'))
      return { name: 'checkmark-circle', color: Colors.success };
    if (t.includes('eliminar') || t.includes('cerrar'))
      return { name: 'warning', color: Colors.error };
    if (t.includes('próximamente'))
      return { name: 'time', color: Colors.warning };
    return { name: 'information-circle', color: Colors.primary };
  };

  const icon = resolveIcon();
  const buttons = opts.buttons && opts.buttons.length > 0 ? opts.buttons : [{ text: 'Aceptar' }];

  return (
    <AlertContext.Provider value={{ showAlert }}>
      {children}

      <Modal
        visible={visible}
        transparent
        animationType="fade"
        onRequestClose={close}
        statusBarTranslucent
      >
        <View style={styles.overlay}>
          <View style={[styles.card, { backgroundColor: colors.surface }]}>
            {/* Icon */}
            <View style={[styles.iconCircle, { backgroundColor: icon.color + '18' }]}>
              <Ionicons name={icon.name} size={36} color={icon.color} />
            </View>

            {/* Title */}
            <Text style={[styles.title, { color: colors.text }]}>{opts.title}</Text>

            {/* Message */}
            {opts.message ? (
              <Text style={[styles.message, { color: colors.textSecondary }]}>{opts.message}</Text>
            ) : null}

            {/* Buttons */}
            <View style={styles.buttonsRow}>
              {buttons.map((btn, i) => (
                <AlertButtonComponent key={i} btn={btn} index={i} total={buttons.length} colors={colors} handlePress={handlePress} />
              ))}
            </View>
          </View>
        </View>
      </Modal>
    </AlertContext.Provider>
  );
};

/* ── Estilos ── */

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
  },
  card: {
    width: '100%',
    maxWidth: 340,
    borderRadius: BorderRadius.xl,
    padding: Spacing.xl,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.15,
    shadowRadius: 24,
    elevation: 12,
  },
  iconCircle: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.md,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
    textAlign: 'center',
    marginBottom: Spacing.xs,
  },
  message: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: Spacing.lg,
  },
  buttonsRow: {
    flexDirection: 'row',
    gap: Spacing.sm,
    marginTop: Spacing.sm,
    width: '100%',
  },
  btn: {
    flex: 1,
    paddingVertical: 12,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
    justifyContent: 'center',
  },
  btnText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
    textAlign: 'center',
  },
});
