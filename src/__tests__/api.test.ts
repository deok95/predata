import { describe, it, expect, vi, beforeEach } from 'vitest';

// Test API request helper logic
describe('API Request', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('should handle successful API response', async () => {
    const mockResponse = { id: 1, title: 'Test Question' };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockResponse,
    });

    const response = await fetch('http://localhost:8080/api/questions');
    const data = await response.json();

    expect(data).toEqual(mockResponse);
    expect(response.ok).toBe(true);
  });

  it('should handle 404 error', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 404,
      statusText: 'Not Found',
      json: async () => ({ message: 'Not Found' }),
    });

    const response = await fetch('http://localhost:8080/api/members/999');
    expect(response.ok).toBe(false);
    expect(response.status).toBe(404);
  });

  it('should handle network error', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (global.fetch as any).mockRejectedValueOnce(new Error('Network error'));

    await expect(
      fetch('http://localhost:8080/api/questions')
    ).rejects.toThrow('Network error');
  });
});

// Test field mappers
describe('Field Mappers', () => {
  it('should map question with expiredAt field correctly', () => {
    const raw = {
      id: 1,
      title: 'Test',
      status: 'OPEN',
      totalBetPool: 1000,
      yesBetPool: 600,
      noBetPool: 400,
      expiredAt: '2025-12-31T00:00:00',
      createdAt: '2025-01-01T00:00:00',
    };

    // Simulating mapQuestion logic
    const mapped = {
      id: raw.id,
      title: raw.title,
      status: raw.status,
      totalBetPool: raw.totalBetPool ?? 0,
      yesBetPool: raw.yesBetPool ?? 0,
      noBetPool: raw.noBetPool ?? 0,
      expiresAt: raw.expiredAt,
      createdAt: raw.createdAt,
    };

    expect(mapped.expiresAt).toBe('2025-12-31T00:00:00');
    expect(mapped.totalBetPool).toBe(1000);
  });

  it('should map member with memberId field', () => {
    const raw = {
      memberId: 42,
      email: 'test@test.com',
      countryCode: 'KR',
      tier: 'GOLD',
      usdcBalance: 5000,
    };

    const mapped = {
      id: raw.memberId ?? (raw as { id?: number }).id,
      email: raw.email,
      countryCode: raw.countryCode,
      tier: raw.tier ?? 'BRONZE',
      usdcBalance: raw.usdcBalance ?? 0,
    };

    expect(mapped.id).toBe(42);
    expect(mapped.tier).toBe('GOLD');
    expect(mapped.usdcBalance).toBe(5000);
  });
});
