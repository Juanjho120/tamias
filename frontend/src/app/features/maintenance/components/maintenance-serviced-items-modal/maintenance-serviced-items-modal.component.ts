import { DecimalPipe } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { MaintenanceRecordServicedItem } from '../../models/maintenance-detail.model';
import { MaintenanceInventoryItemOption } from '../../models/maintenance-reference.model';
import { MaintenanceRecordSummary } from '../../models/maintenance-record.model';
import { MaintenanceDetailService } from '../../services/maintenance-detail.service';

@Component({
  selector: 'app-maintenance-serviced-items-modal',
  standalone: true,
  imports: [DecimalPipe, ReactiveFormsModule, TranslatePipe, ConfirmModalComponent],
  templateUrl: './maintenance-serviced-items-modal.component.html'
})
export class MaintenanceServicedItemsModalComponent implements OnChanges {
  private readonly maintenanceDetailService = inject(MaintenanceDetailService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() maintenanceRecord: MaintenanceRecordSummary | null = null;
  @Input() inventoryItemOptions: MaintenanceInventoryItemOption[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() servicedItemsChanged = new EventEmitter<void>();

  readonly servicedItems = signal<MaintenanceRecordServicedItem[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deleting = signal(false);
  readonly editingItem = signal<MaintenanceRecordServicedItem | null>(null);
  readonly itemToDelete = signal<MaintenanceRecordServicedItem | null>(null);

  readonly servicedItemForm = this.formBuilder.nonNullable.group({
    inventoryItemId: [''],
    itemNameSnapshot: [''],
    quantity: ['', [Validators.min(0.01)]],
    unit: [''],
    notes: ['']
  });

  readonly deleteMessage = computed(() => {
    const item = this.itemToDelete();
    if (!item) {
      return '';
    }

    return this.languageService.instant('maintenance.details.confirmRemoveServicedItemMessage', {
      name: this.servicedItemDisplayName(item)
    });
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['maintenanceRecord']) && this.open && this.maintenanceRecord) {
      this.loadServicedItems();
    }
  }

  requestClose(): void {
    if (this.loading() || this.saving() || this.deleting()) {
      return;
    }

    this.resetState();
    this.close.emit();
  }

  loadServicedItems(): void {
    const record = this.maintenanceRecord;
    if (!record) {
      return;
    }

    this.loading.set(true);
    this.maintenanceDetailService.findServicedItems(record.id).subscribe({
      next: (items) => {
        this.servicedItems.set(items);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.loadError')));
      }
    });
  }

  editItem(item: MaintenanceRecordServicedItem): void {
    this.editingItem.set(item);
    this.servicedItemForm.reset({
      inventoryItemId: item.inventoryItemId ?? '',
      itemNameSnapshot: item.itemNameSnapshot ?? '',
      quantity: item.quantity !== null && item.quantity !== undefined ? String(item.quantity) : '',
      unit: item.unit ?? '',
      notes: item.notes ?? ''
    });
  }

  cancelEdit(): void {
    if (this.saving()) {
      return;
    }

    this.editingItem.set(null);
    this.servicedItemForm.reset({
      inventoryItemId: '',
      itemNameSnapshot: '',
      quantity: '',
      unit: '',
      notes: ''
    });
  }

  saveItem(): void {
    const record = this.maintenanceRecord;
    if (!record) {
      return;
    }

    const rawValue = this.servicedItemForm.getRawValue();
    if (!rawValue.inventoryItemId && !rawValue.itemNameSnapshot.trim()) {
      this.toastService.warning(this.languageService.instant('maintenance.details.messages.servicedItemRequired'));
      return;
    }

    if (this.servicedItemForm.invalid) {
      this.servicedItemForm.markAllAsTouched();
      return;
    }

    const request = {
      inventoryItemId: rawValue.inventoryItemId || null,
      itemNameSnapshot: rawValue.itemNameSnapshot.trim() || null,
      quantity: rawValue.quantity === '' ? null : Number(rawValue.quantity),
      unit: rawValue.unit.trim() || null,
      notes: rawValue.notes.trim() || null
    };

    this.saving.set(true);
    const editingItem = this.editingItem();
    const saveRequest = editingItem
      ? this.maintenanceDetailService.updateServicedItem(record.id, editingItem.id, request)
      : this.maintenanceDetailService.addServicedItem(record.id, request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          editingItem
            ? this.languageService.instant('maintenance.details.messages.servicedItemUpdated')
            : this.languageService.instant('maintenance.details.messages.servicedItemAdded')
        );
        this.cancelEdit();
        this.servicedItemsChanged.emit();
        this.loadServicedItems();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.servicedItemSaveError')));
      }
    });
  }

  requestRemoveItem(item: MaintenanceRecordServicedItem): void {
    this.itemToDelete.set(item);
  }

  cancelDelete(): void {
    if (this.deleting()) {
      return;
    }

    this.itemToDelete.set(null);
  }

  confirmDelete(): void {
    const record = this.maintenanceRecord;
    const item = this.itemToDelete();

    if (!record || !item) {
      return;
    }

    this.deleting.set(true);
    this.maintenanceDetailService.removeServicedItem(record.id, item.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.itemToDelete.set(null);
        this.toastService.success(this.languageService.instant('maintenance.details.messages.servicedItemRemoved'));
        this.servicedItemsChanged.emit();
        this.loadServicedItems();
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('maintenance.details.messages.deleteError')));
      }
    });
  }

  onInventoryItemSelected(inventoryItemId: string): void {
    const inventoryItem = this.inventoryItemOptions.find((item) => item.id === inventoryItemId);
    if (inventoryItem?.unit && !this.servicedItemForm.controls.unit.value) {
      this.servicedItemForm.controls.unit.setValue(inventoryItem.unit);
    }
  }

  inventoryItemDisplayName(inventoryItem: MaintenanceInventoryItemOption): string {
    return inventoryItem.brandName ? `${inventoryItem.name} - ${inventoryItem.brandName}` : inventoryItem.name;
  }

  servicedItemDisplayName(item: MaintenanceRecordServicedItem): string {
    const inventoryItem = item.inventoryItemId
      ? this.inventoryItemOptions.find((candidate) => candidate.id === item.inventoryItemId)
      : null;

    if (inventoryItem) {
      return this.inventoryItemDisplayName(inventoryItem);
    }

    const baseName = item.inventoryItemName ?? item.itemNameSnapshot ?? '—';
    return item.inventoryItemBrandName ? `${baseName} - ${item.inventoryItemBrandName}` : baseName;
  }

  trackById(index: number, item: { id: string }): string {
    return item.id;
  }

  private resetState(): void {
    this.servicedItems.set([]);
    this.itemToDelete.set(null);
    this.editingItem.set(null);
    this.servicedItemForm.reset({ inventoryItemId: '', itemNameSnapshot: '', quantity: '', unit: '', notes: '' });
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
