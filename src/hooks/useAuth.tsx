'use client';

import { useState, useEffect, useCallback, useMemo, createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import { memberApi, authApi, ApiError } from '@/lib/api';
import { safeLocalStorage } from '@/lib/safeLocalStorage';
import { clearAllAuthCookies, setAuthCookies } from '@/lib/cookieUtils';
import type { Member } from '@/types/api';
import { useDisconnect } from 'wagmi';

const STORAGE_KEY = 'predataUser';
const USER_REFRESH_INTERVAL_MS = 30000;

export interface AuthState {
  user: Member | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isGuest: boolean;
  loginAsGuest: () => void;
  loginWithGoogle: (
    googleToken: string,
    additionalInfo?: {
      countryCode?: string;
      jobCategory?: string;
      ageGroup?: number;
    }
  ) => Promise<{ success: boolean; needsAdditionalInfo?: boolean; error?: string }>;
  loginById: (memberId: number) => Promise<Member | null>;
  register: (data: {
    email: string;
    walletAddress?: string;
    countryCode: string;
    jobCategory?: string;
    ageGroup?: string;
  }) => Promise<Member | null>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  persistUser: (member: Member) => void;
}

const AuthContext = createContext<AuthState | null>(null);

function checkIsGuest(user: Member | null): boolean {
  if (!user) return false;
  return user.id < 0 || (user.email?.endsWith('@predata.demo') ?? false);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<Member | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const { disconnect } = useDisconnect();

  const isGuest = useMemo(() => checkIsGuest(user), [user]);

  // Initial load: Restore session from localStorage + validate token
  useEffect(() => {
    const saved = safeLocalStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved) as Member;
        const isGuestUser = parsed.id < 0 || (parsed.email?.endsWith('@predata.demo') ?? false);
        const token = safeLocalStorage.getItem('token');

        if (isGuestUser || token) {
          setUser(parsed);
          if (!isGuestUser && token) {
            setAuthCookies(parsed.role === 'ADMIN');
          }
        } else {
          // Real user without token = stale data → delete
          safeLocalStorage.removeItem(STORAGE_KEY);
          safeLocalStorage.removeItem('memberId');
        }
      } catch {
        safeLocalStorage.removeItem(STORAGE_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  const persistUser = useCallback((member: Member) => {
    setUser(member);
    safeLocalStorage.setItem(STORAGE_KEY, JSON.stringify(member));
  }, []);

  // Declare logout first to prevent TDZ (referenced by loginById, refreshUser)
  const logout = useCallback(() => {
    setUser(null);
    safeLocalStorage.removeItem(STORAGE_KEY);
    safeLocalStorage.removeItem('token');
    safeLocalStorage.removeItem('memberId');
    clearAllAuthCookies();

    // Disconnect wallet
    if (disconnect) {
      disconnect();
    }

    window.location.href = '/';
  }, [disconnect]);

  // Guest: Local only (no backend registration, read-only access)
  const loginAsGuest = useCallback(() => {
    const guestMember: Member = {
      id: -1,
      email: `guest@predata.demo`,
      countryCode: 'KR',
      tier: 'BRONZE',
      tierWeight: 1.0,
      accuracyScore: 0,
      usdcBalance: 0,
      hasVotingPass: false,
      totalPredictions: 0,
      correctPredictions: 0,
      createdAt: new Date().toISOString(),
    };
    persistUser(guestMember);
  }, [persistUser]);

  const loginById = useCallback(async (_memberId: number) => {
    void _memberId;
    setIsLoading(true);
    try {
      const response = await memberApi.getMe();
      if (response.success && response.data) {
        persistUser(response.data);
        setAuthCookies(response.data.role === 'ADMIN');
        return response.data;
      }
    } catch (error: unknown) {
      const status = error instanceof ApiError ? error.status : undefined;
      // 401/404: Token expired or user deleted → logout
      if (status === 404 || status === 401) {
        logout();
      }
      // Backend unavailable or other errors
    } finally {
      setIsLoading(false);
    }
    return null;
  }, [persistUser, logout]);

  const loginWithGoogle = useCallback(async (
    googleToken: string,
    additionalInfo?: {
      countryCode?: string;
      jobCategory?: string;
      ageGroup?: number;
    }
  ) => {
    setIsLoading(true);
    try {
      const response = await authApi.googleLogin(googleToken, additionalInfo);

      if (response.success) {
        if (response.needsAdditionalInfo) {
          // Additional info required
          return { success: true, needsAdditionalInfo: true };
        }

        if (response.token && response.memberId) {
          // Save JWT token and load user info
          const member = await loginById(response.memberId);
          if (!member) {
            // getMe() failed → logout and return error
            logout();
            return { success: false, error: "Unable to load user information" };
          }
          return { success: true };
        }
      }

      return { success: false };
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Google login failed:', error);
      }
      return { success: false };
    } finally {
      setIsLoading(false);
    }
  }, [loginById, logout]);

  const register = useCallback(async (data: {
    email: string;
    walletAddress?: string;
    countryCode: string;
    jobCategory?: string;
    ageGroup?: string;
  }) => {
    setIsLoading(true);
    try {
      const response = await memberApi.create(data);
      if (response.success && response.data) {
        persistUser(response.data);
        return response.data;
      }
    } catch {
      // Network error or backend unavailable
    } finally {
      setIsLoading(false);
    }
    return null;
  }, [persistUser]);

  const refreshUser = useCallback(async () => {
    if (!user || checkIsGuest(user)) return;
    try {
      const response = await memberApi.getMe();
      if (response.success && response.data) {
        persistUser(response.data);
      }
    } catch (error: unknown) {
      const status = error instanceof ApiError ? error.status : undefined;
      // 401/404: Token expired or user deleted → logout
      if (status === 404 || status === 401) {
        logout();
      }
      // Backend unavailable or other errors
    }
  }, [user, persistUser, logout]);

  // Auto-refresh balance: Fetch latest user data from backend every 30 seconds
  useEffect(() => {
    if (!user || checkIsGuest(user)) return;

    const interval = setInterval(() => {
      if (typeof document !== 'undefined' && document.visibilityState !== 'visible') {
        return;
      }
      refreshUser();
    }, USER_REFRESH_INTERVAL_MS); // Refresh every 30 seconds

    return () => clearInterval(interval);
  }, [user, refreshUser]);

  return (
    <AuthContext.Provider value={{
      user,
      isLoading,
      isAuthenticated: !!user,
      isGuest,
      loginAsGuest,
      loginWithGoogle,
      loginById,
      register,
      logout,
      refreshUser,
      persistUser,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthState {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
