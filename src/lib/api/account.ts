import type { ApiResponse, CreateMemberRequest, Member } from '@/types/api';
import { apiRequest } from './core';
import { mapMember } from './mappers';

export const memberApi = {
  /**
   * ❌ REMOVED: getByEmail - 백엔드 엔드포인트 제거됨 (보안상 위험)
   * 대안: /api/members/me 사용 (JWT 인증 필요)
   */

  getByWallet: async (address: string): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<Record<string, unknown>>(`/api/members/by-wallet?address=${address}`);
    return { success: true, data: mapMember(raw) };
  },

  getMe: async (): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<Record<string, unknown>>('/api/members/me');
    return { success: true, data: mapMember(raw) };
  },

  getById: async (id: number): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<Record<string, unknown>>(`/api/members/${id}`);
    return { success: true, data: mapMember(raw) };
  },

  create: async (data: CreateMemberRequest): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<Record<string, unknown>>('/api/members', {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return { success: true, data: mapMember(raw) };
  },

  updateWalletAddress: async (walletAddress: string | null): Promise<ApiResponse<Member>> => {
    const raw = await apiRequest<Record<string, unknown>>('/api/members/wallet', {
      method: 'PUT',
      body: JSON.stringify({ walletAddress }),
    });
    return { success: true, data: mapMember(raw) };
  },
};

export const authApi = {
  sendCode: (email: string) =>
    apiRequest<{ success: boolean; message: string; expiresInSeconds: number }>('/api/auth/send-code', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  verifyCode: (email: string, code: string) =>
    apiRequest<{ success: boolean; message: string }>('/api/auth/verify-code', {
      method: 'POST',
      body: JSON.stringify({ email, code }),
    }),

  completeSignup: (email: string, code: string, password: string, passwordConfirm: string) =>
    apiRequest<{ success: boolean; message: string; token?: string; memberId?: number }>('/api/auth/complete-signup', {
      method: 'POST',
      body: JSON.stringify({ email, code, password, passwordConfirm }),
    }),

  login: (email: string, password: string) =>
    apiRequest<{ success: boolean; message: string; token?: string; memberId?: number }>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  googleLogin: (googleToken: string, additionalInfo?: { countryCode?: string; jobCategory?: string; ageGroup?: number }) =>
    apiRequest<{
      success: boolean;
      message: string;
      token?: string;
      memberId?: number;
      needsAdditionalInfo?: boolean;
    }>('/api/auth/google', {
      method: 'POST',
      body: JSON.stringify({
        googleToken,
        countryCode: additionalInfo?.countryCode,
        jobCategory: additionalInfo?.jobCategory,
        ageGroup: additionalInfo?.ageGroup,
      }),
    }),

  completeGoogleRegistration: (data: {
    googleId: string;
    email: string;
    countryCode: string;
    jobCategory?: string;
    ageGroup?: number;
  }) =>
    apiRequest<{
      success: boolean;
      message: string;
      token?: string;
      memberId?: number;
    }>('/api/auth/google/complete-registration', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
};
