'use client';

import React, { useState, useEffect } from 'react';
import { TrendingUp, Clock, DollarSign, ArrowRight, Filter as FilterIcon } from 'lucide-react';
import Link from 'next/link';

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

export default function MarketplaceHome() {
  const [questions, setQuestions] = useState<Question[]>([]);
  const [filteredQuestions, setFilteredQuestions] = useState<Question[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [sortBy, setSortBy] = useState<'volume' | 'recent'>('volume');

  const categories = ['ALL', 'ECONOMY', 'SPORTS', 'POLITICS', 'TECH', 'CULTURE'];

  useEffect(() => {
    fetchQuestions();
  }, []);

  useEffect(() => {
    filterAndSortQuestions();
  }, [questions, selectedCategory, sortBy]);

  const fetchQuestions = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${BACKEND_URL}/questions`);
      const data = await response.json();
      setQuestions(data);
    } catch (error) {
      console.error('Failed to fetch questions:', error);
    } finally {
      setLoading(false);
    }
  };

  const filterAndSortQuestions = () => {
    let filtered = questions;

    // 카테고리 필터
    if (selectedCategory !== 'ALL') {
      filtered = filtered.filter(q => q.category === selectedCategory);
    }

    // 정렬
    if (sortBy === 'volume') {
      filtered = [...filtered].sort((a, b) => b.totalBetPool - a.totalBetPool);
    } else {
      filtered = [...filtered].sort((a, b) => 
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
    }

    setFilteredQuestions(filtered);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50 flex items-center justify-center">
        <div className="text-center">
          <TrendingUp className="h-16 w-16 text-blue-600 mx-auto mb-4 animate-pulse" />
          <p className="text-xl font-bold text-slate-700">시장 로딩 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50">
      {/* 헤더 */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-black rounded-full text-white flex items-center justify-center text-sm font-bold italic">P</div>
              <h1 className="text-2xl font-black">Predata</h1>
            </div>
            <div className="flex items-center gap-3">
              <Link href="/admin/questions">
                <button className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition font-semibold text-sm">
                  관리자
                </button>
              </Link>
              <Link href="/data-center">
                <button className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition font-semibold text-sm">
                  데이터 센터
                </button>
              </Link>
              <Link href="/premium-data">
                <button className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition font-semibold text-sm">
                  프리미엄 데이터
                </button>
              </Link>
            </div>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-6 py-8">
        
        {/* 타이틀 & 설명 */}
        <div className="mb-8">
          <h2 className="text-4xl font-black mb-2">예측 시장</h2>
          <p className="text-slate-600 text-lg">실시간으로 움직이는 예측 시장에 참여하세요</p>
        </div>

        {/* 필터 & 정렬 */}
        <div className="flex items-center justify-between mb-6">
          {/* 카테고리 필터 */}
          <div className="flex items-center gap-2">
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                className={`px-4 py-2 rounded-lg text-sm font-semibold transition ${
                  selectedCategory === cat
                    ? 'bg-blue-600 text-white'
                    : 'bg-white text-slate-600 hover:bg-slate-100'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* 정렬 */}
          <div className="flex items-center gap-2">
            <FilterIcon size={16} className="text-slate-600" />
            <button
              onClick={() => setSortBy('volume')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                sortBy === 'volume' ? 'bg-slate-800 text-white' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              거래량순
            </button>
            <button
              onClick={() => setSortBy('recent')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition ${
                sortBy === 'recent' ? 'bg-slate-800 text-white' : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              최신순
            </button>
          </div>
        </div>

        {/* 질문 카드 그리드 */}
        {filteredQuestions.length === 0 ? (
          <div className="text-center py-20">
            <p className="text-slate-500 text-lg">해당 카테고리에 질문이 없습니다.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredQuestions.map((question) => (
              <Link key={question.id} href={`/question/${question.id}`}>
                <div className="bg-white rounded-xl shadow-lg hover:shadow-2xl transition-all cursor-pointer border border-slate-200 overflow-hidden group">
                  
                  {/* 상단: 카테고리 & 상태 */}
                  <div className="p-4 border-b border-slate-100 flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wide">{question.category}</span>
                    {question.status === 'SETTLED' && (
                      <span className="px-2 py-1 bg-green-100 text-green-700 text-xs font-bold rounded">
                        정산완료
                      </span>
                    )}
                  </div>

                  {/* 중앙: 제목 */}
                  <div className="p-5">
                    <h3 className="text-lg font-bold text-slate-800 mb-4 line-clamp-2 group-hover:text-blue-600 transition">
                      {question.title}
                    </h3>

                    {/* 배당률 표시 */}
                    <div className="flex gap-2 mb-4">
                      <div className={`flex-1 py-3 rounded-lg text-center ${
                        question.yesPercentage > 50 ? 'bg-green-100' : 'bg-green-50'
                      }`}>
                        <div className="text-xs text-slate-600 mb-1">YES</div>
                        <div className={`text-2xl font-black ${
                          question.yesPercentage > 50 ? 'text-green-700' : 'text-green-600'
                        }`}>
                          {question.yesPercentage.toFixed(0)}%
                        </div>
                      </div>
                      <div className={`flex-1 py-3 rounded-lg text-center ${
                        question.noPercentage > 50 ? 'bg-red-100' : 'bg-red-50'
                      }`}>
                        <div className="text-xs text-slate-600 mb-1">NO</div>
                        <div className={`text-2xl font-black ${
                          question.noPercentage > 50 ? 'text-red-700' : 'text-red-600'
                        }`}>
                          {question.noPercentage.toFixed(0)}%
                        </div>
                      </div>
                    </div>

                    {/* 하단: 거래량 & 마감일 */}
                    <div className="flex items-center justify-between text-xs text-slate-500">
                      <div className="flex items-center gap-1">
                        <DollarSign size={14} />
                        <span>${question.totalBetPool.toLocaleString()}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <Clock size={14} />
                        <span>{new Date(question.expiredAt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })}</span>
                      </div>
                    </div>
                  </div>

                  {/* 호버 시 화살표 */}
                  <div className="px-5 py-3 bg-slate-50 flex items-center justify-between group-hover:bg-blue-50 transition">
                    <span className="text-sm font-semibold text-slate-600 group-hover:text-blue-600">자세히 보기</span>
                    <ArrowRight size={16} className="text-slate-400 group-hover:text-blue-600 group-hover:translate-x-1 transition" />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
