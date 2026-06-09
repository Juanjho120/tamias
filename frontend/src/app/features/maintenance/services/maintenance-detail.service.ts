import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  MaintenanceMaterialUsed,
  MaintenanceMaterialUsedRequest,
  MaintenanceMaterialUsedUpdateRequest,
  MaintenanceRecordPerson,
  MaintenanceRecordPersonRequest
} from '../models/maintenance-detail.model';

@Injectable({
  providedIn: 'root'
})
export class MaintenanceDetailService {
  constructor(private readonly apiService: ApiService) {
  }

  findPeople(maintenanceRecordId: string): Observable<MaintenanceRecordPerson[]> {
    return this.apiService.get<MaintenanceRecordPerson[]>(`/maintenance-records/${maintenanceRecordId}/people`);
  }

  addPerson(maintenanceRecordId: string, request: MaintenanceRecordPersonRequest): Observable<MaintenanceRecordPerson> {
    return this.apiService.post<MaintenanceRecordPerson>(`/maintenance-records/${maintenanceRecordId}/people`, request);
  }

  removePerson(maintenanceRecordId: string, personAssignmentId: string): Observable<void> {
    return this.apiService.delete<void>(`/maintenance-records/${maintenanceRecordId}/people/${personAssignmentId}`);
  }

  findMaterials(maintenanceRecordId: string): Observable<MaintenanceMaterialUsed[]> {
    return this.apiService.get<MaintenanceMaterialUsed[]>(`/maintenance-records/${maintenanceRecordId}/materials`);
  }

  addMaterial(maintenanceRecordId: string, request: MaintenanceMaterialUsedRequest): Observable<MaintenanceMaterialUsed> {
    return this.apiService.post<MaintenanceMaterialUsed>(`/maintenance-records/${maintenanceRecordId}/materials`, request);
  }

  updateMaterial(
    maintenanceRecordId: string,
    materialUsedId: string,
    request: MaintenanceMaterialUsedUpdateRequest
  ): Observable<MaintenanceMaterialUsed> {
    return this.apiService.put<MaintenanceMaterialUsed>(
      `/maintenance-records/${maintenanceRecordId}/materials/${materialUsedId}`,
      request
    );
  }

  removeMaterial(maintenanceRecordId: string, materialUsedId: string): Observable<void> {
    return this.apiService.delete<void>(`/maintenance-records/${maintenanceRecordId}/materials/${materialUsedId}`);
  }
}
