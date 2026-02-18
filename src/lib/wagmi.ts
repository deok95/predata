import { getDefaultConfig } from '@rainbow-me/rainbowkit';
import { polygon, polygonAmoy } from 'wagmi/chains';

export const config = getDefaultConfig({
  appName: 'Predata',
  projectId: process.env.NEXT_PUBLIC_WALLET_CONNECT_PROJECT_ID || 'YOUR_PROJECT_ID',
  chains: [
    polygonAmoy, // 테스트넷 (Amoy)
    polygon,     // 메인넷
  ],
  // SSR/build 환경에서 일부 Web3 SDK가 외부 네트워크 호출을 시도해 빌드를 깨뜨릴 수 있어 비활성화.
  // (예: api.web3modal.org usage fetch)
  ssr: false,
});
