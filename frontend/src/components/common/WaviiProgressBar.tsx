import React, { useEffect, useRef } from 'react';
import { View, StyleSheet, Animated } from 'react-native';
import { Colors, BorderRadius } from '../../theme';

interface WaviiProgressBarProps {
  progress: number; // 0 to 1
  color?: string;
  height?: number;
  animated?: boolean;
}

export const WaviiProgressBar: React.FC<WaviiProgressBarProps> = ({
  progress,
  color = Colors.primary,
  height = 8,
  animated = true,
}) => {
  const animatedWidth = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (animated) {
      Animated.timing(animatedWidth, {
        toValue: Math.min(Math.max(progress, 0), 1),
        duration: 400,
        useNativeDriver: false,
      }).start();
    } else {
      animatedWidth.setValue(Math.min(Math.max(progress, 0), 1));
    }
  }, [progress, animated]);

  return (
    <View style={[styles.track, { height, borderRadius: height / 2 }]}>
      <Animated.View
        style={[
          styles.fill,
          {
            height,
            borderRadius: height / 2,
            backgroundColor: color,
            width: animatedWidth.interpolate({
              inputRange: [0, 1],
              outputRange: ['0%', '100%'],
            }),
          },
        ]}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  track: {
    width: '100%',
    backgroundColor: Colors.border,
    overflow: 'hidden',
  },
  fill: {
    position: 'absolute',
    left: 0,
    top: 0,
  },
});
