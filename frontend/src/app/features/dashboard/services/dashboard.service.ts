import { Injectable } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  DashboardCalendarData,
  DashboardData,
  DashboardDocumentSummary,
  DashboardMaintenanceRecordCalendarItem,
  DashboardMaintenanceRecordSummary,
  DashboardPropertySummary,
  DashboardPurchaseListSummary,
  DashboardReservationDetail,
  DashboardReservationSummary,
  DashboardScheduledMaintenanceCalendarItem,
  DashboardScheduledMaintenanceSummary,
  DashboardTaskListSummary
} from '../models/dashboard.model';

interface DashboardPropertyImage {
  id: string;
  parentId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: string;
  createdAt: string;
  fileUrl: string | null;
  fileUrlExpiresIn: number | null;
}

interface DashboardMaintenanceRecordPerson {
  id: string;
  maintenanceRecordId: string;
  maintenancePersonId: string;
  fullName: string;
  phone: string | null;
  email: string | null;
  notes: string | null;
}

interface DashboardMaintenanceRecordItem {
  id: string;
  maintenanceRecordId: string;
  inventoryItemId: string | null;
  inventoryItemName: string | null;
  itemNameSnapshot: string | null;
  quantity: number | null;
  unit: string | null;
  notes: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  constructor(private readonly apiService: ApiService) {
  }

  loadDashboard(): Observable<DashboardData> {
    return forkJoin({
      properties: this.getPage<DashboardPropertySummary>('/properties', { status: 'ACTIVE', page: 0, size: 1 }),
      activeReservations: this.getPage<DashboardReservationSummary>('/reservations', { status: 'ACTIVE', page: 0, size: 1 }),
      upcomingReservations: this.getPage<DashboardReservationSummary>('/reservations/calendar', {
        startDate: this.today(), endDate: this.daysFromNow(45), page: 0, size: 5, sort: 'checkIn,asc'
      }),
      pendingMaintenance: this.getPage<DashboardMaintenanceRecordSummary>('/maintenance-records', { status: 'PENDING', page: 0, size: 1 }),
      inProgressMaintenance: this.getPage<DashboardMaintenanceRecordSummary>('/maintenance-records', { status: 'IN_PROGRESS', page: 0, size: 1 }),
      recentMaintenance: this.getPage<DashboardMaintenanceRecordSummary>('/maintenance-records', { page: 0, size: 5, sort: 'createdAt,desc' }),
      dueScheduledMaintenance: this.getPage<DashboardScheduledMaintenanceSummary>('/scheduled-maintenance/due', { page: 0, size: 5, sort: 'nextDueDate,asc' }),
      openTasks: this.getPage<DashboardTaskListSummary>('/task-lists', { status: 'OPEN', page: 0, size: 5, sort: 'dueDate,asc' }),
      openPurchases: this.getPage<DashboardPurchaseListSummary>('/purchase-lists', { status: 'OPEN', page: 0, size: 5, sort: 'purchaseDate,desc' }),
      pendingDocuments: this.getPage<DashboardDocumentSummary>('/documents', {
        status: 'ACTIVE', processingStatus: 'PENDING', page: 0, size: 5, sort: 'createdAt,desc'
      }),
      failedDocuments: this.getPage<DashboardDocumentSummary>('/documents', {
        status: 'ACTIVE', processingStatus: 'FAILED', page: 0, size: 5, sort: 'createdAt,desc'
      })
    }).pipe(
      map((response) => ({
        activeProperties: response.properties.totalElements,
        activeReservations: response.activeReservations.totalElements,
        pendingMaintenance: response.pendingMaintenance.totalElements + response.inProgressMaintenance.totalElements,
        dueScheduledMaintenance: response.dueScheduledMaintenance.totalElements,
        openTaskLists: response.openTasks.totalElements,
        openPurchaseLists: response.openPurchases.totalElements,
        pendingDocuments: response.pendingDocuments.totalElements,
        failedDocuments: response.failedDocuments.totalElements,
        upcomingReservations: response.upcomingReservations.content,
        dueMaintenance: response.dueScheduledMaintenance.content,
        recentMaintenance: response.recentMaintenance.content,
        openTasks: response.openTasks.content,
        openPurchases: response.openPurchases.content,
        documentAlerts: [...response.failedDocuments.content, ...response.pendingDocuments.content].slice(0, 5)
      }))
    );
  }

