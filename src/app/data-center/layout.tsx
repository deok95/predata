import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - Data Center',
  description: 'Admin only: Data quality analysis and premium data download',
};

export default function DataCenterLayout({ children }: { children: React.ReactNode }) {
  return children;
}
