import React, { useCallback } from 'react';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { useFonts } from 'expo-font';
import {
  Nunito_400Regular,
  Nunito_500Medium,
  Nunito_600SemiBold,
  Nunito_700Bold,
  Nunito_800ExtraBold,
  Nunito_900Black,
} from '@expo-google-fonts/nunito';
import * as SplashScreen from 'expo-splash-screen';
import { View } from 'react-native';
import { StripeProvider } from '@stripe/stripe-react-native';
import { ThemeProvider, useTheme } from './src/context/ThemeContext';
import { AuthProvider } from './src/context/AuthContext';
import { AlertProvider } from './src/context/AlertContext';
import { TutorialProvider } from './src/context/TutorialContext';
import { RootNavigator } from './src/navigation/RootNavigator';
import { STRIPE_PUBLISHABLE_KEY } from './src/config/stripe';

// Mantener el splash screen visible mientras cargan las fuentes
SplashScreen.preventAutoHideAsync();

const AppContent = () => {
  const { isDark } = useTheme();
  return (
    <>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      <NavigationContainer>
        <RootNavigator />
      </NavigationContainer>
    </>
  );
};

export default function App() {
  const [fontsLoaded, fontError] = useFonts({
    Nunito_400Regular,
    Nunito_500Medium,
    Nunito_600SemiBold,
    Nunito_700Bold,
    Nunito_800ExtraBold,
    Nunito_900Black,
  });

  const onLayoutRootView = useCallback(async () => {
    if (fontsLoaded || fontError) {
      await SplashScreen.hideAsync();
    }
  }, [fontsLoaded, fontError]);

  if (!fontsLoaded && !fontError) {
    return null;
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <StripeProvider publishableKey={STRIPE_PUBLISHABLE_KEY}>
          <View style={{ flex: 1 }} onLayout={onLayoutRootView}>
            <ThemeProvider>
              <AlertProvider>
                <AuthProvider>
                  <TutorialProvider>
                    <AppContent />
                  </TutorialProvider>
                </AuthProvider>
              </AlertProvider>
            </ThemeProvider>
          </View>
        </StripeProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
