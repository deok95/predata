interface DurationOption {
  value: number;
  label: string;
}

interface CreateQuestionFormProps {
  isDark: boolean;
  categories: string[];
  durationOptions: DurationOption[];
  title: string;
  category: string;
  questionType: 'VERIFIABLE' | 'OPINION';
  votingDuration: number;
  bettingDuration: number;
  loading: boolean;
  onChangeTitle: (value: string) => void;
  onChangeCategory: (value: string) => void;
  onChangeQuestionType: (value: 'VERIFIABLE' | 'OPINION') => void;
  onChangeVotingDuration: (value: number) => void;
  onChangeBettingDuration: (value: number) => void;
  onCancel: () => void;
  onCreate: () => void;
}

export default function CreateQuestionForm({
  isDark,
  categories,
  durationOptions,
  title,
  category,
  questionType,
  votingDuration,
  bettingDuration,
  loading,
  onChangeTitle,
  onChangeCategory,
  onChangeQuestionType,
  onChangeVotingDuration,
  onChangeBettingDuration,
  onCancel,
  onCreate,
}: CreateQuestionFormProps) {
  return (
    <div className={`rounded-2xl p-6 mb-8 border-2 ${
      isDark ? 'bg-slate-800/50 border-indigo-500/50' : 'bg-white border-indigo-200 shadow-lg'
    }`}>
      <h2 className={`text-2xl font-bold mb-4 ${isDark ? 'text-white' : 'text-slate-800'}`}>새 질문 만들기</h2>

      <div className="space-y-4">
        <div>
          <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>질문 제목</label>
          <input
            type="text"
            value={title}
            onChange={(e) => onChangeTitle(e.target.value)}
            placeholder="예: Will Bitcoin reach $100,000 by end of 2026?"
            className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-indigo-500 ${
              isDark
                ? 'bg-slate-700 border-slate-600 text-white placeholder-slate-400'
                : 'bg-white border-slate-300 text-slate-800'
            } border`}
          />
        </div>

        <div>
          <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>질문 타입</label>
          <div className="grid grid-cols-2 gap-3">
            <button
              type="button"
              onClick={() => onChangeQuestionType('VERIFIABLE')}
              className={`py-2 rounded-lg font-semibold text-sm transition border-2 ${
                questionType === 'VERIFIABLE'
                  ? 'bg-indigo-600 text-white border-indigo-700'
                  : isDark ? 'bg-slate-700 text-slate-300 border-slate-600' : 'bg-slate-100 text-slate-600 border-slate-300'
              }`}
            >
              VERIFIABLE (검증 가능)
            </button>
            <button
              type="button"
              onClick={() => onChangeQuestionType('OPINION')}
              className={`py-2 rounded-lg font-semibold text-sm transition border-2 ${
                questionType === 'OPINION'
                  ? 'bg-indigo-600 text-white border-indigo-700'
                  : isDark ? 'bg-slate-700 text-slate-300 border-slate-600' : 'bg-slate-100 text-slate-600 border-slate-300'
              }`}
            >
              OPINION (의견 기반)
            </button>
          </div>
        </div>

        <div>
          <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>카테고리</label>
          <select
            value={category}
            onChange={(e) => onChangeCategory(e.target.value)}
            className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-indigo-500 ${
              isDark ? 'bg-slate-700 border-slate-600 text-white' : 'bg-white border-slate-300 text-slate-800'
            } border`}
          >
            {categories.map((cat) => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
        </div>

        <div>
          <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>투표 기간</label>
          <select
            value={votingDuration}
            onChange={(e) => onChangeVotingDuration(Number(e.target.value))}
            className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-indigo-500 ${
              isDark ? 'bg-slate-700 border-slate-600 text-white' : 'bg-white border-slate-300 text-slate-800'
            } border`}
          >
            {durationOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>

        <div>
          <label className={`block text-sm font-semibold mb-2 ${isDark ? 'text-slate-300' : 'text-slate-700'}`}>베팅 기간</label>
          <select
            value={bettingDuration}
            onChange={(e) => onChangeBettingDuration(Number(e.target.value))}
            className={`w-full p-3 rounded-lg focus:ring-2 focus:ring-indigo-500 ${
              isDark ? 'bg-slate-700 border-slate-600 text-white' : 'bg-white border-slate-300 text-slate-800'
            } border`}
          >
            {durationOptions.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>

        <div className="flex gap-3">
          <button
            onClick={onCancel}
            className={`flex-1 py-3 rounded-lg transition font-semibold ${
              isDark ? 'bg-slate-700 text-slate-300 hover:bg-slate-600' : 'bg-slate-200 text-slate-800 hover:bg-slate-300'
            }`}
          >
            취소
          </button>
          <button
            onClick={onCreate}
            disabled={loading}
            className="flex-1 py-3 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition font-semibold disabled:opacity-50"
          >
            {loading ? '생성 중...' : '질문 생성'}
          </button>
        </div>
      </div>
    </div>
  );
}
