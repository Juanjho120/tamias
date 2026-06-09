export type ImageStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface PropertyImage {
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

export interface PropertyImageUploadResponse {
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
