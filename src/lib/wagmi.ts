import { getDefaultConfig } from '@rainbow-me/rainbowkit';
import { polygon, polygonAmoy } from 'wagmi/chains';

export const config = getDefaultConfig({
  appName: 'Predata',
  projectId: process.env.NEXT_PUBLIC_WALLET_CONNECT_PROJECT_ID || 'YOUR_PROJECT_ID',
  chains: [
    polygonAmoy, // Testnet (Amoy)
    polygon,     // Mainnet
  ],
  // Disabled to prevent some Web3 SDKs from breaking build by attempting external network calls in SSR/build environment.
  // (e.g., api.web3modal.org usage fetch)
  ssr: false,
});
