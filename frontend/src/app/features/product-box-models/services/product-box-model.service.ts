import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  ProductBoxFaceName,
  ProductBoxModel,
  ProductBoxModelFace,
  ProductBoxModelFilters,
  ProductBoxModelRequest,
  ProductBoxModelSummary,
  ProductBoxTextureContourDetectionResponse,
  ProductBoxTextureProcessRequest
} from '../models/product-box-model.model';

@Injectable({ providedIn: 'root' })
export class ProductBoxModelService {
  constructor(private readonly apiService: ApiService) {}

  findAll(filters: ProductBoxModelFilters): Observable<PageResponse<ProductBoxModelSummary>> {
    return this.apiService.get<PageResponse<ProductBoxModelSummary>>('/product-box-models', {
      inventoryItemId: filters.inventoryItemId,
      purchaseItemId: filters.purchaseItemId,
      search: filters.search,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }

  findById(id: string): Observable<ProductBoxModel> {
    return this.apiService.get<ProductBoxModel>(`/product-box-models/${id}`);
  }

  create(request: ProductBoxModelRequest): Observable<ProductBoxModel> {
    return this.apiService.post<ProductBoxModel>('/product-box-models', request);
  }

  update(id: string, request: ProductBoxModelRequest): Observable<ProductBoxModel> {
    return this.apiService.put<ProductBoxModel>(`/product-box-models/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/product-box-models/${id}`);
  }

  findFaces(id: string): Observable<ProductBoxModelFace[]> {
    return this.apiService.get<ProductBoxModelFace[]>(`/product-box-models/${id}/faces`);
  }

  uploadFace(
    id: string,
    faceName: ProductBoxFaceName,
    file: File,
    rotationDegrees: number | null = null,
    flipHorizontal = false,
    flipVertical = false
  ): Observable<ProductBoxModelFace> {
    return this.apiService.post<ProductBoxModelFace>(
      `/product-box-models/${id}/faces/${faceName}`,
      this.buildFaceFormData(file, rotationDegrees, flipHorizontal, flipVertical)
    );
  }

  replaceFace(
    id: string,
    faceName: ProductBoxFaceName,
    file: File,
    rotationDegrees: number | null = null,
    flipHorizontal = false,
    flipVertical = false
  ): Observable<ProductBoxModelFace> {
    return this.apiService.put<ProductBoxModelFace>(
      `/product-box-models/${id}/faces/${faceName}`,
      this.buildFaceFormData(file, rotationDegrees, flipHorizontal, flipVertical)
    );
  }

  uploadOriginalTexture(id: string, faceName: ProductBoxFaceName, file: File): Observable<ProductBoxModelFace> {
    const formData = new FormData();
    formData.append('file', file);
    return this.apiService.post<ProductBoxModelFace>(
      `/product-box-models/${id}/faces/${faceName}/texture/original`,
      formData
    );
  }

  detectTextureContour(id: string, faceName: ProductBoxFaceName): Observable<ProductBoxTextureContourDetectionResponse> {
    return this.apiService.post<ProductBoxTextureContourDetectionResponse>(
      `/product-box-models/${id}/faces/${faceName}/texture/detect-contour`,
      {}
    );
  }

  processTexture(
    id: string,
    faceName: ProductBoxFaceName,
    request: ProductBoxTextureProcessRequest
  ): Observable<ProductBoxModelFace> {
    return this.apiService.post<ProductBoxModelFace>(
      `/product-box-models/${id}/faces/${faceName}/texture/process`,
      request
    );
  }

  acceptProcessedTexture(id: string, faceName: ProductBoxFaceName): Observable<ProductBoxModelFace> {
    return this.apiService.post<ProductBoxModelFace>(
      `/product-box-models/${id}/faces/${faceName}/texture/accept`,
      {}
    );
  }

  deleteTexture(id: string, faceName: ProductBoxFaceName): Observable<void> {
    return this.apiService.delete<void>(`/product-box-models/${id}/faces/${faceName}/texture`);
  }

  deleteFace(id: string, faceName: ProductBoxFaceName): Observable<void> {
    return this.apiService.delete<void>(`/product-box-models/${id}/faces/${faceName}`);
  }

  private buildFaceFormData(
    file: File,
    rotationDegrees: number | null,
    flipHorizontal: boolean,
    flipVertical: boolean
  ): FormData {
    const formData = new FormData();
    formData.append('file', file);

    if (rotationDegrees !== null) {
      formData.append('rotationDegrees', String(rotationDegrees));
    }

    formData.append('flipHorizontal', String(flipHorizontal));
    formData.append('flipVertical', String(flipVertical));
    return formData;
  }
}
