'use client';

import React from 'react';
import { LayoutDashboard, Globe, BarChart3, User, LogOut, UserPlus, Vote, Settings } from 'lucide-react';
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
  { id: '/', icon: LayoutDashboard, label: '메인 광장', adminOnly: false },
  { id: '/vote', icon: Vote, label: '투표', adminOnly: false },
  { id: '/marketplace', icon: Globe, label: '마켓 탐색', adminOnly: false },
  { id: '/my-page', icon: User, label: '마이페이지', adminOnly: false },
  // 어드민 전용 메뉴
  { id: '/admin/questions', icon: Settings, label: '질문 관리', adminOnly: true },
  { id: '/data-center', icon: BarChart3, label: '데이터센터', adminOnly: true },
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
    if (item.adminOnly) {
      return isAdmin; // adminOnly 메뉴는 ADMIN만 표시
    }
    return true; // 나머지 메뉴는 모두 표시
  });

  // 모바일용 주요 네비게이션 아이템 (어드민 메뉴 제외)
  const mobileNavItems = navItems.filter(item => !item.adminOnly);

  return (
    <>
      {/* Desktop Sidebar */}
      <aside className={`hidden lg:flex lg:relative z-50 lg:w-72 h-screen border-r flex-col transition-all duration-300 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <div className="p-8">
          <PredataLogo />
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
              <span className="font-bold">{item.label}</span>
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
              <span>회원가입</span>
            </button>
          )}
          <button
            onClick={onLogout}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all font-bold ${isDark ? 'text-rose-400 hover:bg-rose-500/10' : 'text-rose-500 hover:bg-rose-50'}`}
          >
            <LogOut size={20} />
            <span>{isGuest ? '나가기' : '로그아웃'}</span>
          </button>
        </div>
      </aside>

      {/* Mobile Bottom Navigation */}
      <nav className={`lg:hidden fixed bottom-0 left-0 right-0 h-16 border-t z-50 ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <div className="h-full flex items-center justify-around px-2">
          {mobileNavItems.map((item) => (
            <Link
              key={item.id}
              href={item.id}
              className={`flex flex-col items-center justify-center gap-1 px-3 py-2 rounded-lg transition-all ${
                isActive(item.id)
                  ? (isDark ? 'text-indigo-400' : 'text-indigo-600')
                  : (isDark ? 'text-slate-500' : 'text-slate-400')
              }`}
            >
              <item.icon size={20} />
              <span className="text-[10px] font-bold">{item.label}</span>
            </Link>
          ))}
        </div>
      </nav>
    </>
  );
}
