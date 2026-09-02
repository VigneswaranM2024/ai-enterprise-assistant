import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Inject JWT Bearer Token & Tenant Header
apiClient.interceptors.request.use(
  (config) => {
    const { accessToken, tenantSlug } = useAuthStore.getState();

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    if (tenantSlug) {
      config.headers['X-Tenant-ID'] = tenantSlug;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Silent Token Refresh Handling on 401 Unauthorized
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Check if error is 401 Unauthorized and not already retried
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const { refreshToken, updateAccessToken, logout } = useAuthStore.getState();

      if (refreshToken) {
        try {
          // Attempt Silent Token Refresh
          const refreshResponse = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });

          const { accessToken: newAccessToken, refreshToken: newRefreshToken } = refreshResponse.data.data;
          updateAccessToken(newAccessToken, newRefreshToken);

          // Retry original request with new Access Token
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return apiClient(originalRequest);
        } catch (refreshError) {
          console.error('Silent token refresh failed, forcing logout:', refreshError);
          logout();
        }
      } else {
        logout();
      }
    }

    return Promise.reject(error);
  }
);
