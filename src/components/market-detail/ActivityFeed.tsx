'use client';

import { useState, useEffect } from 'react';
import { useTheme } from '@/hooks/useTheme';
import { bettingApi } from '@/lib/api';
import { mockActivities } from '@/lib/mockData';
import type { Activity } from '@/types/api';

interface ActivityFeedProps {
  questionId: number;
  refreshKey: number;
}

export default function ActivityFeed({ questionId, refreshKey }: ActivityFeedProps) {
  const { isDark } = useTheme();
  const [activities, setActivities] = useState<Activity[]>([]);

  useEffect(() => {
    bettingApi.getActivitiesByQuestion(questionId).then(res => {
      if (res.success && res.data && res.data.length > 0) setActivities(res.data);
      else setActivities(mockActivities.filter(a => a.questionId === questionId));
    }).catch(() => {
      setActivities(mockActivities.filter(a => a.questionId === questionId));
    });
  }, [questionId, refreshKey]);

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <h3 className={`font-black text-lg mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>최근 활동</h3>
      {activities.length === 0 ? (
        <p className="text-sm text-slate-400 text-center py-8">아직 활동이 없습니다.</p>
      ) : (
        <div className="space-y-3 max-h-64 overflow-y-auto custom-scrollbar">
          {activities.slice(0, 20).map((act) => (
            <div key={act.id} className={`flex items-center justify-between py-2 px-3 rounded-xl text-sm ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}>
              <div className="flex items-center space-x-3">
                <span className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-black ${
                  act.choice === 'YES' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
                }`}>
                  {act.choice}
                </span>
                <span className="text-slate-400 text-xs">Member #{act.memberId}</span>
              </div>
              <div className="text-right">
                <span className="font-bold text-xs">{act.amount > 0 ? `${act.amount} P` : '투표'}</span>
                <p className="text-[10px] text-slate-500">{new Date(act.createdAt).toLocaleTimeString('ko-KR')}</p>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
