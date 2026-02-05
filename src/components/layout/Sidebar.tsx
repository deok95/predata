'use client';

import React from 'react';
import { LayoutDashboard, Globe, BarChart3, User, LogOut, UserPlus, Vote } from 'lucide-react';
import { usePathname } from 'next/navigation';
import Link from 'next/link';
import PredataLogo from '@/components/ui/PredataLogo';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import type { Member } from '@/types/api';

interface SidebarProps {
  user: Member | null;
  onLogout: () => void;
}

const navItems = [
  { id: '/', icon: LayoutDashboard, label: '메인 광장' },
  { id: '/vote', icon: Vote, label: '투표' },
  { id: '/marketplace', icon: Globe, label: '마켓 탐색' },
  { id: '/data-center', icon: BarChart3, label: '데이터 인사이트' },
  { id: '/my-page', icon: User, label: '마이페이지' },
];

export default function Sidebar({ user, onLogout }: SidebarProps) {
  const pathname = usePathname();
  const { isDark } = useTheme();
  const { isGuest } = useAuth();
  const { open: openRegister } = useRegisterModal();

  const isActive = (path: string) => {
    if (path === '/') return pathname === '/';
    return pathname.startsWith(path);
  };

  // ADMIN 권한 체크
  const isAdmin = user && user.role === 'ADMIN';

  // 권한에 따라 메뉴 필터링
  const visibleNavItems = navItems.filter(item => {
    if (item.id === '/data-center') {
      return isAdmin; // 데이터 센터는 ADMIN만
    }
    return true; // 나머지 메뉴는 모두 표시
  });

  return (
    <aside className={`fixed lg:relative z-50 w-20 lg:w-72 h-screen border-r flex flex-col items-center lg:items-stretch transition-all duration-300 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className="p-8">
        <div className="hidden lg:flex">
          <PredataLogo />
        </div>
        <div className="flex lg:hidden">
          <PredataLogo iconOnly />
        </div>
      </div>

      <nav className="flex-1 px-4 space-y-2 mt-4">
        {visibleNavItems.map((item) => (
          <Link
            key={item.id}
            href={item.id}
            className={`w-full flex items-center space-x-3 px-4 py-3.5 rounded-2xl transition-all ${
              isActive(item.id)
                ? (isDark ? 'bg-indigo-600 text-white' : 'bg-slate-900 text-white shadow-xl')
                : (isDark ? 'text-slate-500 hover:bg-slate-800' : 'text-slate-400 hover:bg-slate-50')
            }`}
          >
            <item.icon size={22} />
            <span className="font-bold hidden lg:block">{item.label}</span>
          </Link>
        ))}
      </nav>

      <div className="p-6 space-y-2">
        {isGuest && (
          <button
            onClick={openRegister}
            className="w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all font-bold bg-indigo-600 text-white hover:bg-indigo-700"
          >
            <UserPlus size={20} />
            <span className="hidden lg:block">회원가입</span>
          </button>
        )}
        <button
          onClick={onLogout}
          className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all font-bold ${isDark ? 'text-rose-400 hover:bg-rose-500/10' : 'text-rose-500 hover:bg-rose-50'}`}
        >
          <LogOut size={20} />
          <span className="hidden lg:block">{isGuest ? '나가기' : '로그아웃'}</span>
        </button>
      </div>
    </aside>
  );
}
