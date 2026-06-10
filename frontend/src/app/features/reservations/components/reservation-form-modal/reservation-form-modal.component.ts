import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import {
  Reservation,
  ReservationGuestRequest,
  ReservationRequest,
  ReservationStatus,
  RESERVATION_STATUSES
} from '../../models/reservation.model';
import {
  ReservationPlatformOption,
  ReservationPropertyOption
} from '../../models/reservation-reference.model';

@Component({
  selector: 'app-reservation-form-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './reservation-form-modal.component.html'
})
export class ReservationFormModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() reservation: Reservation | null = null;
  @Input() properties: ReservationPropertyOption[] = [];
  @Input() platforms: ReservationPlatformOption[] = [];
  @Input() loading = false;

  @Output() save = new EventEmitter<ReservationRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly statuses = RESERVATION_STATUSES.filter((status) => status !== 'DELETED');
  readonly guests = signal<ReservationGuestRequest[]>([]);
  readonly editingGuestIndex = signal<number | null>(null);

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

  readonly primaryGuestName = computed(() => {
    const guest = this.guests().find((item) => item.primary);
    return guest?.fullName ?? '—';
  });

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
      suppliesDelivered: rawValue.suppliesDelivered,
      observations: rawValue.observations.trim() || null,
      reservationValue: rawValue.reservationValue === '' ? null : Number(rawValue.reservationValue),
      invoiceNumber: rawValue.invoiceNumber.trim() || null,
      invoiceSeries: rawValue.invoiceSeries.trim() || null,
      status: rawValue.status,
      guests: this.guests(),
      supplies: null
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

  removeGuest(index: number): void {
    const currentGuests = [...this.guests()];
    currentGuests.splice(index, 1);

    if (!currentGuests.some((item) => item.primary) && currentGuests.length > 0) {
      currentGuests[0].primary = true;
    }

    this.guests.set(currentGuests);
    this.cancelGuestEdit();
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

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  isGuestInvalid(controlName: keyof typeof this.guestForm.controls): boolean {
    const control = this.guestForm.controls[controlName];
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
      this.cancelGuestEdit();
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

    this.cancelGuestEdit();
  }
}
