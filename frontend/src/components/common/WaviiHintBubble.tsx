import React, { useEffect, useRef } from 'react';
import { Animated, Image, Pressable, StyleSheet, Text, View } from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';

interface WaviiHintBubbleProps {
  message: string;
  visible: boolean;
  onDismiss?: () => void;
}

export const WaviiHintBubble: React.FC<WaviiHintBubbleProps> = ({ message, visible, onDismiss }) => {
  const { colors } = useTheme();
  const opacity = useRef(new Animated.Value(0)).current;
  const hasBeenVisible = useRef(false);

  useEffect(() => {
    if (visible) hasBeenVisible.current = true;
    Animated.timing(opacity, {
      toValue: visible ? 1 : 0,
      duration: 260,
      useNativeDriver: true,
    }).start();
  }, [visible, opacity]);

  useEffect(() => {
    if (!visible) return;
    const timer = setTimeout(() => {
      onDismiss?.();
    }, 5000);
    return () => clearTimeout(timer);
  }, [visible, message, onDismiss]);

  if (!visible && !hasBeenVisible.current) return null;

  return (
    <Animated.View style={[styles.wrapper, { opacity }]}>
      <Pressable
        style={[styles.bubble, { backgroundColor: colors.surface, borderColor: colors.border }]}
        onPress={onDismiss}
      >
        <View style={styles.avatar}>
          <Image source={require('../../../assets/wavii/wavii_bienvenida.png')} style={styles.avatarImage} />
        </View>
        <Text style={[styles.message, { color: colors.text }]}>{message}</Text>
      </Pressable>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    position: 'absolute',
    bottom: 16,
    left: Spacing.base,
    right: Spacing.base,
    zIndex: 100,
  },
  bubble: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 6,
    elevation: 3,
  },
  avatar: {
    width: 36,
    height: 36,
    borderRadius: 18,
    overflow: 'hidden',
    flexShrink: 0,
  },
  avatarImage: {
    width: 36,
    height: 36,
    borderRadius: 18,
  },
  message: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
});
