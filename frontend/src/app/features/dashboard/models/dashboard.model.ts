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

export interface DashboardMaintenanceRecordSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  title: string;
  scheduledAt: string | null;
  performedAt: string | null;
  cost: number | null;
  status: string;
  createdAt: string;
}

export interface DashboardScheduledMaintenanceSummary {
  id: string;
  propertyId: string;
  propertyName: string;
  title: string;
  nextDueDate: string | null;
  frequency: string | null;
  status: string;
}

export interface DashboardTaskListSummary {
  id: string;
  propertyId: string;
  propertyName: string;
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
