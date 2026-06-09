export interface ScheduledMaintenanceReferenceOption {
  id: string;
  name: string;
}

export interface ScheduledMaintenancePersonOption {
  id: string;
  fullName: string;
}

export interface ScheduledMaintenancePropertyOption {
  id: string;
  name: string;
  address: string | null;
}
