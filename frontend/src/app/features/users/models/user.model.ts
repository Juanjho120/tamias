export type RoleCode = 'SUPER_ADMIN' | 'ADMINISTRATOR' | 'PROPERTY_MANAGER' | 'MAINTENANCE_STAFF' | 'READ_ONLY';

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'INVITED' | 'LOCKED' | 'DELETED';

export type UserOrganizationMembershipStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

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

export interface UserOrganizationMembership {
  organizationId: string;
  organizationName: string;
  organizationLogoUrl: string | null;
  role: RoleCode;
  status: UserOrganizationMembershipStatus;
  createdAt: string;
  updatedAt: string;
}

export interface UserOrganizationMembershipCreateRequest {
  organizationId: string;
  role: RoleCode;
}

export interface UserOrganizationMembershipUpdateRequest {
  role: RoleCode;
  status: UserOrganizationMembershipStatus;
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

export const ROLE_CODES_WITH_SUPER_ADMIN: RoleCode[] = [
  'SUPER_ADMIN',
  ...ROLE_CODES
];

export const USER_STATUSES: UserStatus[] = [
  'ACTIVE',
  'INACTIVE',
  'INVITED',
  'LOCKED'
];

export const USER_ORGANIZATION_MEMBERSHIP_STATUSES: UserOrganizationMembershipStatus[] = [
  'ACTIVE',
  'INACTIVE'
];
