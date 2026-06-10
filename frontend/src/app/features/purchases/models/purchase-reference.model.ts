export interface PurchasePropertyOption {
  id: string;
  name: string;
  address: string | null;
}

export interface PurchaseCityOption {
  id: string;
  name: string;
  country: string | null;
}

export interface PurchaseSupplierOption {
  id: string;
  name: string;
  phone: string | null;
  email: string | null;
}

export interface PurchaseInventoryItemOption {
  id: string;
  name: string;
  unit: string | null;
  itemType?: string | null;
  internalCode?: string | null;
  barcode?: string | null;
}


export interface PurchaseBrandOption {
  id: string;
  name: string;
}
