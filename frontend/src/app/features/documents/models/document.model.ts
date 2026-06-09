export type DocumentType =
  | 'HOUSE_RULES'
  | 'BATHROOM_RULES'
  | 'PROPERTY_SIGNS'
  | 'BLUEPRINT'
  | 'ELECTRICAL_PLAN'
  | 'PLUMBING_PLAN'
  | 'DRAINAGE_PLAN'
  | 'MANUAL'
  | 'OTHER';

export type DocumentProcessingStatus = 'PENDING' | 'PROCESSING' | 'PROCESSED' | 'FAILED';

export type DocumentStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface DocumentSummary {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  documentType: DocumentType;
  title: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  processingStatus: DocumentProcessingStatus;
  status: DocumentStatus;
  createdAt: string;
}

export interface DocumentDetail {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  documentType: DocumentType;
  title: string;
  description: string | null;
  originalFilename: string;
  s3Key: string | null;
  contentType: string;
  sizeBytes: number;
  processingStatus: DocumentProcessingStatus;
  status: DocumentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentUploadRequest {
  propertyId: string | null;
  documentType: DocumentType;
  title: string;
  description: string | null;
  file: File;
}

export interface DocumentFilters {
  propertyId?: string;
  documentType?: DocumentType | '';
  processingStatus?: DocumentProcessingStatus | '';
  status?: DocumentStatus | '';
  page: number;
  size: number;
  sort?: string;
}

export interface DocumentDownloadUrlResponse {
  url: string;
  expiresIn: number;
}

export interface DocumentProcessingResponse {
  id: string;
  processingStatus: DocumentProcessingStatus;
}

export interface DocumentIndexingResponse {
  documentId: string;
  processingStatus: DocumentProcessingStatus;
  indexedChunks: number;
}

export interface DocumentChunk {
  id: string;
  chunkIndex: number;
  content: string;
  tokenCount: number | null;
  vectorStoreCollection: string | null;
  vectorStoreId: string | null;
}

export const DOCUMENT_TYPES: DocumentType[] = [
  'HOUSE_RULES',
  'BATHROOM_RULES',
  'PROPERTY_SIGNS',
  'BLUEPRINT',
  'ELECTRICAL_PLAN',
  'PLUMBING_PLAN',
  'DRAINAGE_PLAN',
  'MANUAL',
  'OTHER'
];

export const DOCUMENT_PROCESSING_STATUSES: DocumentProcessingStatus[] = [
  'PENDING',
  'PROCESSING',
  'PROCESSED',
  'FAILED'
];

export const DOCUMENT_STATUSES: DocumentStatus[] = ['ACTIVE', 'INACTIVE', 'DELETED'];
