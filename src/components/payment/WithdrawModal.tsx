'use client';

import { useState } from 'react';
import { X, ArrowUpRight, CheckCircle, Loader2, AlertCircle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { useAccount } from 'wagmi';
import { ConnectButton } from '@rainbow-me/rainbowkit';
import { paymentApi } from '@/lib/api';
import { BET_MIN_USDC, BET_MAX_USDC } from '@/lib/contracts';

interface WithdrawModalProps {
  isOpen: boolean;
  onClose: () => void;
}

type WithdrawStep = 'idle' | 'confirming' | 'processing' | 'success' | 'error';

const PRESET_AMOUNTS = [10, 25, 50, 100];

export default function WithdrawModal({ isOpen, onClose }: WithdrawModalProps) {
  const { isDark } = useTheme();
  const { user, refreshUser } = useAuth();
  const { address, isConnected } = useAccount();
  const [amount, setAmount] = useState('');
  const [step, setStep] = useState<WithdrawStep>('idle');
  const [txHash, setTxHash] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !user) return null;

  const numAmount = Number(amount);
  const isValidAmount = numAmount >= BET_MIN_USDC && numAmount <= BET_MAX_USDC && numAmount <= (user.usdcBalance ?? 0);

  const handleWithdraw = async () => {
    if (!isValidAmount || !address) return;

    setStep('processing');
    setError(null);
    try {
      const result = await paymentApi.withdraw(user.id, numAmount, address);
      if (result.success) {
        setTxHash(result.txHash || null);
        await refreshUser();
        setStep('success');
      } else {
        setError(result.message || '출금에 실패했습니다.');
        setStep('error');
      }
    } catch (e: any) {
      setError(e?.message || '출금에 실패했습니다.');
      setStep('error');
    }
  };

  const handleClose = () => {
    setStep('idle');
    setAmount('');
    setTxHash(null);
    setError(null);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center animate-fade-in">
      <div className="absolute inset-0 bg-black/60" onClick={handleClose} />
      <div className={`relative w-full max-w-md rounded-3xl border p-8 shadow-2xl ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <button onClick={handleClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-300">
          <X size={20} />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="w-12 h-12 rounded-2xl bg-rose-600 flex items-center justify-center">
            <ArrowUpRight size={24} className="text-white" />
          </div>
          <div>
            <h2 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>출금하기</h2>
            <p className="text-xs text-slate-400">잔액에서 USDC로 지갑 전송</p>
          </div>
        </div>

        {step === 'idle' && (
          <>
            {!isConnected ? (
              <div className="text-center py-6">
                <p className={`text-sm mb-4 ${isDark ? 'text-slate-400' : 'text-slate-500'}`}>
                  출금하려면 지갑을 연결해야 합니다.
                </p>
                <div className="flex justify-center">
                  <ConnectButton />
                </div>
              </div>
            ) : (
              <>
                <div className={`rounded-xl p-4 mb-4 ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
                  <div className="flex justify-between items-center mb-2">
                    <span className="text-xs text-slate-400">보유 잔액</span>
                    <span className="text-sm font-bold text-indigo-600">{'$'}{(user.usdcBalance ?? 0).toLocaleString()}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-xs text-slate-400">출금 지갑</span>
                    <span className="text-xs font-mono">{address?.slice(0, 6)}...{address?.slice(-4)}</span>
                  </div>
                </div>

                <div className="relative mb-4">
                  <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    placeholder="출금 금액"
                    min={BET_MIN_USDC}
                    max={Math.min(BET_MAX_USDC, user.usdcBalance ?? 0)}
                    className={`w-full px-4 py-3.5 rounded-xl border text-lg font-bold pr-12 ${isDark ? 'bg-slate-800 border-slate-700 text-white' : 'bg-slate-50 border-slate-200 text-slate-900'}`}
                  />
                  <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 font-bold">$</span>
                </div>

                <div className="grid grid-cols-4 gap-2 mb-6">
                  {PRESET_AMOUNTS.map((preset) => (
                    <button
                      key={preset}
                      onClick={() => setAmount(String(Math.min(preset, user.usdcBalance ?? 0)))}
                      disabled={preset > (user.usdcBalance ?? 0)}
                      className={`py-2 rounded-xl text-sm font-bold transition-all ${
                        Number(amount) === preset
                          ? 'bg-rose-600 text-white'
                          : isDark ? 'bg-slate-800 text-slate-300 hover:bg-slate-700' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                      } disabled:opacity-30 disabled:cursor-not-allowed`}
                    >
                      ${preset}
                    </button>
                  ))}
                </div>

                <button
                  onClick={handleWithdraw}
                  disabled={!isValidAmount}
                  className="w-full py-4 rounded-2xl font-black text-lg bg-rose-600 text-white hover:bg-rose-700 transition-all active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  <ArrowUpRight size={20} />
                  {'$'}{amount || '0'} 출금하기
                </button>

                {numAmount > 0 && !isValidAmount && (
                  <p className="text-xs text-rose-400 mt-2 text-center">
                    {numAmount > (user.usdcBalance ?? 0) ? '잔액이 부족합니다.' : `$${BET_MIN_USDC}~$${BET_MAX_USDC} 범위에서 출금 가능합니다.`}
                  </p>
                )}
              </>
            )}
          </>
        )}

        {step === 'processing' && (
          <div className="text-center py-8">
            <Loader2 size={48} className="mx-auto mb-4 text-rose-600 animate-spin" />
            <p className={`font-bold mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>출금 처리 중...</p>
            <p className="text-xs text-slate-400">USDC를 지갑으로 전송하고 있습니다.</p>
          </div>
        )}

        {step === 'success' && (
          <div className="text-center py-8">
            <CheckCircle size={48} className="mx-auto mb-4 text-emerald-500" />
            <p className={`font-black text-lg mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>출금 완료!</p>
            <p className="text-sm text-slate-400">{'$'}{amount}가 지갑으로 전송되었습니다.</p>
            {txHash && (
              <p className="text-xs text-slate-400 font-mono mt-2 break-all">
                TX: {txHash.slice(0, 10)}...{txHash.slice(-8)}
              </p>
            )}
            <button
              onClick={handleClose}
              className="mt-6 px-8 py-3 rounded-2xl font-bold bg-indigo-600 text-white hover:bg-indigo-700 transition-all"
            >
              확인
            </button>
          </div>
        )}

        {step === 'error' && (
          <div className="text-center py-8">
            <AlertCircle size={48} className="mx-auto mb-4 text-rose-500" />
            <p className={`font-bold mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>출금 실패</p>
            <p className="text-sm text-rose-400 mb-6">{error}</p>
            <button
              onClick={() => setStep('idle')}
              className="px-8 py-3 rounded-2xl font-bold bg-indigo-600 text-white hover:bg-indigo-700 transition-all"
            >
              다시 시도
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
