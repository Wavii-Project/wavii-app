import React from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../context/ThemeContext';
import { BorderRadius, FontFamily, FontSize, Spacing } from '../../theme';

interface ScreenHeaderProps {
  title: string;
  onBack: () => void;
  rightElement?: React.ReactNode;
}

export const ScreenHeader: React.FC<ScreenHeaderProps> = ({ title, onBack, rightElement }) => {
  const { colors } = useTheme();

  return (
    <View style={[styles.header, { borderBottomColor: colors.border }]}>
      <Pressable onPress={onBack} style={styles.backBtn} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
        <Ionicons name="arrow-back" size={22} color={colors.text} />
      </Pressable>
      <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>{title}</Text>
      <View style={styles.right}>{rightElement ?? null}</View>
    </View>
  );
};

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.base,
    paddingVertical: 12,
    borderBottomWidth: 1,
    gap: Spacing.sm,
  },
  backBtn: {
    width: 32,
    alignItems: 'flex-start',
  },
  title: {
    flex: 1,
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.base,
    textAlign: 'center',
  },
  right: {
    width: 32,
    alignItems: 'flex-end',
  },
});
