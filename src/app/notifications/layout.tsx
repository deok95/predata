import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - Notification Center',
  description: 'Check notifications for bets, votes, settlements, and more',
};

export default function NotificationsLayout({ children }: { children: React.ReactNode }) {
  return children;
}
