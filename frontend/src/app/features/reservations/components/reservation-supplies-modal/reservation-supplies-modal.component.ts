import { DecimalPipe, NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { ApiError } from '../../../../core/models/api-error.model';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ToastService } from '../../../../shared/toast/toast.service';
import { ReservationSummary, ReservationSupply, ReservationSupplyRequest } from '../../models/reservation.model';
import { ReservationInventoryItemOption } from '../../models/reservation-reference.model';
import { ReservationService } from '../../services/reservation.service';

@Component({
  selector: 'app-reservation-supplies-modal',
  standalone: true,
  imports: [DecimalPipe, NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './reservation-supplies-modal.component.html'
})
export class ReservationSuppliesModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);
  private readonly reservationService = inject(ReservationService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  @Input() open = false;
  @Input() reservationSummary: ReservationSummary | null = null;
  @Input() inventoryItems: ReservationInventoryItemOption[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() changed = new EventEmitter<void>();

  readonly supplies = signal<ReservationSupply[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly editingSupply = signal<ReservationSupply | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    inventoryItemId: ['', [Validators.required]],
    quantity: ['1', [Validators.required]],
    unit: [''],
    notes: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['open'] || changes['reservationSummary']) && this.open && this.reservationSummary) {
      this.loadSupplies();
      this.resetForm();
    }

    if (changes['open'] && !this.open) {
      this.supplies.set([]);
      this.resetForm();
    }
  }

  loadSupplies(): void {
    const reservation = this.reservationSummary;
    if (!reservation) {
      return;
    }

    this.loading.set(true);
    this.reservationService.findSupplies(reservation.id).subscribe({
      next: (supplies) => {
        this.supplies.set(supplies);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.loading.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.supplies.messages.loadError')));
      }
    });
  }

  submit(): void {
    const reservation = this.reservationSummary;
    if (!reservation) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.buildRequest();
    const editingSupply = this.editingSupply();
    this.saving.set(true);
    const saveRequest = editingSupply
      ? this.reservationService.updateSupply(reservation.id, editingSupply.id, request)
      : this.reservationService.addSupply(reservation.id, request);

    saveRequest.subscribe({
      next: () => {
        this.saving.set(false);
        this.toastService.success(
          editingSupply
            ? this.languageService.instant('reservations.supplies.messages.updated')
            : this.languageService.instant('reservations.supplies.messages.created')
        );
        this.resetForm();
        this.loadSupplies();
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.supplies.messages.saveError')));
      }
    });
  }

  editSupply(supply: ReservationSupply): void {
    this.editingSupply.set(supply);
    this.form.reset({
      inventoryItemId: supply.inventoryItemId,
      quantity: String(supply.quantity),
      unit: supply.unit ?? '',
      notes: supply.notes ?? ''
    });
  }

  deleteSupply(supply: ReservationSupply): void {
    const reservation = this.reservationSummary;
    if (!reservation) {
      return;
    }

    this.deletingId.set(supply.id);
    this.reservationService.deleteSupply(reservation.id, supply.id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.toastService.success(this.languageService.instant('reservations.supplies.messages.deleted'));
        this.loadSupplies();
        this.changed.emit();
      },
      error: (error: unknown) => {
        this.deletingId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('reservations.supplies.messages.deleteError')));
      }
    });
  }

  resetForm(): void {
    this.editingSupply.set(null);
    this.form.reset({
      inventoryItemId: '',
      quantity: '1',
      unit: '',
      notes: ''
    });
  }

  onInventoryItemSelected(inventoryItemId: string): void {
    const inventoryItem = this.inventoryItems.find((item) => item.id === inventoryItemId);
    if (inventoryItem?.unit && !this.form.controls.unit.value) {
      this.form.controls.unit.setValue(inventoryItem.unit);
    }
  }

  inventoryItemDisplayName(item: ReservationInventoryItemOption): string {
    return item.brandName ? `${item.name} - ${item.brandName}` : item.name;
  }

  itemDisplayName(supply: ReservationSupply): string {
    const baseName = supply.inventoryItemName || supply.itemNameSnapshot || '—';
    return supply.brandName ? `${baseName} - ${supply.brandName}` : baseName;
  }

  itemCodeLabel(supply: ReservationSupply): string {
    return supply.internalCode || supply.internalCodeSnapshot || supply.barcode || supply.barcodeSnapshot || '—';
  }

  selectedReservationTitle(): string {
    const reservation = this.reservationSummary;
    if (!reservation) {
      return '—';
    }

    return reservation.reservationCode || `${reservation.propertyName} ${reservation.checkIn}`;
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  trackById(_: number, item: ReservationSupply): string {
    return item.id;
  }

  private buildRequest(): ReservationSupplyRequest {
    const rawValue = this.form.getRawValue();
    const quantity = Number(rawValue.quantity);
    const inventoryItem = this.inventoryItems.find((item) => item.id === rawValue.inventoryItemId);

    return {
      inventoryItemId: rawValue.inventoryItemId,
      quantity,
      unit: rawValue.unit.trim() || inventoryItem?.unit || null,
      notes: rawValue.notes.trim() || null
    };
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }
}
