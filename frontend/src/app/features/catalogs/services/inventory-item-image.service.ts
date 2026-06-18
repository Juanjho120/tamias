import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/services/api.service';
import { InventoryItemImage, InventoryItemImageUploadResponse } from '../models/inventory-item-image.model';

@Injectable({
  providedIn: 'root'
})
export class InventoryItemImageService {

  constructor(private readonly apiService: ApiService) {
  }

  findAll(inventoryItemId: string): Observable<InventoryItemImage[]> {
    return this.apiService.get<InventoryItemImage[]>(`/inventory-items/${inventoryItemId}/images`);
  }

  upload(inventoryItemId: string, file: File, cover = false): Observable<InventoryItemImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('cover', String(cover));

    return this.apiService.post<InventoryItemImageUploadResponse>(`/inventory-items/${inventoryItemId}/images`, formData);
  }

  setCover(inventoryItemId: string, imageId: string): Observable<InventoryItemImage> {
    return this.apiService.patch<InventoryItemImage>(`/inventory-items/${inventoryItemId}/images/${imageId}/cover`, {});
  }

  delete(inventoryItemId: string, imageId: string): Observable<void> {
    return this.apiService.delete<void>(`/inventory-items/${inventoryItemId}/images/${imageId}`);
  }
}
