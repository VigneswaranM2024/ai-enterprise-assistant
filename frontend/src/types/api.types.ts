export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  correlationId?: string;
  timestamp?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
