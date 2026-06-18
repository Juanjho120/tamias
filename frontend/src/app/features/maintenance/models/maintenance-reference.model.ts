export interface MaintenanceReferenceOption {
  id: string;
  name: string;
}

export interface MaintenancePersonOption {
  id: string;
  fullName: string;
}

export interface MaintenanceInventoryItemOption {
  id: string;
  name: string;
  unit: string | null;
  brandId?: string | null;
  brandName?: string | null;
  itemType?: string | null;
  internalCode?: string | null;
  barcode?: string | null;
}

export interface PropertyOption {
  id: string;
  name: string;
  address: string | null;
}
