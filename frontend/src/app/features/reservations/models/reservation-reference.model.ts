export interface ReservationPropertyOption {
  id: string;
  name: string;
  address: string | null;
}

export interface ReservationPlatformOption {
  id: string;
  name: string;
}

export interface ReservationInventoryItemOption {
  id: string;
  name: string;
  unit: string | null;
  brandId?: string | null;
  brandName?: string | null;
  itemType: string | null;
  internalCode: string | null;
  barcode: string | null;
}
