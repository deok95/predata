import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - 마켓 탐색',
  description: '다양한 예측 마켓에 참여하고 트렌드를 분석하세요',
};

export default function MarketplaceLayout({ children }: { children: React.ReactNode }) {
  return children;
}
