import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - 리더보드',
  description: '예측 정확도 기준 상위 예측가 랭킹',
};

export default function LeaderboardLayout({ children }: { children: React.ReactNode }) {
  return children;
}
