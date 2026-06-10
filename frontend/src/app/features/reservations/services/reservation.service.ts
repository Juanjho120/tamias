import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  CancelReservationRequest,
  Reservation,
  ReservationCalendarFilters,
  ReservationFilters,
  ReservationRequest,
  ReservationSummary,
  ReservationSupply,
  ReservationSupplyRequest
} from '../models/reservation.model';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: ReservationFilters): Observable<PageResponse<ReservationSummary>> {
    return this.apiService.get<PageResponse<ReservationSummary>>('/reservations', {
      propertyId: filters.propertyId,
      status: filters.status,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'checkIn,desc'
    });
  }

  findCalendar(filters: ReservationCalendarFilters): Observable<PageResponse<ReservationSummary>> {
    return this.apiService.get<PageResponse<ReservationSummary>>('/reservations/calendar', {
      startDate: filters.startDate,
      endDate: filters.endDate,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'checkIn,asc'
    });
  }

  findById(id: string): Observable<Reservation> {
    return this.apiService.get<Reservation>(`/reservations/${id}`);
  }

  create(request: ReservationRequest): Observable<Reservation> {
    return this.apiService.post<Reservation>('/reservations', request);
  }

  update(id: string, request: ReservationRequest): Observable<Reservation> {
    return this.apiService.put<Reservation>(`/reservations/${id}`, request);
  }

  findSupplies(id: string): Observable<ReservationSupply[]> {
    return this.apiService.get<ReservationSupply[]>(`/reservations/${id}/supplies`);
  }

  addSupply(id: string, request: ReservationSupplyRequest): Observable<ReservationSupply> {
    return this.apiService.post<ReservationSupply>(`/reservations/${id}/supplies`, request);
  }

  updateSupply(id: string, supplyId: string, request: ReservationSupplyRequest): Observable<ReservationSupply> {
    return this.apiService.put<ReservationSupply>(`/reservations/${id}/supplies/${supplyId}`, request);
  }

  deleteSupply(id: string, supplyId: string): Observable<void> {
    return this.apiService.delete<void>(`/reservations/${id}/supplies/${supplyId}`);
  }

  cancel(id: string, request: CancelReservationRequest): Observable<Reservation> {
    return this.apiService.patch<Reservation>(`/reservations/${id}/cancel`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/reservations/${id}`);
  }
}
