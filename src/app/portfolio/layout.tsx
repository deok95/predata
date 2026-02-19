import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'Predata - Portfolio',
  description: 'View your betting history and portfolio analysis',
};

export default function PortfolioLayout({ children }: { children: React.ReactNode }) {
  return children;
}
