'use client';

import MainLayout from '@/components/layout/MainLayout';
import UserProfile from '@/components/my-page/UserProfile';
import TierProgress from '@/components/my-page/TierProgress';
import BetHistory from '@/components/my-page/BetHistory';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { UserPlus, TrendingUp, Award, History } from 'lucide-react';

function GuestMyPage() {
  const { isDark } = useTheme();
  const { open: openRegister } = useRegisterModal();

  const features = [
    { icon: TrendingUp, title: '예측 기록', desc: '나의 베팅/투표 히스토리를 한눈에' },
    { icon: Award, title: '티어 & 보상', desc: '정확도에 따른 등급과 보상 확인' },
    { icon: History, title: '수익 분석', desc: '수익률과 상세 정산 내역 추적' },
  ];

  return (
    <div className="max-w-2xl mx-auto text-center py-16 animate-fade-in">
      <div className={`w-20 h-20 rounded-full flex items-center justify-center mx-auto mb-6 ${isDark ? 'bg-indigo-500/10' : 'bg-indigo-50'}`}>
        <UserPlus size={36} className="text-indigo-500" />
      </div>
      <h1 className={`text-3xl font-black mb-3 ${isDark ? 'text-white' : 'text-slate-900'}`}>
        회원가입하고 시작하세요
      </h1>
      <p className={`text-lg mb-10 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
        회원가입하면 나의 예측 기록과 보상을 확인할 수 있어요
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
        회원가입하기
      </button>
    </div>
  );
}

function MyPageContent() {
  const { isDark } = useTheme();
  const { user, isGuest } = useAuth();

  if (!user) {
    return (
      <div className="text-center py-20">
        <p className="text-slate-400 text-lg">로그인이 필요합니다.</p>
      </div>
    );
  }

  if (isGuest) {
    return <GuestMyPage />;
  }

  return (
    <div className="max-w-7xl mx-auto animate-fade-in">
      <div className="mb-8">
        <h1 className={`text-3xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>마이페이지</h1>
        <p className="text-slate-400">내 프로필과 베팅 내역을 확인하세요</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-5 space-y-6">
          <UserProfile user={user} />
          <TierProgress user={user} />
        </div>
        <div className="lg:col-span-7">
          <BetHistory memberId={user.id} />
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
