// Shared TypeScript Interfaces Barrel Export
export type UserRole = 'ROLE_ADMIN' | 'ROLE_EMPLOYEE';

export interface UserProfile {
  id: string;
  email: string;
  fullName: string;
  tenantId: string;
  roles: UserRole[];
}
