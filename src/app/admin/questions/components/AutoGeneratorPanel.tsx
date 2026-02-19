import { Loader2, Sparkles } from 'lucide-react';

interface AutoGeneratorPanelProps {
  isDark: boolean;
  isDemoMode: boolean;
  generating: boolean;
  generatorEnabled: boolean;
  generatorLoading: boolean;
  onGenerateNow: () => void;
  onToggleGenerator: () => void;
}

export default function AutoGeneratorPanel({
  isDark,
  isDemoMode,
  generating,
  generatorEnabled,
  generatorLoading,
  onGenerateNow,
  onToggleGenerator,
}: AutoGeneratorPanelProps) {
  return (
    <div className={`rounded-2xl p-6 mb-8 border ${
      isDark ? 'bg-slate-800/50 border-slate-700' : 'bg-white border-slate-200 shadow-lg'
    }`}>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-3">
          <Sparkles className="text-purple-500" size={24} />
          <h2 className={`text-xl font-bold ${isDark ? 'text-white' : 'text-slate-800'}`}>Auto Question Generator</h2>
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
              Generating...
            </>
          ) : (
            <>
              <Sparkles size={16} />
              Generate Now
            </>
          )}
        </button>
      </div>

      {isDemoMode && (
        <div className={`mb-4 p-3 rounded-lg border ${
          isDark ? 'bg-amber-500/10 border-amber-500/30' : 'bg-amber-50 border-amber-200'
        }`}>
          <p className={`text-sm ${isDark ? 'text-amber-300' : 'text-amber-700'}`}>
            <span className="font-semibold">Demo Mode:</span> Using pre-prepared sample questions as API key is not configured.
          </p>
        </div>
      )}

      <div className="flex items-center gap-3 mb-4">
        <span className={`text-sm font-medium ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>Enable Auto Generation</span>
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
        <div className={`pt-4 border-t ${isDark ? 'border-slate-700' : 'border-slate-200'}`}>
          <p className={`text-sm ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>
            Generates exactly one AMM question per day from the top Google Trend topic.
            The question title and resolution rule are generated in English only.
          </p>
        </div>
      )}
    </div>
  );
}
