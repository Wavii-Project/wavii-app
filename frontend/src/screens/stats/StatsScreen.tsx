import React, { useState, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  TouchableOpacity,
  Image,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { DateTimePickerAndroid } from '@react-native-community/datetimepicker';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import { isBackendSessionToken } from '../../auth/session';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';
import { apiFetchStats, StatsDto } from '../../api/challengeApi';

const MONTH_NAMES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
];
const DAY_LABELS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

/** Calcula el nivel numerico a partir del XP total */
function calculateLevelFromXp(xp: number): number {
  let level = 1;
  let accumulated = 0;
  while (true) {
    const needed = Math.round(100 * Math.pow(level, 1.5));
    if (accumulated + needed > xp) break;
    accumulated += needed;
    level++;
  }
  return level;
}

/** Construye la cuadricula del mes: array de celdas donde cada celda es una fecha ISO o null (padding) */
function buildCalendarGrid(year: number, month: number): (string | null)[] {
  const firstDay = new Date(year, month, 1);
  // getDay() devuelve 0=dom, 1=lun... Necesitamos lunes=0
  const startOffset = (firstDay.getDay() + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const cells: (string | null)[] = Array(startOffset).fill(null);
  for (let d = 1; d <= daysInMonth; d++) {
    const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    cells.push(iso);
  }
  return cells;
}

export const StatsScreen = () => {
  const { user, token } = useAuth();
  const { colors } = useTheme();
  const navigation = useNavigation<any>();
  const hasBackendSession = isBackendSessionToken(token);

  const [stats, setStats] = useState<StatsDto | null>(null);
  const [loading, setLoading] = useState(true);

  // Mes que se esta mostrando en el calendario
  const today = new Date();
  const [calYear, setCalYear]  = useState(today.getFullYear());
  const [calMonth, setCalMonth] = useState(today.getMonth()); // 0-based

  const load = useCallback(async () => {
    if (!hasBackendSession || !token) {
      setStats({
        streak: user?.streak ?? 0,
        bestStreak: user?.bestStreak ?? 0,
        xp: user?.xp ?? 0,
        level: calculateLevelFromXp(user?.xp ?? 0),
        completedDaysThisMonth: [],
        completedThisWeek: 0,
      });
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const data = await apiFetchStats();
      setStats(data);
    } catch (e) {
      console.warn('Error cargando estadisticas:', e);
    } finally {
      setLoading(false);
    }
  }, [hasBackendSession, token, user?.bestStreak, user?.streak, user?.xp]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load])
  );

  const completedSet = new Set(stats?.completedDaysThisMonth ?? []);
  const calendarCells = buildCalendarGrid(calYear, calMonth);
  const todayIso = today.toISOString().slice(0, 10);

  const goToPrevMonth = () => {
    if (calMonth === 0) { setCalYear(y => y - 1); setCalMonth(11); }
    else setCalMonth(m => m - 1);
  };
  const goToNextMonth = () => {
    if (calMonth === 11) { setCalYear(y => y + 1); setCalMonth(0); }
    else setCalMonth(m => m + 1);
  };

  const openNativeMonthYearPicker = () => {
    const seedDate = new Date(calYear, calMonth, 1);
    DateTimePickerAndroid.open({
      value: seedDate,
      mode: 'date',
      is24Hour: true,
      onChange: (_, date) => {
        if (!date) return;
        setCalYear(date.getFullYear());
        setCalMonth(date.getMonth());
      },
    });
  };

  const currentLevel = stats ? calculateLevelFromXp(stats.xp) : 1;

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top']}>
      <View style={[styles.topBar, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.topBarTitle, { color: colors.text }]}>Estadisticas</Text>
        <View style={{ width: 26 }} />
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        {loading || !stats ? (
          <ActivityIndicator color={Colors.primary} style={{ marginTop: Spacing['2xl'] }} />
        ) : (
          <>
            {/* ── Chips de resumen ── */}
            <View style={styles.chipsRow}>
              <StatChip
                icon="flame"
                iconColor={Colors.streakOrange}
                label="Racha actual"
                value={String(stats.streak)}
                colors={colors}
              />
              <StatChip
                icon="trophy"
                iconColor={Colors.warning}
                label="Mejor racha"
                value={String(stats.bestStreak)}
                colors={colors}
              />
              <StatChip
                icon="star"
                iconColor={Colors.primary}
                label={`Nivel ${currentLevel}`}
                value={`${stats.xp.toLocaleString()} XP`}
                colors={colors}
              />
            </View>

            {/* ── Racha textual ── */}
            <View style={[styles.streakBanner, { backgroundColor: colors.surface }]}>
              <Image
                source={require('../../../assets/wavii/wavii_bienvenida.png')}
                style={styles.streakWavii}
                resizeMode="contain"
              />
              <View style={{ flex: 1 }}>
                <Text style={[styles.streakTitle, { color: colors.text }]}>
                  {stats.streak === 0
                    ? 'Empieza tu racha hoy'
                    : stats.streak === 1
                      ? '1 dia de racha'
                      : `${stats.streak} dias de racha`}
                </Text>
                <Text style={[styles.streakSub, { color: colors.textSecondary }]}>
                  {stats.bestStreak > 0
                    ? `Tu record es de ${stats.bestStreak} ${stats.bestStreak === 1 ? 'dia' : 'dias'}`
                    : 'Completa un desafio para empezar'}
                </Text>
              </View>
            </View>

            {/* ── Calendario ── */}
            <View style={[styles.calCard, { backgroundColor: colors.surface }]}>
              <View style={styles.calHeader}>
                <TouchableOpacity onPress={goToPrevMonth} hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}>
                  <Ionicons name="chevron-back" size={22} color={colors.text} />
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.calMonthPicker}
                  onPress={openNativeMonthYearPicker}
                  activeOpacity={0.85}
                >
                  <Text style={[styles.calMonthTitle, { color: colors.text }]}>
                    {MONTH_NAMES[calMonth]} {calYear}
                  </Text>
                </TouchableOpacity>
                <TouchableOpacity
                  onPress={goToNextMonth}
                  disabled={calYear === today.getFullYear() && calMonth >= today.getMonth()}
                  hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
                >
                  <Ionicons
                    name="chevron-forward"
                    size={22}
                    color={
                      calYear === today.getFullYear() && calMonth >= today.getMonth()
                        ? colors.border
                        : colors.text
                    }
                  />
                </TouchableOpacity>
              </View>

              {/* Cabecera dias */}
              <View style={styles.calDaysRow}>
                {DAY_LABELS.map((d) => (
                  <Text key={d} style={[styles.calDayLabel, { color: colors.textSecondary }]}>{d}</Text>
                ))}
              </View>

              {/* Cuadricula */}
              <View style={styles.calGrid}>
                {calendarCells.map((iso, idx) => {
                  if (!iso) return <View key={`pad-${idx}`} style={styles.calCell} />;
                  const isCompleted = completedSet.has(iso);
                  const isToday     = iso === todayIso;
                  const day         = Number(iso.slice(8));
                  return (
                    <View key={iso} style={styles.calCell}>
                      <View
                        style={[
                          styles.calCellInner,
                          isCompleted && styles.calCellInnerCompleted,
                          isToday && !isCompleted && styles.calCellInnerToday,
                        ]}
                      >
                        <Text
                          style={[
                            styles.calCellText,
                            isCompleted ? styles.calCellTextCompleted : { color: colors.text },
                            isToday && !isCompleted && { color: Colors.primary, fontFamily: FontFamily.bold },
                          ]}
                        >
                          {day}
                        </Text>
                      </View>
                    </View>
                  );
                })}
              </View>

              <View style={styles.calLegend}>
                <View style={[styles.calLegendDot, { backgroundColor: Colors.success }]} />
                <Text style={[styles.calLegendText, { color: colors.textSecondary }]}>
                  Dia completado
                </Text>
              </View>
            </View>

            {/* ── Progreso semanal ── */}
            <View style={[styles.weekCard, { backgroundColor: colors.surface }]}>
              <Text style={[styles.weekCardTitle, { color: colors.text }]}>Esta semana</Text>
              <Text style={[styles.weekCardValue, { color: Colors.primary }]}>
                {stats.completedThisWeek}/7 dias
              </Text>
              <Text style={[styles.weekCardSub, { color: colors.textSecondary }]}>
                {stats.completedThisWeek === 0
                  ? 'Completa al menos un desafio hoy'
                  : stats.completedThisWeek === 7
                    ? '¡Semana perfecta!'
                    : `Te quedan ${7 - stats.completedThisWeek} dias para la semana perfecta`}
              </Text>
            </View>
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
};

