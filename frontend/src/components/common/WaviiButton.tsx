import React from 'react';
import {
  TouchableOpacity,
  Text,
  StyleSheet,
  ActivityIndicator,
  ViewStyle,
  TextStyle,
  StyleProp,
} from 'react-native';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, BorderRadius, Spacing } from '../../theme';

interface WaviiButtonProps {
  title: string;
  onPress: () => void;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
  fullWidth?: boolean;
  activeOpacity?: number;
}

export const WaviiButton: React.FC<WaviiButtonProps> = ({
  title,
  onPress,
  variant = 'primary',
  size = 'lg',
  loading = false,
  disabled = false,
  style,
  textStyle,
  fullWidth = true,
  activeOpacity = 0.8,
}) => {
  const { colors } = useTheme();
  const isDisabled = disabled || loading;

  const buttonVariantStyles: Record<NonNullable<WaviiButtonProps['variant']>, ViewStyle> = {
    primary: styles.primary,
    secondary: styles.secondary,
    outline: styles.outline,
    ghost: styles.ghost,
    danger: styles.danger,
  };

  const buttonSizeStyles: Record<NonNullable<WaviiButtonProps['size']>, ViewStyle> = {
    sm: styles.size_sm,
    md: styles.size_md,
    lg: styles.size_lg,
  };

  const textVariantStyles: Record<NonNullable<WaviiButtonProps['variant']>, TextStyle> = {
    primary: styles.text_primary,
    secondary: styles.text_secondary,
    outline: { ...styles.text_outline, color: colors.text },
    ghost: styles.text_ghost,
    danger: styles.text_danger,
  };

  const textSizeStyles: Record<NonNullable<WaviiButtonProps['size']>, TextStyle> = {
    sm: styles.textSize_sm,
    md: styles.textSize_md,
    lg: styles.textSize_lg,
  };

  const getIndicatorColor = () => {
    if (variant === 'primary' || variant === 'danger') return Colors.textLight;
    return Colors.primary;
  };

  return (
    <TouchableOpacity
      onPress={onPress}
      disabled={isDisabled}
      activeOpacity={activeOpacity}
      style={[
        styles.base,
        buttonVariantStyles[variant],
        buttonSizeStyles[size],
        fullWidth && styles.fullWidth,
        isDisabled && styles.disabled,
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={getIndicatorColor()} size="small" />
      ) : (
        <Text
          style={[
            styles.text,
            textVariantStyles[variant],
            textSizeStyles[size],
            textStyle,
          ]}
        >
          {title}
        </Text>
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  base: {
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  fullWidth: {
    width: '100%',
  },

  // Variantes
  primary: {
    backgroundColor: Colors.primary,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  secondary: {
    backgroundColor: Colors.white,
    borderWidth: 1.5,
    borderColor: Colors.primary,
  },
  outline: {
    backgroundColor: Colors.transparent,
    borderWidth: 1.5,
    borderColor: Colors.border,
  },
  ghost: {
    backgroundColor: Colors.transparent,
  },
  danger: {
    backgroundColor: Colors.error,
    shadowColor: Colors.error,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  disabled: {
    opacity: 0.45,
  },

  // Tamaños
  size_sm: { height: 40, paddingHorizontal: Spacing.md },
  size_md: { height: 48, paddingHorizontal: Spacing.lg },
  size_lg: { height: 56, paddingHorizontal: Spacing.xl },

  // Texto base
  text: {
    fontFamily: FontFamily.extraBold,
    letterSpacing: 0.3,
  },
  text_primary: { color: Colors.textLight },
  text_secondary: { color: Colors.primary },
  text_outline: { color: Colors.textPrimary },
  text_ghost: { color: Colors.primary },
  text_danger: { color: Colors.textLight },

  textSize_sm: { fontSize: FontSize.sm },
  textSize_md: { fontSize: FontSize.base },
  textSize_lg: { fontSize: FontSize.md },
});
