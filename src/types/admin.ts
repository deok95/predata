/**
 * Admin-specific types for bulk upload and question management
 */

export interface BulkQuestionItem {
  title: string;
  category?: string;
  bettingDuration?: string;  // e.g., "24h", "3d", "60m"
  seedUsdc?: number;
  feeRate?: number;
  sponsorName?: string;
  sponsorLogo?: string;
  resolution?: {
    type: 'MANUAL' | 'AUTO';
    source?: 'FOOTBALL_API' | 'PRICE_API';
    matchId?: string;
    asset?: string;
    condition?: string;
    resolveBy?: string;
  };
}

export interface BulkUploadResult {
  total: number;
  created: number;
  failed: number;
  results: Array<{
    title: string;
    questionId?: number;
    success: boolean;
    error?: string;
  }>;
}
