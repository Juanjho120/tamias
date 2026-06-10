import { Injectable } from '@angular/core';
import { forkJoin, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  PurchaseBrandOption,
  PurchaseCityOption,
  PurchaseInventoryItemOption,
  PurchasePropertyOption,
  PurchaseSupplierOption
} from '../models/purchase-reference.model';

export interface PurchaseReferenceData {
  properties: PurchasePropertyOption[];
  cities: PurchaseCityOption[];
  suppliers: PurchaseSupplierOption[];
  inventoryItems: PurchaseInventoryItemOption[];
  materials: PurchaseInventoryItemOption[];
  brands: PurchaseBrandOption[];
}

@Injectable({
  providedIn: 'root'
})
export class PurchaseReferenceDataService {
  constructor(private readonly apiService: ApiService) {
  }

  loadAll(): Observable<PurchaseReferenceData> {
    return forkJoin({
      properties: this.apiService.get<PageResponse<PurchasePropertyOption>>('/properties', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      cities: this.apiService.get<PageResponse<PurchaseCityOption>>('/catalogs/cities', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      suppliers: this.apiService.get<PageResponse<PurchaseSupplierOption>>('/catalogs/suppliers', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      inventoryItems: this.apiService.get<PageResponse<PurchaseInventoryItemOption>>('/inventory-items', {
        status: 'ACTIVE',
        availableForPurchases: true,
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content)),
      brands: this.apiService.get<PageResponse<PurchaseBrandOption>>('/catalogs/brands', {
        status: 'ACTIVE',
        page: 0,
        size: 200,
        sort: 'name,asc'
      }).pipe(map((response) => response.content))
    }).pipe(
      map((data) => ({
        ...data,
        materials: data.inventoryItems
      }))
    );
  }
}
