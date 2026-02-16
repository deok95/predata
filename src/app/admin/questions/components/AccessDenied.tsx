import { AlertTriangle } from 'lucide-react';

interface AccessDeniedProps {
  isDark: boolean;
  onGoHome: () => void;
}

export default function AccessDenied({ isDark, onGoHome }: AccessDeniedProps) {
  return (
    <div className={`flex flex-col items-center justify-center min-h-[60vh] gap-4 ${isDark ? 'text-white' : 'text-slate-800'}`}>
      <AlertTriangle className="w-16 h-16 text-amber-500" />
      <h1 className="text-2xl font-bold">접근 권한이 없습니다</h1>
      <p className={isDark ? 'text-slate-400' : 'text-slate-500'}>이 페이지는 관리자만 접근할 수 있습니다.</p>
      <button
        onClick={onGoHome}
        className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition"
      >
        홈으로 돌아가기
      </button>
    </div>
  );
}
