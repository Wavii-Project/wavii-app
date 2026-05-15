import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';

// Auth
import { WelcomeScreen } from '../screens/auth/WelcomeScreen';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { RegisterScreen } from '../screens/auth/RegisterScreen';
import { ForgotPasswordScreen } from '../screens/auth/ForgotPasswordScreen';
import { ResetPasswordScreen } from '../screens/auth/ResetPasswordScreen';
import { EmailVerificationScreen } from '../screens/auth/EmailVerificationScreen';

// Onboarding (ocurre ANTES del registro)
import { OnboardingPresentation } from '../screens/onboarding/OnboardingPresentation';
import { OnboardingRoleSelection } from '../screens/onboarding/OnboardingRoleSelection';
import { OnboardingTabsExplanation } from '../screens/onboarding/OnboardingTabsExplanation';
import { OnboardingSocialExplanation } from '../screens/onboarding/OnboardingSocialExplanation';
import { OnboardingLevelSelection } from '../screens/onboarding/OnboardingLevelSelection';
import { OnboardingSubscription } from '../screens/onboarding/OnboardingSubscription';
import { OnboardingTeacherTools } from '../screens/onboarding/OnboardingTeacherTools';
import { OnboardingTeacherTypeSelection } from '../screens/onboarding/OnboardingTeacherTypeSelection';
import { OnboardingPlusPayment } from '../screens/onboarding/OnboardingPlusPayment';
import { OnboardingScholarPayment } from '../screens/onboarding/OnboardingScholarPayment';
import { OnboardingTeacherVerification } from '../screens/onboarding/OnboardingTeacherVerification';
import { OnboardingTeacherWaiting } from '../screens/onboarding/OnboardingTeacherWaiting';
import { OnboardingCertifiedVerification } from '../screens/onboarding/OnboardingCertifiedVerification';

export type AuthStackParamList = {
  // ── Pantallas iniciales ──
  Welcome: undefined;
  Login: { showEmailSent?: boolean; emailSent?: string } | undefined;
  ForgotPassword: undefined;
  ResetPassword: { token?: string };
  EmailVerification: {
    email: string;
    pendingPlan?: 'plus' | 'scholar';
    teacherType?: 'certificado' | 'particular';
  };
  Register: {
    role?: 'usuario' | 'profesor_particular' | 'profesor_certificado';
    level?: string;
    pendingPlan?: 'plus' | 'scholar';
    teacherType?: 'certificado' | 'particular';
  };

  // ── Onboarding (flujo pre-registro) ──
  OnboardingPresentation: undefined;                        // P2
  OnboardingRoleSelection: undefined;                       // P3
  // Path Usuario
  OnboardingTabsExplanation: undefined;                     // P5A
  OnboardingSocialExplanation: undefined;                   // P6A
  OnboardingLevelSelection: undefined;                      // P7A
  OnboardingSubscription: { level: string };                // P8A
  OnboardingPlusPayment: undefined;                         // P_PAGO_PLUS
  // Path Profesor
  OnboardingTeacherTools: undefined;                        // P4B
  OnboardingTeacherTypeSelection: undefined;                // P5B
  OnboardingScholarPayment: { teacherType: 'certificado' | 'particular' }; // P_PAGO_SCHOLAR
  OnboardingTeacherVerification: { teacherType: 'certificado' | 'particular' }; // P6B (legacy)
  OnboardingCertifiedVerification: undefined;               // P6B — profesor_certificado
  OnboardingTeacherWaiting: undefined;                      // P7B
};

const Stack = createNativeStackNavigator<AuthStackParamList>();

export const AuthNavigator = () => {
  return (
    <Stack.Navigator
      initialRouteName="Welcome"
      screenOptions={{
        headerShown: false,
        animation: 'slide_from_right',
      }}
    >
      {/* ── Auth ── */}
      <Stack.Screen name="Welcome" component={WelcomeScreen} />
      <Stack.Screen name="Login" component={LoginScreen} />
      <Stack.Screen name="Register" component={RegisterScreen} />
      <Stack.Screen name="ForgotPassword" component={ForgotPasswordScreen} />
      <Stack.Screen name="ResetPassword" component={ResetPasswordScreen} />
      <Stack.Screen name="EmailVerification" component={EmailVerificationScreen} />

      {/* ── Onboarding ── */}
      <Stack.Screen name="OnboardingPresentation" component={OnboardingPresentation} />
      <Stack.Screen name="OnboardingRoleSelection" component={OnboardingRoleSelection} />
      <Stack.Screen name="OnboardingTabsExplanation" component={OnboardingTabsExplanation} />
      <Stack.Screen name="OnboardingSocialExplanation" component={OnboardingSocialExplanation} />
      <Stack.Screen name="OnboardingLevelSelection" component={OnboardingLevelSelection} />
      <Stack.Screen name="OnboardingSubscription" component={OnboardingSubscription} />
      <Stack.Screen name="OnboardingPlusPayment" component={OnboardingPlusPayment} />
      <Stack.Screen name="OnboardingTeacherTools" component={OnboardingTeacherTools} />
      <Stack.Screen name="OnboardingTeacherTypeSelection" component={OnboardingTeacherTypeSelection} />
      <Stack.Screen name="OnboardingScholarPayment" component={OnboardingScholarPayment} />
      <Stack.Screen name="OnboardingTeacherVerification" component={OnboardingTeacherVerification} />
      <Stack.Screen name="OnboardingCertifiedVerification" component={OnboardingCertifiedVerification} />
      <Stack.Screen name="OnboardingTeacherWaiting" component={OnboardingTeacherWaiting} />
    </Stack.Navigator>
  );
};
