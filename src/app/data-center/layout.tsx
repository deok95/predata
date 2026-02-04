import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - 데이터센터',
  description: '관리자 전용: 데이터 품질 분석 및 프리미엄 데이터 다운로드',
};

export default function DataCenterLayout({ children }: { children: React.ReactNode }) {
  return children;
}
