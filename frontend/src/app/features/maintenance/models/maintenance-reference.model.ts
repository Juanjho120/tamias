export interface MaintenanceReferenceOption {
  id: string;
  name: string;
}

export interface MaintenancePersonOption {
  id: string;
  fullName: string;
}

export interface MaintenanceMaterialOption {
  id: string;
  name: string;
  unit: string | null;
}

export interface PropertyOption {
  id: string;
  name: string;
  address: string | null;
}
