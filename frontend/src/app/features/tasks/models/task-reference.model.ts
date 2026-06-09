export interface TaskPropertyOption {
  id: string;
  name: string;
  address: string | null;
}

export interface TaskReservationOption {
  id: string;
  propertyId: string;
  propertyName: string;
  reservationCode: string | null;
  checkIn: string;
  checkOut: string;
}

export interface TaskMaintenanceRecordOption {
  id: string;
  propertyId: string;
  propertyName: string;
  title: string;
  status: string;
}

export interface TaskTemplateOption {
  id: string;
  name: string;
  description: string | null;
}
