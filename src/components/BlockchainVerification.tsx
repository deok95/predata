'use client'

import { useState } from 'react'
import { CheckCircle, AlertCircle, ExternalLink, Loader } from 'lucide-react'
import { apiRequest, unwrapApiEnvelope } from '@/lib/api'

const BASESCAN_URL = process.env.NEXT_PUBLIC_BASESCAN_URL || 'https://sepolia.basescan.org'

export default function BlockchainVerification({ questionId }: { questionId: number }) {
  const [onChainData, setOnChainData] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleVerify = async () => {
    setLoading(true)
    setError('')

    try {
      // Fetch on-chain data from backend
      const raw = await apiRequest<Record<string, unknown>>(`/api/blockchain/question/${questionId}`)
      setOnChainData(unwrapApiEnvelope(raw))
    } catch (err) {
      setError('Unable to fetch on-chain data.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="border border-green-500/30 rounded-lg p-4 mt-4 bg-green-900/10">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-bold text-green-400 flex items-center gap-2">
          <CheckCircle size={20} />
          Blockchain Verification
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
              Verifying...
            </>
          ) : (
            'Check On-chain Data'
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
                {'$'}{onChainData.totalBetPool?.toLocaleString()}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">YES Pool:</span>
              <span className="text-blue-400">{'$'}{onChainData.yesBetPool?.toLocaleString()}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-400">NO Pool:</span>
              <span className="text-red-400">{'$'}{onChainData.noBetPool?.toLocaleString()}</span>
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
              This data is permanently recorded on the Base L2 blockchain
            </p>
            <a
              href={`${BASESCAN_URL}/address/${process.env.NEXT_PUBLIC_CONTRACT_ADDRESS}`}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-400 hover:text-blue-300 text-xs flex items-center gap-1"
            >
              View on Basescan
              <ExternalLink size={12} />
            </a>
          </div>

          <button
            onClick={handleVerify}
            disabled={loading}
            className="w-full px-3 py-1.5 bg-gray-700 text-gray-300 text-sm rounded hover:bg-gray-600 transition"
          >
            Refresh
          </button>
        </div>
      )}

      <p className="text-xs text-gray-500 mt-3 text-center">
        💎 On-chain records are publicly verifiable, but personal personas remain private
      </p>
    </div>
  )
}
