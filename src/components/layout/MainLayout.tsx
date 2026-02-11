'use client';

import React, { useEffect } from 'react';
import Sidebar from '@/components/layout/Sidebar';
import AppHeader from '@/components/layout/AppHeader';
import LoginModal from '@/components/LoginModal';
import { RegisterModalProvider } from '@/components/RegisterModal';
import { ToastProvider } from '@/components/ui/Toast';
import { useAuth } from '@/hooks/useAuth';
import { useTheme } from '@/hooks/useTheme';
import { safeLocalStorage } from '@/lib/safeLocalStorage';

export default function MainLayout({ children }: { children: React.ReactNode }) {
  const { user, isLoading, isAuthenticated, logout } = useAuth();
  const { isDark } = useTheme();

  // bfcache 복원 감지: 로그아웃 후 뒤로가기 시 stale state 방지
  useEffect(() => {
    const handlePageShow = (event: PageTransitionEvent) => {
      if (event.persisted) {
        const token = safeLocalStorage.getItem('token');
        const savedUser = safeLocalStorage.getItem('predataUser');
        if (!token && !savedUser) {
          window.location.reload();
        }
      }
    };
    window.addEventListener('pageshow', handlePageShow);
    return () => window.removeEventListener('pageshow', handlePageShow);
  }, []);

  if (isLoading) {
    return (
      <div className={`min-h-screen flex items-center justify-center ${isDark ? 'bg-slate-950' : 'bg-slate-50'}`}>
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-slate-400 font-medium">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <ToastProvider>
      <RegisterModalProvider>
        {!isAuthenticated ? (
          <LoginModal />
        ) : (
          <div className={`min-h-screen flex font-sans transition-colors duration-300 ${isDark ? 'bg-slate-950 text-slate-50' : 'bg-[#FDFDFD] text-slate-900'}`}>
            <Sidebar user={user} onLogout={logout} />
            <main className="flex-1 flex flex-col min-w-0 h-screen overflow-hidden">
              <AppHeader />
              <div className="flex-1 overflow-y-auto p-8 custom-scrollbar">
                {children}
              </div>
            </main>
          </div>
        )}
      </RegisterModalProvider>
    </ToastProvider>
  );
}
