import { apiRequest } from './core';

export interface TicketStatus {
  remainingTickets: number;
  maxTickets: number;
  resetAt: string;
}

export const ticketApi = {
  /**
   * Fetch ticket status
   * GET /api/tickets/status
   */
  getStatus: async (): Promise<TicketStatus> => {
    return apiRequest<TicketStatus>('/api/tickets/status');
  },
};
