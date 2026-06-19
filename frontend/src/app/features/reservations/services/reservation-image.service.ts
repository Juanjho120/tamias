import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { ReservationImage, ReservationImageUploadResponse } from '../models/reservation-image.model';

@Injectable({ providedIn: 'root' })
export class ReservationImageService {
  constructor(private readonly apiService: ApiService) {}

  findAll(reservationId: string): Observable<ReservationImage[]> {
    return this.apiService.get<ReservationImage[]>(`/reservations/${reservationId}/images`);
  }

  upload(reservationId: string, file: File): Observable<ReservationImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.apiService.post<ReservationImageUploadResponse>(`/reservations/${reservationId}/images`, formData);
  }

  delete(reservationId: string, imageId: string): Observable<void> {
    return this.apiService.delete<void>(`/reservations/${reservationId}/images/${imageId}`);
  }
}
