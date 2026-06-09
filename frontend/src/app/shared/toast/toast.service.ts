import { Injectable, signal } from '@angular/core';
import { ToastMessage, ToastType } from './toast.model';

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private nextId = 1;

  private readonly messagesSignal = signal<ToastMessage[]>([]);
  readonly messages = this.messagesSignal.asReadonly();

  show(message: string, type: ToastType = 'info', title?: string): void {
    const toast: ToastMessage = {
      id: this.nextId++,
      type,
      message,
      title
    };

    this.messagesSignal.update((messages) => [...messages, toast]);

    window.setTimeout(() => this.dismiss(toast.id), 4500);
  }

  success(message: string, title?: string): void {
    this.show(message, 'success', title);
  }

  error(message: string, title?: string): void {
    this.show(message, 'danger', title);
  }

  warning(message: string, title?: string): void {
    this.show(message, 'warning', title);
  }

  info(message: string, title?: string): void {
    this.show(message, 'info', title);
  }

  dismiss(id: number): void {
    this.messagesSignal.update((messages) => messages.filter((message) => message.id !== id));
  }
}
