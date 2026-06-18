export interface InventoryItemImage {
  id: string;
  parentId: string;
  originalFilename: string;
  s3Key: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: string;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}

export interface InventoryItemImageUploadResponse {
  id: string;
  parentId: string;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  cover: boolean | null;
  status: string;
  createdAt: string;
  fileUrl: string;
  fileUrlExpiresIn: number;
}
