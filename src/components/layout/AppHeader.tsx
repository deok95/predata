'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Search, Sun, Moon, Bell, X, UserPlus, Gift } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { useRouter } from 'next/navigation';
import { mockQuestions } from '@/lib/mockData';
import { questionApi, faucetApi } from '@/lib/api';
import type { Question } from '@/types/api';

export default function AppHeader() {
  const { isDark, toggleTheme } = useTheme();
  const { user, isGuest, refreshUser } = useAuth();
  const { open: openRegister } = useRegisterModal();
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Question[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [allQuestions, setAllQuestions] = useState<Question[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>(getDefaultNotifications());
  const [showNotifications, setShowNotifications] = useState(false);
  const [faucetClaimed, setFaucetClaimed] = useState(false);
  const [faucetLoading, setFaucetLoading] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);
  const notiRef = useRef<HTMLDivElement>(null);

  // 질문 목록 로드
  useEffect(() => {
    questionApi.getAll().then(res => {
      if (res.success && res.data && res.data.length > 0) setAllQuestions(res.data);
      else setAllQuestions(mockQuestions);
    }).catch(() => setAllQuestions(mockQuestions));
  }, []);

  // 검색 필터
  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    const q = searchQuery.toLowerCase();
    const results = allQuestions.filter(question =>
      question.title.toLowerCase().includes(q) ||
      (question.category ?? '').toLowerCase().includes(q)
    );
    setSearchResults(results.slice(0, 5));
  }, [searchQuery, allQuestions]);

  // Faucet 상태 조회
  useEffect(() => {
    if (!user || isGuest) return;
    faucetApi.getStatus(user.id).then(status => {
      setFaucetClaimed(status.claimed);
    }).catch(() => {});
  }, [user, isGuest]);

  const handleFaucetClaim = async () => {
    if (!user || faucetClaimed || faucetLoading) return;
    setFaucetLoading(true);
    try {
      const result = await faucetApi.claim(user.id);
      if (result.success) {
        setFaucetClaimed(true);
        refreshUser();
        // 간단한 알림 추가
        setNotifications(prev => [{
          id: Date.now(),
          title: '일일 보상 수령',
          message: `${result.amount}P 일일 보상을 받았습니다! 잔액: ${result.newBalance.toLocaleString()}P`,
          time: '방금',
          read: false,
        }, ...prev]);
      }
    } catch {
      // 이미 수령 등
      setFaucetClaimed(true);
    } finally {
      setFaucetLoading(false);
    }
  };

  // 바깥 클릭 시 닫기
  useEffect(() => {
    const handleClick = (e: MouseEvent) => {
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowResults(false);
      }
      if (notiRef.current && !notiRef.current.contains(e.target as Node)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  const handleSearchSelect = (question: Question) => {
    router.push(`/question/${question.id}`);
    setSearchQuery('');
    setShowResults(false);
  };

  const unreadCount = notifications.filter(n => !n.read).length;

  const markAllRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  const displayAddress = user?.walletAddress
    ? `${user.walletAddress.slice(0, 6)}...${user.walletAddress.slice(-4)}`
    : user?.email || '';

  return (
    <header className={`h-20 border-b px-8 flex items-center justify-between sticky top-0 z-40 transition-all ${isDark ? 'bg-slate-950/80 border-slate-800 backdrop-blur-md' : 'bg-white/80 border-slate-100 backdrop-blur-md'}`}>
      <div ref={searchRef} className="flex-1 max-w-xl relative hidden md:block">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            setShowResults(true);
          }}
          onFocus={() => searchQuery.trim() && setShowResults(true)}
          placeholder="마켓 검색..."
          className={`w-full border-none rounded-2xl py-3.5 pl-12 pr-10 text-sm focus:ring-2 focus:ring-indigo-500/20 transition-all ${isDark ? 'bg-slate-900 text-slate-200' : 'bg-slate-50 text-slate-900'}`}
        />
        {searchQuery && (
          <button onClick={() => { setSearchQuery(''); setShowResults(false); }} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-300">
            <X size={16} />
          </button>
        )}

        {showResults && searchQuery.trim() && (
          <div className={`absolute top-full mt-2 left-0 right-0 rounded-2xl border shadow-2xl overflow-hidden z-50 ${isDark ? 'bg-slate-900 border-slate-700' : 'bg-white border-slate-200'}`}>
            {searchResults.length > 0 ? (
              searchResults.map(q => (
                <button
                  key={q.id}
                  onClick={() => handleSearchSelect(q)}
                  className={`w-full px-5 py-3.5 flex items-center gap-3 text-left transition-all ${isDark ? 'hover:bg-slate-800' : 'hover:bg-slate-50'}`}
                >
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-[10px] font-bold ${q.status === 'OPEN' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-slate-500/10 text-slate-500'}`}>
                    {q.status === 'OPEN' ? 'L' : 'C'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm font-bold truncate ${isDark ? 'text-white' : 'text-slate-900'}`}>{q.title}</p>
                    <p className="text-[10px] text-slate-400">{q.category} &middot; {q.totalBetPool.toLocaleString()} P</p>
                  </div>
                  <span className="text-xs font-bold text-indigo-600">
                    {q.totalBetPool > 0 ? Math.round((q.yesBetPool / q.totalBetPool) * 100) : 50}%
                  </span>
                </button>
              ))
            ) : (
              <div className="px-5 py-6 text-center">
                <p className="text-sm text-slate-400">검색 결과가 없습니다</p>
              </div>
            )}
          </div>
        )}
      </div>

      <div className="flex items-center space-x-4">
        <button
          onClick={toggleTheme}
          className={`p-3 rounded-xl transition-all ${isDark ? 'bg-slate-800 text-amber-400 hover:bg-slate-700' : 'bg-slate-50 text-slate-400 hover:bg-slate-100'}`}
        >
          {isDark ? <Sun size={20} /> : <Moon size={20} />}
        </button>

        {!isGuest && user && (
          <button
            onClick={handleFaucetClaim}
            disabled={faucetClaimed || faucetLoading}
            title={faucetClaimed ? '내일 다시 수령 가능' : '일일 100P 보상 받기'}
            className={`p-3 rounded-xl transition-all relative ${
              faucetClaimed
                ? isDark ? 'text-slate-600 bg-slate-800/50 cursor-not-allowed' : 'text-slate-300 bg-slate-50 cursor-not-allowed'
                : isDark ? 'text-emerald-400 bg-emerald-500/10 hover:bg-emerald-500/20' : 'text-emerald-600 bg-emerald-50 hover:bg-emerald-100'
            }`}
          >
            <Gift size={20} />
            {!faucetClaimed && (
              <span className="absolute top-1.5 right-1.5 w-2.5 h-2.5 bg-emerald-500 rounded-full animate-pulse" />
            )}
          </button>
        )}

        <div ref={notiRef} className="relative">
          <button
            onClick={() => setShowNotifications(!showNotifications)}
            className={`p-3 rounded-xl transition-all relative ${isDark ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-400 hover:bg-slate-100'}`}
          >
            <Bell size={20} />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-4 h-4 bg-rose-500 rounded-full text-[9px] font-bold text-white flex items-center justify-center">
                {unreadCount}
              </span>
            )}
          </button>

          {showNotifications && (
            <div className={`absolute right-0 top-full mt-2 w-80 rounded-2xl border shadow-2xl overflow-hidden z-50 ${isDark ? 'bg-slate-900 border-slate-700' : 'bg-white border-slate-200'}`}>
              <div className={`flex items-center justify-between px-5 py-3 border-b ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
                <h4 className={`font-black text-sm ${isDark ? 'text-white' : 'text-slate-900'}`}>알림</h4>
                {unreadCount > 0 && (
                  <button onClick={markAllRead} className="text-xs text-indigo-500 font-bold hover:underline">
                    모두 읽음
                  </button>
                )}
              </div>
              <div className="max-h-72 overflow-y-auto">
                {notifications.length > 0 ? notifications.map((noti) => (
                  <div
                    key={noti.id}
                    className={`px-5 py-3 border-b last:border-b-0 transition-all ${
                      isDark
                        ? `border-slate-800 ${noti.read ? '' : 'bg-slate-800/50'}`
                        : `border-slate-50 ${noti.read ? '' : 'bg-indigo-50/50'}`
                    }`}
                  >
                    <p className={`text-xs font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>{noti.title}</p>
                    <p className="text-[11px] text-slate-400 mt-0.5">{noti.message}</p>
                    <p className="text-[10px] text-slate-500 mt-1">{noti.time}</p>
                  </div>
                )) : (
                  <div className="px-5 py-8 text-center text-sm text-slate-400">알림이 없습니다</div>
                )}
              </div>
            </div>
          )}
        </div>

        {isGuest ? (
          <button
            onClick={openRegister}
            className="flex items-center space-x-2 px-4 py-2.5 rounded-2xl bg-indigo-600 text-white text-sm font-bold hover:bg-indigo-700 transition-all active:scale-95"
          >
            <UserPlus size={16} />
            <span className="hidden lg:block">회원가입</span>
          </button>
        ) : (
          <div className={`flex items-center space-x-3 border pl-2 pr-4 py-1.5 rounded-2xl transition-all ${isDark ? 'bg-slate-900 border-slate-700' : 'bg-white border-slate-200 shadow-sm'}`}>
            <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center text-[10px] text-white font-bold">P</div>
            <p className="text-xs font-bold font-mono truncate max-w-[120px]">{displayAddress}</p>
          </div>
        )}
      </div>
    </header>
  );
}

interface NotificationItem {
  id: number;
  title: string;
  message: string;
  time: string;
  read: boolean;
}

function getDefaultNotifications(): NotificationItem[] {
  return [
    { id: 1, title: '정산 완료', message: '"비트코인 $100K 돌파" 마켓이 정산되었습니다. +2,400P', time: '2분 전', read: false },
    { id: 2, title: '새 마켓 오픈', message: '"2025 챔피언스리그 우승팀은?" 마켓이 열렸습니다.', time: '15분 전', read: false },
    { id: 3, title: '투표 보상', message: '일일 투표를 완료했습니다. 정확도 보너스: +50P', time: '1시간 전', read: true },
    { id: 4, title: '티어 승급', message: 'BRONZE에서 SILVER로 승급했습니다!', time: '3시간 전', read: true },
  ];
}
