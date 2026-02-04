import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - 알림 센터',
  description: '베팅, 투표, 정산 등의 알림을 확인하세요',
};

export default function NotificationsLayout({ children }: { children: React.ReactNode }) {
  return children;
}
