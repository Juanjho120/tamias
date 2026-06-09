export type MaintenanceStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'DELETED';

export interface MaintenanceRecordSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  maintenanceCategoryId: string | null;
  maintenanceCategoryName: string | null;
  maintenanceTypeId: string | null;
  maintenanceTypeName: string | null;
  title: string;
  scheduledAt: string | null;
  performedAt: string | null;
  cost: number | null;
  status: MaintenanceStatus;
  createdAt: string;
}

export interface MaintenanceRecordFilters {
  propertyId?: string;
  status?: MaintenanceStatus | '';
  page: number;
  size: number;
  sort?: string;
}

export const MAINTENANCE_STATUSES: MaintenanceStatus[] = [
  'PENDING',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
  'DELETED'
];
