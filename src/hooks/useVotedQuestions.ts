'use client';

import { useState, useEffect, useCallback } from 'react';
import { bettingApi } from '@/lib/api';

interface VotedInfo {
  questionId: number;
  choice: 'YES' | 'NO';
}

/**
 * 사용자가 투표한 질문 ID와 선택을 추적하는 훅
 */
export function useVotedQuestions(memberId: number | undefined) {
  const [votedMap, setVotedMap] = useState<Map<number, 'YES' | 'NO'>>(new Map());
  const [loading, setLoading] = useState(false);

  const fetchVoted = useCallback(async () => {
    if (!memberId || memberId < 0) return;
    setLoading(true);
    try {
      const res = await bettingApi.getActivitiesByMember('VOTE');
      if (res.success && res.data) {
        const map = new Map<number, 'YES' | 'NO'>();
        for (const activity of res.data) {
          map.set(activity.questionId, activity.choice);
        }
        setVotedMap(map);
      }
    } catch {
      // 실패 시 무시
    } finally {
      setLoading(false);
    }
  }, [memberId]);

  useEffect(() => {
    fetchVoted();
  }, [fetchVoted]);

  const hasVoted = useCallback((questionId: number) => votedMap.has(questionId), [votedMap]);
  const getChoice = useCallback((questionId: number) => votedMap.get(questionId), [votedMap]);

  const markVoted = useCallback((questionId: number, choice: 'YES' | 'NO') => {
    setVotedMap(prev => {
      const next = new Map(prev);
      next.set(questionId, choice);
      return next;
    });
  }, []);

  return { hasVoted, getChoice, markVoted, refresh: fetchVoted, loading, votedMap };
}
