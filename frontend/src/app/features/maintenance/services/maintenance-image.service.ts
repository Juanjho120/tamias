import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  MaintenanceImage,
  MaintenanceImageRole,
  MaintenanceImageRoleRequest,
  MaintenanceImageUploadResponse
} from '../models/maintenance-image.model';

@Injectable({ providedIn: 'root' })
export class MaintenanceImageService {
  constructor(private readonly apiService: ApiService) { }

  findAll(maintenanceRecordId: string): Observable<MaintenanceImage[]> {
    return this.apiService.get(`/maintenance-records/${maintenanceRecordId}/images`);
  }

  upload(
    maintenanceRecordId: string,
    file: File,
    imageRole: MaintenanceImageRole
  ): Observable<MaintenanceImageUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('imageRole', imageRole);

    return this.apiService.post(`/maintenance-records/${maintenanceRecordId}/images`, formData);
  }

  updateRole(
    maintenanceRecordId: string,
    imageId: string,
    imageRole: MaintenanceImageRole
  ): Observable<MaintenanceImage> {
    const request: MaintenanceImageRoleRequest = { imageRole };
    return this.apiService.patch(`/maintenance-records/${maintenanceRecordId}/images/${imageId}/role`, request);
  }

  delete(maintenanceRecordId: string, imageId: string): Observable<void> {
    return this.apiService.delete(`/maintenance-records/${maintenanceRecordId}/images/${imageId}`);
  }
}
