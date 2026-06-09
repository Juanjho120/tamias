export type PropertyStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface PropertySummary {
  id: string;
  name: string;
  address: string | null;
  description: string | null;
  status: PropertyStatus;
  createdAt: string;
}

export interface Property {
  id: string;
  name: string;
  address: string | null;
  description: string | null;
  status: PropertyStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PropertyRequest {
  name: string;
  address: string | null;
  description: string | null;
  status: PropertyStatus;
}

export interface PropertyFilters {
  status?: PropertyStatus | '';
  search?: string;
  page: number;
  size: number;
  sort?: string;
}

export const PROPERTY_STATUSES: PropertyStatus[] = ['ACTIVE', 'INACTIVE', 'DELETED'];
