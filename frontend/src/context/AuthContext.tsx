import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_BASE_URL } from '../api/config';
import { apiCheckEmailVerified, apiLogin, apiRegister, getApiErrorMessage } from '../api/authApi';
import {
  clearStoredSession,
  getStoredAccessToken,
  getStoredRefreshToken,
  isBackendSessionToken,
  saveStoredSession,
  subscribeSessionChanges,
  USER_KEY,
} from '../auth/session';

export interface User {
  id: string;
  name: string;
  email: string;
  city?: string;
  role: 'usuario' | 'profesor_particular' | 'profesor_certificado';
  subscription: 'free' | 'plus' | 'education';
  level: 'principiante' | 'intermedio' | 'avanzado';
  xp: number;
  streak: number;
  bestStreak?: number;
  lastStreakDate?: string;
  avatar?: string;
  onboardingCompleted: boolean;
  emailVerified: boolean;
  teacherVerified?: boolean;
  deletionScheduledAt?: string;
  subscriptionCancelAtPeriodEnd?: boolean;
  subscriptionCurrentPeriodEnd?: string;
  trialUsed?: boolean;
}

interface RegisterData {
  name: string;
  email: string;
  password: string;
  role: 'usuario' | 'profesor_particular' | 'profesor_certificado';
  level?: string;
}

interface OnboardingData {
  level?: User['level'];
  role?: User['role'];
  subscription?: User['subscription'];
  [key: string]: unknown;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  loginWithGoogle: () => Promise<void>;
  loginAsGuest: () => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  confirmEmailVerified: () => Promise<void>;
  logout: () => Promise<void>;
  completeOnboarding: (data?: OnboardingData) => Promise<void>;
  updateUser: (data: Partial<User>) => void;
  pendingUser: User | null;
  pendingToken: string | null;
  /** true justo después de completar el registro por primera vez → navegar a suscripciones */
  isNewRegistration: boolean;
  clearNewRegistration: () => void;
}

const ENABLE_LOCAL_MOCK_LOGIN = /localhost|127\.0\.0\.1|10\.0\.2\.2|192\.168\./.test(API_BASE_URL);
const MOCK_CREDENTIALS = {
  email: 'test@wavii.app',
  password: 'Test1234!',
};

