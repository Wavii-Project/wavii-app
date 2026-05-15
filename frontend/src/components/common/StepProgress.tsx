import React from 'react';
import { View, StyleSheet } from 'react-native';
import { Colors } from '../../theme';

interface StepProgressProps {
  totalSteps: number;
  currentStep: number;
  color?: string;
}

export const StepProgress: React.FC<StepProgressProps> = ({
  totalSteps,
  currentStep,
  color = Colors.primary,
}) => {
  return (
    <View style={styles.container}>
      {Array.from({ length: totalSteps }).map((_, i) => (
        <View
          key={i}
          style={[
            styles.segment,
            {
              backgroundColor: i < currentStep ? color : 'rgba(0,0,0,0.12)',
              flex: 1,
            },
          ]}
        />
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    gap: 6,
    height: 4,
    borderRadius: 2,
    overflow: 'hidden',
  },
  segment: {
    height: 4,
    borderRadius: 2,
  },
});
