export type ReservationStatus = 'ACTIVE' | 'CANCELLED' | 'DELETED';

export interface ReservationGuestRequest {
  guestId: string | null;
  fullName: string | null;
  phone: string | null;
  primary: boolean | null;
}

export interface ReservationGuest {
  id: string;
  guestId: string | null;
  fullName: string | null;
  phone: string | null;
  primary: boolean | null;
}

export interface ReservationSupplyRequest {
  inventoryItemId: string;
  quantity: number;
  unit: string | null;
  notes: string | null;
}

export interface ReservationSupply {
  id: string;
  reservationId: string;
  inventoryItemId: string;
  inventoryItemName: string;
  itemType: string | null;
  internalCode: string | null;
  barcode: string | null;
  quantity: number;
  unit: string | null;
  itemNameSnapshot: string;
  internalCodeSnapshot: string | null;
  barcodeSnapshot: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ReservationSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  platformId: string | null;
  platformName: string | null;
  reservationCode: string | null;
  checkIn: string;
  checkOut: string;
  guestNames: string[];
  reservationValue: number | null;
  status: ReservationStatus;
  createdAt: string;
}

export interface Reservation {
  id: string;
  propertyId: string;
  propertyName: string;
  platformId: string | null;
  platformName: string | null;
  reservationCode: string | null;
  checkIn: string;
  checkOut: string;
  suppliesDelivered: boolean | null;
  observations: string | null;
  reservationValue: number | null;
  invoiceNumber: string | null;
  invoiceSeries: string | null;
  guests: ReservationGuest[];
  supplies: ReservationSupply[];
  status: ReservationStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ReservationRequest {
  propertyId: string;
  platformId: string | null;
  reservationCode: string | null;
  checkIn: string;
  checkOut: string;
  suppliesDelivered: boolean | null;
  observations: string | null;
  reservationValue: number | null;
  invoiceNumber: string | null;
  invoiceSeries: string | null;
  status: ReservationStatus;
  guests: ReservationGuestRequest[];
  supplies: ReservationSupplyRequest[];
}

export interface ReservationFilters {
  propertyId?: string;
  status?: ReservationStatus | '';
  page: number;
  size: number;
  sort?: string;
}

export interface ReservationCalendarFilters {
  startDate: string;
  endDate: string;
  page: number;
  size: number;
  sort?: string;
}

export interface CancelReservationRequest {
  reason: string | null;
}

export const RESERVATION_STATUSES: ReservationStatus[] = ['ACTIVE', 'CANCELLED', 'DELETED'];
