'use client';

import React, { useState } from 'react';
import { Mail, User, MapPin, Briefcase, Calendar } from 'lucide-react';

const BACKEND_URL = 'http://localhost:8080/api';

interface LoginModalProps {
  onLoginSuccess: (memberId: number, memberData: any) => void;
}

interface MemberData {
  email: string;
  countryCode: string;
  jobCategory: string;
  ageGroup: number;
}

export default function LoginModal({ onLoginSuccess }: LoginModalProps) {
  const [step, setStep] = useState<'email' | 'register'>('email');
  const [email, setEmail] = useState('');
  const [memberData, setMemberData] = useState<MemberData>({
    email: '',
    countryCode: 'KR',
    jobCategory: 'IT',
    ageGroup: 30
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // 이메일로 회원 조회
  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // 백엔드에서 이메일로 회원 조회
      const response = await fetch(`${BACKEND_URL}/members/by-email?email=${encodeURIComponent(email)}`);
      
      if (response.ok) {
        const member = await response.json();
        // 기존 회원 - 바로 로그인
        onLoginSuccess(member.memberId, member);
      } else if (response.status === 404) {
        // 신규 회원 - 페르소나 입력으로 이동
        setMemberData({ ...memberData, email });
        setStep('register');
      } else {
        setError('로그인 중 오류가 발생했습니다.');
      }
    } catch (err) {
      setError('서버와 연결할 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 회원가입
  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch(`${BACKEND_URL}/members`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(memberData),
      });

      if (response.ok) {
        const member = await response.json();
        onLoginSuccess(member.memberId, member);
      } else {
        setError('회원가입 중 오류가 발생했습니다.');
      }
    } catch (err) {
      setError('서버와 연결할 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl shadow-2xl max-w-md w-full mx-4 overflow-hidden">
        
        {/* Header */}
        <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white p-6">
          <h2 className="text-2xl font-black">PREDATA</h2>
          <p className="text-sm opacity-90 mt-1">
            {step === 'email' ? '로그인하여 시작하기' : '페르소나 입력'}
          </p>
        </div>

        {/* Body */}
        <div className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-sm">
              {error}
            </div>
          )}

          {step === 'email' ? (
            // 이메일 입력
            <form onSubmit={handleEmailSubmit}>
              <div className="mb-6">
                <label className="block text-sm font-bold text-slate-700 mb-2">
                  이메일 주소
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 text-slate-400" size={20} />
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="your@email.com"
                    className="w-full pl-11 pr-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
                <p className="mt-2 text-xs text-slate-500">
                  💡 데모용이라 비밀번호는 필요 없습니다
                </p>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 transition disabled:opacity-50"
              >
                {loading ? '확인 중...' : '계속하기'}
              </button>
            </form>
          ) : (
            // 페르소나 입력
            <form onSubmit={handleRegister}>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">
                    <MapPin size={16} className="inline mr-1" />
                    국가
                  </label>
                  <select
                    value={memberData.countryCode}
                    onChange={(e) => setMemberData({ ...memberData, countryCode: e.target.value })}
                    className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="KR">🇰🇷 대한민국</option>
                    <option value="US">🇺🇸 미국</option>
                    <option value="JP">🇯🇵 일본</option>
                    <option value="SG">🇸🇬 싱가포르</option>
                    <option value="VN">🇻🇳 베트남</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">
                    <Briefcase size={16} className="inline mr-1" />
                    직업
                  </label>
                  <select
                    value={memberData.jobCategory}
                    onChange={(e) => setMemberData({ ...memberData, jobCategory: e.target.value })}
                    className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="IT">💻 IT</option>
                    <option value="Finance">💰 금융</option>
                    <option value="Medical">🏥 의료</option>
                    <option value="Student">📚 학생</option>
                    <option value="Service">🍔 서비스</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-bold text-slate-700 mb-2">
                    <Calendar size={16} className="inline mr-1" />
                    연령대
                  </label>
                  <select
                    value={memberData.ageGroup}
                    onChange={(e) => setMemberData({ ...memberData, ageGroup: Number(e.target.value) })}
                    className="w-full px-4 py-3 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value={20}>20대</option>
                    <option value={30}>30대</option>
                    <option value={40}>40대</option>
                    <option value={50}>50대</option>
                  </select>
                </div>

                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-xs text-blue-700">
                  <strong>📊 왜 필요한가요?</strong>
                  <p className="mt-1">페르소나 정보는 투표 데이터의 품질을 높이는 데 사용됩니다.</p>
                </div>
              </div>

              <div className="mt-6 flex gap-3">
                <button
                  type="button"
                  onClick={() => setStep('email')}
                  className="flex-1 py-3 border border-slate-200 text-slate-700 rounded-xl font-bold hover:bg-slate-50 transition"
                >
                  뒤로
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 py-3 bg-blue-600 text-white rounded-xl font-bold hover:bg-blue-700 transition disabled:opacity-50"
                >
                  {loading ? '가입 중...' : '시작하기'}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
