/**
 * Tipografía de Wavii.
 * Fuente principal: Nunito (redondeada, amigable y legible).
 */

export const FontFamily = {
  regular: 'Nunito_400Regular',
  medium: 'Nunito_500Medium',
  semiBold: 'Nunito_600SemiBold',
  bold: 'Nunito_700Bold',
  extraBold: 'Nunito_800ExtraBold',
  black: 'Nunito_900Black',
} as const;

export const FontSize = {
  xs: 11,
  sm: 13,
  base: 15,
  md: 17,
  lg: 19,
  xl: 22,
  '2xl': 26,
  '3xl': 32,
  '4xl': 40,
} as const;

export const LineHeight = {
  tight: 1.2,
  normal: 1.4,
  relaxed: 1.6,
} as const;

export const labelStyle = {
  fontFamily: FontFamily.semiBold,
  fontSize: FontSize.xs,
  textTransform: 'uppercase' as const,
  letterSpacing: 1,
} as const;
