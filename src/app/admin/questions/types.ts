export interface QuestionAdminView {
  id: number;
  title: string;
  category: string;
  status: string;
  totalBetPool: number;
  totalVotes: number;
  expiredAt: string;
  createdAt: string;
  disputeDeadline?: string;
}

export type SortBy = 'id' | 'title' | 'category' | 'status' | 'totalBetPool' | 'expiredAt';
export type SortOrder = 'asc' | 'desc';
