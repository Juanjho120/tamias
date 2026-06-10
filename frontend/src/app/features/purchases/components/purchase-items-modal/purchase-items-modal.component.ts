import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { QuetzalCurrencyPipe } from '../../../../shared/pipes/quetzal-currency.pipe';
import { ToastService } from '../../../../shared/toast/toast.service';
import { PurchaseItem, PurchaseList, PurchaseListSummary } from '../../models/purchase-list.model';
import { PurchaseBrandOption, PurchaseInventoryItemOption } from '../../models/purchase-reference.model';
import { PurchaseListService } from '../../services/purchase-list.service';

@Component({
  selector: 'app-purchase-items-modal',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, ConfirmModalComponent, QuetzalCurrencyPipe],
  templateUrl: './purchase-items-modal.component.html'
})
export class PurchaseItemsModalComponent implements OnChanges {
  private readonly purchaseListService = inject(PurchaseListService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() purchaseListSummary: PurchaseListSummary | null = null;
  @Input() inventoryItems: PurchaseInventoryItemOption[] = [];
  @Input() materials: PurchaseInventoryItemOption[] = [];
  @Input() brands: PurchaseBrandOption[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() itemsChanged = new EventEmitter<void>();

  readonly purchaseList = signal<PurchaseList | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly itemToDelete = signal<PurchaseItem | null>(null);
  readonly editingItem = signal<PurchaseItem | null>(null);

  readonly itemForm = this.formBuilder.nonNullable.group({
    inventoryItemId: [''],
    brandId: [''],
    itemNameSnapshot: ['', [Validators.maxLength(150)]],
    quantity: [''],
    unit: ['', [Validators.maxLength(50)]],
    estimatedPrice: [''],
    purchased: [false],
    notes: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['purchaseListSummary']) && this.open && this.purchaseListSummary) {
      this.loadPurchaseList();
    }
  }

  requestClose(): void {
    if (this.loading() || this.saving() || this.deletingId()) {
      return;
    }

    this.resetState();
    this.close.emit();
  }

  loadPurchaseList(): void {
    const summary = this.purchaseListSummary;

    if (!summary) {
      return;
    }

    this.loading.set(true);

    this.purchaseListService.findById(summary.id).subscribe({
      next: (purchaseList) => {
        this.purchaseList.set(purchaseList);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.items.messages.loadError')));
      }
    });
  }

  saveItem(): void {
    const purchaseList = this.purchaseList();

    if (!purchaseList) {
      return;
    }

    const rawValue = this.itemForm.getRawValue();

    if (!rawValue.inventoryItemId && !rawValue.itemNameSnapshot.trim()) {
      this.itemForm.controls.itemNameSnapshot.setErrors({ required: true });
      this.itemForm.controls.itemNameSnapshot.markAsTouched();
      return;
    }

    if (this.itemForm.invalid) {
      this.itemForm.markAllAsTouched();
      return;
    }

    const request = {
      inventoryItemId: rawValue.inventoryItemId || null,
      brandId: rawValue.brandId || null,
      itemNameSnapshot: rawValue.itemNameSnapshot.trim() || null,
      quantity: rawValue.quantity === '' ? null : Number(rawValue.quantity),
      unit: rawValue.unit.trim() || null,
      estimatedPrice: rawValue.estimatedPrice === '' ? null : Number(rawValue.estimatedPrice),
      purchased: rawValue.purchased,
      notes: rawValue.notes.trim() || null
    };

    this.saving.set(true);

    const editingItem = this.editingItem();

    const saveRequest = editingItem
      ? this.purchaseListService.updateItem(purchaseList.id, editingItem.id, request)
      : this.purchaseListService.createItem(purchaseList.id, request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          editingItem
            ? this.languageService.instant('purchases.items.messages.updated')
            : this.languageService.instant('purchases.items.messages.created')
        );
        this.cancelEdit();
        this.itemsChanged.emit();
        this.loadPurchaseList();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.items.messages.saveError')));
      }
    });
  }

  editItem(item: PurchaseItem): void {
    this.editingItem.set(item);
    this.itemForm.reset({
      inventoryItemId: item.inventoryItemId ?? item.materialId ?? '',
      brandId: item.brandId ?? '',
      itemNameSnapshot: item.itemNameSnapshot ?? '',
      quantity: item.quantity !== null && item.quantity !== undefined ? String(item.quantity) : '',
      unit: item.unit ?? '',
      estimatedPrice: item.estimatedPrice !== null && item.estimatedPrice !== undefined ? String(item.estimatedPrice) : '',
      purchased: !!item.purchased,
      notes: item.notes ?? ''
    });
  }

  cancelEdit(): void {
    this.editingItem.set(null);
    this.itemForm.reset({
      inventoryItemId: '',
      brandId: '',
      itemNameSnapshot: '',
      quantity: '',
      unit: '',
      estimatedPrice: '',
      purchased: false,
      notes: ''
    });
  }

  togglePurchased(item: PurchaseItem): void {
    const purchaseList = this.purchaseList();

    if (!purchaseList) {
      return;
    }

    this.purchaseListService.updateItemPurchased(purchaseList.id, item.id, {
      purchased: !item.purchased
    }).subscribe({
      next: () => {
        this.toastService.success(this.languageService.instant('purchases.items.messages.purchasedUpdated'));
        this.itemsChanged.emit();
        this.loadPurchaseList();
      },
      error: (error: unknown) => {
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.items.messages.purchasedError')));
      }
    });
  }

  requestDeleteItem(item: PurchaseItem): void {
    this.itemToDelete.set(item);
  }

  cancelDelete(): void {
    if (this.deletingId()) {
      return;
    }

    this.itemToDelete.set(null);
  }

  confirmDelete(): void {
    const purchaseList = this.purchaseList();
    const item = this.itemToDelete();

    if (!purchaseList || !item) {
      return;
    }

    this.deletingId.set(item.id);

    this.purchaseListService.deleteItem(purchaseList.id, item.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.itemToDelete.set(null);
        this.toastService.success(this.languageService.instant('purchases.items.messages.deleted'));
        this.itemsChanged.emit();
        this.loadPurchaseList();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('purchases.items.messages.deleteError')));
      }
    });
  }

  onInventoryItemSelected(inventoryItemId: string): void {
    const inventoryItems = this.inventoryItems.length ? this.inventoryItems : this.materials;
    const material = inventoryItems.find((item) => item.id === inventoryItemId);

    if (material?.unit && !this.itemForm.controls.unit.value) {
      this.itemForm.controls.unit.setValue(material.unit);
    }

    if (material?.name && !this.itemForm.controls.itemNameSnapshot.value) {
      this.itemForm.controls.itemNameSnapshot.setValue(material.name);
    }
  }

  itemDisplayName(item: PurchaseItem): string {
    return item.inventoryItemName ?? item.materialName ?? item.itemNameSnapshot ?? '—';
  }

  trackById(index: number, item: { id: string }): string {
    return item.id;
  }

  private resetState(): void {
    this.purchaseList.set(null);
    this.itemToDelete.set(null);
    this.cancelEdit();
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
