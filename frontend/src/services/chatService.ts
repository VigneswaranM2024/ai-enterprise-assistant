import { apiClient } from './apiClient';
import { ApiResponse } from '../types/api.types';
import {
  ChatSession,
  ChatSessionDetail,
  SendMessageResponse,
} from '../types/chat.types';

export const chatService = {
  async getSessions(): Promise<ChatSession[]> {
    const response = await apiClient.get<ApiResponse<ChatSession[]>>('/chat/sessions');
    return response.data.data;
  },

  async createSession(title?: string): Promise<ChatSession> {
    const response = await apiClient.post<ApiResponse<ChatSession>>('/chat/sessions', { title });
    return response.data.data;
  },

  async getSessionDetails(id: string): Promise<ChatSessionDetail> {
    const response = await apiClient.get<ApiResponse<ChatSessionDetail>>(`/chat/sessions/${id}`);
    return response.data.data;
  },

  async deleteSession(id: string): Promise<void> {
    await apiClient.delete<ApiResponse<void>>(`/chat/sessions/${id}`);
  },

  async sendMessage(sessionId: string, message: string): Promise<SendMessageResponse> {
    const response = await apiClient.post<ApiResponse<SendMessageResponse>>(
      `/chat/sessions/${sessionId}/messages`,
      { message }
    );
    return response.data.data;
  },
};
