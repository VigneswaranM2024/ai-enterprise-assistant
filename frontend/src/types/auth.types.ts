export type UserRole = 'ROLE_ADMIN' | 'ROLE_EMPLOYEE';

export interface UserProfile {
  id: string;
  tenantId: string;
  tenantSlug: string;
  email: string;
  fullName: string;
  jobTitle?: string;
  securityClassification: string;
  roles: UserRole[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserProfile;
}
