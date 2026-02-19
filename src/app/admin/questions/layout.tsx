import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - Admin Dashboard',
  description: 'Question management, user management, sports management',
};

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return children;
}
