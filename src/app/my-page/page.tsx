'use client';

import { useState } from 'react';
import MainLayout from '@/components/layout/MainLayout';
import UserProfile from '@/components/my-page/UserProfile';
import TierProgress from '@/components/my-page/TierProgress';
import BetHistory from '@/components/my-page/BetHistory';
import TransactionHistory from '@/components/my-page/TransactionHistory';
import TicketShop from '@/components/my-page/TicketShop';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { UserPlus, TrendingUp, Award, History } from 'lucide-react';

function GuestMyPage() {
  const { isDark } = useTheme();
  const { open: openRegister } = useRegisterModal();

  const features = [
    { icon: TrendingUp, title: 'Prediction History', desc: 'View all your betting/voting history' },
    { icon: Award, title: 'Tier & Rewards', desc: 'Check your tier and rewards by accuracy' },
    { icon: History, title: 'Profit Analysis', desc: 'Track profit rate and settlement details' },
  ];

  return (
    <div className="max-w-2xl mx-auto text-center py-16 animate-fade-in">
      <div className={`w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6 ${isDark ? 'bg-indigo-500/10' : 'bg-indigo-50'}`}>
        <UserPlus size={36} className="text-indigo-500" />
      </div>
      <h1 className={`text-3xl font-black mb-3 ${isDark ? 'text-white' : 'text-slate-900'}`}>
        Sign up to get started
      </h1>
      <p className={`text-lg mb-10 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
        View your prediction history and rewards after signing up
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-10">
        {features.map((f) => (
          <div key={f.title} className={`p-5 rounded-2xl ${isDark ? 'bg-slate-900 border border-slate-800' : 'bg-white border border-slate-100 shadow-sm'}`}>
            <f.icon size={24} className="text-indigo-500 mx-auto mb-3" />
            <p className={`font-bold text-sm mb-1 ${isDark ? 'text-white' : 'text-slate-900'}`}>{f.title}</p>
            <p className="text-xs text-slate-400">{f.desc}</p>
          </div>
        ))}
      </div>

      <button
        onClick={openRegister}
        className="px-8 py-4 rounded-2xl font-black text-lg bg-indigo-600 text-white hover:bg-indigo-700 transition-all active:scale-95 shadow-xl shadow-indigo-500/20 inline-flex items-center gap-2"
      >
        <UserPlus size={20} />
        Sign Up
      </button>
    </div>
  );
}

function MyPageContent() {
  const { isDark } = useTheme();
  const { user, isGuest } = useAuth();
  const [rightTab, setRightTab] = useState<'bets' | 'transactions'>('bets');

  if (!user) {
    return (
      <div className="text-center py-20">
        <p className="text-slate-400 text-lg">Login required.</p>
      </div>
    );
  }

  if (isGuest) {
    return <GuestMyPage />;
  }

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <div className="mb-8">
        <h1 className={`text-3xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>My Page</h1>
        <p className="text-slate-400">View your profile and betting history</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-5 space-y-6">
          <UserProfile user={user} />
          <TicketShop user={user} />
          <TierProgress user={user} />
        </div>
        <div className="lg:col-span-7 space-y-4">
          <div className="flex space-x-2">
            <button
              onClick={() => setRightTab('bets')}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                rightTab === 'bets'
                  ? 'bg-indigo-600 text-white'
                  : isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'
              }`}
            >
              Bet History
            </button>
            <button
              onClick={() => setRightTab('transactions')}
              className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                rightTab === 'transactions'
                  ? 'bg-indigo-600 text-white'
                  : isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'
              }`}
            >
              Transaction History
            </button>
          </div>

          {rightTab === 'bets' ? (
            <BetHistory memberId={user.id} />
          ) : (
            <TransactionHistory memberId={user.id} />
          )}
        </div>
      </div>
    </div>
  );
}

export default function MyPage() {
  return (
    <MainLayout>
      <MyPageContent />
    </MainLayout>
  );
}
