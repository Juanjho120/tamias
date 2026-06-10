export interface DashboardAnalyticsResponse {
  kpis: DashboardKpiResponse;
  maintenanceCostByMonth: MonthlyAmountResponse[];
  purchaseCostByMonth: MonthlyAmountResponse[];
  reservationsByMonth: MonthlyCountAmountResponse[];
  topReservationSupplies: TopItemResponse[];
  topPurchasedItems: TopItemResponse[];
}

export interface DashboardKpiResponse {
  activeReservationsToday: number;
  pendingMaintenanceRecords: number;
  overdueTaskLists: number;
  upcomingScheduledMaintenance: number;
  openPurchaseLists: number;
  estimatedOpenPurchaseTotal: number;
}

export interface MonthlyAmountResponse {
  month: string;
  amount: number;
}

export interface MonthlyCountAmountResponse {
  month: string;
  count: number;
  amount: number;
}

export interface TopItemResponse {
  name: string;
  quantity: number;
  amount: number;
}
