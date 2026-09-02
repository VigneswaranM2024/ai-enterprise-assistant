import { useEffect } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { authService, LoginPayload, RegisterPayload } from '../services/authService';

export function useAuth() {
  const {
    user,
    accessToken,
    isAuthenticated,
    isLoading,
    tenantSlug,
    setAuth,
    setLoading,
    logout,
    setUser,
    hasRole,
  } = useAuthStore();

  // Validate session on app initialization
  useEffect(() => {
    async function checkAuthStatus() {
      if (accessToken && !user) {
        setLoading(true);
        try {
          const profile = await authService.getCurrentUser();
          setUser(profile);
        } catch (error) {
          console.error('Failed to validate session on boot:', error);
          logout();
        } finally {
          setLoading(false);
        }
      }
    }
    checkAuthStatus();
  }, [accessToken, user, setUser, setLoading, logout]);

  const handleLogin = async (payload: LoginPayload) => {
    setLoading(true);
    try {
      const response = await authService.login(payload);
      setAuth(response.user, response.accessToken, response.refreshToken);
      return response;
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (payload: RegisterPayload) => {
    setLoading(true);
    try {
      const response = await authService.register(payload);
      setAuth(response.user, response.accessToken, response.refreshToken);
      return response;
    } finally {
      setLoading(false);
    }
  };

  return {
    user,
    accessToken,
    isAuthenticated,
    isLoading,
    tenantSlug,
    login: handleLogin,
    register: handleRegister,
    logout,
    hasRole,
    isAdmin: hasRole('ROLE_ADMIN'),
    isEmployee: hasRole('ROLE_EMPLOYEE'),
  };
}
