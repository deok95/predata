'use client'

import { useState } from 'react'
import { CheckCircle, AlertCircle, ExternalLink, Loader } from 'lucide-react'
import { API_BASE_URL } from '@/lib/api'

const BASESCAN_URL = process.env.NEXT_PUBLIC_BASESCAN_URL || 'https://sepolia.basescan.org'

export default function BlockchainVerification({ questionId }: { questionId: number }) {
  const [onChainData, setOnChainData] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleVerify = async () => {
    setLoading(true)
    setError('')

    try {
      // 백엔드에서 온체인 데이터 조회
      const response = await fetch(`${API_BASE_URL}/blockchain/question/${questionId}`)
      
      if (response.ok) {
        const data = await response.json()
        setOnChainData(data)
      } else {
        setError('온체인 데이터를 가져올 수 없습니다.')
      }
    } catch (err) {
      setError('검증 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="border border-green-500/30 rounded-lg p-4 mt-4 bg-green-900/10">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-bold text-green-400 flex items-center gap-2">
          <CheckCircle size={20} />
          블록체인 검증
        </h3>
        <span className="text-xs text-green-500">Base L2</span>
      </div>

      {!onChainData && (
        <button
          onClick={handleVerify}
          disabled={loading}
          className="w-full px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <Loader className="animate-spin" size={16} />
              검증 중...
            </>
          ) : (
            '온체인 데이터 확인'
          )}
        </button>
      )}

      {error && (
        <div className="flex items-center gap-2 text-red-400 text-sm mt-2">
          <AlertCircle size={16} />
          {error}
        </div>
      )}

      {onChainData && (
        <div className="space-y-3">
          <div className="bg-gray-800/50 p-3 rounded-lg space-y-2 text-sm font-mono">
            <div className="flex justify-between">
              <span className="text-gray-400">Question ID:</span>
              <span className="text-white font-bold">{onChainData.questionId}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Total Pool:</span>
              <span className="text-green-400 font-bold">
                {onChainData.totalBetPool?.toLocaleString()}P
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">YES Pool:</span>
              <span className="text-blue-400">{onChainData.yesBetPool?.toLocaleString()}P</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">NO Pool:</span>
              <span className="text-red-400">{onChainData.noBetPool?.toLocaleString()}P</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">Settled:</span>
              <span className={onChainData.settled ? 'text-green-400' : 'text-yellow-400'}>
                {onChainData.settled ? '✅ Yes' : '⏳ Pending'}
              </span>
            </div>
          </div>

          <div className="bg-blue-900/20 p-3 rounded-lg">
            <p className="text-green-400 text-xs mb-2 flex items-center gap-1">
              <CheckCircle size={14} />
              이 데이터는 Base L2 블록체인에 영구 기록되어 있습니다
            </p>
            <a
              href={`${BASESCAN_URL}/address/${process.env.NEXT_PUBLIC_CONTRACT_ADDRESS}`}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-400 hover:text-blue-300 text-xs flex items-center gap-1"
            >
              Basescan에서 확인하기
              <ExternalLink size={12} />
            </a>
          </div>

          <button
            onClick={handleVerify}
            disabled={loading}
            className="w-full px-3 py-1.5 bg-gray-700 text-gray-300 text-sm rounded hover:bg-gray-600 transition"
          >
            새로고침
          </button>
        </div>
      )}

      <p className="text-xs text-gray-500 mt-3 text-center">
        💎 온체인 기록은 누구나 검증할 수 있지만, 개인 페르소나는 비공개입니다
      </p>
    </div>
  )
}
