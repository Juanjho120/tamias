export type CatalogStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';
export type InventoryItemType = 'MATERIAL' | 'SUPPLY' | 'AMENITY' | 'CLEANING_SUPPLY' | 'TOOL' | 'OTHER';
export type CatalogFieldType = 'text' | 'email' | 'url' | 'textarea' | 'select' | 'checkbox';

export interface CatalogSelectOption {
  value: string;
  labelKey: string;
}

export interface CatalogItem {
  id: string;
  name?: string;
  fullName?: string;
  description?: string | null;
  country?: string | null;
  brandId?: string | null;
  brandName?: string | null;
  unit?: string | null;
  itemType?: InventoryItemType | null;
  internalCode?: string | null;
  barcode?: string | null;
  availableForMaintenance?: boolean | null;
  availableForReservations?: boolean | null;
  availableForPurchases?: boolean | null;
  phone?: string | null;
  email?: string | null;
  website?: string | null;
  notes?: string | null;
  status: CatalogStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CatalogRequest {
  [key: string]: string | boolean | CatalogStatus | InventoryItemType | null;
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
  options?: CatalogSelectOption[];
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
export const INVENTORY_ITEM_TYPES: InventoryItemType[] = [
  'MATERIAL',
  'SUPPLY',
  'AMENITY',
  'CLEANING_SUPPLY',
  'TOOL',
  'OTHER'
];
