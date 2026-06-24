export type PaymentMethod = 'CREDIT' | 'DEBIT' | 'CASH' | 'BANK_TRANSFER';

export type PaymentStatus = 'ACTIVE' | 'DELETED';

export interface Payment {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  categoryId: string;
  categoryName: string;
  name: string;
  description: string | null;
  method: PaymentMethod;
  amount: number | null;
  responsible: string | null;
  payDate: string;
  status: PaymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentRequest {
  propertyId: string | null;
  categoryId: string;
  name: string;
  description: string | null;
  method: PaymentMethod;
  amount: number;
  responsible: string | null;
  payDate: string;
}

export interface PaymentFilters {
  propertyId?: string;
  categoryId?: string;
  method?: PaymentMethod | '';
  dateFrom?: string;
  dateTo?: string;
  search?: string;
  page: number;
  size: number;
  sort?: string;
}

export const PAYMENT_METHODS: PaymentMethod[] = [
  'CREDIT',
  'DEBIT',
  'CASH',
  'BANK_TRANSFER'
];
