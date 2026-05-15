import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Platform, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useTheme } from '../context/ThemeContext';
import { Colors, FontFamily } from '../theme';
import { HomeScreen } from '../screens/home/HomeScreen';
import { BandsScreen } from '../screens/bands/BandsScreen';
import { TabsScreen } from '../screens/tabs/TabsScreen';
import { MenuScreen } from '../screens/menu/MenuScreen';
import { ClassesScreen } from '../screens/classes/ClassesScreen';
import type { ClassEnrollment } from '../api/classesApi';

export type MainTabParamList = {
  Inicio: undefined;
  Clases:
    | {
        refreshKey?: string;
        justRequestedClass?: ClassEnrollment;
      }
    | undefined;
  Bandas: undefined;
  Tablaturas: undefined;
  Menu: undefined;
};

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

const TAB_ICONS: Record<keyof MainTabParamList, { active: IoniconsName; inactive: IoniconsName }> = {
  Inicio: { active: 'home', inactive: 'home-outline' },
  Clases: { active: 'school', inactive: 'school-outline' },
  Bandas: { active: 'people', inactive: 'people-outline' },
  Tablaturas: { active: 'book', inactive: 'book-outline' },
  Menu: { active: 'menu', inactive: 'menu-outline' },
};

const Tab = createBottomTabNavigator<MainTabParamList>();

export const MainNavigator = () => {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarIcon: ({ focused, size }) => {
          const icons = TAB_ICONS[route.name as keyof MainTabParamList];
          return (
            <Ionicons
              name={focused ? icons.active : icons.inactive}
              size={size ?? 22}
              color={focused ? Colors.primary : colors.textSecondary}
            />
          );
        },
        tabBarLabel: ({ focused }) => (
          <Text
            style={{
              fontFamily: FontFamily.bold,
              fontSize: 10,
              color: focused ? Colors.primary : colors.textSecondary,
              marginBottom: 2,
            }}
          >
            {route.name}
          </Text>
        ),
        tabBarStyle: {
          backgroundColor: colors.surface,
          borderTopColor: colors.border,
          borderTopWidth: 1,
          height: (Platform.OS === 'android' ? 64 : 60) + insets.bottom,
          paddingBottom: Math.max(insets.bottom, 4),
          paddingTop: 4,
        },
        tabBarActiveTintColor: Colors.primary,
        tabBarInactiveTintColor: colors.textSecondary,
      })}
    >
      <Tab.Screen name="Inicio" component={HomeScreen} />
      <Tab.Screen name="Clases" component={ClassesScreen} />
      <Tab.Screen name="Bandas" component={BandsScreen} />
      <Tab.Screen name="Tablaturas" component={TabsScreen} />
      <Tab.Screen name="Menu" component={MenuScreen} />
    </Tab.Navigator>
  );
};
