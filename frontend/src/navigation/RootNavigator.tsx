import React from 'react';
import { useAuth } from '../context/AuthContext';
import { AuthNavigator } from './AuthNavigator';
import { AppNavigator } from './AppNavigator';
import { LoadingScreen } from '../screens/auth/LoadingScreen';

export const RootNavigator = () => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <LoadingScreen />;

  return isAuthenticated ? <AppNavigator /> : <AuthNavigator />;
};
