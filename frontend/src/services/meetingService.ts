import { apiClient as api } from './apiClient';
import { MeetingPageResponse, MeetingResponse } from '../types/meeting.types';
import { ApiResponse } from '../types/api.types';

export const meetingService = {
  uploadTranscript: async (formData: FormData): Promise<MeetingResponse> => {
    const response = await api.post<ApiResponse<MeetingResponse>>('/meetings/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data.data;
  },

  getMeetings: async (page = 0, size = 10): Promise<MeetingPageResponse> => {
    const response = await api.get<ApiResponse<MeetingPageResponse>>('/meetings', {
      params: { page, size },
    });
    return response.data.data;
  },

  getMeeting: async (id: string): Promise<MeetingResponse> => {
    const response = await api.get<ApiResponse<MeetingResponse>>(`/meetings/${id}`);
    return response.data.data;
  },

  deleteMeeting: async (id: string): Promise<void> => {
    await api.delete(`/meetings/${id}`);
  },
};
