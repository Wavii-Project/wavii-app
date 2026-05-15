import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { AppStackParamList } from '../../navigation/AppNavigator';
import { useTheme } from '../../context/ThemeContext';
import { useAuth } from '../../context/AuthContext';
import { NotificationBell } from '../../components/common/NotificationBell';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

type IoniconsName = React.ComponentProps<typeof Ionicons>['name'];

interface MenuOption {
  title: string;
  icon: IoniconsName;
  route?: keyof AppStackParamList;
  targetTab?: 'Inicio' | 'Bandas' | 'Tablaturas' | 'Menu';
  color: string;
}

const MENU_OPTIONS: MenuOption[] = [
  { title: 'Perfil', icon: 'person', route: 'Profile', color: Colors.primary },
  { title: 'Social', icon: 'people', route: 'Social', color: Colors.levelPurple },
  { title: 'Tablon de anuncios', icon: 'school', route: 'BulletinBoard', color: Colors.success },
  { title: 'Desafios', icon: 'trophy', route: 'Challenges', color: Colors.streakOrange },
  { title: 'Noticias', icon: 'newspaper', route: 'News', color: Colors.warning },
];

export const MenuScreen: React.FC = () => {
  const { colors } = useTheme();
  const { user } = useAuth();
  const navigation = useNavigation<NativeStackNavigationProp<AppStackParamList>>();
  const hasScholarAccess = user?.subscription?.toLowerCase() === 'education';

  const options: MenuOption[] = [
    ...MENU_OPTIONS,
    {
      title: 'Mis clases',
      icon: hasScholarAccess ? 'calendar-outline' : 'lock-closed-outline',
      route: 'ManageClasses',
      color: hasScholarAccess ? Colors.primary : Colors.textSecondary,
    },
  ];

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <View style={styles.header}>
        <View style={{ flex: 1 }}>
          <Text style={[styles.title, { color: colors.text }]}>Explorar</Text>
          <Text style={[styles.subtitle, { color: colors.textSecondary }]}>
            Descubre todo lo que Wavii tiene para ofrecerte
          </Text>
        </View>
        <NotificationBell />
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.grid}>
          {options.map((option) => (
            <TouchableOpacity
              key={option.title}
              style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}
              onPress={() => {
                if (option.targetTab) {
                  (navigation as any).navigate('MainTabs', { screen: option.targetTab });
                  return;
                }
                if (option.route) {
                  (navigation as any).navigate(option.route);
                }
              }}
              activeOpacity={0.7}
            >
              <View style={[styles.iconWrapper, { backgroundColor: option.color + '20' }]}>
                <Ionicons name={option.icon} size={32} color={option.color} />
              </View>
              <Text style={[styles.cardTitle, { color: colors.text }]}>{option.title}</Text>
              <Ionicons name="chevron-forward" size={18} color={colors.textSecondary} style={styles.arrow} />
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.base,
    paddingHorizontal: Spacing.base,
    paddingTop: Spacing.sm,
    paddingBottom: Spacing.sm,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize['2xl'],
  },
  subtitle: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.xs,
    marginTop: 2,
  },
  scroll: {
    paddingHorizontal: Spacing.base,
    paddingBottom: Spacing.xl,
  },
  grid: {
    gap: Spacing.sm,
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.base,
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
  },
  iconWrapper: {
    width: 56,
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: Spacing.md,
  },
  cardTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.lg,
    flex: 1,
  },
  arrow: {
    marginLeft: Spacing.xs,
  },
});
