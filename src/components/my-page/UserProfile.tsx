'use client';

import { useState } from 'react';
import { Wallet, Award, ArrowDownLeft, ArrowUpRight, Link as LinkIcon } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAccount } from 'wagmi';
import { ConnectButton } from '@rainbow-me/rainbowkit';
import type { Member } from '@/types/api';
import DepositModal from '@/components/payment/DepositModal';
import WithdrawModal from '@/components/payment/WithdrawModal';
import { memberApi, ApiError } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';

interface UserProfileProps {
  user: Member;
}

const tierColors: Record<string, string> = {
  BRONZE: 'text-amber-700 bg-amber-100',
  SILVER: 'text-slate-500 bg-slate-100',
  GOLD: 'text-yellow-600 bg-yellow-100',
  PLATINUM: 'text-indigo-600 bg-indigo-100',
};

export default function UserProfile({ user }: UserProfileProps) {
  const { isDark } = useTheme();
  const { address, isConnected } = useAccount();
  const { refreshUser } = useAuth();
  const [showDeposit, setShowDeposit] = useState(false);
  const [showWithdraw, setShowWithdraw] = useState(false);
  const [showWalletModal, setShowWalletModal] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [walletError, setWalletError] = useState<string | null>(null);

  const handleConnectWallet = async () => {
    if (!isConnected || !address) {
      setShowWalletModal(true);
      return;
    }

    setIsUpdating(true);
    setWalletError(null);
    try {
      await memberApi.updateWalletAddress(address);
      await refreshUser();
      setShowWalletModal(false);
    } catch (error) {
      if (error instanceof ApiError) {
        setWalletError(error.message);
      } else {
        setWalletError('지갑 연결에 실패했습니다.');
      }
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className="flex items-center space-x-4 mb-6">
        <div className="w-16 h-16 bg-indigo-600 rounded-2xl flex items-center justify-center text-white text-2xl font-black">
          {user.email?.[0]?.toUpperCase() || 'P'}
        </div>
        <div>
          <h3 className={`text-lg font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>{user.email}</h3>
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold ${tierColors[user.tier] || tierColors.BRONZE}`}>
            <Award size={12} className="mr-1" /> {user.tier}
          </span>
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400 flex items-center"><Wallet size={14} className="mr-2" /> 지갑</span>
          {user.walletAddress ? (
            <div className="flex items-center gap-2">
              <span className="text-sm font-mono font-bold">{user.walletAddress.slice(0, 6)}...{user.walletAddress.slice(-4)}</span>
              <button
                onClick={() => setShowWalletModal(true)}
                className="text-xs text-indigo-500 hover:text-indigo-600 font-bold"
              >
                변경
              </button>
            </div>
          ) : (
            <button
              onClick={() => setShowWalletModal(true)}
              className="text-xs text-indigo-500 hover:text-indigo-600 font-bold flex items-center gap-1"
            >
              <LinkIcon size={12} /> 연결하기
            </button>
          )}
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">잔액</span>
          <span className="text-lg font-black text-indigo-600">{'$'}{(user.usdcBalance ?? 0).toLocaleString()}</span>
        </div>

        <div className="flex gap-2 pt-1">
          <button
            onClick={() => setShowDeposit(true)}
            className="flex-1 py-2.5 rounded-xl font-bold text-sm bg-indigo-600 text-white hover:bg-indigo-700 transition-all flex items-center justify-center gap-1.5 active:scale-[0.98]"
          >
            <ArrowDownLeft size={14} /> 충전
          </button>
          <button
            onClick={() => setShowWithdraw(true)}
            className={`flex-1 py-2.5 rounded-xl font-bold text-sm transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${isDark ? 'bg-slate-800 text-slate-200 hover:bg-slate-700' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}
          >
            <ArrowUpRight size={14} /> 출금
          </button>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">정확도</span>
          <span className="text-sm font-bold">{((user.accuracyScore ?? 0) * 100).toFixed(1)}%</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="text-sm text-slate-400">총 예측</span>
          <span className="text-sm font-bold">{user.totalPredictions ?? 0}회</span>
        </div>
      </div>

      <DepositModal isOpen={showDeposit} onClose={() => setShowDeposit(false)} />
      <WithdrawModal isOpen={showWithdraw} onClose={() => setShowWithdraw(false)} />

      {/* 지갑 연결 모달 */}
      {showWalletModal && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center animate-fade-in">
          <div className="absolute inset-0 bg-black/60" onClick={() => setShowWalletModal(false)} />
          <div className={`relative w-full max-w-md rounded-3xl border p-8 shadow-2xl ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
            <h3 className={`text-xl font-black mb-4 ${isDark ? 'text-white' : 'text-slate-900'}`}>
              {user.walletAddress ? '지갑 주소 변경' : '지갑 연결하기'}
            </h3>
            <p className="text-sm text-slate-400 mb-6">
              {user.walletAddress
                ? '새로운 지갑을 연결하면 이전 지갑 연결이 해제됩니다.'
                : '충전/출금을 위해 지갑을 연결해주세요.'}
            </p>

            {walletError && (
              <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20">
                <p className="text-sm text-rose-400">{walletError}</p>
              </div>
            )}

            {!isConnected ? (
              <div className="flex justify-center">
                <ConnectButton />
              </div>
            ) : (
              <div className="space-y-4">
                <div className={`p-4 rounded-xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
                  <p className="text-xs text-slate-400 mb-2">연결할 지갑 주소</p>
                  <p className="text-sm font-mono font-bold break-all">{address}</p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => {
                      setShowWalletModal(false);
                      setWalletError(null);
                    }}
                    className={`flex-1 py-3 rounded-xl font-bold transition-all ${isDark ? 'bg-slate-800 text-slate-200 hover:bg-slate-700' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}
                  >
                    취소
                  </button>
                  <button
                    onClick={handleConnectWallet}
                    disabled={isUpdating}
                    className="flex-1 py-3 rounded-xl font-bold bg-indigo-600 text-white hover:bg-indigo-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isUpdating ? '연결 중...' : '연결하기'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
