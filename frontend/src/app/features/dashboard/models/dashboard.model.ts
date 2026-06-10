export interface DashboardMetric {
  key: string;
  titleKey: string;
  descriptionKey: string;
  icon: string;
  route: string;
  value: number;
  loading?: boolean;
}

export interface DashboardPropertySummary {
  id: string;
  name: string;
  address: string | null;
}

export interface DashboardReservationSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  platformName: string | null;
  reservationCode: string | null;
  checkIn: string;
  checkOut: string;
  guestNames: string[];
  reservationValue: number | null;
  status: string;
  createdAt: string;
}

export interface DashboardReservationGuest {
  id: string;
  guestId: string | null;
  fullName: string | null;
  phone: string | null;
  primary: boolean | null;
}

export interface DashboardReservationDetail {
  id: string;
  propertyId: string;
  propertyName: string;
  propertyCoverImageUrl: string | null;
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
  guests: DashboardReservationGuest[];
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardMaintenanceRecordSummary {
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
  status: string;
  createdAt: string;
}

export interface DashboardMaintenanceRecordCalendarItem extends DashboardMaintenanceRecordSummary {
  maintenancePersonId: string | null;
  maintenancePersonName: string | null;
  materialsTotal: number;
  peopleTotal: number;
}

export interface DashboardScheduledMaintenanceSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  maintenanceCategoryId: string | null;
  maintenanceCategoryName: string | null;
  maintenanceTypeId: string | null;
  maintenanceTypeName: string | null;
  title: string;
  nextDueDate: string | null;
  frequency: string | null;
  estimatedCost: number | null;
  status: string;
}

export interface DashboardScheduledMaintenanceCalendarItem extends DashboardScheduledMaintenanceSummary {
  maintenancePersonId: string | null;
  maintenancePersonName: string | null;
  startDate: string;
  endDate: string | null;
}

export interface DashboardTaskListSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  reservationId: string | null;
  reservationLabel: string | null;
  maintenanceRecordId: string | null;
  maintenanceRecordLabel: string | null;
  title: string;
  creationDate: string | null;
  dueDate: string | null;
  status: string;
  totalItems: number;
  completedItems: number;
  createdAt: string;
}

export interface DashboardPurchaseListSummary {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  cityId: string | null;
  cityName: string | null;
  supplierId: string | null;
  supplierName: string | null;
  purchaseDate: string;
  status: string;
  totalItems: number;
  purchasedItems: number;
  estimatedTotal: number | null;
  createdAt: string;
}

export interface DashboardDocumentSummary {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  documentType: string;
  title: string;
  originalFilename: string;
  processingStatus: string;
  status: string;
  createdAt: string;
}

export type DashboardCalendarIconType = 'MAINTENANCE_RECORD' | 'TASK_LIST' | 'PURCHASE_LIST';

export interface DashboardCalendarDayIcon {
  id: string;
  type: DashboardCalendarIconType;
  date: string;
  title: string;
  status: string;
  maintenanceRecord?: DashboardMaintenanceRecordCalendarItem;
  taskList?: DashboardTaskListSummary;
  purchaseList?: DashboardPurchaseListSummary;
}

export interface DashboardCalendarDay {
  date: string;
  dayNumber: number;
  currentMonth: boolean;
  today: boolean;
  icons: DashboardCalendarDayIcon[];
}

export interface DashboardCalendarRow {
  id: string;
  days: DashboardCalendarDay[];
  segments: DashboardReservationCalendarSegment[];
  scheduledMaintenanceSegments: DashboardScheduledMaintenanceCalendarSegment[];
  maxLanes: number;
  maxIconRows: number;
}

export interface DashboardReservationCalendarSegment {
  id: string;
  reservationId: string;
  propertyId: string;
  propertyName: string;
  propertyCoverImageUrl: string | null;
  platformName: string | null;
  reservationCode: string | null;
  checkIn: string;
  checkOut: string;
  guestNames: string[];
  primaryGuestName: string;
  guestCount: number;
  invoiceStatus: 'INVOICED' | 'NOT_INVOICED';
  status: string;
  rangeStartsHere: boolean;
  rangeEndsHere: boolean;
  startsAtCheckIn: boolean;
  endsAtCheckOut: boolean;
  gridColumnStart: number;
  gridColumnEnd: number;
  lane: number;
  topRem: number;
}

export interface DashboardScheduledMaintenanceCalendarSegment {
  id: string;
  scheduledMaintenanceId: string;
  propertyId: string;
  propertyName: string;
  maintenanceCategoryName: string | null;
  maintenanceTypeName: string | null;
  maintenancePersonName: string | null;
  title: string;
  startDate: string;
  endDate: string;
  nextDueDate: string | null;
  estimatedCost: number | null;
  status: string;
  rangeStartsHere: boolean;
  rangeEndsHere: boolean;
  gridColumnStart: number;
  gridColumnEnd: number;
  lane: number;
}

export interface DashboardCalendarData {
  reservations: DashboardReservationDetail[];
  maintenanceRecords: DashboardMaintenanceRecordCalendarItem[];
  scheduledMaintenances: DashboardScheduledMaintenanceCalendarItem[];
  taskLists: DashboardTaskListSummary[];
  purchaseLists: DashboardPurchaseListSummary[];
}

export interface DashboardData {
  activeProperties: number;
  activeReservations: number;
  pendingMaintenance: number;
  dueScheduledMaintenance: number;
  openTaskLists: number;
  openPurchaseLists: number;
  pendingDocuments: number;
  failedDocuments: number;

  upcomingReservations: DashboardReservationSummary[];
  dueMaintenance: DashboardScheduledMaintenanceSummary[];
  recentMaintenance: DashboardMaintenanceRecordSummary[];
  openTasks: DashboardTaskListSummary[];
  openPurchases: DashboardPurchaseListSummary[];
  documentAlerts: DashboardDocumentSummary[];
}
