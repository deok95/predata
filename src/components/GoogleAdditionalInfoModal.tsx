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

        <h2 className="text-2xl font-black mb-4">Additional Information</h2>
        <p className="text-slate-500 mb-6">
          Please provide some additional information for better service.
        </p>

        {/* Country Selection */}
        <div className="mb-4">
          <label className="block text-sm font-bold mb-2">Country *</label>
          <select
            value={countryCode}
            onChange={(e) => setCountryCode(e.target.value)}
            className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-orange-500 focus:outline-none"
          >
            <option value="KR">South Korea</option>
            <option value="US">United States</option>
            <option value="JP">Japan</option>
            <option value="CN">China</option>
            <option value="UK">United Kingdom</option>
          </select>
        </div>

        {/* Job (Optional) */}
        <div className="mb-4">
          <label className="block text-sm font-bold mb-2">Job (Optional)</label>
          <input
            type="text"
            value={jobCategory}
            onChange={(e) => setJobCategory(e.target.value)}
            placeholder="e.g., Developer, Designer"
            className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-orange-500 focus:outline-none"
          />
        </div>

        {/* Age Group (Optional) */}
        <div className="mb-6">
          <label className="block text-sm font-bold mb-2">Age Group (Optional)</label>
          <select
            value={ageGroup || ''}
            onChange={(e) => setAgeGroup(e.target.value ? parseInt(e.target.value) : undefined)}
            className="w-full px-4 py-3 rounded-xl border-2 border-slate-200 focus:border-orange-500 focus:outline-none"
          >
            <option value="">Not specified</option>
            <option value="20">20s</option>
            <option value="30">30s</option>
            <option value="40">40s</option>
            <option value="50">50+</option>
          </select>
        </div>

        <button
          onClick={handleSubmit}
          className="w-full py-4 rounded-2xl font-bold text-lg bg-orange-500 text-white hover:bg-orange-600 transition-all active:scale-95"
        >
          Complete
        </button>
      </div>
    </div>
  );
}
