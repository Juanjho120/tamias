export type OrganizationStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface Organization {
  id: string;
  name: string;
  description: string | null;
  status: OrganizationStatus;
  logoUrl: string | null;
  logoOriginalFilename: string | null;
  logoContentType: string | null;
  logoSizeBytes: number | null;
  logoUpdatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface OrganizationCreateRequest {
  name: string;
  description: string | null;
}

export interface OrganizationUpdateRequest {
  name: string;
  description: string | null;
}

export interface OrganizationFilters {
  page: number;
  size: number;
  sort?: string;
}
