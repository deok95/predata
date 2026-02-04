import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - 관리자 대시보드',
  description: '질문 관리, 유저 관리, 스포츠 관리',
};

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return children;
}
