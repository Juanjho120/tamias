export type ScheduledMaintenanceStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'DELETED';

export type ScheduledMaintenanceFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface ScheduledMaintenanceSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  maintenanceCategoryId: string | null;
  maintenanceCategoryName: string | null;
  maintenanceTypeId: string | null;
  maintenanceTypeName: string | null;
  title: string;
  frequency: ScheduledMaintenanceFrequency;
  intervalValue: number;
  nextDueDate: string | null;
  estimatedCost: number | null;
  status: ScheduledMaintenanceStatus;
  createdAt: string;
}

export interface ScheduledMaintenance {
  id: string;
  propertyId: string;
  propertyName: string;
  maintenanceCategoryId: string | null;
  maintenanceCategoryName: string | null;
  maintenanceTypeId: string | null;
  maintenanceTypeName: string | null;
  maintenancePersonId: string | null;
  maintenancePersonName: string | null;
  title: string;
  description: string | null;
  frequency: ScheduledMaintenanceFrequency;
  intervalValue: number;
  startDate: string;
  endDate: string | null;
  nextDueDate: string | null;
  lastGeneratedAt: string | null;
  estimatedCost: number | null;
  status: ScheduledMaintenanceStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ScheduledMaintenanceRequest {
  propertyId: string;
  maintenanceCategoryId: string | null;
  maintenanceTypeId: string | null;
  maintenancePersonId: string | null;
  title: string;
  description: string | null;
  frequency: ScheduledMaintenanceFrequency;
  intervalValue: number;
  startDate: string;
  endDate: string | null;
  nextDueDate: string | null;
  estimatedCost: number | null;
  status: ScheduledMaintenanceStatus;
}

export interface ScheduledMaintenanceFilters {
  propertyId?: string;
  status?: ScheduledMaintenanceStatus | '';
  page: number;
  size: number;
  sort?: string;
}

export interface ScheduledMaintenanceRescheduleRequest {
  nextDueDate: string;
  reason: string | null;
}

export interface ScheduledMaintenanceStatusChangeRequest {
  reason: string | null;
}

export interface ScheduledMaintenanceHistory {
  id: string;
  scheduledMaintenanceId: string;
  previousStatus: ScheduledMaintenanceStatus | null;
  newStatus: ScheduledMaintenanceStatus | null;
  previousPlannedDate: string | null;
  newPlannedDate: string | null;
  previousPlannedTime: string | null;
  newPlannedTime: string | null;
  reason: string | null;
  changedBy: string | null;
  changedByName: string | null;
  changedAt: string;
}

export const SCHEDULED_MAINTENANCE_STATUSES: ScheduledMaintenanceStatus[] = [
  'ACTIVE',
  'PAUSED',
  'COMPLETED',
  'DELETED'
];

export const SCHEDULED_MAINTENANCE_FREQUENCIES: ScheduledMaintenanceFrequency[] = [
  'DAILY',
  'WEEKLY',
  'MONTHLY',
  'YEARLY'
];
