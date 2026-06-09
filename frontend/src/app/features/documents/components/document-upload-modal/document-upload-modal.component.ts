import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { DocumentType, DOCUMENT_TYPES, DocumentUploadRequest } from '../../models/document.model';
import { DocumentPropertyOption } from '../../models/document-reference.model';

@Component({
  selector: 'app-document-upload-modal',
  standalone: true,
  imports: [NgClass, ReactiveFormsModule, TranslatePipe],
  templateUrl: './document-upload-modal.component.html'
})
export class DocumentUploadModalComponent implements OnChanges {
  private readonly formBuilder = inject(FormBuilder);

  @Input() open = false;
  @Input() properties: DocumentPropertyOption[] = [];
  @Input() loading = false;

  @Output() upload = new EventEmitter<DocumentUploadRequest>();
  @Output() cancel = new EventEmitter<void>();

  readonly documentTypes = DOCUMENT_TYPES;
  readonly selectedFile = signal<File | null>(null);

  readonly form = this.formBuilder.nonNullable.group({
    propertyId: [''],
    documentType: this.formBuilder.nonNullable.control<DocumentType>('OTHER', [Validators.required]),
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['']
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['open'] && this.open) {
      this.resetForm();
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const file = this.selectedFile();

    if (!file) {
      return;
    }

    const rawValue = this.form.getRawValue();

    this.upload.emit({
      propertyId: rawValue.propertyId || null,
      documentType: rawValue.documentType,
      title: rawValue.title.trim(),
      description: rawValue.description.trim() || null,
      file
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    this.selectedFile.set(file);

    if (file && !this.form.controls.title.value) {
      this.form.controls.title.setValue(this.stripExtension(file.name));
    }
  }

  isInvalid(controlName: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[controlName];
    return control.invalid && (control.touched || control.dirty);
  }

  private resetForm(): void {
    this.form.reset({
      propertyId: '',
      documentType: 'OTHER',
      title: '',
      description: ''
    });
    this.selectedFile.set(null);

    const input = document.getElementById('document-upload-file') as HTMLInputElement | null;

    if (input) {
      input.value = '';
    }
  }

  private stripExtension(filename: string): string {
    const lastDot = filename.lastIndexOf('.');

    if (lastDot <= 0) {
      return filename;
    }

    return filename.substring(0, lastDot);
  }
}
