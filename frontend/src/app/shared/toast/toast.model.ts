export type ToastType = 'success' | 'danger' | 'warning' | 'info';

export interface ToastMessage {
  id: number;
  type: ToastType;
  message: string;
  title?: string;
}
