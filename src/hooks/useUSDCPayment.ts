'use client';

import { useState, useCallback } from 'react';
import { useAccount, useWriteContract } from 'wagmi';
import { ERC20_ABI, ACTIVE_USDC_ADDRESS, RECEIVER_WALLET, toUSDCUnits } from '@/lib/contracts';
import { paymentApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';

export type PaymentStep = 'idle' | 'sending' | 'confirming' | 'verifying' | 'success' | 'error';

interface UseUSDCPaymentReturn {
  step: PaymentStep;
  txHash: string | undefined;
  error: string | null;
  isConnected: boolean;
  address: string | undefined;
  sendForDeposit: (amount: number) => Promise<void>;
  reset: () => void;
}

export function useUSDCPayment(): UseUSDCPaymentReturn {
  const { address, isConnected } = useAccount();
  const { user, refreshUser } = useAuth();
  const { writeContractAsync } = useWriteContract();

  const [step, setStep] = useState<PaymentStep>('idle');
  const [txHash, setTxHash] = useState<string | undefined>();
  const [error, setError] = useState<string | null>(null);

  const reset = useCallback(() => {
    setStep('idle');
    setTxHash(undefined);
    setError(null);
  }, []);

  // Execute USDC transfer → return tx hash
  const sendUSDC = useCallback(async (amountUsdc: number): Promise<string> => {
    if (!isConnected || !address) {
      throw new Error('Wallet is not connected.');
    }

    const amountRaw = toUSDCUnits(amountUsdc);

    const hash = await writeContractAsync({
      address: ACTIVE_USDC_ADDRESS,
      abi: ERC20_ABI,
      functionName: 'transfer',
      args: [RECEIVER_WALLET, amountRaw],
    });

    return hash;
  }, [isConnected, address, writeContractAsync]);

  // Wait for tx receipt
  const waitForTx = useCallback(async (hash: string): Promise<void> => {
    const maxAttempts = 60; // 60 * 2s = 120 second timeout
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise(resolve => setTimeout(resolve, 2000));
      try {
        const response = await fetch(
          `https://rpc-amoy.polygon.technology`,
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              jsonrpc: '2.0',
              method: 'eth_getTransactionReceipt',
              params: [hash],
              id: 1,
            }),
          }
        );
        const data = await response.json();
        if (data.result && data.result.status === '0x1') {
          return; // Success
        }
        if (data.result && data.result.status === '0x0') {
          throw new Error('Transaction failed.');
        }
      } catch (e: unknown) {
        const error = e as { message?: string };
        if (error.message === 'Transaction failed.') throw e;
      }
    }
    throw new Error('Transaction confirmation timed out.');
  }, []);

  // USDC transfer for balance deposit + backend verification
  const sendForDeposit = useCallback(async (amount: number) => {
    if (!user || !address) return;
    try {
      reset();
      setStep('sending');

      const hash = await sendUSDC(amount);
      setTxHash(hash);

      setStep('confirming');
      await waitForTx(hash);

      setStep('verifying');
      await paymentApi.verifyDeposit(user.id, hash, amount, address);

      await refreshUser();
      setStep('success');
    } catch (e: unknown) {
      const error = e as { shortMessage?: string; message?: string };
      setError(error?.shortMessage || error?.message || 'Deposit failed.');
      setStep('error');
    }
  }, [user, address, sendUSDC, waitForTx, refreshUser, reset]);

  return {
    step,
    txHash,
    error,
    isConnected,
    address,
    sendForDeposit,
    reset,
  };
}
