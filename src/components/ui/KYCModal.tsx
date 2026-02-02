'use client';

import React, { useState } from 'react';
import { ShieldCheck, ArrowUpRight, CheckCircle, X } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import { safeLocalStorage } from '@/lib/safeLocalStorage';

interface KYCModalProps {
  isOpen: boolean;
  onClose: () => void;
  onKYCComplete: () => void;
}

export default function KYCModal({ isOpen, onClose, onKYCComplete }: KYCModalProps) {
  const { isDark } = useTheme();
  const [kycStep, setKycStep] = useState(1);
  const [kycData, setKycData] = useState({ email: '', birthYear: '', location: '', occupation: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});

  if (!isOpen) return null;

  const validateForm = () => {
    const newErrors: Record<string, string> = {};
    if (!kycData.email.includes('@')) newErrors.email = '올바른 이메일을 입력하세요';
    if (!kycData.birthYear || Number(kycData.birthYear) < 1900 || Number(kycData.birthYear) > 2020) newErrors.birthYear = '올바른 생년을 입력하세요';
    if (!kycData.location) newErrors.location = '거주 지역을 선택하세요';
    if (!kycData.occupation) newErrors.occupation = '직업군을 선택하세요';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleKYCSubmit = () => {
    if (!validateForm()) return;
    safeLocalStorage.setItem('kycData', JSON.stringify(kycData));
    safeLocalStorage.setItem('kycVerified', 'true');
    setKycStep(3);
    setTimeout(() => {
      onKYCComplete();
      onClose();
      setKycStep(1);
      setErrors({});
    }, 2500);
  };

  const inputClass = (field: string) =>
    `w-full p-4 rounded-xl border-2 transition-all ${
      errors[field]
        ? 'border-rose-500'
        : isDark ? 'bg-slate-800 border-slate-700 focus:border-indigo-500' : 'bg-slate-50 border-slate-200 focus:border-indigo-500'
    }`;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
      <div className={`relative w-full max-w-lg rounded-3xl border p-8 shadow-2xl animate-slide-up ${isDark ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-100'}`}>
        <button onClick={onClose} className="absolute top-6 right-6 text-slate-400 hover:text-slate-600 transition-colors">
          <X size={24} />
        </button>

        {kycStep === 1 && (
          <>
            <div className="text-center mb-8">
              <div className="w-16 h-16 bg-indigo-600 rounded-2xl flex items-center justify-center text-white mx-auto mb-4">
                <ShieldCheck size={32} />
              </div>
              <h3 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>Level 2 티케터 인증</h3>
              <p className="text-sm text-slate-400">더 높은 리워드와 전문가 대우를 받으세요</p>
            </div>
            <div className="space-y-4 mb-8">
              <div className={`flex items-center space-x-4 p-4 rounded-2xl ${isDark ? 'bg-slate-800' : 'bg-slate-50'}`}>
                <div className="w-12 h-12 bg-rose-500/10 rounded-xl flex items-center justify-center text-rose-500 font-black">1%</div>
                <div>
                  <p className="font-bold text-sm">Level 1 (현재)</p>
                  <p className="text-xs text-slate-400">리워드 배율 1%</p>
                </div>
              </div>
              <div className="flex items-center justify-center">
                <ArrowUpRight size={24} className="text-indigo-500" />
              </div>
              <div className={`flex items-center space-x-4 p-4 rounded-2xl border-2 border-indigo-600 ${isDark ? 'bg-indigo-950/20' : 'bg-indigo-50'}`}>
                <div className="w-12 h-12 bg-indigo-600 rounded-xl flex items-center justify-center text-white font-black">50%</div>
                <div>
                  <p className="font-bold text-sm">Level 2 (인증 후)</p>
                  <p className="text-xs text-slate-400">리워드 배율 50%</p>
                </div>
              </div>
            </div>
            <button onClick={() => setKycStep(2)} className="w-full bg-indigo-600 text-white py-4 rounded-2xl font-bold hover:bg-indigo-700 transition-all shadow-xl shadow-indigo-500/20">
              인증 시작하기
            </button>
          </>
        )}

        {kycStep === 2 && (
          <>
            <h3 className={`text-xl font-black mb-6 ${isDark ? 'text-white' : 'text-slate-900'}`}>기본 정보 입력</h3>
            <div className="space-y-4 mb-8">
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-2 block">이메일</label>
                <input type="email" value={kycData.email} onChange={(e) => setKycData({...kycData, email: e.target.value})} placeholder="your@email.com" className={inputClass('email')} />
                {errors.email && <p className="text-xs text-rose-500 mt-1">{errors.email}</p>}
              </div>
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-2 block">생년</label>
                <input type="number" value={kycData.birthYear} onChange={(e) => setKycData({...kycData, birthYear: e.target.value})} placeholder="1990" className={inputClass('birthYear')} />
                {errors.birthYear && <p className="text-xs text-rose-500 mt-1">{errors.birthYear}</p>}
              </div>
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-2 block">거주 지역</label>
                <select value={kycData.location} onChange={(e) => setKycData({...kycData, location: e.target.value})} className={inputClass('location')}>
                  <option value="">선택하세요</option>
                  <option value="seoul">서울</option>
                  <option value="gyeonggi">경기</option>
                  <option value="busan">부산</option>
                  <option value="incheon">인천</option>
                  <option value="etc">기타</option>
                </select>
                {errors.location && <p className="text-xs text-rose-500 mt-1">{errors.location}</p>}
              </div>
              <div>
                <label className="text-xs font-bold text-slate-400 uppercase mb-2 block">직업군</label>
                <select value={kycData.occupation} onChange={(e) => setKycData({...kycData, occupation: e.target.value})} className={inputClass('occupation')}>
                  <option value="">선택하세요</option>
                  <option value="it">IT/테크</option>
                  <option value="finance">금융</option>
                  <option value="education">교육</option>
                  <option value="service">서비스업</option>
                  <option value="medical">의료</option>
                  <option value="etc">기타</option>
                </select>
                {errors.occupation && <p className="text-xs text-rose-500 mt-1">{errors.occupation}</p>}
              </div>
            </div>
            <div className="flex space-x-3">
              <button onClick={() => {setKycStep(1); setErrors({});}} className={`flex-1 py-3 rounded-xl font-bold transition-all ${isDark ? 'bg-slate-800 hover:bg-slate-700' : 'bg-slate-100 hover:bg-slate-200'}`}>
                이전
              </button>
              <button onClick={handleKYCSubmit} className="flex-1 bg-indigo-600 text-white py-3 rounded-xl font-bold hover:bg-indigo-700 transition-all">
                제출하기
              </button>
            </div>
          </>
        )}

        {kycStep === 3 && (
          <div className="text-center py-12">
            <div className="w-20 h-20 bg-emerald-500 rounded-full flex items-center justify-center text-white mx-auto mb-6 animate-bounce">
              <CheckCircle size={40} />
            </div>
            <h3 className={`text-2xl font-black mb-2 ${isDark ? 'text-white' : 'text-slate-900'}`}>인증 완료!</h3>
            <p className="text-slate-400 mb-4">Level 2 티케터로 승급되었습니다</p>
            <div className={`rounded-2xl p-4 inline-block ${isDark ? 'bg-emerald-950/20' : 'bg-emerald-50'}`}>
              <p className="text-sm font-bold text-emerald-600">리워드 배율: 1% → 50%</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
