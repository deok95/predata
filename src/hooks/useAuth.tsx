'use client';

import { useState, useEffect, useCallback, useMemo, createContext, useContext } from 'react';
import type { ReactNode } from 'react';
import { useAccount } from 'wagmi';
import { memberApi, ApiError } from '@/lib/api';
import { safeLocalStorage } from '@/lib/safeLocalStorage';
import type { Member } from '@/types/api';

const STORAGE_KEY = 'predataUser';

export interface AuthState {
  user: Member | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isGuest: boolean;
  loginAsGuest: () => void;
  loginWithWallet: (walletAddress: string) => Promise<Member | null>;
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
  const { address, isConnected } = useAccount();

  const isGuest = useMemo(() => checkIsGuest(user), [user]);

  // 초기 로드: localStorage에서 세션 복원
  useEffect(() => {
    const saved = safeLocalStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        setUser(JSON.parse(saved));
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
      pointBalance: 0,
      totalPredictions: 0,
      correctPredictions: 0,
      createdAt: new Date().toISOString(),
    };
    persistUser(guestMember);
  }, [persistUser]);

  const loginWithWallet = useCallback(async (walletAddress: string) => {
    setIsLoading(true);
    try {
      const response = await memberApi.getByWallet(walletAddress);
      if (response.success && response.data) {
        persistUser(response.data);
        return response.data;
      }
    } catch {
      // 404 or network error — 미등록 지갑
    } finally {
      setIsLoading(false);
    }
    return null;
  }, [persistUser]);

  // 지갑 연결 시 자동 로그인
  useEffect(() => {
    if (isConnected && address && !user) {
      loginWithWallet(address).catch(() => {});
    }
  }, [isConnected, address, user, loginWithWallet]);

  const loginById = useCallback(async (memberId: number) => {
    setIsLoading(true);
    try {
      const response = await memberApi.getById(memberId);
      if (response.success && response.data) {
        persistUser(response.data);
        return response.data;
      }
    } catch {
      // Backend unavailable
    } finally {
      setIsLoading(false);
    }
    return null;
  }, [persistUser]);

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
  }, []);

  const refreshUser = useCallback(async () => {
    if (!user || checkIsGuest(user)) return;
    try {
      const response = await memberApi.getById(user.id);
      if (response.success && response.data) {
        persistUser(response.data);
      }
    } catch {
      // Backend unavailable
    }
  }, [user, persistUser]);

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
      loginWithWallet,
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
