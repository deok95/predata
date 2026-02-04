import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - 마이페이지',
  description: '나의 프로필, 베팅 내역, 티어 정보를 확인하세요',
};

export default function MyPageLayout({ children }: { children: React.ReactNode }) {
  return children;
}
