export type RoleCode = 'ADMINISTRATOR' | 'PROPERTY_MANAGER' | 'MAINTENANCE_STAFF' | 'READ_ONLY';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'INVITED' | 'LOCKED' | 'DELETED';

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: RoleCode;
  status: UserStatus;
  createdAt: string;
}

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: RoleCode;
  status: UserStatus;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface UserCreateRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: RoleCode;
}

export interface UserUpdateRequest {
  firstName: string;
  lastName: string;
  email: string;
  role: RoleCode;
  status: UserStatus;
}

export interface UserFilters {
  page: number;
  size: number;
  sort?: string;
}

export const ROLE_CODES: RoleCode[] = [
  'ADMINISTRATOR',
  'PROPERTY_MANAGER',
  'MAINTENANCE_STAFF',
  'READ_ONLY'
];

export const USER_STATUSES: UserStatus[] = [
  'ACTIVE',
  'INACTIVE',
  'INVITED',
  'LOCKED'
];
