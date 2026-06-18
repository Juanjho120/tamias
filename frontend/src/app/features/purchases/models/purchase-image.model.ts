export type PurchaseImageStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface PurchaseImage {
  id: string;
  parentId: string;
  originalFilename: string;
  s3Key: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: PurchaseImageStatus;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}

export interface PurchaseImageUploadResponse {
  id: string;
  parentId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: PurchaseImageStatus;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}
