export interface MaintenanceRecordPerson {
  id: string;
  maintenanceRecordId: string;
  maintenancePersonId: string;
  fullName: string;
  phone: string | null;
  email: string | null;
  notes: string | null;
}

export interface MaintenanceRecordPersonRequest {
  maintenancePersonId: string;
}

export interface MaintenanceRecordItem {
  id: string;
  maintenanceRecordId: string;
  inventoryItemId: string | null;
  inventoryItemName: string | null;
  inventoryItemBrandId?: string | null;
  inventoryItemBrandName?: string | null;
  materialId?: string | null;
  materialName?: string | null;
  materialBrandId?: string | null;
  materialBrandName?: string | null;
  itemNameSnapshot: string | null;
  materialNameSnapshot?: string | null;
  quantity: number | null;
  unit: string | null;
  notes: string | null;
}

export interface MaintenanceRecordItemRequest {
  inventoryItemId: string | null;
  itemNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  notes: string | null;
}

export type MaintenanceRecordItemUpdateRequest = MaintenanceRecordItemRequest;

export interface MaintenanceRecordServicedItem {
  id: string;
  maintenanceRecordId: string;
  inventoryItemId: string | null;
  inventoryItemName: string | null;
  inventoryItemBrandId: string | null;
  inventoryItemBrandName: string | null;
  itemNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  notes: string | null;
}

export interface MaintenanceRecordServicedItemRequest {
  inventoryItemId: string | null;
  itemNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  notes: string | null;
}

export type MaintenanceRecordServicedItemUpdateRequest = MaintenanceRecordServicedItemRequest;
