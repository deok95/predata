'use client';

import React, { useState } from 'react';
import { Mail, User, MapPin, Briefcase, Calendar, Wallet } from 'lucide-react';
import { useAccount, useConnect, useDisconnect } from 'wagmi';
import { ConnectButton } from '@rainbow-me/rainbowkit';

const BACKEND_URL = 'http://localhost:8080/api';

interface LoginModalProps {
  onLoginSuccess: (memberId: number, memberData: any) => void;
}

interface MemberData {
  email: string;
  countryCode: string;
  jobCategory: string;
  ageGroup: number;
  walletAddress?: string;
}

export default function LoginModal({ onLoginSuccess }: LoginModalProps) {
  const [step, setStep] = useState<'choice' | 'email' | 'wallet' | 'register'>('choice');
  const [email, setEmail] = useState('');
  const [memberData, setMemberData] = useState<MemberData>({
    email: '',
    countryCode: 'KR',
    jobCategory: 'IT',
    ageGroup: 30
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const { address, isConnected } = useAccount();

  // 이메일로 회원 조회
  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch(`${BACKEND_URL}/members/by-email?email=${encodeURIComponent(email)}`);
      
      if (response.ok) {
        const member = await response.json();
        onLoginSuccess(member.memberId, member);
      } else if (response.status === 404) {
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

  // 지갑 주소로 회원 조회
  const handleWalletLogin = async () => {
    if (!address) {
      setError('지갑이 연결되지 않았습니다.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      // 지갑 주소로 회원 조회 (백엔드에 엔드포인트 추가 필요)
      const response = await fetch(`${BACKEND_URL}/members/by-wallet?address=${address}`);
      
      if (response.ok) {
        const member = await response.json();
        onLoginSuccess(member.memberId, member);
      } else if (response.status === 404) {
        // 신규 회원 - 페르소나 입력으로 이동
        setMemberData({ ...memberData, walletAddress: address });
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

  // 회원 등록
  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await fetch(`${BACKEND_URL}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: memberData.email || `${address}@wallet.predata`,
          walletAddress: memberData.walletAddress,
          countryCode: memberData.countryCode,
          jobCategory: memberData.jobCategory,
          ageGroup: memberData.ageGroup
        })
      });

      if (response.ok) {
        const newMember = await response.json();
        onLoginSuccess(newMember.memberId, newMember);
      } else {
        const errorData = await response.json();
        setError(errorData.message || '회원가입에 실패했습니다.');
      }
    } catch (err) {
      setError('서버와 연결할 수 없습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-gray-900 p-8 rounded-2xl shadow-2xl w-full max-w-md border border-gray-700">
        <h2 className="text-3xl font-bold text-white mb-6 text-center">
          🔮 Predata
        </h2>

        {/* 로그인 방식 선택 */}
        {step === 'choice' && (
          <div className="space-y-4">
            <p className="text-gray-400 text-center mb-6">
              로그인 방식을 선택하세요
            </p>

            <button
              onClick={() => setStep('wallet')}
              className="w-full flex items-center justify-center gap-2 px-6 py-4 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-xl hover:from-blue-700 hover:to-purple-700 transition font-semibold"
            >
              <Wallet size={20} />
              지갑으로 로그인 (Web3)
            </button>

            <button
              onClick={() => setStep('email')}
              className="w-full flex items-center justify-center gap-2 px-6 py-4 bg-gray-700 text-white rounded-xl hover:bg-gray-600 transition font-semibold"
            >
              <Mail size={20} />
              이메일로 로그인
            </button>

            <p className="text-xs text-gray-500 text-center mt-4">
              💡 Web3 지갑 로그인 시 온체인에 베팅이 기록됩니다
            </p>
          </div>
        )}

        {/* 지갑 연결 */}
        {step === 'wallet' && (
          <div className="space-y-4">
            <button
              onClick={() => setStep('choice')}
              className="text-gray-400 hover:text-white mb-4"
            >
              ← 뒤로
            </button>

            <div className="text-center space-y-4">
              <p className="text-gray-400 mb-4">
                지갑을 연결하세요
              </p>

              <ConnectButton />

              {isConnected && address && (
                <div className="mt-6">
                  <p className="text-sm text-gray-400 mb-2">
                    연결된 지갑: {address.slice(0, 6)}...{address.slice(-4)}
                  </p>
                  <button
                    onClick={handleWalletLogin}
                    disabled={loading}
                    className="w-full px-6 py-3 bg-gradient-to-r from-green-600 to-blue-600 text-white rounded-xl hover:from-green-700 hover:to-blue-700 transition font-semibold disabled:opacity-50"
                  >
                    {loading ? '로그인 중...' : '계속하기'}
                  </button>
                </div>
              )}
            </div>

            {error && (
              <p className="text-red-400 text-sm text-center">{error}</p>
            )}
          </div>
        )}

        {/* 이메일 로그인 */}
        {step === 'email' && (
          <form onSubmit={handleEmailSubmit} className="space-y-4">
            <button
              type="button"
              onClick={() => setStep('choice')}
              className="text-gray-400 hover:text-white mb-4"
            >
              ← 뒤로
            </button>

            <div className="relative">
              <Mail className="absolute left-3 top-3 text-gray-400" size={20} />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="이메일 주소"
                required
                className="w-full pl-10 pr-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-xl hover:from-purple-700 hover:to-pink-700 transition font-semibold disabled:opacity-50"
            >
              {loading ? '확인 중...' : '계속하기'}
            </button>

            {error && (
              <p className="text-red-400 text-sm text-center">{error}</p>
            )}
          </form>
        )}

        {/* 페르소나 입력 */}
        {step === 'register' && (
          <form onSubmit={handleRegister} className="space-y-4">
            <p className="text-gray-400 text-center mb-4">
              페르소나 정보를 입력하세요
            </p>

            {memberData.walletAddress && (
              <div className="bg-blue-900 bg-opacity-30 p-3 rounded-lg mb-4">
                <p className="text-xs text-blue-300">
                  🔗 지갑: {memberData.walletAddress.slice(0, 6)}...{memberData.walletAddress.slice(-4)}
                </p>
              </div>
            )}

            {!memberData.walletAddress && (
              <div className="relative">
                <Mail className="absolute left-3 top-3 text-gray-400" size={20} />
                <input
                  type="email"
                  value={memberData.email}
                  disabled
                  className="w-full pl-10 pr-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white"
                />
              </div>
            )}

            <div className="relative">
              <MapPin className="absolute left-3 top-3 text-gray-400" size={20} />
              <select
                value={memberData.countryCode}
                onChange={(e) => setMemberData({ ...memberData, countryCode: e.target.value })}
                className="w-full pl-10 pr-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white"
              >
                <option value="KR">🇰🇷 한국</option>
                <option value="US">🇺🇸 미국</option>
                <option value="JP">🇯🇵 일본</option>
                <option value="CN">🇨🇳 중국</option>
              </select>
            </div>

            <div className="relative">
              <Briefcase className="absolute left-3 top-3 text-gray-400" size={20} />
              <select
                value={memberData.jobCategory}
                onChange={(e) => setMemberData({ ...memberData, jobCategory: e.target.value })}
                className="w-full pl-10 pr-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white"
              >
                <option value="IT">💻 IT/개발</option>
                <option value="FINANCE">💰 금융</option>
                <option value="ART">🎨 예술/디자인</option>
                <option value="EDUCATION">📚 교육</option>
                <option value="OTHER">🔧 기타</option>
              </select>
            </div>

            <div className="relative">
              <Calendar className="absolute left-3 top-3 text-gray-400" size={20} />
              <select
                value={memberData.ageGroup}
                onChange={(e) => setMemberData({ ...memberData, ageGroup: parseInt(e.target.value) })}
                className="w-full pl-10 pr-4 py-3 bg-gray-800 border border-gray-700 rounded-lg text-white"
              >
                <option value={20}>20대</option>
                <option value={30}>30대</option>
                <option value={40}>40대</option>
                <option value={50}>50대 이상</option>
              </select>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full px-6 py-3 bg-gradient-to-r from-green-600 to-blue-600 text-white rounded-xl hover:from-green-700 hover:to-blue-700 transition font-semibold disabled:opacity-50"
            >
              {loading ? '가입 중...' : '가입 완료'}
            </button>

            {error && (
              <p className="text-red-400 text-sm text-center">{error}</p>
            )}
          </form>
        )}
      </div>
    </div>
  );
}
