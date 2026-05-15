import React from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { useNavigation } from '@react-navigation/native';
import { useTheme } from '../../context/ThemeContext';
import { Colors, FontFamily, FontSize, Spacing } from '../../theme';

export const TermsScreen = () => {
  const { colors } = useTheme();
  const navigation = useNavigation();

  return (
    <SafeAreaView style={[styles.safe, { backgroundColor: colors.background }]}>
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} hitSlop={{ top: 12, bottom: 12, left: 12, right: 12 }}>
          <Ionicons name="chevron-back" size={26} color={colors.text} />
        </TouchableOpacity>
        <Text style={[styles.title, { color: colors.text }]}>Términos y Condiciones</Text>
        <View style={{ width: 26 }} />
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={[styles.heading, { color: colors.text }]}>1. Uso de la Aplicación</Text>
        <Text style={[styles.text, { color: colors.textSecondary }]}>
          Al utilizar Wavii, aceptas los términos y condiciones de servicio. La aplicación está diseñada para facilitar el aprendizaje musical y la conexión entre estudiantes y profesores.
        </Text>

        <Text style={[styles.heading, { color: colors.text }]}>2. Privacidad y Datos</Text>
        <Text style={[styles.text, { color: colors.textSecondary }]}>
          Respetamos tu privacidad. Tus datos personales, como el correo electrónico y nombre, solo se utilizan para la creación de cuenta y no se comparten con terceros sin tu consentimiento.
        </Text>

        <Text style={[styles.heading, { color: colors.text }]}>3. Suscripciones</Text>
        <Text style={[styles.text, { color: colors.textSecondary }]}>
          Las suscripciones Plus y Scholar tienen un ciclo de facturación mensual. Puedes cancelar en cualquier momento desde los ajustes de la aplicación, manteniendo los beneficios hasta el final del periodo facturado.
        </Text>

        <Text style={[styles.heading, { color: colors.text }]}>4. Responsabilidad</Text>
        <Text style={[styles.text, { color: colors.textSecondary }]}>
          Wavii actúa como intermediario en el tablón de profesores y no se hace responsable por la calidad de las clases particulares impartidas ni por las transacciones fuera de la plataforma.
        </Text>
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
    padding: Spacing.xl,
    gap: Spacing.md,
  },
  heading: {
    fontFamily: FontFamily.bold,
    fontSize: FontSize.md,
    marginTop: Spacing.sm,
  },
  text: {
    fontFamily: FontFamily.regular,
    fontSize: FontSize.sm,
    lineHeight: 22,
  },
});
