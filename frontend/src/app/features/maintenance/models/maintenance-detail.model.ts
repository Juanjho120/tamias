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

export interface MaintenanceMaterialUsed {
  id: string;
  maintenanceRecordId: string;
  materialId: string | null;
  materialName: string | null;
  materialNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  notes: string | null;
}

export interface MaintenanceMaterialUsedRequest {
  materialId: string | null;
  materialNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  notes: string | null;
}

export type MaintenanceMaterialUsedUpdateRequest = MaintenanceMaterialUsedRequest;
