export type CatalogStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export type CatalogFieldType = 'text' | 'email' | 'url' | 'textarea';

export interface CatalogItem {
  id: string;
  name?: string;
  fullName?: string;
  description?: string | null;
  country?: string | null;
  unit?: string | null;
  phone?: string | null;
  email?: string | null;
  website?: string | null;
  notes?: string | null;
  status: CatalogStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CatalogRequest {
  [key: string]: string | CatalogStatus | null;
}

export interface CatalogFilters {
  status?: CatalogStatus | '';
  page: number;
  size: number;
  sort?: string;
}

export interface CatalogFieldConfig {
  key: string;
  labelKey: string;
  type: CatalogFieldType;
  required?: boolean;
  maxLength?: number;
  rows?: number;
  table?: boolean;
  primary?: boolean;
}

export interface CatalogConfig {
  key: string;
  titleKey: string;
  descriptionKey: string;
  endpoint: string;
  fields: CatalogFieldConfig[];
  defaultSort?: string;
}

export const CATALOG_STATUSES: CatalogStatus[] = ['ACTIVE', 'INACTIVE', 'DELETED'];
