import { AlertTriangle, X } from 'lucide-react';
import type { QuestionAdminView } from '../types';

interface SettlementModalProps {
  isDark: boolean;
  target: QuestionAdminView | null;
  finalResult: 'YES' | 'NO';
  sourceUrl: string;
  loading: boolean;
  onClose: () => void;
  onChangeFinalResult: (value: 'YES' | 'NO') => void;
  onChangeSourceUrl: (value: string) => void;
  onSubmit: () => void;
}

export default function SettlementModal({
  isDark,
  target,
  finalResult,
  sourceUrl,
  loading,
  onClose,
  onChangeFinalResult,
  onChangeSourceUrl,
  onSubmit,
}: SettlementModalProps) {
  if (!target) {
    return null;
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className={`rounded-2xl shadow-2xl p-8 max-w-lg w-full mx-4 ${isDark ? 'bg-slate-800' : 'bg-white'}`}>
        <div className="flex items-center justify-between mb-6">
          <h3 className={`text-xl font-black ${isDark ? 'text-white' : 'text-slate-800'}`}>정산 시작</h3>
          <button onClick={onClose} className={isDark ? 'text-slate-400 hover:text-slate-300' : 'text-slate-400 hover:text-slate-600'}>
            <X size={20} />
          </button>
        </div>

        <p className={`text-sm mb-4 font-medium ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>
          &quot;{target.title}&quot;
        </p>

        <div className="mb-4">
          <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>최종 결과</label>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => onChangeFinalResult('YES')}
              className={`py-3 rounded-xl font-bold text-sm transition-all border-2 ${
                finalResult === 'YES'
                  ? 'bg-emerald-500 text-white border-emerald-600'
                  : isDark
                    ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30 hover:bg-emerald-500/20'
                    : 'bg-emerald-50 text-emerald-600 border-emerald-200 hover:bg-emerald-100'
              }`}
            >
              YES
            </button>
            <button
              onClick={() => onChangeFinalResult('NO')}
              className={`py-3 rounded-xl font-bold text-sm transition-all border-2 ${
                finalResult === 'NO'
                  ? 'bg-rose-500 text-white border-rose-600'
                  : isDark
                    ? 'bg-rose-500/10 text-rose-400 border-rose-500/30 hover:bg-rose-500/20'
                    : 'bg-rose-50 text-rose-600 border-rose-200 hover:bg-rose-100'
              }`}
            >
              NO
            </button>
          </div>
        </div>

        <div className="mb-6">
          <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
            정산 근거 URL (선택)
          </label>
          <input
            type="url"
            value={sourceUrl}
            onChange={(e) => onChangeSourceUrl(e.target.value)}
            placeholder="https://news.example.com/article/..."
            className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-amber-500 text-sm ${
              isDark ? 'bg-slate-700 border-slate-600 text-white placeholder-slate-400' : 'bg-white border-slate-300 text-slate-800'
            } border`}
          />
          <p className={`text-xs mt-1 ${isDark ? 'text-slate-500' : 'text-slate-400'}`}>
            결과 판정의 근거가 되는 뉴스/기사 링크를 입력하세요
          </p>
        </div>

        <div className={`rounded-xl p-4 mb-6 border ${isDark ? 'bg-amber-500/10 border-amber-500/30' : 'bg-amber-50 border-amber-200'}`}>
          <div className="flex items-start gap-2">
            <AlertTriangle size={16} className="text-amber-500 mt-0.5 flex-shrink-0" />
            <p className={`text-xs ${isDark ? 'text-amber-300' : 'text-amber-700'}`}>
              정산을 시작하면 24시간 이의 제기 기간이 시작됩니다. 이 기간 동안 배당금은 분배되지 않으며, &quot;확정&quot; 버튼을 눌러야 최종 정산됩니다.
            </p>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            onClick={onClose}
            className={`flex-1 py-3 rounded-lg transition font-semibold ${
              isDark ? 'bg-slate-700 text-slate-300 hover:bg-slate-600' : 'bg-slate-200 text-slate-800 hover:bg-slate-300'
            }`}
          >
            취소
          </button>
          <button
            onClick={onSubmit}
            disabled={loading}
            className="flex-1 py-3 bg-amber-600 text-white rounded-lg hover:bg-amber-700 transition font-bold disabled:opacity-50"
          >
            {loading ? '처리 중...' : '정산 시작'}
          </button>
        </div>
      </div>
    </div>
  );
}
