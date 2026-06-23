export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthOrganization {
  id: string;
  name: string;
  logoUrl: string | null;
}

export interface AuthUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  organization: AuthOrganization;
  passwordChangeRequired: boolean;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}