  loadCalendarData(startDate: string, endDate: string): Observable<DashboardCalendarData> {
    return forkJoin({
      reservations: this.loadReservationCalendar(startDate, endDate),
      maintenanceRecords: this.loadMaintenanceRecordCalendarItems(startDate, endDate),
      scheduledMaintenances: this.loadScheduledMaintenanceCalendarItems(startDate, endDate),
      taskLists: this.loadTaskListCalendarItems(startDate, endDate),
      purchaseLists: this.loadPurchaseListCalendarItems(startDate, endDate)
    });
  }

  loadReservationCalendar(startDate: string, endDate: string): Observable<DashboardReservationDetail[]> {
    return this.getPage<DashboardReservationSummary>('/reservations/calendar', {
      startDate,
      endDate,
      page: 0,
      size: 500,
      sort: 'checkIn,asc'
    }).pipe(
      switchMap((response) => {
        const detailRequests = response.content.map((reservation) =>
          this.apiService.get<DashboardReservationDetail>(`/reservations/${reservation.id}`)
        );

        return detailRequests.length ? forkJoin(detailRequests) : of([]);
      }),
      switchMap((reservations) => this.attachPropertyCoverImages(reservations))
    );
  }

  private loadMaintenanceRecordCalendarItems(startDate: string, endDate: string): Observable<DashboardMaintenanceRecordCalendarItem[]> {
    return this.getPage<DashboardMaintenanceRecordSummary>('/maintenance-records', {
      page: 0,
      size: 500,
      sort: 'scheduledAt,asc'
    }).pipe(
      map((response) => response.content.filter((record) => {
        const calendarDate = this.normalizeDate(record.performedAt ?? record.scheduledAt);

        return !!calendarDate && calendarDate >= startDate && calendarDate <= endDate;
      })),
      switchMap((records) => {
        const requests = records.map((record) =>
          forkJoin({
            detail: this.apiService.get<DashboardMaintenanceRecordCalendarItem>(`/maintenance-records/${record.id}`),
            people: this.apiService.get<DashboardMaintenanceRecordPerson[]>(`/maintenance-records/${record.id}/people`),
            items: this.apiService.get<DashboardMaintenanceRecordItem[]>(`/maintenance-records/${record.id}/items`)
          }).pipe(
            map(({ detail, people, items }) => ({
              ...detail,
              materialsTotal: items.length,
              peopleTotal: people.length
            }))
          )
        );

        return requests.length ? forkJoin(requests) : of([]);
      })
    );
  }

  private loadScheduledMaintenanceCalendarItems(startDate: string, endDate: string): Observable<DashboardScheduledMaintenanceCalendarItem[]> {
    return this.getPage<DashboardScheduledMaintenanceSummary>('/scheduled-maintenance', {
      page: 0,
      size: 500,
      sort: 'startDate,asc'
    }).pipe(
      switchMap((response) => {
        const detailRequests = response.content.map((item) =>
          this.apiService.get<DashboardScheduledMaintenanceCalendarItem>(`/scheduled-maintenance/${item.id}`)
        );

        return detailRequests.length ? forkJoin(detailRequests) : of([]);
      }),
      map((items) => items.filter((item) => {
        const itemStart = this.normalizeDate(item.startDate);
        const itemEnd = this.normalizeDate(item.endDate) ?? itemStart;

        return !!itemStart && !!itemEnd && itemEnd >= startDate && itemStart <= endDate;
      }))
    );
  }

  private loadTaskListCalendarItems(startDate: string, endDate: string): Observable<DashboardTaskListSummary[]> {
    return this.getPage<DashboardTaskListSummary>('/task-lists', {
      page: 0,
      size: 500,
      sort: 'dueDate,asc'
    }).pipe(
      map((response) => response.content.filter((taskList) => {
        const dueDate = this.normalizeDate(taskList.dueDate);
        return !!dueDate && dueDate >= startDate && dueDate <= endDate;
      })),
      switchMap((taskLists) => this.attachTaskAssociationLabels(taskLists))
    );
  }

