'use client';

import React, { useState, useEffect } from 'react';
import { TrendingUp, Ticket, ChevronDown, Info, Vote as VoteIcon, DollarSign, LogOut, User, Shield, Receipt, CheckCircle, Database, Download } from 'lucide-react';
import LoginModal from '@/components/LoginModal';
import AdminPanel from '@/components/AdminPanel';
import MyBets from '@/components/MyBets';

const BACKEND_URL = 'http://localhost:8080/api';

interface Question {
  id: number;
  title: string;
  category: string;
  status: string;
  finalResult?: string;
  totalBetPool: number;
  yesBetPool: number;
  noBetPool: number;
  yesPercentage: number;
  noPercentage: number;
  expiredAt: string;
  createdAt: string;
}

interface OddsData {
  questionId: number;
  yesOdds: string;
  noOdds: string;
  yesPrice: string;
  noPrice: string;
  poolYes: number;
  poolNo: number;
  totalPool: number;
}

interface TicketStatus {
  remainingCount: number;
  resetDate: string;
}

interface MemberData {
  memberId: number;
  email: string;
  countryCode: string;
  jobCategory: string;
  ageGroup: number;
  tier: string;
  pointBalance: number;
}

export default function PolymarketStyleHome() {
  const [currentUser, setCurrentUser] = useState<MemberData | null>(null);
  const [question, setQuestion] = useState<Question | null>(null);
  const [odds, setOdds] = useState<OddsData | null>(null);
  const [prevOdds, setPrevOdds] = useState<OddsData | null>(null);
  const [ticketStatus, setTicketStatus] = useState<TicketStatus | null>(null);
  const [selection, setSelection] = useState<'YES' | 'NO'>('YES');
  const [amount, setAmount] = useState<string>('');
  const [mode, setMode] = useState<'VOTE' | 'BET'>('VOTE');
  const [loading, setLoading] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isAdminPanelOpen, setIsAdminPanelOpen] = useState(false);
  const [isMyBetsOpen, setIsMyBetsOpen] = useState(false);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);
  const [isPersonaModalOpen, setIsPersonaModalOpen] = useState(false);

  // 질문 데이터 가져오기
  const fetchQuestion = async () => {
    try {
      const response = await fetch(`${BACKEND_URL}/questions`);
      const questions = await response.json();
      if (questions.length > 0) {
        const firstQuestion = questions[0];
        setQuestion(firstQuestion);
        fetchOdds(firstQuestion.id);
      }
    } catch (error) {
      console.error('Failed to fetch question:', error);
    }
  };

  // 배당률 가져오기
  const fetchOdds = async (questionId: number, silent = false) => {
    try {
      if (!silent) setIsUpdating(true);
      
      const response = await fetch(`${BACKEND_URL}/questions/${questionId}/odds`);
      const data = await response.json();
      
      // 이전 배당률 저장 (애니메이션용)
      if (odds && (odds.yesOdds !== data.yesOdds || odds.noOdds !== data.noOdds)) {
        setPrevOdds(odds);
        setTimeout(() => setPrevOdds(null), 1000); // 1초 후 애니메이션 제거
      }
      
      setOdds(data);
    } catch (error) {
      console.error('Failed to fetch odds:', error);
    } finally {
      if (!silent) setIsUpdating(false);
    }
  };

  // 티켓 상태 가져오기
  const fetchTicketStatus = async () => {
    if (!currentUser) return; // currentUser가 없으면 실행 안 함
    
    try {
      const response = await fetch(`${BACKEND_URL}/tickets/${currentUser.memberId}`);
      const data = await response.json();
      setTicketStatus(data);
    } catch (error) {
      console.error('Failed to fetch ticket status:', error);
    }
  };

  // 투표 실행 (티켓 필요)
  const handleVote = async () => {
    if (!question) return;

    // 티켓 체크
    if (!ticketStatus || ticketStatus.remainingCount <= 0) {
      alert('⚠️ 티켓이 부족합니다!\n\n투표는 하루에 5번만 가능합니다.\n다음날 00시에 티켓이 리셋됩니다.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/vote`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          memberId: currentUser.memberId,
          questionId: question.id,
          choice: selection,
          latencyMs: Math.floor(Math.random() * 10000) + 2000
        }),
      });

      const result = await response.json();

      if (result.success) {
        alert(`✅ 투표 완료!\n\n${selection}에 투표했습니다.\n남은 티켓: ${result.remainingTickets}개`);
        await fetchQuestion();
        await fetchTicketStatus();
        // 배당률 즉시 갱신
        if (question) {
          await fetchOdds(question.id);
        }
      } else {
        alert(`❌ 투표 실패\n\n${result.message}`);
      }
    } catch (error) {
      console.error('Vote failed:', error);
      alert('❌ 투표 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  // 베팅 실행 (돈 필요)
  const handleBet = async () => {
    if (!amount || Number(amount) <= 0 || !question) {
      alert('⚠️ 금액을 입력해주세요!');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/bet`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          memberId: currentUser.memberId,
          questionId: question.id,
          choice: selection,
          amount: Number(amount),
          latencyMs: Math.floor(Math.random() * 10000) + 2000
        }),
      });

      const result = await response.json();

      if (result.success) {
        alert(`✅ 베팅 성공!\n\n${selection}에 $${amount} 베팅했습니다.`);
        await fetchQuestion();
        setAmount('');
        // 배당률 즉시 갱신
        if (question) {
          await fetchOdds(question.id);
        }
      } else {
        alert(`❌ 베팅 실패\n\n${result.message}`);
      }
    } catch (error) {
      console.error('Bet failed:', error);
      alert('❌ 베팅 중 오류가 발생했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // 로그인 상태 확인
    const savedUser = localStorage.getItem('predataUser');
    if (savedUser) {
      setCurrentUser(JSON.parse(savedUser));
    }
  }, []);

  // currentUser가 변경될 때 데이터 로드
  useEffect(() => {
    if (!currentUser) return;

    fetchQuestion();
    fetchTicketStatus();

    // 5초마다 배당률 자동 갱신
    const intervalId = setInterval(() => {
      if (question) {
        fetchOdds(question.id, true); // silent mode: 로딩 표시 안 함
      }
    }, 5000);

    // 컴포넌트 언마운트 시 interval 제거
    return () => clearInterval(intervalId);
  }, [currentUser, question?.id]); // currentUser와 question.id가 변경될 때마다 실행

  // 로그인 성공 핸들러
  const handleLoginSuccess = (memberId: number, memberData: MemberData) => {
    setCurrentUser(memberData);
    localStorage.setItem('predataUser', JSON.stringify(memberData));
  };

  // 로그아웃
  const handleLogout = () => {
    setCurrentUser(null);
    localStorage.removeItem('predataUser');
  };

  // 정산 완료 후 새로고침
  const handleSettled = () => {
    fetchQuestion();
    if (currentUser) {
      fetchTicketStatus();
    }
  };

  // 로딩 중
  if (!currentUser) {
    return <LoginModal onLoginSuccess={handleLoginSuccess} />;
  }

  if (!question || !odds) {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-4 border-blue-600 mx-auto mb-4"></div>
          <div className="text-lg font-bold text-slate-700">Loading Protocol...</div>
          <div className="text-sm text-slate-400 mt-2">데이터를 불러오는 중입니다</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-white text-[#0F172A] font-sans">
      {/* 상단 티켓 상태바 */}
      {ticketStatus && (
        <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white py-3 px-6">
          <div className="max-w-[1100px] mx-auto flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Ticket size={24} />
              <div>
                <div className="text-sm font-bold">5-Lock 티켓</div>
                <div className="text-xs opacity-90">
                  남은 투표권: {ticketStatus.remainingCount}/5
                </div>
              </div>
            </div>
            <div className="flex items-center gap-4">
              {isUpdating && (
                <div className="flex items-center gap-2 text-xs">
                  <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-white"></div>
                  <span>업데이트 중...</span>
                </div>
              )}
              <div className="flex items-center gap-2 text-xs">
                <User size={14} />
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold">{currentUser.email}</span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      currentUser.tier === 'PLATINUM' ? 'bg-purple-400 text-white' :
                      currentUser.tier === 'GOLD' ? 'bg-yellow-400 text-yellow-900' :
                      currentUser.tier === 'SILVER' ? 'bg-slate-300 text-slate-700' :
                      'bg-orange-400 text-white'
                    }`}>
                      {currentUser.tier}
                    </span>
                  </div>
                  <div className="opacity-75">{currentUser.countryCode} · {currentUser.jobCategory}</div>
                </div>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center gap-1 px-3 py-1.5 bg-white/20 hover:bg-white/30 rounded-lg text-xs font-bold transition"
              >
                <LogOut size={14} />
                로그아웃
              </button>
              <button
                onClick={() => window.location.href = '/data-center'}
                className="flex items-center gap-1 px-3 py-1.5 bg-blue-500 hover:bg-blue-600 rounded-lg text-xs font-bold transition text-white"
              >
                <Database size={14} />
                데이터센터
              </button>
              <button
                onClick={() => window.location.href = '/premium-data'}
                className="flex items-center gap-1 px-3 py-1.5 bg-purple-500 hover:bg-purple-600 rounded-lg text-xs font-bold transition text-white"
              >
                <Download size={14} />
                프리미엄
              </button>
              <button
                onClick={() => setIsMyBetsOpen(true)}
                className="flex items-center gap-1 px-3 py-1.5 bg-white/20 hover:bg-white/30 rounded-lg text-xs font-bold transition"
              >
                <Receipt size={14} />
                내 베팅
              </button>
              <button
                onClick={() => setIsAdminPanelOpen(true)}
                className="flex items-center gap-1 px-3 py-1.5 bg-red-500 hover:bg-red-600 rounded-lg text-xs font-bold transition text-white"
              >
                <Shield size={14} />
                관리자
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Main Content Area */}
      <main className="max-w-[1100px] mx-auto px-6 py-10 grid grid-cols-1 lg:grid-cols-12 gap-10">
        
        {/* Left Side: Graph & Info (2/3) */}
        <div className="lg:col-span-8">
          <div className="flex items-center gap-2 mb-4">
             <div className="w-6 h-6 bg-black rounded-full text-white flex items-center justify-center text-[10px] font-bold italic">P</div>
             <span className="text-sm font-semibold text-slate-500 uppercase tracking-tight">{question.category} Market</span>
          </div>
          <h2 className="text-3xl font-bold mb-6 tracking-tight leading-tight">{question.title}</h2>

          {/* 정산 완료 배너 */}
          {question.status === 'SETTLED' && (
            <div className="mb-6 p-4 bg-gradient-to-r from-green-50 to-blue-50 border-2 border-green-200 rounded-lg">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-green-500 rounded-full flex items-center justify-center">
                  <CheckCircle className="text-white" size={28} />
                </div>
                <div>
                  <div className="text-lg font-bold text-green-700">정산 완료</div>
                  <div className="text-sm text-slate-600">
                    최종 결과: <span className={`font-bold ${question.finalResult === 'YES' ? 'text-green-600' : 'text-red-600'}`}>
                      {question.finalResult}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          )}
          
          {/* 그래프 영역 */}
          <div className="h-[350px] border-b border-slate-100 relative mb-8">
             <div className="absolute top-0 left-0 text-2xl font-black text-blue-600">
               {question.yesPercentage.toFixed(0)}%
             </div>
             <div className="w-full h-full bg-gradient-to-t from-blue-50/20 to-transparent flex items-end">
                <TrendingUp size={100} className="text-blue-200 mb-20 ml-20 opacity-30" />
             </div>
          </div>

          <div className="flex gap-10 border-b pb-8">
             <div>
                <p className="text-[11px] font-bold text-slate-400 uppercase mb-1">Vol.</p>
                <p className="text-sm font-bold">${question.totalBetPool.toLocaleString()}</p>
             </div>
             <div>
                <p className="text-[11px] font-bold text-slate-400 uppercase mb-1">Ends</p>
                <p className="text-sm font-bold">{new Date(question.expiredAt).toLocaleDateString()}</p>
             </div>
              </div>
            </div>

        {/* Right Side: Trading Widget (1/3) */}
        <div className="lg:col-span-4">
          <div className="border border-slate-200 rounded-2xl shadow-sm overflow-hidden sticky top-10">
            
            {/* Vote / Bet 탭 */}
            <div className="flex border-b text-xs font-black uppercase tracking-widest">
               <button 
                 onClick={() => setMode('VOTE')}
                 className={`flex-1 py-4 flex items-center justify-center gap-2 transition ${
                   mode === 'VOTE' 
                     ? 'border-b-2 border-blue-600 bg-white' 
                     : 'text-slate-300 hover:bg-slate-50'
                 }`}
               >
                 <VoteIcon size={14} />
                 Vote
              </button>
               <button 
                 onClick={() => setMode('BET')}
                 className={`flex-1 py-4 flex items-center justify-center gap-2 transition ${
                   mode === 'BET' 
                     ? 'border-b-2 border-green-600 bg-white' 
                     : 'text-slate-300 hover:bg-slate-50'
                 }`}
               >
                 <DollarSign size={14} />
                 Bet
              </button>
            </div>

            <div className="p-5 space-y-5">
               {/* 모드 설명 */}
               <div className="text-xs text-slate-500 bg-slate-50 p-3 rounded-lg">
                 {mode === 'VOTE' ? (
                   <div className="flex items-start gap-2">
                     <Ticket size={14} className="mt-0.5 flex-shrink-0" />
                     <div>
                       <div className="font-bold text-slate-700">투표 모드</div>
                       <div className="mt-1">티켓을 사용하여 투표합니다. (하루 5번 제한)</div>
                </div>
              </div>
                 ) : (
                   <div className="flex items-start gap-2">
                     <DollarSign size={14} className="mt-0.5 flex-shrink-0" />
                     <div>
                       <div className="font-bold text-slate-700">베팅 모드</div>
                       <div className="mt-1">돈을 걸어 배당을 받습니다. 정답 맞추면 수익!</div>
              </div>
            </div>
                 )}
               </div>

               {/* YES/NO Selection Buttons */}
               <div className="flex gap-2">
                  <button 
                    onClick={() => setSelection('YES')}
                    className={`flex-1 py-3 rounded-xl font-bold text-lg transition-all ${
                      selection === 'YES' 
                        ? 'bg-[#10B981] text-white shadow-lg' 
                        : 'bg-slate-50 text-slate-400'
                    } ${prevOdds && prevOdds.yesOdds !== odds.yesOdds ? 'animate-pulse' : ''}`}
                  >
                    <div>Yes</div>
                    <div className="text-sm">
                      {Math.round(Number(odds.yesPrice) * 100)}¢
                      {prevOdds && prevOdds.yesPrice !== odds.yesPrice && (
                        <span className="ml-1 text-xs">
                          {Number(odds.yesPrice) > Number(prevOdds.yesPrice) ? '↑' : '↓'}
                        </span>
                      )}
                    </div>
                  </button>
                  <button 
                    onClick={() => setSelection('NO')}
                    className={`flex-1 py-3 rounded-xl font-bold text-lg transition-all ${
                      selection === 'NO' 
                        ? 'bg-[#EF4444] text-white shadow-lg' 
                        : 'bg-slate-50 text-slate-400'
                    } ${prevOdds && prevOdds.noOdds !== odds.noOdds ? 'animate-pulse' : ''}`}
                  >
                    <div>No</div>
                    <div className="text-sm">
                      {Math.round(Number(odds.noPrice) * 100)}¢
                      {prevOdds && prevOdds.noPrice !== odds.noPrice && (
                        <span className="ml-1 text-xs">
                          {Number(odds.noPrice) > Number(prevOdds.noPrice) ? '↑' : '↓'}
                        </span>
                      )}
                    </div>
                  </button>
          </div>

               {/* Amount Input - BET 모드일 때만 표시 */}
               {mode === 'BET' && (
                 <>
                   <div className="space-y-2">
                      <div className="flex justify-between text-[11px] font-bold text-slate-400 uppercase">
                         <span>Amount</span>
                         <span>$0 Max</span>
                      </div>
                      <div className="relative">
                         <input 
                           type="number"
                           value={amount}
                           onChange={(e) => setAmount(e.target.value)}
                           placeholder="0"
                           className="w-full bg-slate-50 border border-slate-100 rounded-xl py-4 px-4 text-xl font-black focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                         />
                         <span className="absolute right-4 top-1/2 -translate-y-1/2 font-bold text-slate-300">$</span>
              </div>
            </div>
            
                   {/* Quick Select Buttons */}
                   <div className="flex gap-1">
                      {['1', '20', '100', '500'].map((val) => (
                        <button 
                          key={val}
                          onClick={() => setAmount(val)}
                          className="flex-1 py-1.5 bg-slate-50 border border-slate-100 rounded-lg text-[10px] font-bold text-slate-500 hover:bg-white transition"
                        >
                          ${val}
                        </button>
                      ))}
              </div>
                 </>
               )}

               {/* Action Button */}
               <button 
                 onClick={mode === 'VOTE' ? handleVote : handleBet}
                 disabled={loading || (mode === 'BET' && (!amount || Number(amount) <= 0))}
                 className={`w-full py-4 rounded-xl font-black text-lg transition-all shadow-xl disabled:opacity-50 disabled:cursor-not-allowed ${
                   mode === 'VOTE'
                     ? 'bg-blue-600 text-white hover:bg-blue-700 shadow-blue-100'
                     : 'bg-green-600 text-white hover:bg-green-700 shadow-green-100'
                 }`}
               >
                  {loading 
                    ? (mode === 'VOTE' ? 'Voting...' : 'Betting...') 
                    : (mode === 'VOTE' 
                        ? `🎫 Vote ${selection}` 
                        : `💰 Bet $${amount || '0'}`
                      )
                  }
              </button>

               {/* Info Section */}
               <div className="pt-2 flex flex-col gap-2 text-[11px] text-slate-400 font-medium">
                  {mode === 'VOTE' ? (
                    <>
                      <div className="flex justify-between">
                         <span>티켓 소모</span>
                         <span className="font-bold text-blue-600">1개</span>
                      </div>
                      <div className="flex justify-between">
                         <span>남은 티켓</span>
                         <span className="font-bold">{ticketStatus?.remainingCount || 0}/5</span>
                      </div>
                    </>
                  ) : (
                    <>
                      <div className="flex justify-between">
                         <span>Avg price</span>
                         <span>{selection === 'YES' ? odds.yesPrice : odds.noPrice}¢</span>
                      </div>
                      <div className="flex justify-between">
                         <span>Shares</span>
                         <span>{amount ? (Number(amount) / Number(selection === 'YES' ? odds.yesPrice : odds.noPrice)).toFixed(2) : '0.00'}</span>
                      </div>
                      <div className="flex justify-between border-t pt-2 mt-1">
                         <span className="flex items-center gap-1">Potential return <Info size={10} /></span>
                         <span className="text-green-500 font-bold">
                           ${amount ? (Number(amount) / Number(selection === 'YES' ? odds.yesPrice : odds.noPrice)).toFixed(2) : '0.00'} 
                           ({amount ? ((1 / Number(selection === 'YES' ? odds.yesPrice : odds.noPrice) - 1) * 100).toFixed(0) : '0'}%)
                         </span>
                      </div>
                    </>
                  )}
               </div>
            </div>
          </div>
        </div>
      </main>

      {/* 관리자 패널 모달 */}
      {isAdminPanelOpen && question && (
        <AdminPanel
          questionId={question.id}
          questionTitle={question.title}
          onClose={() => setIsAdminPanelOpen(false)}
          onSettled={handleSettled}
        />
      )}

      {/* 내 베팅 내역 모달 */}
      {isMyBetsOpen && currentUser && (
        <MyBets
          memberId={currentUser.memberId}
          onClose={() => setIsMyBetsOpen(false)}
        />
      )}

      {/* 로그인 모달 */}
      {isLoginModalOpen && (
        <LoginModal
          onLoginSuccess={handleLoginSuccess}
          onOpenPersonaModal={() => setIsPersonaModalOpen(true)}
          isPersonaModalOpen={isPersonaModalOpen}
          onClosePersonaModal={() => setIsPersonaModalOpen(false)}
        />
      )}
    </div>
  );
}
