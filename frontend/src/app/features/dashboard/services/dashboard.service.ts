import { Injectable } from '@angular/core';
import { forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  DashboardData,
  DashboardDocumentSummary,
  DashboardMaintenanceRecordSummary,
  DashboardPropertySummary,
  DashboardPurchaseListSummary,
  DashboardReservationDetail,
  DashboardReservationSummary,
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

  loadReservationDetailsForMonth(year: number, month: number): Observable<DashboardReservationDetail[]> {
    const monthStart = this.toDateString(new Date(year, month, 1));
    const monthEnd = this.toDateString(new Date(year, month + 1, 1));

    return this.getPage<DashboardReservationSummary>('/reservations/calendar', {
      startDate: monthStart,
      endDate: monthEnd,
      page: 0,
      size: 200,
      sort: 'checkIn,asc'
    }).pipe(
      switchMap((response) => {
        if (response.content.length === 0) {
          return of([]);
        }

        return forkJoin(response.content.map((reservation) => this.findReservationById(reservation.id)));
      })
    );
  }

  private findReservationById(id: string): Observable<DashboardReservationDetail> {
    return this.apiService.get<DashboardReservationDetail>(`/reservations/${id}`);
  }


  loadReservationCalendar(startDate: string, endDate: string): Observable<DashboardReservationDetail[]> {
    return this.getPage<DashboardReservationSummary>('/reservations/calendar', {
      startDate,
      endDate,
      page: 0,
      size: 200,
      sort: 'checkIn,asc'
    }).pipe(
      switchMap((response) => {
        const detailRequests = response.content.map((reservation) =>
          this.apiService.get<DashboardReservationDetail>(`/reservations/${reservation.id}`)
        );

        return detailRequests.length
          ? forkJoin(detailRequests)
          : new Observable<DashboardReservationDetail[]>((subscriber) => {
            subscriber.next([]);
            subscriber.complete();
          });
      }),
      switchMap((reservations) => {
        const propertyIds = [...new Set(reservations.map((reservation) => reservation.propertyId))];

        if (propertyIds.length === 0) {
          return new Observable<DashboardReservationDetail[]>((subscriber) => {
            subscriber.next(reservations);
            subscriber.complete();
          });
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
      })
    );
  }

  private getPage<T>(path: string, params: Record<string, string | number | boolean | null | undefined>): Observable<PageResponse<T>> {
    return this.apiService.get<PageResponse<T>>(path, params);
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private daysFromNow(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() + days);
    return date.toISOString().slice(0, 10);
  }

  private toDateString(date: Date): string {
    return date.toISOString().slice(0, 10);
  }

  private findCoverImageUrl(images: DashboardPropertyImage[]): string | null {
    const activeImages = images.filter((image) => image.status === 'ACTIVE' && !!image.fileUrl);
    const coverImage = activeImages.find((image) => image.cover);

    return coverImage?.fileUrl ?? activeImages[0]?.fileUrl ?? null;
  }
}
