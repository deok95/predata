import { ApiEnvelope, ApiErrorDetail, DraftOpenResponse, DraftSubmitRequest, OpenPositionItem, PortfolioSummary, SettlementHistoryItem, SwapRequest, VoteRequest, VoteStatusResponse } from './predata-api-types';
import { PREdataEndpoints } from './predata-endpoints';

export class PredataApiClient {
  constructor(private readonly baseUrl: string, private readonly getToken?: () => string | null) {}

  private async request<T>(path: string, init: RequestInit = {}, authRequired = true): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(init.headers as Record<string, string> | undefined),
    };

    if (authRequired && this.getToken) {
      const token = this.getToken();
      if (token) headers.Authorization = `Bearer ${token}`;
    }

    const res = await fetch(`${this.baseUrl}${path}`, { ...init, headers });
    const json = (await res.json().catch(() => null)) as ApiEnvelope<T> | ApiErrorDetail | null;

    if (!res.ok) {
      const error = (json as ApiErrorDetail) || { code: 'HTTP_ERROR', message: `HTTP ${res.status}`, status: res.status };
      throw error;
    }

    if (json && 'success' in json) {
      if (!json.success) throw json.error || { code: 'API_ERROR', message: 'Unknown API error', status: 500 };
      return json.data as T;
    }

    return json as T;
  }

  getQuestions(params: { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc' } = {}) {
    const qs = new URLSearchParams();
    if (params.page != null) qs.set('page', String(params.page));
    if (params.size != null) qs.set('size', String(params.size));
    if (params.sortBy) qs.set('sortBy', params.sortBy);
    if (params.sortDir) qs.set('sortDir', params.sortDir);
    const query = qs.toString();
    return this.request<any[]>(`${PREdataEndpoints.question.list}${query ? `?${query}` : ''}`, { method: 'GET' }, false);
  }

  getQuestionDetail(id: number | string) {
    return this.request<any>(PREdataEndpoints.question.detail(id), { method: 'GET' }, false);
  }

  getVoteStatus(questionId: number | string) {
    return this.request<VoteStatusResponse>(PREdataEndpoints.voting.status(questionId), { method: 'GET' }, false);
  }

  postVote(body: VoteRequest) {
    return this.request<any>(PREdataEndpoints.voting.vote, { method: 'POST', body: JSON.stringify(body) }, true);
  }

  postSwap(body: SwapRequest) {
    return this.request<any>(PREdataEndpoints.market.swap, { method: 'POST', body: JSON.stringify(body) }, true);
  }

  getPortfolioSummary() {
    return this.request<PortfolioSummary>(PREdataEndpoints.portfolio.summary, { method: 'GET' }, true);
  }

  getPortfolioPositions(params: { page?: number; size?: number; sortBy?: string; sortDir?: 'asc' | 'desc' } = {}) {
    const qs = new URLSearchParams();
    if (params.page != null) qs.set('page', String(params.page));
    if (params.size != null) qs.set('size', String(params.size));
    if (params.sortBy) qs.set('sortBy', params.sortBy);
    if (params.sortDir) qs.set('sortDir', params.sortDir);
    const query = qs.toString();
    return this.request<OpenPositionItem[]>(`${PREdataEndpoints.portfolio.positions}${query ? `?${query}` : ''}`, { method: 'GET' }, true);
  }

  getMySettlementHistory() {
    return this.request<SettlementHistoryItem[]>(PREdataEndpoints.settlement.myHistory, { method: 'GET' }, true);
  }

  openQuestionDraft() {
    return this.request<DraftOpenResponse>(PREdataEndpoints.question.draftOpen, { method: 'POST' }, true);
  }

  submitQuestionDraft(draftId: string, idempotencyKey: string, body: DraftSubmitRequest) {
    return this.request<any>(PREdataEndpoints.question.draftSubmit(draftId), {
      method: 'POST',
      headers: { 'Idempotency-Key': idempotencyKey },
      body: JSON.stringify(body),
    }, true);
  }

  cancelQuestionDraft(draftId: string) {
    return this.request<void>(PREdataEndpoints.question.draftCancel(draftId), { method: 'POST' }, true);
  }
}
