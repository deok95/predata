'use client';

import { useState, useEffect } from 'react';
import { Bell, Check, CheckCheck } from 'lucide-react';
import MainLayout from '@/components/layout/MainLayout';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { notificationApi } from '@/lib/api';
import { useRouter } from 'next/navigation';
import { Skeleton } from '@/components/ui/Skeleton';
import type { Notification } from '@/types/api';
import { useI18n } from '@/lib/i18n';

function timeAgo(dateStr: string, t: (key: string) => string): string {
  const now = new Date();
  const date = new Date(dateStr);
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000);
  if (diff < 60) return t('time.justNow');
  if (diff < 3600) return `${Math.floor(diff / 60)}${t('time.minutesAgo')}`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}${t('time.hoursAgo')}`;
  return `${Math.floor(diff / 86400)}${t('time.daysAgo')}`;
}

const typeIcons: Record<string, { emoji: string; color: string }> = {
  BET_PLACED: { emoji: '🎲', color: 'bg-indigo-500/10 text-indigo-500' },
  VOTE_RECORDED: { emoji: '🗳️', color: 'bg-emerald-500/10 text-emerald-500' },
  SETTLEMENT_COMPLETE: { emoji: '⚖️', color: 'bg-amber-500/10 text-amber-500' },
  FAUCET_CLAIMED: { emoji: '🎁', color: 'bg-purple-500/10 text-purple-500' },
  TIER_CHANGE: { emoji: '🏆', color: 'bg-yellow-500/10 text-yellow-600' },
};

function NotificationsContent() {
  const { isDark } = useTheme();
  const { user, isGuest } = useAuth();
  const { t } = useI18n();
  const router = useRouter();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(!user || isGuest ? false : true);
  const [filter, setFilter] = useState<'all' | 'unread'>('all');

  useEffect(() => {
    if (!user || isGuest) {
      return;
    }
    notificationApi.getAll(user.id).then(res => {
      if (res.success && res.data) setNotifications(res.data);
    }).catch(() => {}).finally(() => setLoading(false));
  }, [user, isGuest]);

  const handleMarkAsRead = async (id: number) => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
    } catch (e) {
      // silently fail
    }
  };

  const handleMarkAllAsRead = async () => {
    if (!user) return;
    try {
      await notificationApi.markAllAsRead(user.id);
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
    } catch (e) {
      // silently fail
    }
  };

  const filtered = filter === 'unread' ? notifications.filter(n => !n.isRead) : notifications;
  const unreadCount = notifications.filter(n => !n.isRead).length;

  if (!user || isGuest) {
    return (
      <div className="text-center py-20">
        <Bell size={48} className="text-slate-400 mx-auto mb-4" />
        <p className="text-slate-400 text-lg">{t('noti.loginRequired')}</p>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto animate-fade-in">
      <div className="flex items-center justify-between mb-8">
        <div>
          <div className="flex items-center gap-3 mb-2">
            <Bell size={28} className="text-indigo-600" />
            <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('noti.title')}</h1>
            {unreadCount > 0 && (
              <span className="px-2.5 py-1 bg-rose-500 text-white rounded-full text-xs font-bold">{unreadCount}</span>
            )}
          </div>
          <p className="text-slate-400">{t('noti.subtitle')}</p>
        </div>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllAsRead}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold text-indigo-600 hover:bg-indigo-50 dark:hover:bg-indigo-950/20 transition-all"
          >
            <CheckCheck size={16} />
            {t('noti.markAllRead')}
          </button>
        )}
      </div>

      <div className="flex gap-2 mb-6">
        {(['all', 'unread'] as const).map(f => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              filter === f
                ? 'bg-indigo-600 text-white'
                : isDark ? 'bg-slate-800 text-slate-400' : 'bg-slate-100 text-slate-500'
            }`}
          >
            {f === 'all' ? `${t('noti.allCount')} (${notifications.length})` : `${t('noti.unreadCount')} (${unreadCount})`}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className={`p-5 rounded-2xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
              <div className="flex items-start gap-4">
                <Skeleton className="w-10 h-10 rounded-xl" />
                <div className="flex-1 space-y-2">
                  <Skeleton className="h-4 w-40" />
                  <Skeleton className="h-3 w-full" />
                  <Skeleton className="h-3 w-20" />
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className={`text-center py-16 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
          <Bell size={48} className="text-slate-400 mx-auto mb-4" />
          <p className={`font-bold ${isDark ? 'text-white' : 'text-slate-900'}`}>
            {filter === 'unread' ? t('noti.noUnread') : t('noti.noNotifications')}
          </p>
          <p className="text-sm text-slate-400 mt-1">{t('noti.activityHint')}</p>
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map((noti) => {
            const typeInfo = typeIcons[noti.type] || { emoji: '📌', color: 'bg-slate-500/10 text-slate-500' };
            return (
              <button
                key={noti.id}
                onClick={() => {
                  if (!noti.isRead) handleMarkAsRead(noti.id);
                  if (noti.relatedQuestionId) router.push(`/question/${noti.relatedQuestionId}`);
                }}
                className={`w-full text-left p-5 rounded-2xl border transition-all ${
                  noti.isRead
                    ? isDark ? 'bg-slate-900 border-slate-800 hover:bg-slate-800' : 'bg-white border-slate-100 hover:bg-slate-50'
                    : isDark ? 'bg-indigo-950/20 border-indigo-900/30 hover:bg-indigo-950/30' : 'bg-indigo-50/50 border-indigo-100 hover:bg-indigo-50'
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center text-lg ${typeInfo.color}`}>
                    {typeInfo.emoji}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className={`font-bold text-sm ${isDark ? 'text-white' : 'text-slate-900'}`}>{noti.title}</p>
                      {!noti.isRead && <span className="w-2 h-2 bg-indigo-500 rounded-full flex-shrink-0" />}
                    </div>
                    <p className="text-sm text-slate-400 mt-0.5">{noti.message}</p>
                    <p className="text-xs text-slate-500 mt-1.5">{timeAgo(noti.createdAt, t)}</p>
                  </div>
                  {!noti.isRead && (
                    <button
                      onClick={(e) => { e.stopPropagation(); handleMarkAsRead(noti.id); }}
                      className="p-2 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700 transition-all text-slate-400"
                      title={t('noti.markRead')}
                    >
                      <Check size={14} />
                    </button>
                  )}
                </div>
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function NotificationsPage() {
  return (
    <MainLayout>
      <NotificationsContent />
    </MainLayout>
  );
}
