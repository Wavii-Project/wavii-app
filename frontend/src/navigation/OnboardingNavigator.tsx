import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AuthStackParamList } from './AuthNavigator';
import { OnboardingPresentation } from '../screens/onboarding/OnboardingPresentation';
import { OnboardingRoleSelection } from '../screens/onboarding/OnboardingRoleSelection';
import { OnboardingTabsExplanation } from '../screens/onboarding/OnboardingTabsExplanation';
import { OnboardingSocialExplanation } from '../screens/onboarding/OnboardingSocialExplanation';
import { OnboardingLevelSelection } from '../screens/onboarding/OnboardingLevelSelection';
import { OnboardingSubscription } from '../screens/onboarding/OnboardingSubscription';
import { OnboardingTeacherTools } from '../screens/onboarding/OnboardingTeacherTools';
import { OnboardingTeacherTypeSelection } from '../screens/onboarding/OnboardingTeacherTypeSelection';
import { OnboardingScholarPayment } from '../screens/onboarding/OnboardingScholarPayment';
import { OnboardingTeacherVerification } from '../screens/onboarding/OnboardingTeacherVerification';
import { OnboardingTeacherWaiting } from '../screens/onboarding/OnboardingTeacherWaiting';
import { RegisterScreen } from '../screens/auth/RegisterScreen';

export type OnboardingStackParamList = AuthStackParamList;

const Stack = createNativeStackNavigator<OnboardingStackParamList>();

export const OnboardingNavigator = () => {
  return (
    <Stack.Navigator
      initialRouteName="OnboardingPresentation"
      screenOptions={{
        headerShown: false,
        animation: 'slide_from_right',
      }}
    >
      <Stack.Screen name="OnboardingPresentation" component={OnboardingPresentation} />
      <Stack.Screen name="OnboardingRoleSelection" component={OnboardingRoleSelection} />
      <Stack.Screen name="OnboardingTabsExplanation" component={OnboardingTabsExplanation} />
      <Stack.Screen name="OnboardingSocialExplanation" component={OnboardingSocialExplanation} />
      <Stack.Screen name="OnboardingLevelSelection" component={OnboardingLevelSelection} />
      <Stack.Screen name="OnboardingSubscription" component={OnboardingSubscription} />
      <Stack.Screen name="OnboardingTeacherTools" component={OnboardingTeacherTools} />
      <Stack.Screen name="OnboardingTeacherTypeSelection" component={OnboardingTeacherTypeSelection} />
      <Stack.Screen name="OnboardingScholarPayment" component={OnboardingScholarPayment} />
      <Stack.Screen name="OnboardingTeacherVerification" component={OnboardingTeacherVerification} />
      <Stack.Screen name="OnboardingTeacherWaiting" component={OnboardingTeacherWaiting} />
      <Stack.Screen name="Register" component={RegisterScreen} />
    </Stack.Navigator>
  );
};
