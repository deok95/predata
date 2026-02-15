'use client';

import { useState } from 'react';
import { X, DollarSign, Wallet, CheckCircle, Loader2, AlertCircle } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useUSDCPayment, PaymentStep } from '@/hooks/useUSDCPayment';
import { ConnectButton } from '@rainbow-me/rainbowkit';
import { BET_MIN_USDC, BET_MAX_USDC } from '@/lib/contracts';

interface DepositModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const PRESET_AMOUNTS = [10, 25, 50, 100];

export default function DepositModal({ isOpen, onClose }: DepositModalProps) {
  const { isDark } = useTheme();
  const { step, txHash, error, isConnected, sendForDeposit, reset } = useUSDCPayment();
  const [amount, setAmount] = useState<string>('');

  if (!isOpen) return null;

  const numAmount = parseFloat(amount) || 0;
  const isValidAmount = numAmount >= 1;

  const handleDeposit = () => {
    if (!isValidAmount) return;
    sendForDeposit(numAmount);
  };

  const handleClose = () => {
    reset();
    setAmount('');
    onClose();
  };

  const stepLabels: Record<PaymentStep, string> = {
    idle: '',
    sending: 'MetaMask에서 트랜잭션을 승인해주세요...',
    confirming: '블록체인에서 트랜잭션 확인 중...',
    verifying: '서버에서 충전을 검증 중...',
    success: '충전이 완료되었습니다!',
    error: error || '충전에 실패했습니다.',
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center animate-fade-in">
      <div className="absolute inset-0 bg-black/60" onClick={handleClose} />
      <div className={`relative w-full max-w-md rounded-3xl border p-8 shadow-2xl ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <button onClick={handleClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-300">
          <X size={20} />
        </button>

        <div className="flex items-center gap-3 mb-6">
          <div className="w-12 h-12 rounded-2xl bg-emerald-600 flex items-center justify-center">
            <DollarSign size={24} className="text-white" />
          </div>
          <div>
            <h2 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>$ 충전</h2>
            <p className="text-xs text-slate-400">최소 $1부터</p>
          </div>
        </div>

        {step === 'idle' && (
          <>
            {/* 금액 입력 */}
            <div className="mb-4">
              <div className="relative">
                <input
                  type="number"
                  min={1}
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="충전할 금액"
                  className={`w-full p-5 rounded-2xl border-2 font-black text-2xl transition-all ${
                    isDark
                      ? 'bg-slate-800 border-slate-700 text-white focus:border-emerald-500'
                      : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-emerald-500'
                  }`}
                />
                <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 font-bold">$</span>
              </div>
            </div>

            {/* 프리셋 버튼 */}
            <div className="grid grid-cols-4 gap-2 mb-6">
              {PRESET_AMOUNTS.map((preset) => (
                <button
                  key={preset}
                  onClick={() => setAmount(preset.toString())}
                  className={`py-2.5 rounded-xl text-sm font-bold transition-all ${
                    numAmount === preset
                      ? 'bg-emerald-600 text-white'
                      : isDark ? 'bg-slate-800 text-slate-400 hover:bg-emerald-600 hover:text-white' : 'bg-slate-100 text-slate-500 hover:bg-emerald-600 hover:text-white'
                  }`}
                >
                  ${preset}
                </button>
              ))}
            </div>

            {/* 안내 메시지 */}
            {amount && !isValidAmount && (
              <p className="text-xs text-rose-400 mb-4">
                최소 $1 이상 충전해야 합니다.
              </p>
            )}

            {/* 지갑 연결 & 충전 */}
            {!isConnected ? (
              <div className="flex justify-center">
                <ConnectButton />
              </div>
            ) : (
              <button
                onClick={handleDeposit}
                disabled={!isValidAmount}
                className="w-full py-4 rounded-2xl font-black text-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 transition-all active:scale-[0.98] flex items-center justify-center gap-2"
              >
                <Wallet size={20} />
                {isValidAmount ? `$${numAmount} 충전하기` : '금액을 입력하세요'}
              </button>
            )}
          </>
        )}

        {/* 진행 중 상태 */}
        {(step === 'sending' || step === 'confirming' || step === 'verifying') && (
          <div className="text-center py-8">
            <Loader2 size={48} className="mx-auto mb-4 text-emerald-500 animate-spin" />
            <p className={`font-bold mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>{stepLabels[step]}</p>
            {txHash && (
              <p className="text-xs text-slate-400 font-mono break-all">
                TX: {txHash.slice(0, 10)}...{txHash.slice(-8)}
              </p>
            )}
          </div>
        )}

        {/* 성공 */}
        {step === 'success' && (
          <div className="text-center py-8">
            <CheckCircle size={48} className="mx-auto mb-4 text-emerald-500" />
            <p className={`font-black text-lg mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>충전 완료!</p>
            <p className="text-sm text-slate-400">${numAmount}가 계정에 반영되었습니다.</p>
            <button
              onClick={handleClose}
              className="mt-6 px-8 py-3 rounded-2xl font-bold bg-emerald-600 text-white hover:bg-emerald-700 transition-all"
            >
              확인
            </button>
          </div>
        )}

        {/* 에러 */}
        {step === 'error' && (
          <div className="text-center py-8">
            <AlertCircle size={48} className="mx-auto mb-4 text-rose-500" />
            <p className={`font-bold mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>충전 실패</p>
            <p className="text-sm text-rose-400 mb-6">{error}</p>
            <button
              onClick={reset}
              className="px-8 py-3 rounded-2xl font-bold bg-emerald-600 text-white hover:bg-emerald-700 transition-all"
            >
              다시 시도
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
