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
  itemNameSnapshot: string | null;
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

