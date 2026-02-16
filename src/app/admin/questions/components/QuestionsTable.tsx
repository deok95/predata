import { ChevronDown, ChevronUp, ChevronsUpDown, Gavel, Trash2, TrendingUp } from 'lucide-react';
import type { QuestionAdminView, SortBy, SortOrder } from '../types';

interface QuestionsTableProps {
  isDark: boolean;
  questions: QuestionAdminView[];
  sortBy: SortBy;
  sortOrder: SortOrder;
  onSort: (column: SortBy) => void;
  onOpenQuestion: (id: number) => void;
  onOpenSettle: (question: QuestionAdminView) => void;
  onFinalize: (id: number) => void;
  onCancelSettle: (id: number) => void;
  onDelete: (id: number) => void;
}

function SortIcon({ column, sortBy, sortOrder }: { column: SortBy; sortBy: SortBy; sortOrder: SortOrder }) {
  if (sortBy !== column) {
    return <ChevronsUpDown size={14} className="opacity-40" />;
  }

  return sortOrder === 'asc' ? <ChevronUp size={14} /> : <ChevronDown size={14} />;
}

function renderStatus(status: string) {
  if (status === 'VOTING') return '투표중';
  if (status === 'BREAK') return '휴식';
  if (status === 'BETTING') return '베팅중';
  if (status === 'SETTLED') return '정산완료';
  return status;
}

function statusClass(status: string) {
  if (status === 'VOTING') return 'bg-blue-500/20 text-blue-400';
  if (status === 'BREAK') return 'bg-amber-500/20 text-amber-400';
  if (status === 'BETTING') return 'bg-emerald-500/20 text-emerald-400';
  if (status === 'SETTLED') return 'bg-purple-500/20 text-purple-400';
  return 'bg-slate-500/20 text-slate-400';
}

export default function QuestionsTable({
  isDark,
  questions,
  sortBy,
  sortOrder,
  onSort,
  onOpenQuestion,
  onOpenSettle,
  onFinalize,
  onCancelSettle,
  onDelete,
}: QuestionsTableProps) {
  return (
    <div className={`rounded-2xl overflow-hidden ${isDark ? 'bg-slate-800/50 border border-slate-700' : 'bg-white shadow-lg'}`}>
      <table className="w-full">
        <thead className={isDark ? 'bg-slate-700/50' : 'bg-slate-100'}>
          <tr>
            <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
              <button onClick={() => onSort('id')} className="flex items-center gap-1 hover:text-indigo-400 transition">
                ID <SortIcon column="id" sortBy={sortBy} sortOrder={sortOrder} />
              </button>
            </th>
            <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
              <button onClick={() => onSort('title')} className="flex items-center gap-1 hover:text-indigo-400 transition">
                제목 <SortIcon column="title" sortBy={sortBy} sortOrder={sortOrder} />
              </button>
            </th>
            <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
              <button onClick={() => onSort('category')} className="flex items-center gap-1 hover:text-indigo-400 transition">
                카테고리 <SortIcon column="category" sortBy={sortBy} sortOrder={sortOrder} />
              </button>
            </th>
            <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
              <button onClick={() => onSort('status')} className="flex items-center gap-1 hover:text-indigo-400 transition">
                상태 <SortIcon column="status" sortBy={sortBy} sortOrder={sortOrder} />
              </button>
            </th>
            <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
              <button onClick={() => onSort('totalBetPool')} className="flex items-center gap-1 hover:text-indigo-400 transition">
                거래량 <SortIcon column="totalBetPool" sortBy={sortBy} sortOrder={sortOrder} />
              </button>
            </th>
            <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>
              <button onClick={() => onSort('expiredAt')} className="flex items-center gap-1 hover:text-indigo-400 transition">
                마감일 <SortIcon column="expiredAt" sortBy={sortBy} sortOrder={sortOrder} />
              </button>
            </th>
            <th className={`p-4 text-left font-bold text-sm ${isDark ? 'text-white' : 'text-slate-800'}`}>작업</th>
          </tr>
        </thead>
        <tbody>
          {questions.map((question) => (
            <tr
              key={question.id}
              className={`border-b ${
                isDark ? 'border-slate-700 hover:bg-slate-700/50' : 'border-slate-100 hover:bg-slate-50'
              }`}
            >
              <td className={`p-4 text-sm font-mono ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>#{question.id}</td>
              <td className={`p-4 text-sm font-semibold max-w-md truncate ${isDark ? 'text-white' : 'text-slate-800'}`}>{question.title}</td>
              <td className="p-4">
                <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs font-bold">{question.category}</span>
              </td>
              <td className="p-4">
                <span className={`px-2 py-1 rounded text-xs font-bold ${statusClass(question.status)}`}>
                  {renderStatus(question.status)}
                </span>
              </td>
              <td className={`p-4 text-sm ${isDark ? 'text-slate-300' : 'text-slate-600'}`}>${question.totalBetPool.toLocaleString()}</td>
              <td className={`p-4 text-sm ${isDark ? 'text-slate-400' : 'text-slate-600'}`}>{new Date(question.expiredAt).toLocaleDateString('ko-KR')}</td>
              <td className="p-4">
                <div className="flex gap-2">
                  <button
                    onClick={() => onOpenQuestion(question.id)}
                    className={`p-2 rounded transition ${isDark ? 'text-blue-400 hover:bg-blue-500/20' : 'text-blue-600 hover:bg-blue-50'}`}
                    title="보기"
                  >
                    <TrendingUp size={16} />
                  </button>
                  {(question.status === 'BETTING' || question.status === 'VOTING') && (
                    <button
                      onClick={() => onOpenSettle(question)}
                      className={`p-2 rounded transition ${isDark ? 'text-amber-400 hover:bg-amber-500/20' : 'text-amber-600 hover:bg-amber-50'}`}
                      title="정산 시작"
                    >
                      <Gavel size={16} />
                    </button>
                  )}
                  {question.status === 'SETTLED' && question.disputeDeadline && new Date(question.disputeDeadline) > new Date() && (
                    <>
                      <button
                        onClick={() => onFinalize(question.id)}
                        className="px-2 py-1 text-xs font-bold text-emerald-400 bg-emerald-500/20 hover:bg-emerald-500/30 rounded transition"
                      >
                        확정
                      </button>
                      <button
                        onClick={() => onCancelSettle(question.id)}
                        className="px-2 py-1 text-xs font-bold text-red-400 bg-red-500/20 hover:bg-red-500/30 rounded transition"
                      >
                        취소
                      </button>
                    </>
                  )}
                  {question.status !== 'SETTLED' && (
                    <button
                      onClick={() => onDelete(question.id)}
                      className={`p-2 rounded transition ${isDark ? 'text-red-400 hover:bg-red-500/20' : 'text-red-600 hover:bg-red-50'}`}
                      title="삭제"
                    >
                      <Trash2 size={16} />
                    </button>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
