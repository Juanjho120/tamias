import { DatePipe, DecimalPipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { DocumentChunksModalComponent } from '../../components/document-chunks-modal/document-chunks-modal.component';
import { DocumentUploadModalComponent } from '../../components/document-upload-modal/document-upload-modal.component';
import {
  DocumentChunk,
  DocumentProcessingStatus,
  DocumentStatus,
  DocumentSummary,
  DocumentType,
  DocumentUploadRequest,
  DOCUMENT_PROCESSING_STATUSES,
  DOCUMENT_STATUSES,
  DOCUMENT_TYPES
} from '../../models/document.model';
import { DocumentPropertyOption } from '../../models/document-reference.model';
import { DocumentReferenceDataService } from '../../services/document-reference-data.service';
import { DocumentService } from '../../services/document.service';

@Component({
  selector: 'app-documents-page',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    FormsModule,
    NgClass,
    TranslatePipe,
    ConfirmModalComponent,
    DocumentChunksModalComponent,
    DocumentUploadModalComponent
  ],
  templateUrl: './documents-page.component.html'
})
export class DocumentsPageComponent implements OnInit {
  private readonly documentService = inject(DocumentService);
  private readonly referenceDataService = inject(DocumentReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly documentTypes = DOCUMENT_TYPES;
  readonly processingStatuses = DOCUMENT_PROCESSING_STATUSES;
  readonly statuses = DOCUMENT_STATUSES;

  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly loadingReferences = signal(false);
  readonly processingId = signal<string | null>(null);
  readonly indexingId = signal<string | null>(null);
  readonly downloadId = signal<string | null>(null);
  readonly deletingId = signal<string | null>(null);
  readonly loadingChunks = signal(false);

  readonly documents = signal<DocumentSummary[]>([]);
  readonly properties = signal<DocumentPropertyOption[]>([]);
  readonly documentToDelete = signal<DocumentSummary | null>(null);
  readonly selectedDocumentForChunks = signal<DocumentSummary | null>(null);
  readonly chunks = signal<DocumentChunk[]>([]);
  readonly uploadModalVisible = signal(false);

  readonly propertyId = signal('');
  readonly documentType = signal<DocumentType | ''>('');
  readonly processingStatus = signal<DocumentProcessingStatus | ''>('');
  readonly status = signal<DocumentStatus | ''>('ACTIVE');
  readonly page = signal(0);
  readonly size = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly first = signal(true);
  readonly last = signal(true);

  readonly pageLabel = computed(() => {
    if (this.totalElements() === 0) {
      return this.languageService.instant('documents.pagination.noItems');
    }

    return this.languageService.instant('documents.pagination.pageOf', {
      page: this.page() + 1,
      totalPages: this.totalPages()
    });
  });

  readonly deleteMessage = computed(() => {
    const document = this.documentToDelete();

    if (!document) {
      return '';
    }

    return this.languageService.instant('documents.confirmDeleteMessage', {
      title: document.title
    });
  });

  ngOnInit(): void {
    this.loadProperties();
    this.loadDocuments();
  }

  loadProperties(): void {
    this.loadingReferences.set(true);

    this.referenceDataService.loadProperties().subscribe({
      next: (properties) => {
        this.properties.set(properties);
        this.loadingReferences.set(false);
      },
      error: (error: unknown) => {
        this.loadingReferences.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.referencesError')));
      }
    });
  }

  loadDocuments(): void {
    this.loading.set(true);

    this.documentService.findAll({
      propertyId: this.propertyId() || undefined,
      documentType: this.documentType(),
      processingStatus: this.processingStatus(),
      status: this.status(),
      page: this.page(),
      size: this.size(),
      sort: 'createdAt,desc'
    }).subscribe({
      next: (response: PageResponse<DocumentSummary>) => {
        this.documents.set(response.content);
        this.page.set(response.page);
        this.size.set(response.size);
        this.totalElements.set(response.totalElements);
        this.totalPages.set(response.totalPages);
        this.first.set(response.first);
        this.last.set(response.last);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.loadError')));
      }
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.loadDocuments();
  }

  clearFilters(): void {
    this.propertyId.set('');
    this.documentType.set('');
    this.processingStatus.set('');
    this.status.set('ACTIVE');
    this.page.set(0);
    this.loadDocuments();
  }

  previousPage(): void {
    if (this.first()) {
      return;
    }

    this.page.update((value) => value - 1);
    this.loadDocuments();
  }

  nextPage(): void {
    if (this.last()) {
      return;
    }

    this.page.update((value) => value + 1);
    this.loadDocuments();
  }

  changePageSize(value: string): void {
    this.size.set(Number(value));
    this.page.set(0);
    this.loadDocuments();
  }

  openUploadModal(): void {
    this.uploadModalVisible.set(true);
  }

  closeUploadModal(): void {
    if (this.uploading()) {
      return;
    }

    this.uploadModalVisible.set(false);
  }

  uploadDocument(request: DocumentUploadRequest): void {
    this.uploading.set(true);

    this.documentService.upload(request).subscribe({
      next: () => {
        this.uploading.set(false);
        this.toastService.success(this.languageService.instant('documents.messages.uploaded'));
        this.closeUploadModal();
        this.loadDocuments();
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.uploadError')));
      }
    });
  }

  download(document: DocumentSummary): void {
    this.downloadId.set(document.id);

    this.documentService.getDownloadUrl(document.id).subscribe({
      next: (response) => {
        this.downloadId.set(null);
        window.open(response.url, '_blank', 'noopener,noreferrer');
      },
      error: (error: unknown) => {
        this.downloadId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.downloadError')));
      }
    });
  }

  process(document: DocumentSummary): void {
    this.processingId.set(document.id);

    this.documentService.process(document.id).subscribe({
      next: () => {
        this.processingId.set(null);
        this.toastService.success(this.languageService.instant('documents.messages.processed'));
        this.loadDocuments();
      },
      error: (error: unknown) => {
        this.processingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.processError')));
      }
    });
  }

  index(document: DocumentSummary): void {
    this.indexingId.set(document.id);

    this.documentService.index(document.id).subscribe({
      next: (response) => {
        this.indexingId.set(null);
        this.toastService.success(this.languageService.instant('documents.messages.indexed', {
          chunks: response.indexedChunks
        }));
        this.loadDocuments();
      },
      error: (error: unknown) => {
        this.indexingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.indexError')));
      }
    });
  }

  openChunks(document: DocumentSummary): void {
    this.selectedDocumentForChunks.set(document);
    this.chunks.set([]);
    this.loadingChunks.set(true);

    this.documentService.findChunks(document.id).subscribe({
      next: (chunks) => {
        this.chunks.set([...chunks].sort((a, b) => a.chunkIndex - b.chunkIndex));
        this.loadingChunks.set(false);
      },
      error: (error: unknown) => {
        this.loadingChunks.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.chunksError')));
      }
    });
  }

  closeChunks(): void {
    if (this.loadingChunks()) {
      return;
    }

    this.selectedDocumentForChunks.set(null);
    this.chunks.set([]);
  }

  requestDelete(document: DocumentSummary): void {
    this.documentToDelete.set(document);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.documentToDelete.set(null);
  }

  confirmDelete(): void {
    const document = this.documentToDelete();

    if (!document) {
      return;
    }

    this.deletingId.set(document.id);

    this.documentService.delete(document.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.documentToDelete.set(null);
        this.toastService.success(this.languageService.instant('documents.messages.deleted'));
        this.loadDocuments();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('documents.messages.deleteError')));
      }
    });
  }

  processingBadgeClass(status: DocumentProcessingStatus): string {
    switch (status) {
      case 'PENDING':
        return 'text-bg-secondary';
      case 'PROCESSING':
        return 'text-bg-info';
      case 'PROCESSED':
        return 'text-bg-success';
      case 'FAILED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  statusBadgeClass(status: DocumentStatus): string {
    switch (status) {
      case 'ACTIVE':
        return 'text-bg-success';
      case 'INACTIVE':
        return 'text-bg-secondary';
      case 'DELETED':
        return 'text-bg-danger';
      default:
        return 'text-bg-secondary';
    }
  }

  canProcess(document: DocumentSummary): boolean {
    return document.processingStatus !== 'PROCESSING';
  }

  canIndex(document: DocumentSummary): boolean {
    return document.processingStatus === 'PROCESSED';
  }

  sizeInMb(sizeBytes: number): number {
    return sizeBytes / 1024 / 1024;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
