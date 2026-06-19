import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  PRODUCT_BOX_UNITS,
  ProductBoxInventoryItemOption,
  ProductBoxModel,
  ProductBoxModelRequest,
  ProductBoxUnit
} from '../../models/product-box-model.model';

type FormMode = 'create' | 'edit';

@Component({
  selector: 'app-product-box-model-form-modal',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './product-box-model-form-modal.component.html'
})
export class ProductBoxModelFormModalComponent implements OnChanges {
  @Input() open = false;
  @Input() mode: FormMode = 'create';
  @Input() model: ProductBoxModel | null = null;
  @Input() saving = false;
  @Input() inventoryItems: ProductBoxInventoryItemOption[] = [];

  @Output() closed = new EventEmitter<void>();
  @Output() save = new EventEmitter<ProductBoxModelRequest>();

  readonly units = PRODUCT_BOX_UNITS;
  readonly submitted = signal(false);

  form: ProductBoxModelRequest = this.emptyForm();

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.open) {
      this.submitted.set(false);
      return;
    }

    if (changes['open'] || changes['model'] || changes['mode']) {
      this.form = this.model ? this.toForm(this.model) : this.emptyForm();
      this.submitted.set(false);
    }
  }

  close(): void {
    if (this.saving) {
      return;
    }

    this.closed.emit();
  }

  submit(): void {
    this.submitted.set(true);

    if (!this.isValid()) {
      return;
    }

    this.save.emit({
      inventoryItemId: this.form.inventoryItemId || null,
      purchaseItemId: this.form.purchaseItemId?.trim() || null,
      name: this.form.name.trim(),
      description: this.form.description?.trim() || null,
      width: Number(this.form.width),
      height: Number(this.form.height),
      depth: Number(this.form.depth),
      unit: this.form.unit
    });
  }

  isValid(): boolean {
    return !!this.form.name.trim()
      && this.isPositive(this.form.width)
      && this.isPositive(this.form.height)
      && this.isPositive(this.form.depth)
      && !!this.form.unit;
  }

  inventoryItemLabel(item: ProductBoxInventoryItemOption): string {
    return item.fullName || [item.name, item.brandName].filter(Boolean).join(' - ') || item.id;
  }

  private emptyForm(): ProductBoxModelRequest {
    return {
      inventoryItemId: null,
      purchaseItemId: null,
      name: '',
      description: null,
      width: null,
      height: null,
      depth: null,
      unit: 'cm'
    };
  }

  private toForm(model: ProductBoxModel): ProductBoxModelRequest {
    return {
      inventoryItemId: model.inventoryItemId,
      purchaseItemId: model.purchaseItemId,
      name: model.name,
      description: model.description,
      width: model.width,
      height: model.height,
      depth: model.depth,
      unit: model.unit as ProductBoxUnit
    };
  }

  private isPositive(value: number | null): boolean {
    return value !== null && Number(value) > 0;
  }
}
