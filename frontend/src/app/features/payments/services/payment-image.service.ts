import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { PaymentImage, PaymentImageUploadResponse } from '../models/payment-image.model';

@Injectable({ providedIn: 'root' })
export class PaymentImageService {
  constructor(private readonly apiService: ApiService) {}

  findAll(paymentId: string): Observable<PaymentImage[]> {
    return this.apiService.get<PaymentImage[]>(`/payments/${paymentId}/images`);
  }

  upload(paymentId: string, file: File): Observable<PaymentImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiService.post<PaymentImageUploadResponse>(`/payments/${paymentId}/images`, formData);
  }

  delete(paymentId: string, imageId: string): Observable<void> {
    return this.apiService.delete<void>(`/payments/${paymentId}/images/${imageId}`);
  }
}
