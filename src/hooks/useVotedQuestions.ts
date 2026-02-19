'use client';

import { useState, useEffect, useCallback } from 'react';
import { bettingApi } from '@/lib/api';

interface VotedInfo {
  questionId: number;
  choice: 'YES' | 'NO';
}

/**
 * Hook for tracking question IDs and choices that user has voted on
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
      // Ignore on failure
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
