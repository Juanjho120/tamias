export type AuthRoleCode =
  | 'SUPER_ADMIN'
  | 'ADMINISTRATOR'
  | 'PROPERTY_MANAGER'
  | 'MAINTENANCE_STAFF'
  | 'READ_ONLY';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthOrganization {
  id: string;
  name: string;
  logoUrl: string | null;
}

export interface AuthOrganizationOption {
  id: string;
  name: string;
  role: AuthRoleCode;
  logoUrl: string | null;
  current: boolean;
}

export interface SwitchOrganizationRequest {
  organizationId: string;
}

export interface AuthUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: AuthRoleCode;
  organization: AuthOrganization;
  passwordChangeRequired: boolean;
}

export interface LoginResponse {
  accessToken: string | null;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}
