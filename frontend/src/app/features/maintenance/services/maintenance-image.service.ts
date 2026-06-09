import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { MaintenanceImage, MaintenanceImageUploadResponse } from '../models/maintenance-image.model';

@Injectable({
  providedIn: 'root'
})
export class MaintenanceImageService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(maintenanceRecordId: string): Observable<MaintenanceImage[]> {
    return this.apiService.get<MaintenanceImage[]>(`/maintenance-records/${maintenanceRecordId}/images`);
  }

  upload(maintenanceRecordId: string, file: File): Observable<MaintenanceImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.apiService.post<MaintenanceImageUploadResponse>(`/maintenance-records/${maintenanceRecordId}/images`, formData);
  }

  delete(maintenanceRecordId: string, imageId: string): Observable<void> {
    return this.apiService.delete<void>(`/maintenance-records/${maintenanceRecordId}/images/${imageId}`);
  }
}
