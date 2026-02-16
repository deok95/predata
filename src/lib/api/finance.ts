import type {
  ApiResponse,
  FaucetClaimResponse,
  FaucetStatus,
  PaymentVerifyResponse,
  TransactionHistoryPage,
  VotingPassPurchaseResponse,
  WithdrawResponse,
} from '@/types/api';
import { apiRequest } from './core';

export const faucetApi = {
  claim: (memberId: number) =>
    apiRequest<FaucetClaimResponse>(`/api/faucet/claim/${memberId}`, {
      method: 'POST',
    }),

  getStatus: (memberId: number) =>
    apiRequest<FaucetStatus>(`/api/faucet/status/${memberId}`),
};

export const votingPassApi = {
  purchase: async (memberId: number): Promise<ApiResponse<VotingPassPurchaseResponse>> => {
    const raw = await apiRequest<VotingPassPurchaseResponse>('/api/voting-pass/purchase', {
      method: 'POST',
      body: JSON.stringify({ memberId }),
    });
    return { success: true, data: raw };
  },
};

export const paymentApi = {
  verifyDeposit: async (memberId: number, txHash: string, amount: number, fromAddress?: string): Promise<PaymentVerifyResponse> =>
    apiRequest<PaymentVerifyResponse>('/api/payments/verify-deposit', {
      method: 'POST',
      body: JSON.stringify({ memberId, txHash, amount, fromAddress }),
    }),

  withdraw: async (memberId: number, amount: number, walletAddress: string): Promise<WithdrawResponse> =>
    apiRequest<WithdrawResponse>('/api/payments/withdraw', {
      method: 'POST',
      body: JSON.stringify({ memberId, amount, walletAddress }),
    }),
};

export const transactionApi = {
  getMyTransactions: async (
    memberId: number,
    type?: string,
    page: number = 0,
    size: number = 20
  ): Promise<TransactionHistoryPage> => {
    const params = new URLSearchParams({
      memberId: memberId.toString(),
      page: page.toString(),
      size: size.toString(),
    });

    if (type) {
      params.set('type', type);
    }

    return apiRequest<TransactionHistoryPage>(`/api/transactions/my?${params.toString()}`);
  },
};
