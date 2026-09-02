import { apiClient } from './apiClient';
import { DocumentItem } from '../types/document.types';
import { PaginatedResponse } from '../types/api.types';

export const documentService = {
  async uploadDocument(formData: FormData): Promise<DocumentItem> {
    const response = await apiClient.post<DocumentItem>('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  async getDocuments(query?: string, category?: string, page = 0, size = 20): Promise<PaginatedResponse<DocumentItem>> {
    const response = await apiClient.get<PaginatedResponse<DocumentItem>>('/documents', {
      params: { query, category, page, size, sortBy: 'createdAt', sortDir: 'desc' },
    });
    return response.data;
  },

  async getDocumentById(id: string): Promise<DocumentItem> {
    const response = await apiClient.get<DocumentItem>(`/documents/${id}`);
    return response.data;
  },

  async downloadDocument(id: string, title: string): Promise<void> {
    const response = await apiClient.get(`/documents/${id}/download`, {
      responseType: 'blob',
    });

    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', title);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },

  async deleteDocument(id: string): Promise<void> {
    await apiClient.delete(`/documents/${id}`);
  },
};
