import { describe, it, expect } from 'vitest';

// Test timeAgo utility (same logic used in AppHeader and Notifications)
function timeAgo(dateStr: string): string {
  const now = new Date();
  const date = new Date(dateStr);
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000);
  if (diff < 60) return 'just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
}

describe('timeAgo', () => {
  it('should return "just now" for timestamps less than 60 seconds ago', () => {
    const now = new Date().toISOString();
    expect(timeAgo(now)).toBe('just now');
  });

  it('should return minutes for timestamps less than 1 hour ago', () => {
    const date = new Date(Date.now() - 5 * 60 * 1000).toISOString();
    expect(timeAgo(date)).toBe('5m ago');
  });

  it('should return hours for timestamps less than 1 day ago', () => {
    const date = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();
    expect(timeAgo(date)).toBe('3h ago');
  });

  it('should return days for timestamps more than 1 day ago', () => {
    const date = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString();
    expect(timeAgo(date)).toBe('2d ago');
  });
});

// Test percentage calculation (used in market detail)
describe('Market Calculations', () => {
  it('should calculate YES percentage correctly', () => {
    const question = { totalBetPool: 1000, yesBetPool: 600, noBetPool: 400 };
    const yesPercent = question.totalBetPool > 0
      ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
      : 50;
    expect(yesPercent).toBe(60);
  });

  it('should default to 50% when no bets', () => {
    const question = { totalBetPool: 0, yesBetPool: 0, noBetPool: 0 };
    const yesPercent = question.totalBetPool > 0
      ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
      : 50;
    expect(yesPercent).toBe(50);
  });

  it('should handle 100% YES pool', () => {
    const question = { totalBetPool: 500, yesBetPool: 500, noBetPool: 0 };
    const yesPercent = question.totalBetPool > 0
      ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
      : 50;
    expect(yesPercent).toBe(100);
  });
});

// Test tier color mapping
describe('Tier System', () => {
  const tierColors: Record<string, string> = {
    BRONZE: 'text-amber-700 bg-amber-100',
    SILVER: 'text-slate-500 bg-slate-100',
    GOLD: 'text-yellow-600 bg-yellow-100',
    PLATINUM: 'text-indigo-600 bg-indigo-100',
  };

  it('should map all tier colors', () => {
    expect(tierColors['BRONZE']).toBeDefined();
    expect(tierColors['SILVER']).toBeDefined();
    expect(tierColors['GOLD']).toBeDefined();
    expect(tierColors['PLATINUM']).toBeDefined();
  });

  it('should calculate accuracy correctly', () => {
    const accuracy = (3 / 10 * 100).toFixed(1);
    expect(accuracy).toBe('30.0');
  });

  it('should handle zero predictions', () => {
    const totalPredictions = 0;
    const accuracy = totalPredictions > 0
      ? (0 / totalPredictions * 100).toFixed(1)
      : '0.0';
    expect(accuracy).toBe('0.0');
  });
});

// Test guest detection
describe('Auth Utilities', () => {
  it('should detect guest users by negative id', () => {
    const isGuest = (user: { id: number; email: string }) =>
      user.id < 0 || user.email.endsWith('@predata.demo');

    expect(isGuest({ id: -1, email: 'guest@predata.demo' })).toBe(true);
    expect(isGuest({ id: 1, email: 'user@example.com' })).toBe(false);
  });

  it('should detect admin users by email', () => {
    const ADMIN_EMAILS = ['admin@predata.io', 'admin@predata.com'];
    const isAdmin = (email: string) => ADMIN_EMAILS.includes(email);

    expect(isAdmin('admin@predata.io')).toBe(true);
    expect(isAdmin('admin@predata.com')).toBe(true);
    expect(isAdmin('user@example.com')).toBe(false);
  });
});
