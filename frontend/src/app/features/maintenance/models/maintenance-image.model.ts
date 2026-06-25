export type ImageStatus = 'ACTIVE' | 'INACTIVE' | 'DELETED';
export type MaintenanceImageRole = 'BEFORE' | 'AFTER' | 'GENERAL';

export interface MaintenanceImage {
  id: string;
  parentId: string;
  originalFilename: string;
  s3Key: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  imageRole: MaintenanceImageRole | null;
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
  imageRole: MaintenanceImageRole | null;
  status: ImageStatus;
  createdAt: string;
  fileUrl: string | null;
  fileUrlExpiresIn: number | null;
}

export interface MaintenanceImageRoleRequest {
  imageRole: MaintenanceImageRole;
}
