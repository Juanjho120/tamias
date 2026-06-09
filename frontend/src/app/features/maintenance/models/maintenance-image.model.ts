export type ImageStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface MaintenanceImage {
  id: string;
  parentId: string;
  originalFilename: string;
  s3Key: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: ImageStatus;
  createdAt: string;
  fileUrl: string | null;
  fileUrlExpiresIn: number | null;
}

export interface MaintenanceImageUploadResponse {
  id: string;
  parentId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: ImageStatus;
  createdAt: string;
  fileUrl: string | null;
  fileUrlExpiresIn: number | null;
}
