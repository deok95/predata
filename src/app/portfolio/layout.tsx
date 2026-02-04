import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - 포트폴리오',
  description: '베팅 내역과 포트폴리오 분석을 확인하세요',
};

export default function PortfolioLayout({ children }: { children: React.ReactNode }) {
  return children;
}
