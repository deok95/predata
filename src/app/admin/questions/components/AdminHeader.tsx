import { Plus, Settings } from 'lucide-react';

interface AdminHeaderProps {
  isDark: boolean;
  showCreateForm: boolean;
  onToggleCreateForm: () => void;
}

export default function AdminHeader({ isDark, showCreateForm, onToggleCreateForm }: AdminHeaderProps) {
  return (
    <div className="mb-8 flex items-center justify-between">
      <div>
        <div className="flex items-center gap-3 mb-2">
          <Settings className={`w-8 h-8 ${isDark ? 'text-indigo-400' : 'text-indigo-600'}`} />
          <h1 className={`text-3xl font-black ${isDark ? 'text-white' : 'text-slate-900'}`}>질문 관리</h1>
        </div>
        <p className={isDark ? 'text-slate-400' : 'text-slate-600'}>예측 시장 질문을 생성하고 관리하세요</p>
      </div>
      <button
        onClick={onToggleCreateForm}
        className="flex items-center gap-2 px-6 py-3 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 transition font-bold"
      >
        <Plus size={20} />
        {showCreateForm ? '생성 닫기' : '새 질문 생성'}
      </button>
    </div>
  );
}
