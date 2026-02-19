import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - My Page',
  description: 'Check your profile, betting history, and tier information',
};

export default function MyPageLayout({ children }: { children: React.ReactNode }) {
  return children;
}
