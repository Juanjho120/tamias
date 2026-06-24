export type PaymentImageStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface PaymentImage {
  id: string;
  parentId: string;
  originalFilename: string;
  s3Key: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: PaymentImageStatus;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}

export interface PaymentImageUploadResponse {
  id: string;
  parentId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: PaymentImageStatus;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}
