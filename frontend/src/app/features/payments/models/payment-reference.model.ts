export interface PaymentPropertyOption {
  id: string;
  name: string;
  address: string | null;
}

export interface PaymentCategoryOption {
  id: string;
  name: string;
  description: string | null;
  status: string;
}