  private loadPurchaseListCalendarItems(startDate: string, endDate: string): Observable<DashboardPurchaseListSummary[]> {
    return this.getPage<DashboardPurchaseListSummary>('/purchase-lists', {
      page: 0,
      size: 500,
      sort: 'purchaseDate,asc'
    }).pipe(
      map((response) => response.content.filter((purchaseList) => {
        const purchaseDate = this.normalizeDate(purchaseList.purchaseDate);
        return !!purchaseDate && purchaseDate >= startDate && purchaseDate <= endDate;
      }))
    );
  }

  private attachPropertyCoverImages(reservations: DashboardReservationDetail[]): Observable<DashboardReservationDetail[]> {
    const propertyIds = [...new Set(reservations.map((reservation) => reservation.propertyId))];

    if (propertyIds.length === 0) {
      return of(reservations);
    }

    const imageRequests = propertyIds.map((propertyId) =>
      this.apiService.get<DashboardPropertyImage[]>(`/properties/${propertyId}/images`).pipe(
        map((images) => ({
          propertyId,
          coverImageUrl: this.findCoverImageUrl(images)
        }))
      )
    );

    return forkJoin(imageRequests).pipe(
      map((coverImages) => {
        const coverByPropertyId = new Map(
          coverImages.map((item) => [item.propertyId, item.coverImageUrl])
        );

        return reservations.map((reservation) => ({
          ...reservation,
          propertyCoverImageUrl: coverByPropertyId.get(reservation.propertyId) ?? null
        }));
      })
    );
  }

  private attachTaskAssociationLabels(taskLists: DashboardTaskListSummary[]): Observable<DashboardTaskListSummary[]> {
    const requests = taskLists.map((taskList) => {
      const reservationRequest = taskList.reservationId
        ? this.apiService.get<DashboardReservationDetail>(`/reservations/${taskList.reservationId}`).pipe(
          map((reservation) => reservation.reservationCode || reservation.propertyName || reservation.id)
        )
        : of(null);

      const maintenanceRequest = taskList.maintenanceRecordId
        ? this.apiService.get<DashboardMaintenanceRecordCalendarItem>(`/maintenance-records/${taskList.maintenanceRecordId}`).pipe(
          map((maintenance) => maintenance.title || maintenance.id)
        )
        : of(null);

      return forkJoin({
        reservationLabel: reservationRequest,
        maintenanceRecordLabel: maintenanceRequest
      }).pipe(
        map(({ reservationLabel, maintenanceRecordLabel }) => ({
          ...taskList,
          reservationLabel,
          maintenanceRecordLabel
        }))
      );
    });

    return requests.length ? forkJoin(requests) : of([]);
  }

  private findCoverImageUrl(images: DashboardPropertyImage[]): string | null {
    const activeImages = images.filter((image) => image.status === 'ACTIVE' && !!image.fileUrl);
    const coverImage = activeImages.find((image) => image.cover);

    return coverImage?.fileUrl ?? activeImages[0]?.fileUrl ?? null;
  }

  private getPage<T>(path: string, params: Record<string, string | number | boolean | null | undefined>): Observable<PageResponse<T>> {
    return this.apiService.get<PageResponse<T>>(path, params);
  }

  private today(): string {
    return this.toLocalDateString(new Date());
  }

  private daysFromNow(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() + days);

    return this.toLocalDateString(date);
  }

  private normalizeDate(value: string | null | undefined): string | null {
    const rawValue = String(value ?? '').trim();

    if (!rawValue) {
      return null;
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(rawValue)) {
      return rawValue;
    }

    const parsedDate = new Date(rawValue);

    if (Number.isNaN(parsedDate.getTime())) {
      return null;
    }

    return this.toLocalDateString(parsedDate);
  }

  private toLocalDateString(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');

    return `${year}-${month}-${day}`;
  }
}
