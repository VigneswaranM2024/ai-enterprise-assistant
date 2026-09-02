export interface MeetingResponse {
  id: string;
  title: string;
  meetingDate: string;
  participants: string;
  summary: string;
  decisions: string;
  actionItems: string;
  risks: string;
  status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
}

export interface MeetingPageResponse {
  content: MeetingResponse[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
}
