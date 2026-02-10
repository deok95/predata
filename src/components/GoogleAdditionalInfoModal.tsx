'use client';

import { useState } from 'react';
import { X } from 'lucide-react';

interface Props {
  googleToken: string;
  onSubmit: (info: {
    countryCode: string;
    jobCategory?: string;
    ageGroup?: number;
  }) => void;
  onClose: () => void;
}

export default function GoogleAdditionalInfoModal({ googleToken, onSubmit, onClose }: Props) {
  const [countryCode, setCountryCode] = useState('KR');
  const [jobCategory, setJobCategory] = useState('');
  const [ageGroup, setAgeGroup] = useState<number | undefined>();

  const handleSubmit = () => {
    onSubmit({
      countryCode,
      jobCategory: jobCategory || undefined,
      ageGroup,
    });
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/60" onClick={onClose} />
      <div className="relative bg-white rounded-3xl p-8 max-w-md w-full">
        <button onClick={onClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
          <X size={24} />
        </button>

        <h2 className="text-2xl font-black mb-4">추가 정보 입력</h2>
        <p className="text-slate-500 mb-6">
          더 나은 서비스를 위해 몇 가지 정보를 입력해주세요.
        </p>

        {/* 국가 선택 */}
        <div className="mb-4">
          <label className="block text-sm font-bold mb-2">국가 *</label>
          <select
            value={countryCode}
            onChange={(e) => setCountryCode(e.target.value)}
            className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-orange-500 focus:outline-none"
          >
            <option value="KR">대한민국</option>
            <option value="US">미국</option>
            <option value="JP">일본</option>
            <option value="CN">중국</option>
            <option value="UK">영국</option>
          </select>
        </div>

        {/* 직업 (선택) */}
        <div className="mb-4">
          <label className="block text-sm font-bold mb-2">직업 (선택)</label>
          <input
            type="text"
            value={jobCategory}
            onChange={(e) => setJobCategory(e.target.value)}
            placeholder="예: 개발자, 디자이너"
            className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-orange-500 focus:outline-none"
          />
        </div>

        {/* 연령대 (선택) */}
        <div className="mb-6">
          <label className="block text-sm font-bold mb-2">연령대 (선택)</label>
          <select
            value={ageGroup || ''}
            onChange={(e) => setAgeGroup(e.target.value ? parseInt(e.target.value) : undefined)}
            className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-orange-500 focus:outline-none"
          >
            <option value="">선택 안 함</option>
            <option value="20">20대</option>
            <option value="30">30대</option>
            <option value="40">40대</option>
            <option value="50">50대 이상</option>
          </select>
        </div>

        <button
          onClick={handleSubmit}
          className="w-full py-4 rounded-2xl font-bold text-lg bg-orange-500 text-white hover:bg-orange-600 transition-all active:scale-95"
        >
          완료
        </button>
      </div>
    </div>
  );
}
