import type { ApiResponse, Notification } from '@/types/api';
import { apiRequest } from './core';

export const notificationApi = {
  getAll: async (memberId: number): Promise<ApiResponse<Notification[]>> => {
    const raw = await apiRequest<Notification[]>(`/api/notifications?memberId=${memberId}`);
    return { success: true, data: Array.isArray(raw) ? raw : [] };
  },

  markAsRead: async (id: number): Promise<ApiResponse<void>> => {
    const raw = await apiRequest<{ success?: boolean }>(`/api/notifications/${id}/read`, {
      method: 'POST',
    });
    return { success: raw?.success ?? true };
  },

  markAllAsRead: async (memberId: number): Promise<ApiResponse<void>> => {
    const raw = await apiRequest<{ success?: boolean }>('/api/notifications/read-all', {
      method: 'POST',
      body: JSON.stringify({ memberId }),
    });
    return { success: raw?.success ?? true };
  },
};
