'use client';

import { useState, useEffect, useCallback, useMemo, createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import { memberApi, authApi, ApiError } from '@/lib/api';
import { safeLocalStorage } from '@/lib/safeLocalStorage';
import { clearAllAuthCookies, setAuthCookies } from '@/lib/cookieUtils';
import type { Member } from '@/types/api';
import { useDisconnect } from 'wagmi';

const STORAGE_KEY = 'predataUser';

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
  ) => Promise<{ success: boolean; needsAdditionalInfo?: boolean }>;
  loginWithEmail: (email: string) => Promise<{ member: Member | null; isNew: boolean }>;
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

  // 초기 로드: localStorage에서 세션 복원 + 토큰 유효성 검증
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
          // 토큰 없는 실유저 = stale 데이터 → 삭제
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

  // 게스트: 로컬 전용 (백엔드 등록 없음, 조회만 가능)
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

  const loginById = useCallback(async (memberId: number) => {
    setIsLoading(true);
    try {
      const response = await memberApi.getMe();
      if (response.success && response.data) {
        persistUser(response.data);
        setAuthCookies(response.data.role === 'ADMIN');
        return response.data;
      }
    } catch (error: any) {
      // 401/404: 토큰 만료 또는 사용자 삭제됨 → 로그아웃
      if (error?.status === 404 || error?.status === 401) {
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
          // 추가 정보 입력 필요
          return { success: true, needsAdditionalInfo: true };
        }

        if (response.token && response.memberId) {
          // JWT 토큰 저장 및 사용자 정보 조회
          await loginById(response.memberId);
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
  }, [loginById]);

  const loginWithEmail = useCallback(async (email: string) => {
    setIsLoading(true);
    try {
      const response = await memberApi.getByEmail(email);
      if (response.success && response.data) {
        persistUser(response.data);
        return { member: response.data, isNew: false };
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        return { member: null, isNew: true };
      }
    } finally {
      setIsLoading(false);
    }
    return { member: null, isNew: false };
  }, [persistUser]);

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

  const logout = useCallback(() => {
    setUser(null);
    safeLocalStorage.removeItem(STORAGE_KEY);
    safeLocalStorage.removeItem('token');
    safeLocalStorage.removeItem('memberId');
    clearAllAuthCookies();

    // 지갑 연결 해제
    if (disconnect) {
      disconnect();
    }

    window.location.href = '/';
  }, [disconnect]);

  const refreshUser = useCallback(async () => {
    if (!user || checkIsGuest(user)) return;
    try {
      const response = await memberApi.getMe();
      if (response.success && response.data) {
        persistUser(response.data);
      }
    } catch (error: any) {
      // 401/404: 토큰 만료 또는 사용자 삭제됨 → 로그아웃
      if (error?.status === 404 || error?.status === 401) {
        logout();
      }
      // Backend unavailable or other errors
    }
  }, [user, persistUser, logout]);

  // 자동 잔액 갱신: 10초마다 백엔드에서 최신 사용자 데이터 가져오기
  useEffect(() => {
    if (!user || checkIsGuest(user)) return;

    const interval = setInterval(() => {
      refreshUser();
    }, 10000); // 10초마다 갱신

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
      loginWithEmail,
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
