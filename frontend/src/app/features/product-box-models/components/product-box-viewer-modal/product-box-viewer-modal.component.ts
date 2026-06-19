import { NgClass } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ProductBoxModel } from '../../models/product-box-model.model';
import { ProductBoxViewerComponent } from '../product-box-viewer/product-box-viewer.component';

@Component({
  selector: 'app-product-box-viewer-modal',
  standalone: true,
  imports: [NgClass, TranslatePipe, ProductBoxViewerComponent],
  templateUrl: './product-box-viewer-modal.component.html'
})
export class ProductBoxViewerModalComponent {
  @Input() open = false;
  @Input() model: ProductBoxModel | null = null;
  @Output() closed = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }

  dimensionsLabel(): string {
    if (!this.model) {
      return '—';
    }

    return `${this.model.width} × ${this.model.height} × ${this.model.depth} ${this.model.unit}`;
  }
}
