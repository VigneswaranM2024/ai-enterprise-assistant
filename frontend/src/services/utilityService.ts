import { apiClient as api } from './apiClient';
import { ApiResponse } from '../types/api.types';

export interface UtilityResponse {
  result: string;
}

export interface EmailGenerationRequest {
  context: string;
  tone?: string;
  targetAudience?: string;
}

export interface SqlGenerationRequest {
  prompt: string;
  schemaContext?: string;
  dialect?: string;
}

export interface CodeGenerationRequest {
  prompt: string;
  language?: string;
}

export const utilityService = {
  generateEmail: async (request: EmailGenerationRequest): Promise<UtilityResponse> => {
    const response = await api.post<ApiResponse<UtilityResponse>>('/utilities/email', request);
    return response.data.data;
  },

  generateSql: async (request: SqlGenerationRequest): Promise<UtilityResponse> => {
    const response = await api.post<ApiResponse<UtilityResponse>>('/utilities/sql', request);
    return response.data.data;
  },

  generateCode: async (request: CodeGenerationRequest): Promise<UtilityResponse> => {
    const response = await api.post<ApiResponse<UtilityResponse>>('/utilities/code', request);
    return response.data.data;
  }
};
