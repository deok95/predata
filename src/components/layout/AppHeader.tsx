'use client';

import React, { useState, useEffect, useRef } from 'react';
import { Search, Bell, X, UserPlus, User } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useRegisterModal } from '@/components/RegisterModal';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { questionApi, notificationApi } from '@/lib/api';
import type { Question, Notification } from '@/types/api';
import PredataLogo from '@/components/ui/PredataLogo';

export default function AppHeader() {
  const { isDark } = useTheme();
  const { user, isGuest } = useAuth();
  const { open: openRegister } = useRegisterModal();
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Question[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [allQuestions, setAllQuestions] = useState<Question[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [notificationsLoading, setNotificationsLoading] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const searchRef = useRef<HTMLDivElement>(null);
  const notiRef = useRef<HTMLDivElement>(null);

  // Load questions list
  useEffect(() => {
    questionApi.getAll().then(res => {
      if (res.success && res.data && res.data.length > 0) setAllQuestions(res.data);
      else setAllQuestions([]);
    }).catch(() => setAllQuestions([]));
  }, []);

  // Load notifications list
  useEffect(() => {
    if (!user || isGuest) {
      return;
    }

    // eslint-disable-next-line react-hooks/set-state-in-effect
    setNotificationsLoading(true);
    notificationApi.getAll(user.id)
      .then(res => {
        if (res.success && res.data) {
          setNotifications(res.data);
        }
      })
      .catch(() => {
        // Silently fail, keep empty notifications
      })
      .finally(() => {
        setNotificationsLoading(false);
      });
  }, [user, isGuest]);

  // Search filter
  useEffect(() => {
    if (!searchQuery.trim()) {
      return;
    }
    const q = searchQuery.toLowerCase();
    const results = allQuestions.filter(question =>
      question.title.toLowerCase().includes(q) ||
      (question.category ?? '').toLowerCase().includes(q)
    );
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setSearchResults(results.slice(0, 5));
  }, [searchQuery, allQuestions]);

  // Close on outside click
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

  const timeAgo = (dateStr: string): string => {
    const now = new Date();
    const date = new Date(dateStr);
    const diff = Math.floor((now.getTime() - date.getTime()) / 1000);

    if (diff < 60) return 'Just now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
  };

  const unreadCount = notifications.filter(n => !n.isRead).length;

  const markAllRead = async () => {
    if (!user) return;
    try {
      await notificationApi.markAllAsRead(user.id);
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (e) {
      // Silently fail
    }
  };

  const handleNotificationClick = async (notification: Notification) => {
    // Mark as read if unread
    if (!notification.isRead) {
      try {
        await notificationApi.markAsRead(notification.id);
        setNotifications(prev => prev.map(n =>
          n.id === notification.id ? { ...n, isRead: true } : n
        ));
      } catch (e) {
        // Silently fail
      }
    }

    // Navigate to related question if exists
    if (notification.relatedQuestionId) {
      router.push(`/question/${notification.relatedQuestionId}`);
      setShowNotifications(false);
    }
  };

  return (
    <header className={`h-16 lg:h-20 border-b px-4 lg:px-8 flex items-center justify-between sticky top-0 z-40 transition-all ${isDark ? 'bg-slate-950/80 border-slate-800 backdrop-blur-md' : 'bg-white/80 border-slate-100 backdrop-blur-md'}`}>
      {/* Left: Logo */}
      <div className="flex items-center flex-shrink-0">
        <Link href="/">
          <PredataLogo iconOnly className="w-8 h-8 lg:w-10 lg:h-10" />
        </Link>
      </div>

      {/* Center: Search Bar */}
      <div ref={searchRef} className="flex-1 mx-2 lg:mx-4 max-w-xl relative">
        <Search className="absolute left-3 lg:left-4 top-1/2 -translate-y-1/2 text-slate-400" size={16} />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            setShowResults(true);
          }}
          onFocus={() => searchQuery.trim() && setShowResults(true)}
          placeholder="Search markets..."
          className={`w-full border-none rounded-2xl py-2.5 lg:py-3.5 pl-9 lg:pl-12 pr-8 lg:pr-10 text-xs lg:text-sm focus:ring-2 focus:ring-indigo-500/20 transition-all ${isDark ? 'bg-slate-900 text-slate-200' : 'bg-slate-50 text-slate-900'}`}
        />
        {searchQuery && (
          <button onClick={() => { setSearchQuery(''); setShowResults(false); }} className="absolute right-2 lg:right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-300">
            <X size={14} />
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
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-[10px] font-bold ${q.status !== 'SETTLED' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-slate-500/10 text-slate-500'}`}>
                    {q.status !== 'SETTLED' ? 'L' : 'C'}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className={`text-sm font-bold truncate ${isDark ? 'text-white' : 'text-slate-900'}`}>{q.title}</p>
                    <p className="text-[10px] text-slate-400">{q.category} &middot; ${q.totalBetPool.toLocaleString()}</p>
                  </div>
                  <span className="text-xs font-bold text-indigo-600">
                    {q.totalBetPool > 0 ? Math.round((q.yesBetPool / q.totalBetPool) * 100) : 50}%
                  </span>
                </button>
              ))
            ) : (
              <div className="px-5 py-6 text-center">
                <p className="text-sm text-slate-400">No results found</p>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Right: Notifications + User */}
      <div className="flex items-center space-x-3 flex-shrink-0">

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
            <div className={`fixed left-4 right-4 top-16 max-h-[70vh] overflow-y-auto lg:absolute lg:left-auto lg:right-0 lg:top-full lg:mt-2 lg:w-80 lg:max-h-none lg:overflow-visible rounded-2xl border shadow-2xl z-50 ${isDark ? 'bg-slate-900 border-slate-700' : 'bg-white border-slate-200'}`}>
              <div className={`flex items-center justify-between px-5 py-3 border-b ${isDark ? 'border-slate-800' : 'border-slate-100'}`}>
                <h4 className={`font-black text-sm ${isDark ? 'text-white' : 'text-slate-900'}`}>Notifications</h4>
                {unreadCount > 0 && (
                  <button onClick={markAllRead} className="text-xs text-indigo-500 font-bold hover:underline">
                    Mark all read
                  </button>
                )}
              </div>
              <div className="max-h-72 overflow-y-auto">
                {notificationsLoading ? (
                  <div className="px-5 py-8 text-center">
                    <div className="w-8 h-8 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto" />
                  </div>
                ) : notifications.length > 0 ? (
                  notifications.map((noti) => (
                    <button
                      key={noti.id}
                      onClick={() => handleNotificationClick(noti)}
                      className={`w-full px-5 py-3 border-b last:border-b-0 transition-all text-left ${
                        isDark
                          ? `border-slate-800 ${noti.isRead ? '' : 'bg-slate-800/50'} hover:bg-slate-800`
                          : `border-slate-50 ${noti.isRead ? '' : 'bg-indigo-50/50'} hover:bg-slate-50`
                      } ${noti.relatedQuestionId ? 'cursor-pointer' : 'cursor-default'}`}
                    >
                      <p className={`text-xs font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>
                        {noti.title}
                      </p>
                      <p className="text-[11px] text-slate-400 mt-0.5">{noti.message}</p>
                      <p className="text-[10px] text-slate-500 mt-1">{timeAgo(noti.createdAt)}</p>
                    </button>
                  ))
                ) : (
                  <div className="px-5 py-8 text-center text-sm text-slate-400">
                    No notifications
                  </div>
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
            <span className="hidden lg:block">Sign Up</span>
          </button>
        ) : (
          <button
            onClick={() => router.push('/my-page')}
            className={`p-3 rounded-xl transition-all ${isDark ? 'bg-slate-800 text-indigo-400 hover:bg-slate-700' : 'bg-slate-100 text-indigo-600 hover:bg-slate-200'}`}
            title="My Page"
          >
            <User size={20} />
          </button>
        )}
      </div>
    </header>
  );
}
