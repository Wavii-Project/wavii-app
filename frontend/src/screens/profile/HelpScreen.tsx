import React, { useState } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Linking } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../../context/ThemeContext';
import { useAlert } from '../../context/AlertContext';
import { useTutorial } from '../../context/TutorialContext';
import { Colors, FontFamily, FontSize, Spacing, BorderRadius } from '../../theme';

const FAQS = [
  {
    q: '¿Cómo encuentro profesores en Wavii?',
    a: 'Dirígete al Tablón de Anuncios desde el menú principal. Allí podrás filtrar por instrumento o tipo de profesor.',
  },
  {
    q: '¿Cómo funcionan los desafíos diarios?',
    a: 'Cada día te asignamos tareas para mejorar tus habilidades. Complétalas para ganar XP y mantener tu racha.',
  },
  {
    q: '¿Cuál es la diferencia entre profesor certificado y particular?',
    a: 'Los certificados han sido validados por Wavii. Los particulares son músicos o estudiantes no verificados formalmente.',
  },
];

export const HelpScreen = () => {
  const { colors } = useTheme();
  const { showAlert } = useAlert();
  const { startTutorial } = useTutorial();
  const navigation = useNavigation();
  const [expandedIndex, setExpandedIndex] = useState<number | null>(null);

  const handleSupportEmail = () => {
    Linking.openURL('mailto:wavii.verificacion@gmail.com').catch(() => {
      showAlert({ title: 'Error', message: 'No se pudo abrir la aplicación de correo.' });
    });
  };

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]} edges={['top', 'bottom']}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Ayuda y FAQ</Text>
        <View style={{ width: 26 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>¿Cómo empezar?</Text>
        <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border }]}>
          <Text style={[styles.text, { color: colors.text }]}>1. Completa tu perfil indicando tu instrumento.</Text>
          <Text style={[styles.text, { color: colors.text }]}>2. Revisa tus objetivos diarios en Inicio.</Text>
          <Text style={[styles.text, { color: colors.text }]}>3. Explora tablaturas, bandas y el tablón si encaja con tu plan.</Text>
          <Text style={[styles.text, { color: colors.text }]}>4. Busca profesores o tablaturas para seguir avanzando.</Text>
        </View>

        <TouchableOpacity style={[styles.tutorialBtn, { backgroundColor: Colors.primary }]} onPress={startTutorial}>
          <Ionicons name="play-circle-outline" size={20} color={Colors.white} />
          <Text style={styles.contactBtnText}>Repetir tutorial</Text>
        </TouchableOpacity>

        <Text style={[styles.sectionTitle, { color: colors.text }]}>Preguntas frecuentes</Text>
        <View style={[styles.card, { backgroundColor: colors.surface, borderColor: colors.border, padding: 0 }]}>
          {FAQS.map((faq, index) => {
            const isExpanded = expandedIndex === index;
            return (
              <View
                key={faq.q}
                style={[styles.faqItem, index !== FAQS.length - 1 && { borderBottomWidth: 1, borderBottomColor: colors.border }]}
              >
                <TouchableOpacity
                  style={styles.faqHeader}
                  onPress={() => setExpandedIndex(isExpanded ? null : index)}
                  activeOpacity={0.7}
                >
                  <Text style={[styles.faqQ, { color: colors.text }]}>{faq.q}</Text>
                  <Ionicons name={isExpanded ? 'chevron-up' : 'chevron-down'} size={20} color={colors.textSecondary} />
                </TouchableOpacity>
                {isExpanded ? (
                  <View style={styles.faqBody}>
                    <Text style={[styles.faqA, { color: colors.textSecondary }]}>{faq.a}</Text>
                  </View>
                ) : null}
              </View>
            );
          })}
        </View>

        <Text style={[styles.sectionTitle, { color: colors.text }]}>¿Necesitas más ayuda?</Text>
        <TouchableOpacity style={[styles.contactBtn, { backgroundColor: Colors.primary }]} onPress={handleSupportEmail}>
          <Ionicons name="mail" size={20} color={Colors.white} />
          <Text style={styles.contactBtnText}>Contactar soporte</Text>
        </TouchableOpacity>
        <Text style={[styles.contactEmail, { color: colors.textSecondary }]}>wavii.verificacion@gmail.com</Text>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  safe: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.base,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 1,
  },
  title: {
    fontFamily: FontFamily.extraBold,
    fontSize: FontSize.lg,
  },
  content: {
    padding: Spacing.base,
    gap: Spacing.base,
    paddingBottom: Spacing.xl,
  },
  sectionTitle: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.md,
    marginTop: Spacing.sm,
  },
  card: {
    borderRadius: BorderRadius.xl,
    borderWidth: 1,
    padding: Spacing.base,
    gap: Spacing.sm,
  },
  text: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  faqItem: {
    width: '100%',
  },
  faqHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: Spacing.base,
  },
  faqQ: {
    fontFamily: FontFamily.semiBold,
    fontSize: FontSize.sm,
    flex: 1,
    marginRight: Spacing.sm,
  },
  faqBody: {
    paddingHorizontal: Spacing.base,
    paddingBottom: Spacing.base,
  },
  faqA: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 20,
  },
  contactBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
    borderRadius: BorderRadius.md,
    gap: Spacing.sm,
  },
  tutorialBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 14,
    borderRadius: BorderRadius.md,
    gap: Spacing.sm,
  },
  contactBtnText: {
    color: Colors.white,
    fontFamily: FontFamily.bold,
    fontSize: FontSize.base,
  },
  contactEmail: {
    textAlign: 'center',
    fontFamily: FontFamily.medium,
    fontSize: FontSize.xs,
    marginTop: -8,
  },
});
