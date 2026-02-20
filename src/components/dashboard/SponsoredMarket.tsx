'use client';

import Link from 'next/link';
import { useEffect, useMemo, useRef, useState } from 'react';
import { ChevronLeft, ChevronRight, ShieldCheck } from 'lucide-react';
import { useTheme } from '@/hooks/useTheme';
import type { Question } from '@/types/api';

interface SponsoredMarketProps {
  questions: Question[];
}

export default function SponsoredMarket({ questions }: SponsoredMarketProps) {
  const { isDark } = useTheme();
  const carouselRef = useRef<HTMLDivElement | null>(null);

  const slides = useMemo(() => questions.slice(0, 8), [questions]);
  const totalSlides = slides.length;
  const [currentIndex, setCurrentIndex] = useState(0);

  const scrollToIndex = (index: number) => {
    const container = carouselRef.current;
    if (!container || totalSlides === 0) return;
    const safeIndex = ((index % totalSlides) + totalSlides) % totalSlides;
    container.scrollTo({
      left: safeIndex * container.clientWidth,
      behavior: 'smooth',
    });
    setCurrentIndex(safeIndex);
  };

  useEffect(() => {
    if (totalSlides <= 1) return;
    const timer = setInterval(() => {
      scrollToIndex(currentIndex + 1);
    }, 5000);
    return () => clearInterval(timer);
  }, [currentIndex, totalSlides]);

  const handleCarouselScroll = () => {
    const container = carouselRef.current;
    if (!container || container.clientWidth === 0) return;
    const index = Math.round(container.scrollLeft / container.clientWidth);
    if (index !== currentIndex) {
      setCurrentIndex(index);
    }
  };

  if (slides.length === 0) return null;

  return (
    <div className="space-y-10">
      <div className="relative">
        <div
          ref={carouselRef}
          onScroll={handleCarouselScroll}
          className="flex overflow-x-auto snap-x snap-mandatory scroll-smooth [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          {slides.map((question) => {
            const yesPercent = question.totalBetPool > 0
              ? Math.round((question.yesBetPool / question.totalBetPool) * 100)
              : 50;

            return (
              <div key={question.id} className="w-full shrink-0 snap-start">
                <Link href={`/question/${question.id}`}>
                  <div className="bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 p-8 rounded-[2.5rem] text-white relative overflow-hidden cursor-pointer hover:shadow-2xl transition-all group h-[390px] md:h-[420px] flex flex-col">
                    <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -translate-y-1/2 translate-x-1/2 group-hover:scale-110 transition-transform" />
                    <div className="relative z-10 h-full flex flex-col">
                      <div className="flex items-center space-x-2 mb-4">
                        <span className="bg-white/20 px-3 py-1 rounded-full text-[10px] font-black uppercase backdrop-blur-sm">Featured</span>
                        <span className="text-xs opacity-80">by PRE(D)ATA</span>
                        <span className="text-xs opacity-80">• {question.status === 'VOTING' ? 'Voting Priority' : 'Betting'}</span>
                      </div>
                      <h3 className="text-3xl font-black mb-4 leading-tight line-clamp-4 min-h-[168px]">{question.title}</h3>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                        <div><p className="text-xs opacity-70 mb-1">Yes Probability</p><p className="font-black text-lg">{yesPercent}%</p></div>
                        <div><p className="text-xs opacity-70 mb-1">Category</p><p className="font-black text-lg">{question.category || '-'}</p></div>
                        <div><p className="text-xs opacity-70 mb-1">Total Pool</p><p className="font-black text-lg">{'$'}{question.totalBetPool.toLocaleString()}</p></div>
                        <div><p className="text-xs opacity-70 mb-1">Ends</p><p className="font-black text-lg">{(() => { if (!question.expiredAt) return 'TBD'; const d = new Date(question.expiredAt); return isNaN(d.getTime()) ? 'TBD' : d.toLocaleDateString('en-US'); })()}</p></div>
                      </div>
                      <span className="bg-white text-indigo-600 px-8 py-4 rounded-2xl font-black text-sm hover:shadow-2xl transition-all active:scale-95 inline-block mt-auto">View Details & Vote →</span>
                    </div>
                  </div>
                </Link>
              </div>
            );
          })}
        </div>

        {totalSlides > 1 && (
          <>
            <button
              type="button"
              aria-label="Previous featured market"
              onClick={() => scrollToIndex(currentIndex - 1)}
              className="absolute left-3 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full bg-black/30 hover:bg-black/45 text-white flex items-center justify-center backdrop-blur"
            >
              <ChevronLeft size={18} />
            </button>
            <button
              type="button"
              aria-label="Next featured market"
              onClick={() => scrollToIndex(currentIndex + 1)}
              className="absolute right-3 top-1/2 -translate-y-1/2 h-10 w-10 rounded-full bg-black/30 hover:bg-black/45 text-white flex items-center justify-center backdrop-blur"
            >
              <ChevronRight size={18} />
            </button>

            <div className="absolute bottom-4 right-6 flex items-center gap-2">
              {slides.map((_, index) => (
                <button
                  key={index}
                  type="button"
                  aria-label={`Go to featured market ${index + 1}`}
                  onClick={() => scrollToIndex(index)}
                  className={`h-2.5 rounded-full transition-all ${
                    index === currentIndex ? 'w-6 bg-white' : 'w-2.5 bg-white/45 hover:bg-white/70'
                  }`}
                />
              ))}
            </div>
          </>
        )}
      </div>

      <div className={`rounded-3xl p-8 flex flex-col md:flex-row items-center justify-between ${isDark ? 'bg-indigo-950/20 border border-indigo-900/30' : 'bg-slate-900'}`}>
        <div className="flex items-center space-x-6">
          <div className="w-14 h-14 bg-indigo-600 rounded-2xl flex items-center justify-center text-white"><ShieldCheck size={28} /></div>
          <div>
            <h4 className="text-white font-bold">Security & Verification Complete</h4>
            <p className="text-slate-400 text-sm">All data is verified in real-time by oracle nodes.</p>
          </div>
        </div>
        <div className="flex items-center space-x-2 mt-4 md:mt-0">
          <span className="text-emerald-500 font-bold text-xs uppercase tracking-widest">System Live</span>
          <div className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
        </div>
      </div>
    </div>
  );
}
