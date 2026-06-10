export type PurchaseListStatus = 'OPEN' | 'PARTIALLY_PURCHASED' | 'COMPLETED' | 'CANCELLED' | 'DELETED';

export interface PurchaseItemRequest {
  inventoryItemId: string | null;
  brandId: string | null;
  itemNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  estimatedPrice: number | null;
  purchased: boolean | null;
  notes: string | null;
}

export interface PurchaseItemUpdateRequest extends PurchaseItemRequest {
}

export interface PurchaseItemPurchasedRequest {
  purchased: boolean;
}

export interface PurchaseItem {
  id: string;
  inventoryItemId: string | null;
  inventoryItemName: string | null;
  materialId?: string | null;
  materialName?: string | null;
  brandId: string | null;
  brandName: string | null;
  itemNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  estimatedPrice: number | null;
  purchased: boolean | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PurchaseListSummary {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  cityId: string | null;
  cityName: string | null;
  supplierId: string | null;
  supplierName: string | null;
  purchaseDate: string;
  status: PurchaseListStatus;
  totalItems: number;
  purchasedItems: number;
  estimatedTotal: number | null;
  createdAt: string;
}

export interface PurchaseList {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  cityId: string | null;
  cityName: string | null;
  supplierId: string | null;
  supplierName: string | null;
  purchaseDate: string;
  notes: string | null;
  status: PurchaseListStatus;
  items: PurchaseItem[];
  estimatedTotal: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface PurchaseListRequest {
  propertyId: string | null;
  cityId: string | null;
  supplierId: string | null;
  purchaseDate: string;
  notes: string | null;
  status: PurchaseListStatus;
  items: PurchaseItemRequest[];
}

export interface PurchaseListFilters {
  propertyId?: string;
  supplierId?: string;
  cityId?: string;
  status?: PurchaseListStatus | '';
  page: number;
  size: number;
  sort?: string;
}

export const PURCHASE_LIST_STATUSES: PurchaseListStatus[] = [
  'OPEN',
  'PARTIALLY_PURCHASED',
  'COMPLETED',
  'CANCELLED',
  'DELETED'
];
