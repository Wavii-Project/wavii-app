import React, { useState } from 'react';
import {
  View,
  TextInput,
  Text,
  TouchableOpacity,
  StyleSheet,
  ViewStyle,
  TextInputProps,
  KeyboardTypeOptions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { useTheme } from '../../context/ThemeContext';

interface WaviiInputProps extends Omit<TextInputProps, 'style'> {
  label?: string;
  placeholder?: string;
  value?: string;
  onChangeText?: (text: string) => void;
  isPassword?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  error?: string;
  hint?: string;
  keyboardType?: KeyboardTypeOptions;
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  autoComplete?: TextInputProps['autoComplete'];
  containerStyle?: ViewStyle;
}

export const WaviiInput: React.FC<WaviiInputProps> = ({
  label,
  placeholder,
  value,
  onChangeText,
  isPassword = false,
  leftIcon,
  rightIcon,
  error,
  hint,
  keyboardType,
  autoCapitalize,
  autoComplete,
  containerStyle,
  ...rest
}) => {
  const { colors } = useTheme();
  const [showPassword, setShowPassword] = useState(false);
  const [isFocused, setIsFocused] = useState(false);

  const isMultiline = !!rest.multiline;

  return (
    <View style={[styles.container, containerStyle]}>
      {label && (
        <Text style={[styles.label, { color: colors.text }]}>{label}</Text>
      )}
      <View
        style={[
          styles.inputWrapper,
          isMultiline ? styles.inputWrapperMultiline : null,
          {
            backgroundColor: colors.surface,
            borderColor: error
              ? Colors.error
              : isFocused
              ? Colors.primary
              : colors.border,
          },
        ]}
      >
        {leftIcon && (
          <View style={styles.iconLeft}>{leftIcon}</View>
        )}

        <TextInput
          {...rest}
          placeholder={placeholder}
          value={value}
          onChangeText={onChangeText}
          keyboardType={keyboardType}
          autoCapitalize={autoCapitalize}
          autoComplete={autoComplete}
          secureTextEntry={isPassword && !showPassword}
          onFocus={(e) => { setIsFocused(true); rest.onFocus?.(e); }}
          onBlur={(e) => { setIsFocused(false); rest.onBlur?.(e); }}
          style={[
            styles.input,
            isMultiline ? styles.inputMultiline : null,
            { color: colors.text },
            leftIcon ? styles.inputWithLeftIcon : null,
            (rightIcon || isPassword) ? styles.inputWithRightIcon : null,
          ]}
          placeholderTextColor={colors.textSecondary}
        />

        {isPassword && (
          <TouchableOpacity
            onPress={() => setShowPassword((v) => !v)}
            style={styles.iconRight}
            hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
          >
            <Ionicons
              name={showPassword ? 'eye-outline' : 'eye-off-outline'}
              size={20}
              color={colors.textSecondary}
            />
          </TouchableOpacity>
        )}

        {!isPassword && rightIcon && (
          <View style={styles.iconRight}>{rightIcon}</View>
        )}
      </View>

      {error && (
        <Text style={styles.error}>{error}</Text>
      )}
      {hint && !error && (
        <Text style={[styles.hint, { color: colors.textSecondary }]}>{hint}</Text>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginBottom: Spacing.base,
  },
  label: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    marginBottom: Spacing.xs,
  },
  inputWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderRadius: BorderRadius.lg,
    height: 52,
  },
  inputWrapperMultiline: {
    minHeight: 120,
    height: 'auto',
    alignItems: 'flex-start',
  },
  input: {
    flex: 1,
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    paddingHorizontal: Spacing.md,
    height: '100%',
  },
  inputMultiline: {
    minHeight: 100,
    textAlignVertical: 'top',
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.sm,
  },
  inputWithLeftIcon: {
    paddingLeft: Spacing.xs,
  },
  inputWithRightIcon: {
    paddingRight: Spacing.xs,
  },
  iconLeft: {
    paddingLeft: Spacing.md,
    justifyContent: 'center',
    alignItems: 'center',
  },
  iconRight: {
    paddingRight: Spacing.md,
    justifyContent: 'center',
    alignItems: 'center',
  },
  error: {
    fontFamily: FontFamily.medium,
    fontSize: FontSize.xs,
    color: Colors.error,
    marginTop: 4,
  },
  hint: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginTop: 4,
  },
});
