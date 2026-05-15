import React from 'react';
import { View, Text, Image, StyleSheet } from 'react-native';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

const WAVII = require('../../../assets/wavii/wavii_bienvenida.png');

const BORDER = '#DEDEDE';
const FILL = '#FFFFFF';
const TAIL = 16;
const OVERLAP = 8;
const BW = 1.5;

interface WaviiSpeechProps {
  text: string;
  mascotSize?: number;
  variant?: 'side' | 'above';
}

export const WaviiSpeech: React.FC<WaviiSpeechProps> = ({
  text,
  mascotSize,
  variant = 'side',
}) => {
  if (variant === 'above') {
    const size = mascotSize ?? 180;
    return (
      <View style={styles.aboveRoot}>
        {/* Gray bg + no bottom padding = border on 3 sides, open at bottom where tail connects */}
        <View style={styles.aboveBorderBox}>
          <View style={styles.aboveFill}>
            <Text style={styles.text}>{text}</Text>
          </View>
        </View>
        <View style={styles.tailDown} />
        <Image source={WAVII} style={{ width: size, height: size }} resizeMode="contain" />
      </View>
    );
  }

  // side variant
  const size = mascotSize ?? 130;
  return (
    <View style={styles.sideRoot}>
      <Image source={WAVII} style={{ width: size, height: size }} resizeMode="contain" />
      <View style={styles.sideContent}>
        <View style={styles.tailLeft} />
        {/* Gray bg + no left padding = border on 3 sides, open at left where tail connects */}
        <View style={styles.sideBorderBox}>
          <View style={styles.sideFill}>
            <Text style={styles.text}>{text}</Text>
          </View>
        </View>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  /* ── above variant ── */
  aboveRoot: { alignItems: 'center' },
  aboveBorderBox: {
    alignSelf: 'stretch',
    backgroundColor: BORDER,
    borderRadius: BorderRadius.xl,
    paddingTop: BW,
    paddingHorizontal: BW,
    paddingBottom: BW,
    zIndex: 1,
  },
  aboveFill: {
    backgroundColor: FILL,
    borderRadius: BorderRadius.xl - BW,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
  },
  tailDown: {
    width: TAIL,
    height: TAIL,
    backgroundColor: FILL,
    borderRightWidth: BW,
    borderBottomWidth: BW,
    borderColor: BORDER,
    transform: [{ rotate: '45deg' }],
    marginTop: -OVERLAP,
    zIndex: 2,
  },

  /* ── side variant ── */
  sideRoot: { flexDirection: 'row', alignItems: 'center' },
  sideContent: { flex: 1, flexDirection: 'row', alignItems: 'center' },
  tailLeft: {
    width: TAIL,
    height: TAIL,
    backgroundColor: FILL,
    borderBottomWidth: BW,
    borderLeftWidth: BW,
    borderColor: BORDER,
    transform: [{ rotate: '45deg' }],
    marginRight: -OVERLAP,
    zIndex: 1,
  },
  sideBorderBox: {
    flex: 1,
    backgroundColor: BORDER,
    borderRadius: BorderRadius.xl,
    paddingTop: BW,
    paddingRight: BW,
    paddingBottom: BW,
    paddingLeft: BW,
    zIndex: 0,
  },
  sideFill: {
    backgroundColor: FILL,
    borderRadius: BorderRadius.xl - BW,
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.md,
  },

  /* ── shared ── */
  text: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
    color: Colors.textPrimary,
    lineHeight: 22,
  },
});
