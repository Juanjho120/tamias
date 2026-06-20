export type ProductBoxUnit = 'cm' | 'mm' | 'in';
export type ProductBoxFaceName = 'front' | 'back' | 'left' | 'right' | 'top' | 'bottom';
export type ProductBoxTextureStatus = 'UPLOADED' | 'POINTS_SELECTED' | 'PROCESSED' | 'ACCEPTED' | 'FAILED';

export const PRODUCT_BOX_UNITS: ProductBoxUnit[] = ['cm', 'mm', 'in'];
export const PRODUCT_BOX_FACE_NAMES: ProductBoxFaceName[] = ['front', 'back', 'left', 'right', 'top', 'bottom'];

export interface ProductBoxTexturePoint {
  x: number;
  y: number;
}

export interface ProductBoxTextureProcessRequest {
  topLeft: ProductBoxTexturePoint;
  topRight: ProductBoxTexturePoint;
  bottomRight: ProductBoxTexturePoint;
  bottomLeft: ProductBoxTexturePoint;
}

export interface ProductBoxModelFace {
  id: string;
  faceName: ProductBoxFaceName;
  imageKey: string | null;
  filepath: string | null;
  originalFilename: string | null;
  contentType: string | null;
  sizeBytes: number | null;
  rotationDegrees: number | null;
  flipHorizontal: boolean;
  flipVertical: boolean;
  imageUrl: string | null;
  imageUrlExpiresIn: number | null;

  originalImageKey: string | null;
  originalFilepath: string | null;
  originalUploadFilename: string | null;
  originalContentType: string | null;
  originalSizeBytes: number | null;
  originalWidthPx: number | null;
  originalHeightPx: number | null;
  originalImageUrl: string | null;

  processedImageKey: string | null;
  processedFilepath: string | null;
  processedFilename: string | null;
  processedContentType: string | null;
  processedSizeBytes: number | null;
  processedWidthPx: number | null;
  processedHeightPx: number | null;
  processedImageUrl: string | null;

  targetAspectRatio: number | null;
  pointsJson: string | null;
  textureStatus: ProductBoxTextureStatus | null;
  processingError: string | null;
  processedAt: string | null;
  acceptedAt: string | null;

  createdAt: string;
  updatedAt: string | null;
}

export interface ProductBoxModelSummary {
  id: string;
  inventoryItemId: string | null;
  inventoryItemName: string | null;
  purchaseItemId: string | null;
  purchaseItemName: string | null;
  name: string;
  description: string | null;
  width: number;
  height: number;
  depth: number;
  unit: ProductBoxUnit;
  createdAt: string;
  updatedAt: string | null;
  faceCount?: number | null;
  faces?: Partial<Record<ProductBoxFaceName, ProductBoxModelFace>> | null;
}

export interface ProductBoxModel extends ProductBoxModelSummary {
  faces: Partial<Record<ProductBoxFaceName, ProductBoxModelFace>>;
}

export interface ProductBoxModelRequest {
  inventoryItemId: string | null;
  purchaseItemId: string | null;
  name: string;
  description: string | null;
  width: number | null;
  height: number | null;
  depth: number | null;
  unit: ProductBoxUnit;
}

export interface ProductBoxModelFilters {
  inventoryItemId?: string;
  purchaseItemId?: string;
  search?: string;
  page: number;
  size: number;
  sort?: string;
}

export interface ProductBoxInventoryItemOption {
  id: string;
  name?: string;
  fullName?: string;
  brandName?: string | null;
  itemType?: string | null;
  status: string;
}
