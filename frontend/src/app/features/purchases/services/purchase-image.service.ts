import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { PurchaseImage, PurchaseImageUploadResponse } from '../models/purchase-image.model';

@Injectable({ providedIn: 'root' })
export class PurchaseImageService {
  constructor(private readonly apiService: ApiService) {}

  findAll(purchaseListId: string): Observable<PurchaseImage[]> {
    return this.apiService.get<PurchaseImage[]>(`/purchase-lists/${purchaseListId}/images`);
  }

  upload(purchaseListId: string, file: File): Observable<PurchaseImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiService.post<PurchaseImageUploadResponse>(`/purchase-lists/${purchaseListId}/images`, formData);
  }

  delete(purchaseListId: string, imageId: string): Observable<void> {
    return this.apiService.delete<void>(`/purchase-lists/${purchaseListId}/images/${imageId}`);
  }
}
