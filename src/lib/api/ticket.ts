import { apiRequest } from './core';

export interface TicketStatus {
  remainingTickets: number;
  maxTickets: number;
  resetAt: string;
}

export const ticketApi = {
  /**
   * 티켓 상태 조회
   * GET /api/tickets/status
   */
  getStatus: async (): Promise<TicketStatus> => {
    return apiRequest<TicketStatus>('/api/tickets/status');
  },
};
