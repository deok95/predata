import { useState, useEffect } from "react";

const AMOY_CHAIN_ID = 80002;
const AMOY_CHAIN_HEX = "0x13882";

declare global {
  interface Window {
    ethereum?: any;
  }
}

export function useWallet() {
  const [account, setAccount] = useState<string | null>(null);
  const [chainId, setChainId] = useState<number | null>(null);
  const [connecting, setConnecting] = useState(false);

  const isMetaMask = () =>
    typeof window.ethereum !== "undefined" && window.ethereum?.isMetaMask === true;

  // Restore connected account on mount
  useEffect(() => {
    if (!isMetaMask()) return;
    window.ethereum
      .request({ method: "eth_accounts" })
      .then((accounts: string[]) => {
        if (accounts.length > 0) {
          setAccount(accounts[0]);
          window.ethereum
            .request({ method: "eth_chainId" })
            .then((cid: string) => setChainId(parseInt(cid, 16)));
        }
      })
      .catch(() => {});

    const onAccountsChanged = (accounts: string[]) =>
      setAccount(accounts.length > 0 ? accounts[0] : null);
    const onChainChanged = (cid: string) => setChainId(parseInt(cid, 16));

    window.ethereum.on("accountsChanged", onAccountsChanged);
    window.ethereum.on("chainChanged", onChainChanged);
    return () => {
      window.ethereum.removeListener("accountsChanged", onAccountsChanged);
      window.ethereum.removeListener("chainChanged", onChainChanged);
    };
  }, []);

  const connect = async () => {
    if (!isMetaMask()) {
      window.open("https://metamask.io/download/", "_blank");
      return;
    }
    setConnecting(true);
    try {
      const accounts = await Promise.race<string[]>([
        window.ethereum.request({ method: "eth_requestAccounts" }),
        new Promise<never>((_, reject) =>
          setTimeout(
            () => reject(new Error("Connection timeout. Please open MetaMask and try again.")),
            10_000
          )
        ),
      ]);
      setAccount(accounts[0]);
      const cid = await window.ethereum.request({ method: "eth_chainId" });
      setChainId(parseInt(cid, 16));
    } catch {
      // user rejected
    } finally {
      setConnecting(false);
    }
  };

  const switchToAmoy = async () => {
    if (!isMetaMask()) return;
    try {
      await window.ethereum.request({
        method: "wallet_switchEthereumChain",
        params: [{ chainId: AMOY_CHAIN_HEX }],
      });
    } catch (e: any) {
      if (e.code === 4902) {
        await window.ethereum.request({
          method: "wallet_addEthereumChain",
          params: [
            {
              chainId: AMOY_CHAIN_HEX,
              chainName: "Polygon Amoy Testnet",
              nativeCurrency: { name: "MATIC", symbol: "MATIC", decimals: 18 },
              rpcUrls: ["https://rpc-amoy.polygon.technology"],
              blockExplorerUrls: ["https://amoy.polygonscan.com/"],
            },
          ],
        });
      }
    }
  };

  // ERC-20 transfer(address, uint256) — USDC has 6 decimals
  const sendUSDC = async (
    to: string,
    usdcContract: string,
    amountUSDC: number
  ): Promise<string> => {
    if (!account) throw new Error("Wallet not connected");
    if (chainId !== AMOY_CHAIN_ID) await switchToAmoy();

    const paddedTo = to.replace("0x", "").toLowerCase().padStart(64, "0");
    const amountRaw = BigInt(Math.round(amountUSDC * 1_000_000));
    const paddedAmount = amountRaw.toString(16).padStart(64, "0");
    const data = `0xa9059cbb${paddedTo}${paddedAmount}`;

    const txHash = await window.ethereum.request({
      method: "eth_sendTransaction",
      params: [{ from: account, to: usdcContract, data }],
    });
    return txHash as string;
  };

  const shortAddress = (addr: string | null) =>
    addr ? `${addr.slice(0, 6)}...${addr.slice(-4)}` : null;

  return {
    account,
    chainId,
    connecting,
    isCorrectChain: chainId === AMOY_CHAIN_ID,
    connect,
    switchToAmoy,
    sendUSDC,
    shortAddress,
    isMetaMask,
  };
}
