import type { QuestionAdminView } from '../types';

interface QuestionStatsProps {
  isDark: boolean;
  questions: QuestionAdminView[];
}

export default function QuestionStats({ isDark, questions }: QuestionStatsProps) {
  return (
    <div className="grid grid-cols-4 gap-4 mb-8">
      <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
        <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>Total Questions</p>
        <p className="text-3xl font-black text-blue-500">{questions.length}</p>
      </div>
      <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
        <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>Voting</p>
        <p className="text-3xl font-black text-blue-500">{questions.filter((q) => q.status === 'VOTING').length}</p>
      </div>
      <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
        <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>Betting</p>
        <p className="text-3xl font-black text-emerald-500">{questions.filter((q) => q.status === 'BETTING').length}</p>
      </div>
      <div className={`rounded-2xl p-6 ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
        <p className={`text-sm mb-1 ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>Settled</p>
        <p className="text-3xl font-black text-purple-500">{questions.filter((q) => q.status === 'SETTLED').length}</p>
      </div>
    </div>
  );
}
