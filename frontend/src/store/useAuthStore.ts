import { create } from 'zustand';
import { UserProfile, UserRole } from '../types/auth.types';

interface AuthState {
  user: UserProfile | null;
  accessToken: string | null;
  refreshToken: string | null;
  tenantSlug: string;
  isAuthenticated: boolean;
  isLoading: boolean;

  setAuth: (user: UserProfile, accessToken: string, refreshToken: string) => void;
  updateAccessToken: (accessToken: string, refreshToken?: string) => void;
  setTenantSlug: (tenantSlug: string) => void;
  setUser: (user: UserProfile) => void;
  setLoading: (isLoading: boolean) => void;
  logout: () => void;
  hasRole: (role: UserRole) => boolean;
}

const TOKEN_KEY = 'ai_assistant_access_token';
const REFRESH_TOKEN_KEY = 'ai_assistant_refresh_token';
const USER_KEY = 'ai_assistant_user';
const TENANT_KEY = 'ai_assistant_tenant_slug';

const safeJsonParse = <T>(value: string | null, fallback: T): T => {
  if (!value || value === 'undefined' || value === 'null') {
    return fallback;
  }
  try {
    return JSON.parse(value) as T;
  } catch (error) {
    console.error('Failed to parse JSON from localStorage:', error);
    return fallback;
  }
};

export const useAuthStore = create<AuthState>((set, get) => ({
  user: safeJsonParse<UserProfile | null>(localStorage.getItem(USER_KEY), null),
  accessToken: localStorage.getItem(TOKEN_KEY),
  refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
  tenantSlug: localStorage.getItem(TENANT_KEY) || 'acme-corp',
  isAuthenticated: !!localStorage.getItem(TOKEN_KEY),
  isLoading: false,

  setAuth: (user, accessToken, refreshToken) => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(TENANT_KEY, user.tenantSlug);

    set({
      user,
      accessToken,
      refreshToken,
      tenantSlug: user.tenantSlug,
      isAuthenticated: true,
      isLoading: false,
    });
  },

  updateAccessToken: (accessToken, refreshToken) => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    if (refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    }
    set((state) => ({
      accessToken,
      refreshToken: refreshToken || state.refreshToken,
      isAuthenticated: true,
    }));
  },

  setTenantSlug: (tenantSlug) => {
    localStorage.setItem(TENANT_KEY, tenantSlug);
    set({ tenantSlug });
  },

  setUser: (user) => {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    set({ user });
  },

  setLoading: (isLoading) => set({ isLoading }),

  logout: () => {
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  hasRole: (role) => {
    const { user } = get();
    return user ? user.roles.includes(role) : false;
  },
}));
