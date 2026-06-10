import { Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  ReservationInventoryItemOption,
  ReservationPlatformOption,
  ReservationPropertyOption
} from '../models/reservation-reference.model';

export interface ReservationReferenceData {
  properties: ReservationPropertyOption[];
  platforms: ReservationPlatformOption[];
  inventoryItems: ReservationInventoryItemOption[];
}

@Injectable({
  providedIn: 'root'
})
export class ReservationReferenceDataService {
  constructor(private readonly apiService: ApiService) {
  }

  loadAll(): Observable<ReservationReferenceData> {
    return forkJoin({
      properties: this.apiService.get<PageResponse<ReservationPropertyOption>>('/properties', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      platforms: this.apiService.get<PageResponse<ReservationPlatformOption>>('/catalogs/platforms', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      inventoryItems: this.apiService.get<PageResponse<ReservationInventoryItemOption>>('/inventory-items', {
        status: 'ACTIVE',
        availableForReservations: true,
        page: 0,
        size: 500,
        sort: 'name,asc'
      }).pipe(map((response) => response.content))
    });
  }
}
