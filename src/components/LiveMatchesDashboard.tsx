'use client';

import React, { useState, useEffect } from 'react';
import { Activity, Trophy, Clock, AlertTriangle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { API_BASE_URL, authFetch, apiRequest, unwrapApiEnvelope } from '@/lib/api';

interface LiveMatch {
  matchId: number;
  questionId: number | null;
  leagueName: string;
  homeTeam: string;
  awayTeam: string;
  homeScore: number;
  awayScore: number;
  matchDate: string;
  status: string;
}

interface BettingSuspension {
  suspended: boolean;
  resumeAt: string | null;
  remainingSeconds: number;
}

// League color mapping
const leagueColors: Record<string, { text: string; bg: string; border: string }> = {
  'EPL': { text: 'text-purple-500', bg: 'bg-purple-500', border: 'border-purple-500/30' },
  'La Liga': { text: 'text-orange-500', bg: 'bg-orange-500', border: 'border-orange-500/30' },
  'Serie A': { text: 'text-blue-500', bg: 'bg-blue-500', border: 'border-blue-500/30' },
  'Bundesliga': { text: 'text-red-600', bg: 'bg-red-600', border: 'border-red-600/30' },
  'Ligue 1': { text: 'text-green-500', bg: 'bg-green-500', border: 'border-green-500/30' },
  'K-League': { text: 'text-teal-500', bg: 'bg-teal-500', border: 'border-teal-500/30' },
  'UCL': { text: 'text-yellow-500', bg: 'bg-yellow-500', border: 'border-yellow-500/30' },
};

const getLeagueColor = (leagueName: string) =>
  leagueColors[leagueName] || { text: 'text-red-500', bg: 'bg-red-500', border: 'border-red-500/30' };

export default function LiveMatchesDashboard() {
  const { isDark } = useTheme();
  const [liveMatches, setLiveMatches] = useState<LiveMatch[]>([]);
  const [suspensionStatuses, setSuspensionStatuses] = useState<Map<number, BettingSuspension>>(new Map());
  const [loading, setLoading] = useState(true);

  const checkSuspensionStatus = useCallback(async (questionId: number) => {
    try {
      const raw = await apiRequest<BettingSuspension>(`/api/betting/suspension/question/${questionId}`);
      const status = unwrapApiEnvelope(raw);
      setSuspensionStatuses(prev => new Map(prev).set(questionId, status));
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to check suspension status:', error);
      }
    }
  }, []);

  const fetchLiveMatches = useCallback(async () => {
    try {
      const response = await authFetch(`${API_BASE_URL}/admin/sports/live`);
      const data = await response.json();
      const matches = Array.isArray(data) ? data : [];
      setLiveMatches(matches);

      // Check betting suspension status for each match
      for (const match of matches) {
        if (match.questionId) {
          checkSuspensionStatus(match.questionId);
        }
      }

      setLoading(false);
    } catch (error) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Failed to fetch live matches:', error);
      }
      setLoading(false);
    }
  }, [checkSuspensionStatus]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchLiveMatches();

    // Real-time updates every 5 seconds
    const interval = setInterval(() => {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      void fetchLiveMatches();
    }, 5000);

    return () => clearInterval(interval);
  }, [fetchLiveMatches]);

  if (loading) {
    return (
      <div className={`rounded-2xl p-6 border-2 ${
        isDark
          ? 'bg-slate-800/50 border-red-500/50'
          : 'bg-white shadow-lg border-red-500'
      }`}>
        <div className="flex items-center gap-2 mb-4">
          <Activity className="text-red-500 animate-pulse" size={24} />
          <h2 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-slate-800'}`}>LIVE Matches</h2>
        </div>
        <p className={isDark ? 'text-slate-400' : 'text-slate-500'}>Loading...</p>
      </div>
    );
  }

  if (liveMatches.length === 0) {
    return (
      <div className={`rounded-2xl p-6 border ${
        isDark
          ? 'bg-slate-800/50 border-slate-700'
          : 'bg-white shadow-lg border-slate-200'
      }`}>
        <div className="flex items-center gap-2 mb-4">
          <Activity className={isDark ? 'text-slate-500' : 'text-slate-400'} size={24} />
          <h2 className={`text-xl font-bold ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>LIVE Matches</h2>
        </div>
        <p className={isDark ? 'text-slate-500' : 'text-slate-500'}>No live matches at the moment.</p>
      </div>
    );
  }

  return (
    <div className={`rounded-2xl p-6 border-2 ${
      isDark
        ? 'bg-slate-800/50 border-red-500/50'
        : 'bg-white shadow-lg border-red-500'
    }`}>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Activity className="text-red-500 animate-pulse" size={24} />
          <h2 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-slate-800'}`}>
            LIVE Matches
          </h2>
        </div>
        <span className={`text-xs ${isDark ? 'text-slate-500' : 'text-slate-500'}`}>
          Auto-updates every 5s
        </span>
      </div>

      <div className="space-y-4">
        {liveMatches.map((match) => {
          const suspension = match.questionId ? suspensionStatuses.get(match.questionId) : null;
          const isSuspended = suspension?.suspended || false;

          return (
            <div
              key={match.matchId}
              className={`rounded-xl p-4 border ${
                isSuspended
                  ? isDark
                    ? 'bg-yellow-500/10 border-yellow-500/30'
                    : 'bg-gradient-to-r from-yellow-50 to-orange-50 border-yellow-300'
                  : isDark
                    ? 'bg-red-500/10 border-red-500/30'
                    : 'bg-gradient-to-r from-red-50 to-orange-50 border-red-200'
              }`}
            >
              {/* League info */}
              <div className="flex items-center gap-2 mb-3">
                <Trophy size={16} className={getLeagueColor(match.leagueName).text} />
                <span className={`text-xs font-bold uppercase tracking-wide ${getLeagueColor(match.leagueName).text}`}>{match.leagueName}</span>
                <span className={`ml-auto px-2 py-1 ${getLeagueColor(match.leagueName).bg} text-white text-xs font-bold rounded animate-pulse`}>
                  LIVE
                </span>
              </div>

              {/* Betting suspension notice */}
              {isSuspended && suspension && (
                <div className={`mb-3 p-3 rounded-lg border ${
                  isDark
                    ? 'bg-yellow-500/20 border-yellow-500/30'
                    : 'bg-yellow-100 border-yellow-300'
                }`}>
                  <div className="flex items-center gap-2">
                    <AlertTriangle size={18} className={isDark ? 'text-yellow-400' : 'text-yellow-700'} />
                    <div className="flex-1">
                      <p className={`text-sm font-bold ${isDark ? 'text-yellow-300' : 'text-yellow-800'}`}>
                        Betting Suspended
                      </p>
                      <p className={`text-xs ${isDark ? 'text-yellow-400' : 'text-yellow-700'}`}>
                        Betting suspended for 30s after goal. Resuming in {suspension.remainingSeconds}s
                      </p>
                    </div>
                    <div className={`text-2xl font-black ${isDark ? 'text-yellow-400' : 'text-yellow-700'}`}>
                      {suspension.remainingSeconds}s
                    </div>
                  </div>
                </div>
              )}

              {/* Scoreboard */}
              <div className="grid grid-cols-3 gap-2 items-center">
                {/* Home team */}
                <div className="text-right">
                  <p className={`text-sm font-bold ${isDark ? 'text-white' : 'text-slate-800'}`}>{match.homeTeam}</p>
                </div>

                {/* Score */}
                <div className="flex items-center justify-center gap-3">
                  <span className={`text-3xl font-black ${
                    match.homeScore > match.awayScore
                      ? 'text-emerald-500'
                      : isDark ? 'text-white' : 'text-slate-800'
                  }`}>
                    {match.homeScore}
                  </span>
                  <span className={`text-2xl font-bold ${isDark ? 'text-slate-500' : 'text-slate-400'}`}>-</span>
                  <span className={`text-3xl font-black ${
                    match.awayScore > match.homeScore
                      ? 'text-emerald-500'
                      : isDark ? 'text-white' : 'text-slate-800'
                  }`}>
                    {match.awayScore}
                  </span>
                </div>

                {/* Away team */}
                <div className="text-left">
                  <p className={`text-sm font-bold ${isDark ? 'text-white' : 'text-slate-800'}`}>{match.awayTeam}</p>
                </div>
              </div>

              {/* Betting link */}
              {match.questionId && (
                <div className={`mt-3 pt-3 border-t ${isDark ? 'border-red-500/30' : 'border-red-200'}`}>
                  <a
                    href={`/question/${match.questionId}`}
                    className={`block w-full py-2 text-center rounded-lg transition font-bold text-sm ${
                      isSuspended
                        ? 'bg-yellow-500/30 text-yellow-400 cursor-not-allowed opacity-60'
                        : 'bg-red-600 text-white hover:bg-red-700'
                    }`}
                    onClick={(e) => {
                      if (isSuspended) {
                        e.preventDefault();
                        alert(`Betting is suspended. Resuming in ${suspension?.remainingSeconds}s.`);
                      }
                    }}
                  >
                    {isSuspended ? 'Betting Suspended...' : 'Place Live Bet'}
                  </a>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
