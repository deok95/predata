'use client';

import React, { useState, useEffect } from 'react';
import { Activity, Trophy, Clock, AlertTriangle } from 'lucide-react';

const BACKEND_URL = 'http://localhost:8080/api';

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

export default function LiveMatchesDashboard() {
  const [liveMatches, setLiveMatches] = useState<LiveMatch[]>([]);
  const [suspensionStatuses, setSuspensionStatuses] = useState<Map<number, BettingSuspension>>(new Map());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchLiveMatches();
    
    // 5초마다 실시간 업데이트
    const interval = setInterval(() => {
      fetchLiveMatches();
    }, 5000);

    return () => clearInterval(interval);
  }, []);

  const fetchLiveMatches = async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/admin/sports/live`);
      const data = await response.json();
      setLiveMatches(data);
      
      // 각 경기의 베팅 중지 상태 확인
      for (const match of data) {
        if (match.questionId) {
          checkSuspensionStatus(match.questionId);
        }
      }
      
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch live matches:', error);
      setLoading(false);
    }
  };

  const checkSuspensionStatus = async (questionId: number) => {
    try {
      const response = await fetch(`${BACKEND_URL}/betting/suspension/question/${questionId}`);
      const status: BettingSuspension = await response.json();
      setSuspensionStatuses(prev => new Map(prev).set(questionId, status));
    } catch (error) {
      console.error('Failed to check suspension status:', error);
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-xl shadow-lg p-6 border-2 border-red-500">
        <div className="flex items-center gap-2 mb-4">
          <Activity className="text-red-500 animate-pulse" size={24} />
          <h2 className="text-xl font-bold">LIVE 경기</h2>
        </div>
        <p className="text-slate-500">로딩 중...</p>
      </div>
    );
  }

  if (liveMatches.length === 0) {
    return (
      <div className="bg-white rounded-xl shadow-lg p-6 border-2 border-slate-200">
        <div className="flex items-center gap-2 mb-4">
          <Activity className="text-slate-400" size={24} />
          <h2 className="text-xl font-bold text-slate-600">LIVE 경기</h2>
        </div>
        <p className="text-slate-500">현재 진행 중인 경기가 없습니다.</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-xl shadow-lg p-6 border-2 border-red-500">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Activity className="text-red-500 animate-pulse" size={24} />
          <h2 className="text-xl font-bold">🔴 LIVE 경기</h2>
        </div>
        <span className="text-xs text-slate-500">5초마다 자동 업데이트</span>
      </div>

      <div className="space-y-4">
        {liveMatches.map((match) => {
          const suspension = match.questionId ? suspensionStatuses.get(match.questionId) : null;
          const isSuspended = suspension?.suspended || false;

          return (
            <div
              key={match.matchId}
              className={`rounded-lg p-4 border ${
                isSuspended 
                  ? 'bg-gradient-to-r from-yellow-50 to-orange-50 border-yellow-300'
                  : 'bg-gradient-to-r from-red-50 to-orange-50 border-red-200'
              }`}
            >
              {/* 리그 정보 */}
              <div className="flex items-center gap-2 mb-3">
                <Trophy size={16} className="text-red-600" />
                <span className="text-xs font-bold text-red-600 uppercase tracking-wide">{match.leagueName}</span>
                <span className="ml-auto px-2 py-1 bg-red-600 text-white text-xs font-bold rounded animate-pulse">
                  LIVE
                </span>
              </div>

              {/* 베팅 일시 중지 알림 */}
              {isSuspended && suspension && (
                <div className="mb-3 p-3 bg-yellow-100 border border-yellow-300 rounded-lg">
                  <div className="flex items-center gap-2">
                    <AlertTriangle size={18} className="text-yellow-700" />
                    <div className="flex-1">
                      <p className="text-sm font-bold text-yellow-800">⚠️ 베팅 일시 중지</p>
                      <p className="text-xs text-yellow-700">
                        골 직후 30초간 베팅이 중지됩니다. {suspension.remainingSeconds}초 후 재개
                      </p>
                    </div>
                    <div className="text-2xl font-black text-yellow-700">
                      {suspension.remainingSeconds}s
                    </div>
                  </div>
                </div>
              )}

              {/* 스코어보드 */}
              <div className="grid grid-cols-3 gap-2 items-center">
                {/* 홈팀 */}
                <div className="text-right">
                  <p className="text-sm font-bold text-slate-800">{match.homeTeam}</p>
                </div>

                {/* 스코어 */}
                <div className="flex items-center justify-center gap-3">
                  <span className={`text-3xl font-black ${
                    match.homeScore > match.awayScore ? 'text-green-600' : 'text-slate-800'
                  }`}>
                    {match.homeScore}
                  </span>
                  <span className="text-2xl font-bold text-slate-400">-</span>
                  <span className={`text-3xl font-black ${
                    match.awayScore > match.homeScore ? 'text-green-600' : 'text-slate-800'
                  }`}>
                    {match.awayScore}
                  </span>
                </div>

                {/* 원정팀 */}
                <div className="text-left">
                  <p className="text-sm font-bold text-slate-800">{match.awayTeam}</p>
                </div>
              </div>

              {/* 베팅 링크 */}
              {match.questionId && (
                <div className="mt-3 pt-3 border-t border-red-200">
                  <a
                    href={`/question/${match.questionId}`}
                    className={`block w-full py-2 text-center rounded-lg transition font-bold text-sm ${
                      isSuspended
                        ? 'bg-yellow-300 text-yellow-800 cursor-not-allowed opacity-60'
                        : 'bg-red-600 text-white hover:bg-red-700'
                    }`}
                    onClick={(e) => {
                      if (isSuspended) {
                        e.preventDefault();
                        alert(`⚠️ 골 직후 베팅이 일시 중지되었습니다. ${suspension?.remainingSeconds}초 후 재개됩니다.`);
                      }
                    }}
                  >
                    {isSuspended ? '⏳ 베팅 중지 중...' : '🎯 실시간 베팅하기'}
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
