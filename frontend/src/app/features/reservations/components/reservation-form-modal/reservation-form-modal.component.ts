import { DecimalPipe, NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  Reservation,
  ReservationGuestRequest,
  ReservationRequest,
  ReservationStatus,
  ReservationSupplyRequest,
  RESERVATION_STATUSES
} from '../../models/reservation.model';
import {
  ReservationInventoryItemOption,
  ReservationPlatformOption,
  ReservationPropertyOption
} from '../../models/reservation-reference.model';

@Component({
  selector: 'app-reservation-form-modal',
  standalone: true,
  imports: [DecimalPipe, NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './reservation-form-modal.component.html'
})
export class ReservationFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() reservation: Reservation | null = null;
  @Input() properties: ReservationPropertyOption[] = [];
  @Input() platforms: ReservationPlatformOption[] = [];
  @Input() inventoryItems: ReservationInventoryItemOption[] = [];
  @Input() loading = false;

  @Output() save = new EventEmitter<ReservationRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = RESERVATION_STATUSES.filter((status) => status !== 'DELETED');
  readonly guests = signal<ReservationGuestRequest[]>([]);
  readonly supplies = signal<ReservationSupplyRequest[]>([]);
  readonly editingGuestIndex = signal<number | null>(null);
  readonly editingSupplyIndex = signal<number | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    propertyId: ['', [Validators.required]],
    platformId: [''],
    reservationCode: ['', [Validators.maxLength(150)]],
    checkIn: ['', [Validators.required]],
    checkOut: ['', [Validators.required]],
    suppliesDelivered: [false],
    observations: [''],
    reservationValue: [''],
    invoiceNumber: ['', [Validators.maxLength(100)]],
    invoiceSeries: ['', [Validators.maxLength(100)]],
    status: this.formBuilder.nonNullable.control<ReservationStatus>('ACTIVE', [Validators.required])
  });

  readonly guestForm = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(150)]],
    phone: ['', [Validators.maxLength(50)]],
    primary: [false]
  });

  readonly supplyForm = this.formBuilder.nonNullable.group({
    inventoryItemId: ['', [Validators.required]],
    quantity: ['1', [Validators.required]],
    unit: [''],
    notes: ['']
  });

  readonly primaryGuestName = computed(() => {
    const guest = this.guests().find((item) => item.primary);
    return guest?.fullName ?? '—';
  });

  readonly suppliesCount = computed(() => this.supplies().length);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['reservation'] || changes['open']) {
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
      propertyId: rawValue.propertyId,
      platformId: rawValue.platformId || null,
      reservationCode: rawValue.reservationCode.trim() || null,
      checkIn: rawValue.checkIn,
      checkOut: rawValue.checkOut,
      suppliesDelivered: this.supplies().length > 0 || rawValue.suppliesDelivered,
      observations: rawValue.observations.trim() || null,
      reservationValue: rawValue.reservationValue === '' ? null : Number(rawValue.reservationValue),
      invoiceNumber: rawValue.invoiceNumber.trim() || null,
      invoiceSeries: rawValue.invoiceSeries.trim() || null,
      status: rawValue.status,
      guests: this.guests(),
      supplies: this.supplies()
    });
  }

  addOrUpdateGuest(): void {
    if (this.guestForm.invalid) {
      this.guestForm.markAllAsTouched();
      return;
    }

    const rawValue = this.guestForm.getRawValue();
    const guest: ReservationGuestRequest = {
      guestId: null,
      fullName: rawValue.fullName.trim(),
      phone: rawValue.phone.trim() || null,
      primary: rawValue.primary
    };

    const index = this.editingGuestIndex();
    const currentGuests = [...this.guests()];

    if (guest.primary) {
      currentGuests.forEach((item) => item.primary = false);
    }

    if (index === null) {
      if (currentGuests.length === 0) {
        guest.primary = true;
      }

      currentGuests.push(guest);
    } else {
      currentGuests[index] = guest;
    }

    if (!currentGuests.some((item) => item.primary) && currentGuests.length > 0) {
      currentGuests[0].primary = true;
    }

    this.guests.set(currentGuests);
    this.cancelGuestEdit();
  }

  addOrUpdateSupply(): void {
    if (this.supplyForm.invalid) {
      this.supplyForm.markAllAsTouched();
      return;
    }

    const rawValue = this.supplyForm.getRawValue();
    const quantity = Number(rawValue.quantity);

    if (!Number.isFinite(quantity) || quantity <= 0) {
      this.supplyForm.controls.quantity.setErrors({ min: true });
      this.supplyForm.controls.quantity.markAsTouched();
      return;
    }

    const inventoryItem = this.inventoryItems.find((item) => item.id === rawValue.inventoryItemId);
    const supply: ReservationSupplyRequest = {
      inventoryItemId: rawValue.inventoryItemId,
      quantity,
      unit: rawValue.unit.trim() || inventoryItem?.unit || null,
      notes: rawValue.notes.trim() || null
    };

    const index = this.editingSupplyIndex();
    const currentSupplies = [...this.supplies()];

    if (index === null) {
      currentSupplies.push(supply);
    } else {
      currentSupplies[index] = supply;
    }

    this.supplies.set(currentSupplies);
    this.form.controls.suppliesDelivered.setValue(currentSupplies.length > 0);
    this.cancelSupplyEdit();
  }

  editGuest(index: number): void {
    const guest = this.guests()[index];

    if (!guest) {
      return;
    }

    this.editingGuestIndex.set(index);
    this.guestForm.reset({
      fullName: guest.fullName ?? '',
      phone: guest.phone ?? '',
      primary: !!guest.primary
    });
  }

  editSupply(index: number): void {
    const supply = this.supplies()[index];

    if (!supply) {
      return;
    }

    this.editingSupplyIndex.set(index);
    this.supplyForm.reset({
      inventoryItemId: supply.inventoryItemId,
      quantity: String(supply.quantity),
      unit: supply.unit ?? '',
      notes: supply.notes ?? ''
    });
  }

  removeGuest(index: number): void {
    const currentGuests = [...this.guests()];
    currentGuests.splice(index, 1);

    if (!currentGuests.some((item) => item.primary) && currentGuests.length > 0) {
      currentGuests[0].primary = true;
    }

    this.guests.set(currentGuests);
    this.cancelGuestEdit();
  }

  removeSupply(index: number): void {
    const currentSupplies = [...this.supplies()];
    currentSupplies.splice(index, 1);

    this.supplies.set(currentSupplies);
    this.form.controls.suppliesDelivered.setValue(currentSupplies.length > 0);
    this.cancelSupplyEdit();
  }

  setPrimaryGuest(index: number): void {
    this.guests.set(this.guests().map((guest, currentIndex) => ({
      ...guest,
      primary: currentIndex === index
    })));
  }

  cancelGuestEdit(): void {
    this.editingGuestIndex.set(null);
    this.guestForm.reset({
      fullName: '',
      phone: '',
      primary: false
    });
  }

  cancelSupplyEdit(): void {
    this.editingSupplyIndex.set(null);
    this.supplyForm.reset({
      inventoryItemId: '',
      quantity: '1',
      unit: '',
      notes: ''
    });
  }

  onSupplyInventoryItemSelected(inventoryItemId: string): void {
    const inventoryItem = this.inventoryItems.find((item) => item.id === inventoryItemId);

    if (inventoryItem?.unit && !this.supplyForm.controls.unit.value) {
      this.supplyForm.controls.unit.setValue(inventoryItem.unit);
    }
  }

  supplyItemName(supply: ReservationSupplyRequest): string {
    return this.inventoryItems.find((item) => item.id === supply.inventoryItemId)?.name ?? '—';
  }

  supplyItemCode(supply: ReservationSupplyRequest): string {
    const item = this.inventoryItems.find((option) => option.id === supply.inventoryItemId);
    return item?.internalCode || item?.barcode || '—';
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  isGuestInvalid(controlName: keyof typeof this.guestForm.controls): boolean {
    const control = this.guestForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  isSupplyInvalid(controlName: keyof typeof this.supplyForm.controls): boolean {
    const control = this.supplyForm.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private patchForm(): void {
    if (!this.reservation) {
      this.form.reset({
        propertyId: '',
        platformId: '',
        reservationCode: '',
        checkIn: '',
        checkOut: '',
        suppliesDelivered: false,
        observations: '',
        reservationValue: '',
        invoiceNumber: '',
        invoiceSeries: '',
        status: 'ACTIVE'
      });
      this.guests.set([]);
      this.supplies.set([]);
      this.cancelGuestEdit();
      this.cancelSupplyEdit();
      return;
    }

    this.form.reset({
      propertyId: this.reservation.propertyId,
      platformId: this.reservation.platformId ?? '',
      reservationCode: this.reservation.reservationCode ?? '',
      checkIn: this.reservation.checkIn,
      checkOut: this.reservation.checkOut,
      suppliesDelivered: !!this.reservation.suppliesDelivered,
      observations: this.reservation.observations ?? '',
      reservationValue: this.reservation.reservationValue !== null && this.reservation.reservationValue !== undefined
        ? String(this.reservation.reservationValue)
        : '',
      invoiceNumber: this.reservation.invoiceNumber ?? '',
      invoiceSeries: this.reservation.invoiceSeries ?? '',
      status: this.reservation.status === 'DELETED' ? 'CANCELLED' : this.reservation.status
    });

    this.guests.set((this.reservation.guests ?? []).map((guest) => ({
      guestId: guest.guestId,
      fullName: guest.fullName,
      phone: guest.phone,
      primary: guest.primary
    })));

    this.supplies.set((this.reservation.supplies ?? []).map((supply) => ({
      inventoryItemId: supply.inventoryItemId,
      quantity: supply.quantity,
      unit: supply.unit,
      notes: supply.notes
    })));

    this.cancelGuestEdit();
    this.cancelSupplyEdit();
  }
}
