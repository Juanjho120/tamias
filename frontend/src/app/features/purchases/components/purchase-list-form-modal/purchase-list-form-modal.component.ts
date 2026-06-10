import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { QuetzalCurrencyPipe } from '../../../../shared/pipes/quetzal-currency.pipe';
import {
  PurchaseItemRequest,
  PurchaseList,
  PurchaseListRequest,
  PurchaseListStatus,
  PURCHASE_LIST_STATUSES
} from '../../models/purchase-list.model';
import {
  PurchaseBrandOption,
  PurchaseCityOption,
  PurchaseInventoryItemOption,
  PurchasePropertyOption,
  PurchaseSupplierOption
} from '../../models/purchase-reference.model';

@Component({
  selector: 'app-purchase-list-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe, QuetzalCurrencyPipe],
  templateUrl: './purchase-list-form-modal.component.html'
})
export class PurchaseListFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() purchaseList: PurchaseList | null = null;
  @Input() properties: PurchasePropertyOption[] = [];
  @Input() cities: PurchaseCityOption[] = [];
  @Input() suppliers: PurchaseSupplierOption[] = [];
  @Input() inventoryItems: PurchaseInventoryItemOption[] = [];
  @Input() brands: PurchaseBrandOption[] = [];
  @Input() loading = false;

  @Output() save = new EventEmitter<PurchaseListRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = PURCHASE_LIST_STATUSES.filter((status) => status !== 'DELETED');
  readonly items = signal<PurchaseItemRequest[]>([]);
  readonly editingItemIndex = signal<number | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    propertyId: [''],
    cityId: [''],
    supplierId: [''],
    purchaseDate: ['', [Validators.required]],
    notes: [''],
    status: this.formBuilder.nonNullable.control<PurchaseListStatus>('OPEN', [Validators.required])
  });

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

  readonly estimatedTotal = computed(() => this.items().reduce((total, item) => total + (item.estimatedPrice ?? 0), 0));

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['purchaseList'] || changes['open']) {
      this.patchForm();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();

    this.save.emit({
      propertyId: rawValue.propertyId || null,
      cityId: rawValue.cityId || null,
      supplierId: rawValue.supplierId || null,
      purchaseDate: rawValue.purchaseDate,
      notes: rawValue.notes.trim() || null,
      status: rawValue.status,
      items: this.items()
    });
  }

  addOrUpdateItem(): void {
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

    const item: PurchaseItemRequest = {
      inventoryItemId: rawValue.inventoryItemId || null,
      brandId: rawValue.brandId || null,
      itemNameSnapshot: rawValue.itemNameSnapshot.trim() || null,
      quantity: rawValue.quantity === '' ? null : Number(rawValue.quantity),
      unit: rawValue.unit.trim() || null,
      estimatedPrice: rawValue.estimatedPrice === '' ? null : Number(rawValue.estimatedPrice),
      purchased: rawValue.purchased,
      notes: rawValue.notes.trim() || null
    };

    const currentItems = [...this.items()];
    const index = this.editingItemIndex();

    if (index === null) {
      currentItems.push(item);
    } else {
      currentItems[index] = item;
    }

    this.items.set(currentItems);
    this.cancelItemEdit();
  }

  editItem(index: number): void {
    const item = this.items()[index];

    if (!item) {
      return;
    }

    this.editingItemIndex.set(index);
    this.itemForm.reset({
      inventoryItemId: item.inventoryItemId ?? '',
      brandId: item.brandId ?? '',
      itemNameSnapshot: item.itemNameSnapshot ?? '',
      quantity: item.quantity !== null && item.quantity !== undefined ? String(item.quantity) : '',
      unit: item.unit ?? '',
      estimatedPrice: item.estimatedPrice !== null && item.estimatedPrice !== undefined ? String(item.estimatedPrice) : '',
      purchased: !!item.purchased,
      notes: item.notes ?? ''
    });
  }

  removeItem(index: number): void {
    const currentItems = [...this.items()];
    currentItems.splice(index, 1);
    this.items.set(currentItems);
    this.cancelItemEdit();
  }

  cancelItemEdit(): void {
    this.editingItemIndex.set(null);
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

  onInventoryItemSelected(inventoryItemId: string): void {
    const inventoryItem = this.inventoryItems.find((item) => item.id === inventoryItemId);

    if (inventoryItem?.unit && !this.itemForm.controls.unit.value) {
      this.itemForm.controls.unit.setValue(inventoryItem.unit);
    }

    if (inventoryItem?.name && !this.itemForm.controls.itemNameSnapshot.value) {
      this.itemForm.controls.itemNameSnapshot.setValue(inventoryItem.name);
    }
  }

  itemDisplayName(item: PurchaseItemRequest): string {
    const inventoryItem = item.inventoryItemId ? this.inventoryItems.find((candidate) => candidate.id === item.inventoryItemId) : null;
    return inventoryItem?.name ?? item.itemNameSnapshot ?? '—';
  }

  brandDisplayName(item: PurchaseItemRequest): string {
    const brand = item.brandId ? this.brands.find((candidate) => candidate.id === item.brandId) : null;
    return brand?.name ?? '—';
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  isItemInvalid(controlName: keyof typeof this.itemForm.controls): boolean {
    const control = this.itemForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.purchaseList) {
      this.form.reset({
        propertyId: '',
        cityId: '',
        supplierId: '',
        purchaseDate: this.today(),
        notes: '',
        status: 'OPEN'
      });
      this.items.set([]);
      this.cancelItemEdit();
      return;
    }

    this.form.reset({
      propertyId: this.purchaseList.propertyId ?? '',
      cityId: this.purchaseList.cityId ?? '',
      supplierId: this.purchaseList.supplierId ?? '',
      purchaseDate: this.purchaseList.purchaseDate,
      notes: this.purchaseList.notes ?? '',
      status: this.purchaseList.status === 'DELETED' ? 'CANCELLED' : this.purchaseList.status
    });

    this.items.set((this.purchaseList.items ?? []).map((item) => ({
      inventoryItemId: item.inventoryItemId ?? null,
      brandId: item.brandId,
      itemNameSnapshot: item.itemNameSnapshot,
      quantity: item.quantity,
      unit: item.unit,
      estimatedPrice: item.estimatedPrice,
      purchased: item.purchased,
      notes: item.notes
    })));

    this.cancelItemEdit();
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
