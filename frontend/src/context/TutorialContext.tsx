import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Image, Modal, ScrollView, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useAuth } from './AuthContext';
import { useAlert } from './AlertContext';
import { useTheme } from './ThemeContext';
import { BorderRadius, Colors, FontFamily, FontSize, Spacing } from '../theme';

const TUTORIAL_KEY = 'wavii_tutorial_completed_v1';

type TutorialStep = {
  title: string;
  body: string;
  image: any;
};

const STEPS: TutorialStep[] = [
  {
    title: 'Bienvenido a Wavii',
    body: 'Te acompañamos por lo esencial para que empieces rápido: inicio, desafíos, tablaturas, banda y perfil.',
    image: require('../../assets/wavii/wavii_bienvenida.png'),
  },
  {
    title: 'Inicio',
    body: 'Aquí verás tus desafíos diarios, tablaturas populares y noticias sin salir de la app.',
    image: require('../../assets/wavii/wavii_tablatura.png'),
  },
  {
    title: 'Desafíos',
    body: 'Completa tablaturas de tu nivel para ganar XP y mantener tu racha. El botón Hecho pide confirmación antes de contar.',
    image: require('../../assets/wavii/wavii_nivel.png'),
  },
  {
    title: 'Tablaturas y bandas',
    body: 'Sube tablaturas con portada y descripción, reporta contenido incorrecto y publica anuncios musicales con filtros claros.',
    image: require('../../assets/wavii/wavii_red_social.png'),
  },
  {
    title: 'Perfil',
    body: 'Desde aquí gestionas tu suscripción, repites este tutorial, cambias nombre o revisas tu cuenta.',
    image: require('../../assets/wavii/wavii_profesor.png'),
  },
];

type TutorialContextValue = {
  startTutorial: () => void;
  hasSeenTutorial: boolean;
};

const TutorialContext = createContext<TutorialContextValue>({
  startTutorial: () => {},
  hasSeenTutorial: false,
});

export const useTutorial = () => useContext(TutorialContext);

export const TutorialProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user } = useAuth();
  const { showAlert } = useAlert();
  const { colors } = useTheme();
  const [visible, setVisible] = useState(false);
  const [stepIndex, setStepIndex] = useState(0);
  const [hasSeenTutorial, setHasSeenTutorial] = useState(true);

  const loadFlag = useCallback(async () => {
    const value = await AsyncStorage.getItem(TUTORIAL_KEY);
    setHasSeenTutorial(value === '1');
    if (value !== '1' && user) {
      setStepIndex(0);
      setVisible(true);
    }
  }, [user]);

  useEffect(() => {
    loadFlag().catch(() => {});
  }, [loadFlag]);

  const markCompleted = useCallback(async () => {
    setVisible(false);
    setHasSeenTutorial(true);
    await AsyncStorage.setItem(TUTORIAL_KEY, '1');
  }, []);

  const startTutorial = useCallback(() => {
    setStepIndex(0);
    setVisible(true);
  }, []);

  const skipTutorial = useCallback(() => {
    showAlert({
      title: 'Salir del tutorial',
      message: 'Si lo saltas, podrás repetirlo más tarde desde el perfil.',
      buttons: [
        { text: 'Volver', style: 'cancel' },
        { text: 'Salir', style: 'destructive', onPress: markCompleted, delaySeconds: 10 },
      ],
    });
  }, [markCompleted, showAlert]);

  const currentStep = STEPS[stepIndex];
  const isLastStep = stepIndex === STEPS.length - 1;

  const value = useMemo(() => ({ startTutorial, hasSeenTutorial }), [startTutorial, hasSeenTutorial]);

  return (
    <TutorialContext.Provider value={value}>
      {children}

      <Modal visible={visible} transparent animationType="fade" onRequestClose={skipTutorial} statusBarTranslucent>
        <View style={styles.overlay}>
          <View style={[styles.card, { backgroundColor: colors.surface }]}>
            <TouchableOpacity style={styles.skipBtn} onPress={skipTutorial}>
              <Text style={[styles.skipText, { color: colors.textSecondary }]}>Saltar</Text>
            </TouchableOpacity>

            <View style={styles.progressRow}>
              {STEPS.map((_, index) => (
                <View key={index} style={[styles.dot, index === stepIndex ? styles.dotActive : styles.dotInactive]} />
              ))}
            </View>

            <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.content}>
              <Image source={currentStep.image} style={styles.image} resizeMode="contain" />
              <Text style={[styles.title, { color: colors.text }]}>{currentStep.title}</Text>
              <Text style={[styles.body, { color: colors.textSecondary }]}>{currentStep.body}</Text>
            </ScrollView>

            <View style={styles.actions}>
              <TouchableOpacity
                style={[styles.secondaryBtn, { borderColor: colors.border, backgroundColor: colors.background }]}
                onPress={() => setStepIndex((prev) => Math.max(0, prev - 1))}
                disabled={stepIndex === 0}
              >
                <Text style={[styles.secondaryText, { color: colors.textSecondary }]}>Atrás</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={[styles.primaryBtn, { backgroundColor: Colors.primary }]}
                onPress={async () => {
                  if (isLastStep) {
                    await markCompleted();
                  } else {
                    setStepIndex((prev) => Math.min(STEPS.length - 1, prev + 1));
                  }
                }}
              >
                <Text style={styles.primaryText}>{isLastStep ? 'Terminar' : 'Siguiente'}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </TutorialContext.Provider>
  );
};

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    padding: Spacing.xl,
  },
  card: {
    borderRadius: BorderRadius.xl,
    padding: Spacing.lg,
    minHeight: 520,
  },
  skipBtn: {
    position: 'absolute',
    top: Spacing.sm,
    right: Spacing.sm,
    zIndex: 2,
    padding: Spacing.xs,
  },
  skipText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.xs,
  },
  progressRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
    paddingTop: Spacing.xs,
    paddingBottom: Spacing.sm,
  },
  dot: {
    height: 8,
    borderRadius: 4,
  },
  dotActive: {
    width: 24,
    backgroundColor: Colors.primary,
  },
  dotInactive: {
    width: 8,
    backgroundColor: Colors.border,
  },
  content: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.sm,
    paddingVertical: Spacing.md,
  },
  image: {
    width: '100%',
    height: 220,
  },
  title: {
    fontFamily: FontFamily.black,
    fontSize: FontSize['2xl'],
    textAlign: 'center',
  },
  body: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.base,
    lineHeight: 23,
    textAlign: 'center',
  },
  actions: {
    flexDirection: 'row',
    gap: Spacing.sm,
    paddingTop: Spacing.sm,
  },
  secondaryBtn: {
    flex: 1,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryText: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
  primaryBtn: {
    flex: 1,
    borderRadius: BorderRadius.lg,
    minHeight: 48,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryText: {
    color: Colors.white,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.sm,
  },
});
