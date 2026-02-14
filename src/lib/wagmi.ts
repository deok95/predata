import { getDefaultConfig } from '@rainbow-me/rainbowkit';
import { polygon, polygonAmoy } from 'wagmi/chains';

export const config = getDefaultConfig({
  appName: 'Predata',
  projectId: process.env.NEXT_PUBLIC_WALLET_CONNECT_PROJECT_ID || 'YOUR_PROJECT_ID',
  chains: [
    polygonAmoy, // 테스트넷 (Amoy)
    polygon,     // 메인넷
  ],
  ssr: true, // Next.js SSR 지원
});
