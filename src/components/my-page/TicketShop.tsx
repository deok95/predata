'use client';

import { useState } from 'react';
import { ShieldCheck, Loader2 } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useAuth } from '@/hooks/useAuth';
import { votingPassApi } from '@/lib/api';
import { VOTING_PASS_PRICE } from '@/lib/contracts';
import DepositModal from '@/components/payment/DepositModal';
import type { Member } from '@/types/api';

interface VotingPassShopProps {
  user: Member;
}

export default function TicketShop({ user }: VotingPassShopProps) {
  const { isDark } = useTheme();
  const { refreshUser } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [showDeposit, setShowDeposit] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const handlePurchase = async () => {
    if (user.hasVotingPass) return;
    setIsLoading(true);
    setMessage(null);
    setShowConfirm(false);
    try {
      const res = await votingPassApi.purchase(user.id);
      if (res.success && res.data?.success) {
        setMessage('Voting pass purchased successfully!');
        await refreshUser();
      } else {
        setMessage(res.data?.message || 'Purchase failed.');
      }
    } catch (e: any) {
      setMessage(e?.message || 'Purchase failed.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className="flex items-center gap-3 mb-5">
        <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${user.hasVotingPass ? 'bg-emerald-600' : 'bg-indigo-600'}`}>
          <ShieldCheck size={20} className="text-white" />
        </div>
        <div>
          <h3 className={`font-black text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>Voting Pass</h3>
          <p className="text-xs text-slate-400">
            {user.hasVotingPass ? 'Unlimited voting available' : `$${VOTING_PASS_PRICE} one-time purchase`}
          </p>
        </div>
      </div>

      {user.hasVotingPass ? (
        <div className={`p-4 rounded-xl text-center ${isDark ? 'bg-emerald-900/30 border border-emerald-800' : 'bg-emerald-50 border border-emerald-200'}`}>
          <p className="text-emerald-500 font-bold text-sm">Voting Pass Active</p>
          <p className="text-xs text-slate-400 mt-1">Unlimited voting on all questions</p>
        </div>
      ) : (
        <>
          <div className={`rounded-xl p-4 mb-4 ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
            <div className="flex justify-between items-center">
              <span className="text-sm text-slate-400">Voting Pass Price</span>
              <span className="text-xl font-black text-indigo-600">${VOTING_PASS_PRICE}</span>
            </div>
            <p className="text-xs text-slate-400 mt-2">Unlimited voting on all questions after purchase</p>
          </div>

          {user.usdcBalance < VOTING_PASS_PRICE ? (
            <>
              <button
                onClick={() => setShowDeposit(true)}
                className="w-full py-3.5 rounded-xl font-bold text-sm bg-amber-600 text-white hover:bg-amber-700 transition-all flex items-center justify-center gap-2 active:scale-[0.98]"
              >
                Insufficient balance. Please deposit
              </button>
              <DepositModal isOpen={showDeposit} onClose={() => setShowDeposit(false)} />
            </>
          ) : (
            <button
              onClick={() => setShowConfirm(true)}
              disabled={isLoading}
              className="w-full py-3.5 rounded-xl font-bold text-sm bg-indigo-600 text-white hover:bg-indigo-700 transition-all flex items-center justify-center gap-2 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? (
                <><Loader2 size={16} className="animate-spin" /> Purchasing...</>
              ) : (
                `Purchase Voting Pass for $${VOTING_PASS_PRICE}`
              )}
            </button>
          )}
        </>
      )}

      {message && (
        <p className={`text-xs mt-3 text-center ${message.includes('failed') || message.includes('Failed') ? 'text-rose-400' : 'text-emerald-400'}`}>
          {message}
        </p>
      )}

      {/* Purchase Confirmation Modal */}
      {showConfirm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={() => setShowConfirm(false)}>
          <div
            className={`max-w-sm w-full rounded-2xl p-6 ${isDark ? 'bg-slate-900 border border-slate-800' : 'bg-white'} shadow-2xl`}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="text-center mb-6">
              <div className={`w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 ${isDark ? 'bg-indigo-500/10' : 'bg-indigo-50'}`}>
                <ShieldCheck size={32} className="text-indigo-600" />
              </div>
              <h3 className={`text-xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>Purchase Voting Pass</h3>
              <p className={`text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>
                Do you want to purchase a voting pass for ${VOTING_PASS_PRICE}?
              </p>
              <p className="text-xs text-slate-400 mt-2">
                After purchase, you can vote unlimited times on all questions.
              </p>
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => setShowConfirm(false)}
                className={`flex-1 py-3 rounded-xl font-bold text-sm transition-all ${
                  isDark ? 'bg-slate-800 text-slate-300 hover:bg-slate-700' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                Cancel
              </button>
              <button
                onClick={handlePurchase}
                disabled={isLoading}
                className="flex-1 py-3 rounded-xl font-bold text-sm bg-indigo-600 text-white hover:bg-indigo-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? 'Purchasing...' : 'Purchase'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
