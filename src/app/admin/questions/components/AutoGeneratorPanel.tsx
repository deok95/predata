import { Loader2, Sparkles } from 'lucide-react';

interface IntervalOption {
  value: number;
  label: string;
}

interface AutoGeneratorPanelProps {
  isDark: boolean;
  isDemoMode: boolean;
  generating: boolean;
  generatorEnabled: boolean;
  generatorLoading: boolean;
  generatorInterval: number;
  generatorCategories: string[];
  categories: string[];
  intervalOptions: IntervalOption[];
  onGenerateNow: () => void;
  onToggleGenerator: () => void;
  onChangeInterval: (intervalSeconds: number) => void;
  onToggleCategory: (category: string) => void;
}

export default function AutoGeneratorPanel({
  isDark,
  isDemoMode,
  generating,
  generatorEnabled,
  generatorLoading,
  generatorInterval,
  generatorCategories,
  categories,
  intervalOptions,
  onGenerateNow,
  onToggleGenerator,
  onChangeInterval,
  onToggleCategory,
}: AutoGeneratorPanelProps) {
  return (
    <div className={`rounded-2xl p-6 mb-8 border ${
      isDark ? 'bg-slate-800/50 border-slate-700' : 'bg-white border-slate-200 shadow-lg'
    }`}>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <Sparkles className="text-purple-500" size={24} />
          <h2 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-slate-800'}`}>자동 질문 생성기</h2>
          {isDemoMode && (
            <span className="px-3 py-1 bg-amber-500/20 text-amber-400 text-xs font-bold rounded-full border border-amber-500/30">
              DEMO MODE
            </span>
          )}
        </div>
        <button
          onClick={onGenerateNow}
          disabled={generating}
          className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition font-semibold disabled:opacity-50"
        >
          {generating ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              생성 중...
            </>
          ) : (
            <>
              <Sparkles size={16} />
              지금 생성
            </>
          )}
        </button>
      </div>

      {isDemoMode && (
        <div className={`mb-4 p-3 rounded-lg border ${
          isDark ? 'bg-amber-500/10 border-amber-500/30' : 'bg-amber-50 border-amber-200'
        }`}>
          <p className={`text-sm ${isDark ? 'text-amber-300' : 'text-amber-700'}`}>
            <span className="font-semibold">데모 모드:</span> API 키가 설정되지 않아 미리 준비된 샘플 질문을 사용합니다.
          </p>
        </div>
      )}

      <div className="flex items-center gap-3 mb-4">
        <span className={`text-sm font-medium ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>자동 생성 활성화</span>
        <button
          onClick={onToggleGenerator}
          disabled={generatorLoading}
          className={`relative w-12 h-6 rounded-full transition-colors ${
            generatorEnabled ? 'bg-purple-600' : isDark ? 'bg-slate-600' : 'bg-slate-300'
          } ${generatorLoading ? 'opacity-50' : ''}`}
        >
          <span
            className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${
              generatorEnabled ? 'left-7' : 'left-1'
            }`}
          />
        </button>
      </div>

      {generatorEnabled && (
        <div className={`space-y-4 pt-4 border-t ${isDark ? 'border-slate-700' : 'border-slate-200'}`}>
          <div className="flex items-center gap-4">
            <label className={`text-sm font-medium w-24 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>생성 간격</label>
            <select
              value={generatorInterval}
              onChange={(e) => onChangeInterval(Number(e.target.value))}
              disabled={generatorLoading}
              className={`px-3 py-2 rounded-lg focus:ring-2 focus:ring-purple-500 text-sm ${
                isDark ? 'bg-slate-700 border-slate-600 text-white' : 'bg-white border-slate-300 text-slate-800'
              } border`}
            >
              {intervalOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-4">
            <label className={`text-sm font-medium w-24 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>카테고리</label>
            <div className="flex flex-wrap gap-3">
              {categories.map((cat) => (
                <label key={cat} className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={generatorCategories.includes(cat)}
                    onChange={() => onToggleCategory(cat)}
                    disabled={generatorLoading}
                    className="w-4 h-4 rounded border-slate-300 text-purple-600 focus:ring-purple-500"
                  />
                  <span className={`text-sm ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>{cat}</span>
                </label>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
