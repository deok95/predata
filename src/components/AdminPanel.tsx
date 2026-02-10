'use client';

import React, { useState } from 'react';
import { Shield, CheckCircle, Loader2 } from 'lucide-react';
import { settlementApi, ApiError } from '@/lib/api';

interface AdminPanelProps {
  questionId: number;
  questionTitle: string;
  onClose: () => void;
  onSettled: () => void;
}

export default function AdminPanel({ questionId, questionTitle, onClose, onSettled }: AdminPanelProps) {
  const [selectedResult, setSelectedResult] = useState<'YES' | 'NO' | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleSettle = async () => {
    if (!selectedResult) {
      alert('결과를 선택해주세요.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await settlementApi.settle(questionId, {
        finalResult: selectedResult
      });

      if (response.success && response.data) {
        setSuccess(true);
        alert(
          `✅ 정산 완료!\n\n` +
          `총 베팅: ${response.data.totalBets}건\n` +
          `승자: ${response.data.totalWinners}명\n` +
          `총 배당금: ${response.data.totalPayout.toLocaleString()}P`
        );
        onSettled();
        setTimeout(() => onClose(), 2000);
      } else {
        setError(response.message || '정산에 실패했습니다.');
      }
    } catch (err) {
      if (process.env.NODE_ENV === 'development') {
        console.error('Settlement error:', err);
      }
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('서버와 통신 중 오류가 발생했습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-md">
        <div className="flex items-center gap-2 mb-6">
          <Shield className="text-blue-600" size={24} />
          <h2 className="text-2xl font-bold">관리자 패널</h2>
        </div>

        <div className="mb-6">
          <h3 className="text-sm font-semibold text-slate-700 mb-2">질문</h3>
          <p className="text-sm text-slate-600 bg-slate-50 p-3 rounded">{questionTitle}</p>
        </div>

        <div className="mb-6">
          <h3 className="text-sm font-semibold text-slate-700 mb-3">최종 결과 선택</h3>
          <div className="flex gap-3">
            <button
              onClick={() => setSelectedResult('YES')}
              className={`flex-1 py-3 rounded-lg font-bold text-lg transition-all ${
                selectedResult === 'YES'
                  ? 'bg-green-600 text-white shadow-lg'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              YES
            </button>
            <button
              onClick={() => setSelectedResult('NO')}
              className={`flex-1 py-3 rounded-lg font-bold text-lg transition-all ${
                selectedResult === 'NO'
                  ? 'bg-red-600 text-white shadow-lg'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              NO
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-600 text-sm">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded text-green-600 text-sm flex items-center gap-2">
            <CheckCircle size={16} />
            정산이 완료되었습니다!
          </div>
        )}

        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 py-3 px-4 bg-slate-200 text-slate-800 rounded-md hover:bg-slate-300 transition font-semibold"
            disabled={loading}
          >
            취소
          </button>
          <button
            onClick={handleSettle}
            className="flex-1 py-3 px-4 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition font-semibold flex items-center justify-center"
            disabled={loading || !selectedResult || success}
          >
            {loading && <Loader2 className="animate-spin mr-2" size={20} />}
            정산 실행
          </button>
        </div>
      </div>
    </div>
  );
}
