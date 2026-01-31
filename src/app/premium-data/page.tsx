'use client';

import React, { useState } from 'react';
import { Download, Filter, Eye, Database, CheckCircle, XCircle } from 'lucide-react';

const BACKEND_URL = 'http://localhost:8080/api';

interface PremiumDataRequest {
  questionId: number;
  countryCode?: string;
  jobCategory?: string;
  ageGroup?: number;
  minTier?: string;
  minLatencyMs?: number;
}

interface PremiumDataPoint {
  voteId: number;
  questionTitle: string;
  choice: string;
  latencyMs: number;
  countryCode: string;
  jobCategory?: string;
  ageGroup?: number;
  tier: string;
  tierWeight: number;
  timestamp: string;
}

interface PremiumDataResponse {
  questionId: number;
  questionTitle: string;
  filters: PremiumDataRequest;
  totalCount: number;
  yesCount: number;
  noCount: number;
  yesPercentage: number;
  noPercentage: number;
  data: PremiumDataPoint[];
}

export default function PremiumDataExport() {
  const [questionId] = useState(1); // 일단 질문 1번 고정
  const [filters, setFilters] = useState<PremiumDataRequest>({
    questionId: 1,
    minLatencyMs: 5000 // 기본값: 5초 이상 (고품질)
  });

  const [previewData, setPreviewData] = useState<PremiumDataResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const handlePreview = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/premium-data/preview`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(filters)
      });
      const data = await response.json();
      setPreviewData(data);
    } catch (error) {
      console.error('Failed to preview:', error);
      alert('미리보기에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async (format: 'json' | 'csv') => {
    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/premium-data/export`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(filters)
      });
      const data: PremiumDataResponse = await response.json();

      if (format === 'json') {
        downloadJSON(data);
      } else {
        downloadCSV(data);
      }
    } catch (error) {
      console.error('Failed to export:', error);
      alert('다운로드에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const downloadJSON = (data: PremiumDataResponse) => {
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `premium-data-${questionId}-${Date.now()}.json`;
    a.click();
  };

  const downloadCSV = (data: PremiumDataResponse) => {
    const headers = ['voteId', 'choice', 'latencyMs', 'countryCode', 'jobCategory', 'ageGroup', 'tier', 'tierWeight', 'timestamp'];
    const rows = data.data.map(d => [
      d.voteId,
      d.choice,
      d.latencyMs,
      d.countryCode,
      d.jobCategory || '',
      d.ageGroup || '',
      d.tier,
      d.tierWeight,
      d.timestamp
    ]);

    const csv = [
      headers.join(','),
      ...rows.map(row => row.join(','))
    ].join('\n');

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `premium-data-${questionId}-${Date.now()}.csv`;
    a.click();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-purple-50 py-8 px-4">
      <div className="max-w-6xl mx-auto">
        
        {/* 헤더 */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <Database className="h-8 w-8 text-purple-600" />
            <h1 className="text-4xl font-black text-slate-800">프리미엄 데이터 추출</h1>
          </div>
          <p className="text-slate-600 text-lg">고품질 예측 데이터를 필터링하여 다운로드하세요</p>
        </div>

        {/* 필터 섹션 */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-6">
          <div className="flex items-center gap-2 mb-4">
            <Filter className="h-6 w-6 text-purple-600" />
            <h2 className="text-2xl font-bold">필터 설정</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* 국가 */}
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">국가</label>
              <select
                value={filters.countryCode || ''}
                onChange={(e) => setFilters({ ...filters, countryCode: e.target.value || undefined })}
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
              >
                <option value="">전체</option>
                <option value="KR">한국</option>
                <option value="US">미국</option>
                <option value="JP">일본</option>
                <option value="SG">싱가포르</option>
                <option value="VN">베트남</option>
              </select>
            </div>

            {/* 직업 */}
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">직업</label>
              <select
                value={filters.jobCategory || ''}
                onChange={(e) => setFilters({ ...filters, jobCategory: e.target.value || undefined })}
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
              >
                <option value="">전체</option>
                <option value="IT">IT</option>
                <option value="Finance">Finance</option>
                <option value="Medical">Medical</option>
                <option value="Student">Student</option>
                <option value="Service">Service</option>
              </select>
            </div>

            {/* 최소 응답시간 (품질 필터) */}
            <div>
              <label className="block text-sm font-semibold text-slate-700 mb-2">최소 응답시간 (어뷰징 제거)</label>
              <select
                value={filters.minLatencyMs || ''}
                onChange={(e) => setFilters({ ...filters, minLatencyMs: e.target.value ? Number(e.target.value) : undefined })}
                className="w-full p-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-purple-500"
              >
                <option value="">전체</option>
                <option value="2000">2초 이상 (일반)</option>
                <option value="5000">5초 이상 (고품질)</option>
                <option value="10000">10초 이상 (최고품질)</option>
              </select>
            </div>
          </div>

          <div className="mt-4 flex gap-3">
            <button
              onClick={handlePreview}
              disabled={loading}
              className="flex items-center gap-2 px-6 py-3 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition font-semibold disabled:opacity-50"
            >
              <Eye size={20} />
              미리보기
            </button>
            <button
              onClick={() => handleExport('json')}
              disabled={loading}
              className="flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-semibold disabled:opacity-50"
            >
              <Download size={20} />
              JSON 다운로드
            </button>
            <button
              onClick={() => handleExport('csv')}
              disabled={loading}
              className="flex items-center gap-2 px-6 py-3 bg-green-600 text-white rounded-lg hover:bg-green-700 transition font-semibold disabled:opacity-50"
            >
              <Download size={20} />
              CSV 다운로드
            </button>
          </div>
        </div>

        {/* 미리보기 결과 */}
        {previewData && (
          <div className="bg-white rounded-xl shadow-lg p-6">
            <h2 className="text-2xl font-bold mb-4">데이터 미리보기</h2>
            
            {/* 통계 */}
            <div className="grid grid-cols-4 gap-4 mb-6">
              <div className="p-4 bg-purple-50 rounded-lg">
                <p className="text-sm text-slate-600">총 데이터</p>
                <p className="text-2xl font-bold text-purple-600">{previewData.totalCount.toLocaleString()}</p>
              </div>
              <div className="p-4 bg-green-50 rounded-lg">
                <p className="text-sm text-slate-600">YES</p>
                <p className="text-2xl font-bold text-green-600">{previewData.yesPercentage.toFixed(1)}%</p>
              </div>
              <div className="p-4 bg-red-50 rounded-lg">
                <p className="text-sm text-slate-600">NO</p>
                <p className="text-2xl font-bold text-red-600">{previewData.noPercentage.toFixed(1)}%</p>
              </div>
              <div className="p-4 bg-blue-50 rounded-lg">
                <p className="text-sm text-slate-600">품질</p>
                <p className="text-2xl font-bold text-blue-600">
                  {previewData.filters.minLatencyMs ? `${previewData.filters.minLatencyMs / 1000}초+` : '전체'}
                </p>
              </div>
            </div>

            {/* 데이터 테이블 */}
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-100">
                  <tr>
                    <th className="p-3 text-left font-semibold">선택</th>
                    <th className="p-3 text-left font-semibold">응답시간</th>
                    <th className="p-3 text-left font-semibold">국가</th>
                    <th className="p-3 text-left font-semibold">직업</th>
                    <th className="p-3 text-left font-semibold">연령</th>
                    <th className="p-3 text-left font-semibold">티어</th>
                  </tr>
                </thead>
                <tbody>
                  {previewData.data.slice(0, 10).map((item, idx) => (
                    <tr key={idx} className="border-b hover:bg-slate-50">
                      <td className="p-3">
                        <span className={`px-2 py-1 rounded text-xs font-bold ${item.choice === 'YES' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                          {item.choice}
                        </span>
                      </td>
                      <td className="p-3 text-slate-600">{(item.latencyMs / 1000).toFixed(1)}초</td>
                      <td className="p-3 text-slate-600">{item.countryCode}</td>
                      <td className="p-3 text-slate-600">{item.jobCategory || '-'}</td>
                      <td className="p-3 text-slate-600">{item.ageGroup ? `${item.ageGroup}세` : '-'}</td>
                      <td className="p-3">
                        <span className={`px-2 py-1 rounded text-xs font-bold ${
                          item.tier === 'PLATINUM' ? 'bg-purple-100 text-purple-700' :
                          item.tier === 'GOLD' ? 'bg-yellow-100 text-yellow-700' :
                          item.tier === 'SILVER' ? 'bg-slate-200 text-slate-700' :
                          'bg-orange-100 text-orange-700'
                        }`}>
                          {item.tier}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p className="text-sm text-slate-500 mt-4">
              * 미리보기는 최대 10개만 표시됩니다. 전체 데이터를 받으려면 다운로드 버튼을 클릭하세요.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