const StatChip = ({
  icon,
  iconColor,
  label,
  value,
  colors,
}: {
  icon: React.ComponentProps<typeof Ionicons>['name'];
  iconColor: string;
  label: string;
  value: string;
  colors: any;
}) => (
  <View style={[styles.chip, { backgroundColor: colors.surface }]}>
    <Ionicons name={icon} size={20} color={iconColor} />
    <Text style={[styles.chipValue, { color: colors.text }]}>{value}</Text>
    <Text style={[styles.chipLabel, { color: colors.textSecondary }]}>{label}</Text>
  </View>
);

const styles = StyleSheet.create({
  safe:   { flex: 1 },
  scroll: { paddingHorizontal: Spacing.base, paddingBottom: Spacing['2xl'], gap: Spacing.base, paddingTop: Spacing.base },

  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  topBarTitle: { fontFamily: FontFamily.extraBold, fontSize: FontSize.lg },

  // Chips
  chipsRow: { flexDirection: 'row', gap: Spacing.sm },
  chip: {
    flex: 1,
    alignItems: 'center',
    borderRadius: BorderRadius.xl,
    padding: Spacing.sm,
    gap: 3,
  },
  chipValue: { fontFamily: FontFamily.black,   fontSize: FontSize.base },
  chipLabel: { fontFamily: FontFamily.regular, fontSize: 10, textAlign: 'center' },

  // Racha banner
  streakBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    overflow: 'hidden',
  },
  streakWavii: {
    width: 48,
    height: 48,
    flexShrink: 0,
  },
  streakTitle: { fontFamily: FontFamily.bold,    fontSize: FontSize.base },
  streakSub:   { fontFamily: FontFamily.regular, fontSize: FontSize.sm },

  // Calendario
  calCard: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  calHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: Spacing.xs,
  },
  calMonthTitle: { fontFamily: FontFamily.bold, fontSize: FontSize.base },
  calMonthPicker: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.xs,
    minHeight: 30,
  },
  calDaysRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 4,
  },
  calDayLabel: { fontFamily: FontFamily.semiBold, fontSize: FontSize.xs, width: 36, textAlign: 'center' },
  calGrid: { flexDirection: 'row', flexWrap: 'wrap' },
  calCell: {
    width: `${100 / 7}%`,
    aspectRatio: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  calCellInner: {
    width: '80%',
    aspectRatio: 1,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: BorderRadius.full,
  },
  calCellInnerCompleted: {
    backgroundColor: Colors.success,
  },
  calCellInnerToday: {
    borderWidth: 1.5,
    borderColor: Colors.primary,
  },
  calCellText: { fontFamily: FontFamily.medium, fontSize: FontSize.sm },
  calCellTextCompleted: { color: '#fff', fontFamily: FontFamily.bold },
  calLegend: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 4 },
  calLegendDot: { width: 10, height: 10, borderRadius: 5 },
  calLegendText: { fontFamily: FontFamily.regular, fontSize: FontSize.xs },

  // Semana
  weekCard: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.base,
    gap: 4,
  },
  weekCardTitle: { fontFamily: FontFamily.bold,    fontSize: FontSize.base },
  weekCardValue: { fontFamily: FontFamily.black,   fontSize: FontSize['2xl'] },
  weekCardSub:   { fontFamily: FontFamily.regular, fontSize: FontSize.sm },
});
