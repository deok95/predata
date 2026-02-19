import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - Marketplace',
  description: 'Participate in various prediction markets and analyze trends',
};

export default function MarketplaceLayout({ children }: { children: React.ReactNode }) {
  return children;
}
