import { useQuery } from '@tanstack/react-query';
import { questionApi, globalApi, bettingApi, settlementApi } from '@/lib/api';
import {
  mockQuestions,
  mockGlobalStats,
  mockActivities,
  mockSettlementHistory,
  mockGuestActivities,
  mockQualityDashboard,
  generateMockDashboard,
} from '@/lib/mockData';
import type { Question, GlobalStats, Activity, SettlementHistory, QualityDashboard } from '@/types/api';

// === Questions ===

export function useQuestions() {
  return useQuery<Question[]>({
    queryKey: ['questions'],
    queryFn: async () => {
      try {
        const res = await questionApi.getAll();
        if (res.success && res.data && res.data.length > 0) return res.data;
        return mockQuestions;
      } catch {
        return mockQuestions;
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
        return mockQuestions.find(q => q.id === id) || null;
      } catch {
        return mockQuestions.find(q => q.id === id) || null;
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
        return mockGlobalStats;
      } catch {
        return mockGlobalStats;
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
        return mockActivities.filter(a => a.questionId === questionId);
      } catch {
        return mockActivities.filter(a => a.questionId === questionId);
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
        const res = await bettingApi.getActivitiesByMember(memberId);
        if (res.success && res.data && res.data.length > 0) return res.data;
        return mockGuestActivities;
      } catch {
        return mockGuestActivities;
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
        const res = await settlementApi.getHistory(memberId);
        if (res.success && res.data && res.data.length > 0) return res.data;
        return mockSettlementHistory;
      } catch {
        return mockSettlementHistory;
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
        return mockActivities.filter(a => a.questionId === questionId);
      } catch {
        return mockActivities.filter(a => a.questionId === questionId);
      }
    },
    enabled: !!questionId,
    staleTime: 60_000,
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
        return generateMockDashboard(questionId);
      } catch {
        return generateMockDashboard(questionId);
      }
    },
    staleTime: 60_000,
  });
}
