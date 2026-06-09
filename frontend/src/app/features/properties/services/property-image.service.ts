import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { PropertyImage, PropertyImageUploadResponse } from '../models/property-image.model';

@Injectable({
  providedIn: 'root'
})
export class PropertyImageService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(propertyId: string): Observable<PropertyImage[]> {
    return this.apiService.get<PropertyImage[]>(`/properties/${propertyId}/images`);
  }

  upload(propertyId: string, file: File, cover: boolean): Observable<PropertyImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('cover', String(cover));

    return this.apiService.post<PropertyImageUploadResponse>(`/properties/${propertyId}/images`, formData);
  }

  setCover(propertyId: string, imageId: string): Observable<PropertyImage> {
    return this.apiService.patch<PropertyImage>(`/properties/${propertyId}/images/${imageId}/cover`, {});
  }

  delete(propertyId: string, imageId: string): Observable<void> {
    return this.apiService.delete<void>(`/properties/${propertyId}/images/${imageId}`);
  }
}
