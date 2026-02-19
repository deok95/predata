import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - Leaderboard',
  description: 'Top predictors ranking by accuracy',
};

export default function LeaderboardLayout({ children }: { children: React.ReactNode }) {
  return children;
}
