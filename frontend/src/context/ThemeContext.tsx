import React, { createContext, useContext, useState, useEffect } from 'react';
import { useColorScheme } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Colors } from '../theme/colors';

type ThemeMode = 'light' | 'dark' | 'system';

interface ThemeContextType {
  isDark: boolean;
  themeMode: ThemeMode;
  setThemeMode: (mode: ThemeMode) => void;
  colors: ThemeColors;
}

interface ThemeColors {
  background: string;
  surface: string;
  text: string;
  textSecondary: string;
  border: string;
  card: string;
  primary: string;
  primaryLight: string;
}

const lightTheme: ThemeColors = {
  background: Colors.backgroundLight,
  surface: Colors.surface,
  text: Colors.textPrimary,
  textSecondary: Colors.textSecondary,
  border: Colors.border,
  card: Colors.surface,
  primary: Colors.primary,
  primaryLight: Colors.primaryLight,
};

const darkTheme: ThemeColors = {
  background: Colors.backgroundDark,
  surface: Colors.surfaceDark,
  text: Colors.textLight,
  textSecondary: Colors.grayLight,
  border: Colors.borderDark,
  card: Colors.surfaceDark,
  primary: Colors.primary,
  primaryLight: Colors.primaryLight,
};

const ThemeContext = createContext<ThemeContextType>({
  isDark: false,
  themeMode: 'system',
  setThemeMode: () => {},
  colors: lightTheme,
});

const THEME_STORAGE_KEY = '@wavii_theme';

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const systemScheme = useColorScheme();
  const [themeMode, setThemeModeState] = useState<ThemeMode>('system');

  useEffect(() => {
    AsyncStorage.getItem(THEME_STORAGE_KEY).then((stored) => {
      if (stored === 'light' || stored === 'dark' || stored === 'system') {
        setThemeModeState(stored);
      }
    });
  }, []);

  const setThemeMode = async (mode: ThemeMode) => {
    setThemeModeState(mode);
    await AsyncStorage.setItem(THEME_STORAGE_KEY, mode);
  };

  const isDark =
    themeMode === 'dark' || (themeMode === 'system' && systemScheme === 'dark');

  return (
    <ThemeContext.Provider
      value={{
        isDark,
        themeMode,
        setThemeMode,
        colors: isDark ? darkTheme : lightTheme,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => useContext(ThemeContext);
