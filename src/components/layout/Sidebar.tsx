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
  { id: '/', icon: LayoutDashboard, label: 'Home', adminOnly: false },
  { id: '/vote', icon: Vote, label: 'Vote', adminOnly: false },
  { id: '/marketplace', icon: Globe, label: 'Explore Markets', adminOnly: false },
  { id: '/my-page', icon: User, label: 'My Page', adminOnly: false },
  // Admin only menu
  { id: '/admin/questions', icon: Settings, label: 'Admin', adminOnly: true },
  { id: '/data-center', icon: BarChart3, label: 'Data Center', adminOnly: true },
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

  // Check ADMIN permission
  const isAdmin = user && user.role === 'ADMIN';

  // Filter menu items based on permission
  const visibleNavItems = navItems.filter(item => {
    if (item.adminOnly) {
      return isAdmin; // Only show admin menu items to ADMIN users
    }
    return true; // Show all other menu items
  });

  // Mobile navigation items (exclude admin menu)
  const mobileNavItems = navItems.filter(item => !item.adminOnly);

  return (
    <>
      {/* Desktop Sidebar */}
      <aside className={`hidden lg:flex sticky top-0 z-50 lg:w-72 h-screen border-r flex-col transition-all duration-300 overflow-y-auto ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
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
              <span>Sign Up</span>
            </button>
          )}
          <button
            onClick={onLogout}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl transition-all font-bold ${isDark ? 'text-rose-400 hover:bg-rose-500/10' : 'text-rose-500 hover:bg-rose-50'}`}
          >
            <LogOut size={20} />
            <span>{isGuest ? 'Exit' : 'Logout'}</span>
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
