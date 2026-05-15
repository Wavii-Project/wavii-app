import React from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Colors } from '../../theme';

export const LoadingScreen = () => (
  <SafeAreaView style={styles.safe}>
    <View style={styles.container}>
      <ActivityIndicator size="large" color={Colors.primary} />
    </View>
  </SafeAreaView>
);

const styles = StyleSheet.create({
  safe: {
    flex: 1,

  },
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
