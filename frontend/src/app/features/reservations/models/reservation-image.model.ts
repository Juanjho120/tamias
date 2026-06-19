export type ReservationImageStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';

export interface ReservationImage {
  id: string;
  parentId: string;
  originalFilename: string;
  s3Key: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: ReservationImageStatus;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}

export interface ReservationImageUploadResponse {
  id: string;
  parentId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: ReservationImageStatus;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}
