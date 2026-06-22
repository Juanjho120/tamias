export type ProductBoxUnit = 'cm' | 'mm' | 'in';
export type ProductBoxFaceName = 'front' | 'back' | 'left' | 'right' | 'top' | 'bottom';
export type ProductBoxTextureStatus = 'UPLOADED' | 'POINTS_SELECTED' | 'PROCESSED' | 'ACCEPTED' | 'FAILED';
export type ProductBoxTextureEnhancementMode = 'none' | 'basic' | 'strong';
export type ProductBoxAiEnhancementStatus = 'NOT_REQUESTED' | 'REQUESTED' | 'PROCESSING' | 'GENERATED' | 'ACCEPTED' | 'FAILED';
export type ProductBoxActiveTextureSource = 'unknown' | 'direct_upload' | 'opencv_processed' | 'ai_enhanced';

export const PRODUCT_BOX_UNITS: ProductBoxUnit[] = ['cm', 'mm', 'in'];
export const PRODUCT_BOX_FACE_NAMES: ProductBoxFaceName[] = ['front', 'back', 'left', 'right', 'top', 'bottom'];
export const PRODUCT_BOX_TEXTURE_ENHANCEMENT_MODES: ProductBoxTextureEnhancementMode[] = ['none', 'basic', 'strong'];

export interface ProductBoxRuntimeCapabilities {
  opencvEnabled: boolean;
  aiTextureEnhancementEnabled: boolean;
  opencvDisabledMessage: string | null;
  aiTextureEnhancementDisabledMessage: string | null;
}

export interface ProductBoxTexturePoint {
  x: number;
  y: number;
}

export interface ProductBoxTextureProcessRequest {
  topLeft: ProductBoxTexturePoint;
  topRight: ProductBoxTexturePoint;
  bottomRight: ProductBoxTexturePoint;
  bottomLeft: ProductBoxTexturePoint;
  enhancementMode?: ProductBoxTextureEnhancementMode | null;
}

export interface ProductBoxTextureContourDetectionResponse {
  detected: boolean;
  confidence: number | null;
  points: ProductBoxTextureProcessRequest | null;
  message: string | null;
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
  autoDetectedPoints: boolean | null;
  contourConfidence: number | null;
  enhancementMode: ProductBoxTextureEnhancementMode | null;
  aiEnhancedImageKey: string | null;
  aiEnhancedFilepath: string | null;
  aiEnhancedFilename: string | null;
  aiEnhancedContentType: string | null;
  aiEnhancedSizeBytes: number | null;
  aiEnhancedWidthPx: number | null;
  aiEnhancedHeightPx: number | null;
  aiEnhancedImageUrl: string | null;
  aiEnhancementStatus: ProductBoxAiEnhancementStatus | null;
  aiEnhancementProvider: string | null;
  aiEnhancementModel: string | null;
  aiEnhancementPromptVersion: string | null;
  aiEnhancementError: string | null;
  aiEnhancedAt: string | null;
  activeTextureSource: ProductBoxActiveTextureSource | null;
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
