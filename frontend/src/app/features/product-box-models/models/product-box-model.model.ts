export type ProductBoxUnit = 'cm' | 'mm' | 'in';

export type ProductBoxFaceName = 'front' | 'back' | 'left' | 'right' | 'top' | 'bottom';

export const PRODUCT_BOX_UNITS: ProductBoxUnit[] = ['cm', 'mm', 'in'];

export const PRODUCT_BOX_FACE_NAMES: ProductBoxFaceName[] = ['front', 'back', 'left', 'right', 'top', 'bottom'];

export interface ProductBoxModelFace {
  id: string;
  faceName: ProductBoxFaceName;
  imageKey: string;
  filepath: string | null;
  originalFilename: string | null;
  contentType: string | null;
  sizeBytes: number | null;
  rotationDegrees: number | null;
  flipHorizontal: boolean;
  flipVertical: boolean;
  imageUrl: string | null;
  imageUrlExpiresIn: number | null;
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
