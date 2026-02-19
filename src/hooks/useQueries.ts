import { useQuery } from '@tanstack/react-query';
import { questionApi, globalApi, bettingApi, settlementApi } from '@/lib/api';
import type { Question, GlobalStats, Activity, SettlementHistory, QualityDashboard } from '@/types/api';
import { swapApi, type SwapHistoryResponse } from '@/lib/api/swap';

// === Questions ===

export function useQuestions() {
  return useQuery<Question[]>({
    queryKey: ['questions'],
    queryFn: async () => {
      try {
        const res = await questionApi.getAll();
        if (res.success && res.data && res.data.length > 0) return res.data;
        return [];
      } catch {
        return [];
      }
    },
    staleTime: 30_000,
  });
}

export function useQuestion(id: number) {
  return useQuery<Question | null>({
    queryKey: ['question', id],
    queryFn: async () => {
      try {
        const res = await questionApi.getById(id);
        if (res.success && res.data) return res.data;
        return null;
      } catch {
        return null;
      }
    },
    enabled: !!id && !isNaN(id),
    refetchInterval: 5000,
  });
}

// === Global Stats ===

export function useGlobalStats() {
  return useQuery<GlobalStats>({
    queryKey: ['globalStats'],
    queryFn: async () => {
      try {
        const res = await globalApi.getStats();
        if (res.success && res.data) return res.data;
        return { totalPredictions: 0, tvl: 0, activeUsers: 0, cumulativeRewards: 0, activeMarkets: 0 };
      } catch {
        return { totalPredictions: 0, tvl: 0, activeUsers: 0, cumulativeRewards: 0, activeMarkets: 0 };
      }
    },
    staleTime: 60_000,
  });
}

// === Activities ===

export function useActivitiesByQuestion(questionId: number, refreshKey: number) {
  return useQuery<Activity[]>({
    queryKey: ['activities', 'question', questionId, refreshKey],
    queryFn: async () => {
      try {
        const res = await bettingApi.getActivitiesByQuestion(questionId);
        if (res.success && res.data && res.data.length > 0) return res.data;
        return [];
      } catch {
        return [];
      }
    },
    enabled: !!questionId,
  });
}

export function useActivitiesByMember(memberId: number) {
  return useQuery<Activity[]>({
    queryKey: ['activities', 'member', memberId],
    queryFn: async () => {
      try {
        const res = await bettingApi.getActivitiesByMember('BET');
        if (res.success && res.data && res.data.length > 0) return res.data;
        return [];
      } catch {
        return [];
      }
    },
    enabled: !!memberId,
  });
}

// === Settlement History ===

export function useSettlementHistory(memberId: number) {
  return useQuery<SettlementHistory[]>({
    queryKey: ['settlement', 'history', memberId],
    queryFn: async () => {
      try {
        const res = await settlementApi.getHistory();
        if (res.success && res.data && res.data.length > 0) return res.data;
        return [];
      } catch {
        return [];
      }
    },
    enabled: !!memberId,
  });
}

// === Data Center Activities (for time-series derivation) ===

export function useQuestionActivities(questionId: number) {
  return useQuery<Activity[]>({
    queryKey: ['datacenter', 'activities', questionId],
    queryFn: async () => {
      try {
        const res = await bettingApi.getActivitiesByQuestion(questionId);
        if (res.success && res.data && res.data.length > 0) return res.data;
        return [];
      } catch {
        return [];
      }
    },
    enabled: !!questionId,
    staleTime: 0,
    refetchInterval: 5000,
  });
}

export function useQuestionSwapHistory(questionId: number) {
  return useQuery<SwapHistoryResponse[]>({
    queryKey: ['datacenter', 'swap-history', questionId],
    queryFn: async () => {
      try {
        const data = await swapApi.getSwapHistory(questionId, 100);
        return Array.isArray(data) ? data : [];
      } catch {
        return [];
      }
    },
    enabled: !!questionId,
    staleTime: 0,
    refetchInterval: 5000,
  });
}

// === Data Center / Quality Dashboard ===

export function useQualityDashboard(questionId: number) {
  return useQuery<QualityDashboard | null>({
    queryKey: ['qualityDashboard', questionId],
    queryFn: async () => {
      try {
        const { analyticsApi } = await import('@/lib/api');
        const res = await analyticsApi.getDashboard(questionId);
        if (res.success && res.data) {
          const data = res.data as unknown as QualityDashboard;
          if (data.gapAnalysis && data.demographics && data.filteringEffect) return data;
        }
        return null;
      } catch {
        return null;
      }
    },
    staleTime: 60_000,
  });
}
