'use client';

import { useState, useMemo } from 'react';
import { Ticket, Plus, Minus, ShoppingCart } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { useToast } from '@/components/ui/Toast';
import { ticketApi } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import { useI18n } from '@/lib/i18n';

interface TicketShopProps {
  memberId: number;
}

const TICKET_PRICE = 10; // 10P per ticket

export default function TicketShop({ memberId }: TicketShopProps) {
  const { t } = useI18n();
  const { isDark } = useTheme();

  const TICKET_PACKAGES = useMemo(() => [
    { qty: 5, label: t('ticketShop.tickets').replace('{n}', '5'), discount: 0 },
    { qty: 10, label: t('ticketShop.tickets').replace('{n}', '10'), discount: 5 },
    { qty: 25, label: t('ticketShop.tickets').replace('{n}', '25'), discount: 10 },
    { qty: 50, label: t('ticketShop.tickets').replace('{n}', '50'), discount: 15 },
  ], [t]);
  const { showToast } = useToast();
  const { refreshUser } = useAuth();
  const [quantity, setQuantity] = useState(5);
  const [loading, setLoading] = useState(false);

  const totalCost = quantity * TICKET_PRICE;

  const handlePurchase = async () => {
    if (loading) return;
    setLoading(true);
    try {
      const res = await ticketApi.purchase(quantity);
      if (res.success) {
        showToast(t('ticketShop.bought').replace('{qty}', String(quantity)), 'success');
        await refreshUser();
      } else {
        showToast(t('ticketShop.purchaseFail'), 'error');
      }
    } catch {
      showToast(t('ticketShop.purchaseError'), 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={`p-6 rounded-3xl border ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
      <div className="flex items-center gap-3 mb-5">
        <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center">
          <Ticket size={20} className="text-white" />
        </div>
        <div>
          <h3 className={`font-black text-lg ${isDark ? 'text-white' : 'text-slate-900'}`}>{t('ticketShop.title')}</h3>
          <p className="text-xs text-slate-400">{t('ticketShop.pricePerTicket').replace('{price}', String(TICKET_PRICE))}</p>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2 mb-5">
        {TICKET_PACKAGES.map((pkg) => (
          <button
            key={pkg.qty}
            onClick={() => setQuantity(pkg.qty)}
            className={`p-3 rounded-xl text-center transition-all border-2 ${
              quantity === pkg.qty
                ? 'border-indigo-600 bg-indigo-600/10'
                : isDark ? 'border-slate-700 hover:border-slate-600' : 'border-slate-100 hover:border-slate-200'
            }`}
          >
            <p className={`font-black text-sm ${quantity === pkg.qty ? 'text-indigo-600' : isDark ? 'text-white' : 'text-slate-900'}`}>
              {pkg.label}
            </p>
            <p className="text-[10px] text-slate-400">
              {pkg.qty * TICKET_PRICE}P
              {pkg.discount > 0 && <span className="text-emerald-500 ml-1">-{pkg.discount}%</span>}
            </p>
          </button>
        ))}
      </div>

      <div className={`flex items-center justify-between p-4 rounded-xl mb-4 ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setQuantity(Math.max(1, quantity - 1))}
            className={`w-8 h-8 rounded-lg flex items-center justify-center transition-all ${isDark ? 'bg-slate-700 hover:bg-slate-600' : 'bg-white hover:bg-slate-100 border border-slate-200'}`}
          >
            <Minus size={14} />
          </button>
          <span className={`font-black text-lg w-10 text-center ${isDark ? 'text-white' : 'text-slate-900'}`}>{quantity}</span>
          <button
            onClick={() => setQuantity(Math.min(100, quantity + 1))}
            className={`w-8 h-8 rounded-lg flex items-center justify-center transition-all ${isDark ? 'bg-slate-700 hover:bg-slate-600' : 'bg-white hover:bg-slate-100 border border-slate-200'}`}
          >
            <Plus size={14} />
          </button>
        </div>
        <div className="text-right">
          <p className="text-xs text-slate-400">{t('ticketShop.total')}</p>
          <p className="font-black text-indigo-600">{totalCost.toLocaleString()} P</p>
        </div>
      </div>

      <button
        onClick={handlePurchase}
        disabled={loading}
        className="w-full py-3.5 rounded-xl font-bold text-sm bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 transition-all flex items-center justify-center gap-2 active:scale-[0.98]"
      >
        <ShoppingCart size={16} />
        {loading ? t('ticketShop.purchasing') : t('ticketShop.buyBtn').replace('{qty}', String(quantity))}
      </button>
    </div>
  );
}
