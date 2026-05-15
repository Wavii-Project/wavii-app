import React from 'react';
import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../../theme';
import { useTheme } from '../../context/ThemeContext';

type Tone = 'primary' | 'education' | 'success' | 'neutral';

interface WaviiPromoBannerProps {
  title: string;
  body: string;
  icon: React.ComponentProps<typeof Ionicons>['name'];
  tone?: Tone;
  ctaLabel?: string;
  onPress?: () => void;
}

const TONE_STYLES: Record<Tone, { backgroundColor: string; borderColor: string; iconColor: string; buttonColor: string }> = {
  primary: {
    backgroundColor: Colors.primaryOpacity10,
    borderColor: Colors.primaryOpacity20,
    iconColor: Colors.primary,
    buttonColor: Colors.primary,
  },
  education: {
    backgroundColor: 'rgba(124,58,237,0.10)',
    borderColor: 'rgba(124,58,237,0.24)',
    iconColor: Colors.educationTier,
    buttonColor: Colors.educationTier,
  },
  success: {
    backgroundColor: Colors.successLight,
    borderColor: 'rgba(34,197,94,0.24)',
    iconColor: Colors.success,
    buttonColor: Colors.success,
  },
  neutral: {
    backgroundColor: 'rgba(107,114,128,0.10)',
    borderColor: 'rgba(107,114,128,0.18)',
    iconColor: Colors.freeTier,
    buttonColor: Colors.freeTier,
  },
};

export const WaviiPromoBanner: React.FC<WaviiPromoBannerProps> = ({
  title,
  body,
  icon,
  tone = 'primary',
  ctaLabel,
  onPress,
}) => {
  const { colors } = useTheme();
  const toneStyle = TONE_STYLES[tone];

  return (
    <View
      style={[
        styles.card,
        {
          backgroundColor: toneStyle.backgroundColor,
          borderColor: toneStyle.borderColor,
        },
      ]}
    >
      <View style={[styles.iconWrap, { backgroundColor: `${toneStyle.iconColor}18` }]}>
        <Ionicons name={icon} size={18} color={toneStyle.iconColor} />
      </View>

      <View style={styles.bodyWrap}>
        <Text style={[styles.title, { color: colors.text }]}>{title}</Text>
        <Text style={[styles.body, { color: colors.textSecondary }]}>{body}</Text>
        {ctaLabel && onPress ? (
          <TouchableOpacity
            style={[styles.button, { backgroundColor: toneStyle.buttonColor }]}
            onPress={onPress}
            activeOpacity={0.85}
          >
            <Text style={styles.buttonText}>{ctaLabel}</Text>
            <Ionicons name="arrow-forward" size={14} color={Colors.white} />
          </TouchableOpacity>
        ) : null}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    gap: Spacing.sm,
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
  },
  iconWrap: {
    width: 38,
    height: 38,
    borderRadius: BorderRadius.full,
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
  },
  bodyWrap: {
    flex: 1,
    gap: 6,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.sm,
  },
  body: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  button: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginTop: 2,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.md,
    paddingVertical: 8,
  },
  buttonText: {
    color: Colors.white,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
});