const AuthContext = createContext<AuthContextType>({} as AuthContextType);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [pendingUser, setPendingUser] = useState<User | null>(null);
  const [pendingToken, setPendingToken] = useState<string | null>(null);
  const [pendingRefreshToken, setPendingRefreshToken] = useState<string | null>(null);
  const [isNewRegistration, setIsNewRegistration] = useState(false);

  useEffect(() => {
    const loadSession = async () => {
      try {
        const storedToken = await getStoredAccessToken();
        const storedRefreshToken = await getStoredRefreshToken();
        const storedUser = await AsyncStorage.getItem(USER_KEY);
        const hasBrokenBackendSession = isBackendSessionToken(storedToken) && !storedRefreshToken;

        if (hasBrokenBackendSession) {
          await clearStoredSession();
          return;
        }

        if (storedToken && storedUser) {
          setToken(storedToken);
          setUser(JSON.parse(storedUser) as User);
        }
      } catch (e) {
        console.warn('Error al cargar sesión:', e);
      } finally {
        setIsLoading(false);
      }
    };
    loadSession();
  }, []);

  useEffect(() => {
    const unsubscribe = subscribeSessionChanges(({ accessToken }) => {
      if (!accessToken) {
        setToken(null);
        setUser(null);
        return;
      }

      setToken(accessToken);
    });

    return unsubscribe;
  }, []);

  const saveSession = async (accessToken: string, refreshToken: string | null, nextUser: User) => {
    await saveStoredSession(accessToken, refreshToken);
    await AsyncStorage.setItem(USER_KEY, JSON.stringify(nextUser));
    setToken(accessToken);
    setUser(nextUser);
  };

  const login = async (email: string, password: string) => {
    const isLocalMockLogin =
      ENABLE_LOCAL_MOCK_LOGIN &&
      email.toLowerCase().trim() === MOCK_CREDENTIALS.email &&
      password === MOCK_CREDENTIALS.password;
    try {
      const response = await apiLogin({ email: email.trim(), password });
      const loggedUser: User = {
        id: response.userId,
        name: response.name,
        email: response.email,
        city: response.city,
        role: (response.role as User['role']) ?? 'usuario',
        subscription: (response.subscription as User['subscription']) ?? 'free',
        level: 'principiante',
        xp: 0,
        streak: 0,
        onboardingCompleted: response.onboardingCompleted,
        emailVerified: response.emailVerified,
        teacherVerified: response.teacherVerified,
      };
      await saveSession(response.accessToken, response.refreshToken ?? null, loggedUser);
    } catch (err) {
      const msg = getApiErrorMessage(err);
      const isNetwork = msg.includes('servidor') || msg.includes('conectar');

      if (isNetwork && isLocalMockLogin) {
        const mockUser: User = {
          id: 'mock-user-1',
          name: 'Alex Wavii',
        email: MOCK_CREDENTIALS.email,
        city: undefined,
        role: 'usuario',
          subscription: 'free',
          level: 'intermedio',
          xp: 1250,
          streak: 5,
          onboardingCompleted: false,
          emailVerified: true,
          teacherVerified: false,
        };
        await saveSession('mock-jwt-token-dev', null, mockUser);
        return;
      }

      if (isNetwork) {
        throw new Error('No se puede conectar al servidor. Verifica tu conexión.');
      }

      throw new Error(msg);
    }
  };

  const loginWithGoogle = async () => {
    throw new Error('Google login próximamente disponible');
  };

  const loginAsGuest = async () => {
    const guestUser: User = {
      id: `guest-${Date.now()}`,
      name: 'Invitado',
        email: '',
        city: undefined,
        role: 'usuario',
      subscription: 'free',
      level: 'principiante',
      xp: 0,
      streak: 0,
      onboardingCompleted: true,
      emailVerified: false,
      teacherVerified: false,
    };
    await saveSession(`guest-token-${Date.now()}`, null, guestUser);
  };

  const register = async (data: RegisterData) => {
    try {
      const response = await apiRegister({
        name: data.name,
        email: data.email,
        password: data.password,
      });
      const newUser: User = {
        id: response.userId,
        name: response.name,
        email: response.email,
        city: response.city,
        role: data.role,
        subscription: 'free',
        level: (data.level as User['level']) ?? 'principiante',
        xp: 0,
        streak: 0,
        onboardingCompleted: true,
        emailVerified: false,
        teacherVerified: false,
      };
      setPendingUser(newUser);
      setPendingToken(response.accessToken ?? null);
      setPendingRefreshToken(response.refreshToken ?? null);
    } catch (err) {
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { status?: number; data?: { message?: string; field?: string } } };
        if (axiosErr.response?.status === 409) {
          const field = axiosErr.response.data?.field;
          const msg = axiosErr.response.data?.message ?? 'Conflicto al registrar';
          throw new Error(field === 'name' ? `__NAME__${msg}` : msg);
        }
        throw new Error(axiosErr.response?.data?.message ?? 'Error al registrar usuario');
      }
      console.warn('[AuthContext] Backend no disponible, modo mock:', getApiErrorMessage(err));
      const newUser: User = {
        id: `user-${Date.now()}`,
        name: data.name,
        email: data.email,
        city: undefined,
        role: data.role,
        subscription: 'free',
        level: (data.level as User['level']) ?? 'principiante',
        xp: 0,
        streak: 0,
        onboardingCompleted: true,
        emailVerified: false,
        teacherVerified: false,
      };
      setPendingUser(newUser);
      setPendingToken(null);
      setPendingRefreshToken(null);
    }
  };

  const confirmEmailVerified = async () => {
    const userToActivate = pendingUser;
    if (!userToActivate) return;

    try {
      const verified = await apiCheckEmailVerified(userToActivate.email);
      if (!verified) {
        throw new Error('El email aún no ha sido verificado. Revisa tu bandeja de entrada.');
      }
    } catch (err) {
      const msg = getApiErrorMessage(err);
      const isNetworkError = msg.includes('servidor') || msg.includes('conectar');
      if (!isNetworkError) throw err;
      console.warn('[AuthContext] Sin backend, confirmando en modo mock');
    }

    const verifiedUser: User = { ...userToActivate, emailVerified: true };
    const sessionToken = pendingToken ?? `verified-token-${Date.now()}`;
    const refreshToken = pendingRefreshToken ?? null;
    setPendingUser(null);
    setPendingToken(null);
    setPendingRefreshToken(null);
    setIsNewRegistration(true);
    await saveSession(sessionToken, refreshToken, verifiedUser);
  };

  const logout = async () => {
    try {
      await clearStoredSession();
    } catch (e) {
      console.warn('Error al cerrar sesión:', e);
    }
    setToken(null);
    setUser(null);
  };

  const completeOnboarding = async (data?: OnboardingData) => {
    if (!user) return;
    const updated: User = {
      ...user,
      onboardingCompleted: true,
      ...(data?.level ? { level: data.level } : {}),
      ...(data?.role ? { role: data.role } : {}),
      ...(data?.subscription ? { subscription: data.subscription } : {}),
    };
    setUser(updated);
    await AsyncStorage.setItem(USER_KEY, JSON.stringify(updated));
  };

  const updateUser = useCallback((data: Partial<User>) => {
    setUser((currentUser) => {
      if (!currentUser) return currentUser;

      const updated = { ...currentUser, ...data };
      AsyncStorage.setItem(USER_KEY, JSON.stringify(updated)).catch((e) =>
        console.warn('Error actualizando usuario:', e)
      );
      return updated;
    });
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isAuthenticated: !!token && !!user,
        login,
        loginWithGoogle,
        loginAsGuest,
        register,
        confirmEmailVerified,
        logout,
        completeOnboarding,
        updateUser,
        pendingUser,
        pendingToken,
        isNewRegistration,
        clearNewRegistration: () => setIsNewRegistration(false),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
