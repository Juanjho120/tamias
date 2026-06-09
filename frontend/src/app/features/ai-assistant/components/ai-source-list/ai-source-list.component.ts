import { DecimalPipe, SlicePipe } from '@angular/common';
import { Component, Input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { AiSource } from '../../models/ai-assistant.model';

@Component({
  selector: 'app-ai-source-list',
  standalone: true,
  imports: [DecimalPipe, SlicePipe, TranslatePipe],
  templateUrl: './ai-source-list.component.html'
})
export class AiSourceListComponent {
  @Input() sources: AiSource[] = [];
  @Input() compact = false;

  trackBySource(index: number, source: AiSource): string {
    return source.chunkId ?? source.vectorId ?? `${source.documentId}-${source.chunkIndex}-${index}`;
  }

  scorePercent(score: number | null): number | null {
    if (score === null || score === undefined) {
      return null;
    }

    return score * 100;
  }
}
