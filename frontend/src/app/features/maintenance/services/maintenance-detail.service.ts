import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  MaintenanceRecordItem,
  MaintenanceRecordItemRequest,
  MaintenanceRecordItemUpdateRequest,
  MaintenanceRecordPerson,
  MaintenanceRecordPersonRequest,
  MaintenanceRecordServicedItem,
  MaintenanceRecordServicedItemRequest,
  MaintenanceRecordServicedItemUpdateRequest
} from '../models/maintenance-detail.model';

@Injectable({ providedIn: 'root' })
export class MaintenanceDetailService {
  constructor(private readonly apiService: ApiService) { }

  findPeople(maintenanceRecordId: string): Observable<MaintenanceRecordPerson[]> {
    return this.apiService.get(`/maintenance-records/${maintenanceRecordId}/people`);
  }

  addPerson(maintenanceRecordId: string, request: MaintenanceRecordPersonRequest): Observable<MaintenanceRecordPerson> {
    return this.apiService.post(`/maintenance-records/${maintenanceRecordId}/people`, request);
  }

  removePerson(maintenanceRecordId: string, personAssignmentId: string): Observable<void> {
    return this.apiService.delete(`/maintenance-records/${maintenanceRecordId}/people/${personAssignmentId}`);
  }

  findItems(maintenanceRecordId: string): Observable<MaintenanceRecordItem[]> {
    return this.apiService.get(`/maintenance-records/${maintenanceRecordId}/items`);
  }

  addItem(maintenanceRecordId: string, request: MaintenanceRecordItemRequest): Observable<MaintenanceRecordItem> {
    return this.apiService.post(`/maintenance-records/${maintenanceRecordId}/items`, request);
  }

  updateItem(
    maintenanceRecordId: string,
    itemId: string,
    request: MaintenanceRecordItemUpdateRequest
  ): Observable<MaintenanceRecordItem> {
    return this.apiService.put(`/maintenance-records/${maintenanceRecordId}/items/${itemId}`, request);
  }

  removeItem(maintenanceRecordId: string, itemId: string): Observable<void> {
    return this.apiService.delete(`/maintenance-records/${maintenanceRecordId}/items/${itemId}`);
  }

  findServicedItems(maintenanceRecordId: string): Observable<MaintenanceRecordServicedItem[]> {
    return this.apiService.get(`/maintenance-records/${maintenanceRecordId}/serviced-items`);
  }

  addServicedItem(
    maintenanceRecordId: string,
    request: MaintenanceRecordServicedItemRequest
  ): Observable<MaintenanceRecordServicedItem> {
    return this.apiService.post(`/maintenance-records/${maintenanceRecordId}/serviced-items`, request);
  }

  updateServicedItem(
    maintenanceRecordId: string,
    servicedItemId: string,
    request: MaintenanceRecordServicedItemUpdateRequest
  ): Observable<MaintenanceRecordServicedItem> {
    return this.apiService.put(`/maintenance-records/${maintenanceRecordId}/serviced-items/${servicedItemId}`, request);
  }

  removeServicedItem(maintenanceRecordId: string, servicedItemId: string): Observable<void> {
    return this.apiService.delete(`/maintenance-records/${maintenanceRecordId}/serviced-items/${servicedItemId}`);
  }
}
