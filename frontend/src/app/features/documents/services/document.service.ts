import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  DocumentChunk,
  DocumentDetail,
  DocumentDownloadUrlResponse,
  DocumentFilters,
  DocumentIndexingResponse,
  DocumentProcessingResponse,
  DocumentSummary,
  DocumentUploadRequest
} from '../models/document.model';

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  constructor(private readonly apiService: ApiService) {
  }

  findAll(filters: DocumentFilters): Observable<PageResponse<DocumentSummary>> {
    return this.apiService.get<PageResponse<DocumentSummary>>('/documents', {
      propertyId: filters.propertyId,
      documentType: filters.documentType,
      processingStatus: filters.processingStatus,
      status: filters.status,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'createdAt,desc'
    });
  }

  findById(id: string): Observable<DocumentDetail> {
    return this.apiService.get<DocumentDetail>(`/documents/${id}`);
  }

  upload(request: DocumentUploadRequest): Observable<DocumentDetail> {
    const formData = new FormData();

    if (request.propertyId) {
      formData.append('propertyId', request.propertyId);
    }

    formData.append('documentType', request.documentType);
    formData.append('title', request.title);

    if (request.description) {
      formData.append('description', request.description);
    }

    formData.append('file', request.file);

    return this.apiService.post<DocumentDetail>('/documents', formData);
  }

  getDownloadUrl(id: string): Observable<DocumentDownloadUrlResponse> {
    return this.apiService.get<DocumentDownloadUrlResponse>(`/documents/${id}/download-url`);
  }

  process(id: string): Observable<DocumentProcessingResponse> {
    return this.apiService.post<DocumentProcessingResponse>(`/documents/${id}/process`, {});
  }

  index(id: string): Observable<DocumentIndexingResponse> {
    return this.apiService.post<DocumentIndexingResponse>(`/documents/${id}/index`, {});
  }

  findChunks(id: string): Observable<DocumentChunk[]> {
    return this.apiService.get<DocumentChunk[]>(`/documents/${id}/chunks`);
  }

  delete(id: string): Observable<void> {
    return this.apiService.delete<void>(`/documents/${id}`);
  }
}
