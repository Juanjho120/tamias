import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  MaintenanceRecordItem,
  MaintenanceRecordItemRequest,
  MaintenanceRecordItemUpdateRequest,
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

  findItems(maintenanceRecordId: string): Observable<MaintenanceRecordItem[]> {
    return this.apiService.get<MaintenanceRecordItem[]>(`/maintenance-records/${maintenanceRecordId}/items`);
  }

  addItem(maintenanceRecordId: string, request: MaintenanceRecordItemRequest): Observable<MaintenanceRecordItem> {
    return this.apiService.post<MaintenanceRecordItem>(`/maintenance-records/${maintenanceRecordId}/items`, request);
  }

  updateItem(
    maintenanceRecordId: string,
    itemId: string,
    request: MaintenanceRecordItemUpdateRequest
  ): Observable<MaintenanceRecordItem> {
    return this.apiService.put<MaintenanceRecordItem>(`/maintenance-records/${maintenanceRecordId}/items/${itemId}`, request);
  }

  removeItem(maintenanceRecordId: string, itemId: string): Observable<void> {
    return this.apiService.delete<void>(`/maintenance-records/${maintenanceRecordId}/items/${itemId}`);
  }

  findMaterials(maintenanceRecordId: string): Observable<MaintenanceRecordItem[]> {
    return this.findItems(maintenanceRecordId);
  }

  addMaterial(maintenanceRecordId: string, request: MaintenanceRecordItemRequest): Observable<MaintenanceRecordItem> {
    return this.addItem(maintenanceRecordId, request);
  }

  updateMaterial(
    maintenanceRecordId: string,
    materialUsedId: string,
    request: MaintenanceRecordItemUpdateRequest
  ): Observable<MaintenanceRecordItem> {
    return this.updateItem(maintenanceRecordId, materialUsedId, request);
  }

  removeMaterial(maintenanceRecordId: string, materialUsedId: string): Observable<void> {
    return this.removeItem(maintenanceRecordId, materialUsedId);
  }
}
