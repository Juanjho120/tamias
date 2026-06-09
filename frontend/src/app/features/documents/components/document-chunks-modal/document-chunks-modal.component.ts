import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { DocumentChunk, DocumentSummary } from '../../models/document.model';

@Component({
  selector: 'app-document-chunks-modal',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './document-chunks-modal.component.html'
})
export class DocumentChunksModalComponent {
  @Input() open = false;
  @Input() document: DocumentSummary | null = null;
  @Input() chunks: DocumentChunk[] = [];
  @Input() loading = false;

  @Output() close = new EventEmitter<void>();

  trackByChunkId(index: number, chunk: DocumentChunk): string {
    return chunk.id;
  }
}
