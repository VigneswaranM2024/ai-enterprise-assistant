import { apiClient } from './apiClient';
import { AuthResponse, UserProfile } from '../types/auth.types';
import { ApiResponse } from '../types/api.types';

export interface LoginPayload {
  email: string;
  password: string;
  tenantSlug: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  jobTitle?: string;
  tenantSlug: string;
}

export const authService = {
  async login(payload: LoginPayload): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', payload);
    return response.data.data;
  },

  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', payload);
    return response.data.data;
  },

  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken });
    return response.data.data;
  },

  async getCurrentUser(): Promise<UserProfile> {
    const response = await apiClient.get<ApiResponse<UserProfile>>('/auth/me');
    return response.data.data;
  },
};
