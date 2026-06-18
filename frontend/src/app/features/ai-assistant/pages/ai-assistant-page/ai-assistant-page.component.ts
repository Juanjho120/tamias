import { DatePipe, NgClass, TitleCasePipe } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ToastService } from '../../../../shared/toast/toast.service';
import { AiSourceListComponent } from '../../components/ai-source-list/ai-source-list.component';
import { AiSessionTitleModalComponent } from '../../components/ai-session-title-modal/ai-session-title-modal.component';
import {
  AiChatMessage,
  AiChatSession,
  AiChatSessionSummary,
  AiLocalMessage,
  AiSearchResponse,
  AiSource,
  AiToolEvidence
} from '../../models/ai-assistant.model';
import { AiPropertyOption } from '../../models/ai-reference.model';
import { AiAssistantService } from '../../services/ai-assistant.service';
import { AiReferenceDataService } from '../../services/ai-reference-data.service';

type AssistantMode = 'chat' | 'search';

@Component({
  selector: 'app-ai-assistant-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TitleCasePipe,
    TranslatePipe,
    AiSourceListComponent,
    AiSessionTitleModalComponent
  ],
  templateUrl: './ai-assistant-page.component.html'
})
export class AiAssistantPageComponent implements OnInit {
  @ViewChild('messagesContainer') messagesContainer?: ElementRef<HTMLElement>;

  private readonly aiAssistantService = inject(AiAssistantService);
  private readonly referenceDataService = inject(AiReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);

  readonly loadingReferences = signal(false);
  readonly loadingSessions = signal(false);
  readonly loadingSession = signal(false);
  readonly sending = signal(false);
  readonly searching = signal(false);
  readonly renaming = signal(false);

  readonly properties = signal<AiPropertyOption[]>([]);
  readonly sessions = signal<AiChatSessionSummary[]>([]);
  readonly activeSession = signal<AiChatSession | null>(null);
  readonly messages = signal<AiLocalMessage[]>([]);
  readonly searchResult = signal<AiSearchResponse | null>(null);
  readonly sessionToRename = signal<AiChatSessionSummary | null>(null);

  readonly propertyId = signal('');
  readonly mode = signal<AssistantMode>('chat');
  readonly question = signal('');
  readonly topK = signal(5);
  readonly similarityThreshold = signal(0.3);

  readonly sessionPage = signal(0);
  readonly sessionSize = signal(20);
  readonly sessionTotalElements = signal(0);
  readonly sessionTotalPages = signal(0);
  readonly sessionFirst = signal(true);
  readonly sessionLast = signal(true);

  readonly activeSessionTitle = computed(() => {
    const session = this.activeSession();

    if (session?.title) {
      return session.title;
    }

    return this.languageService.instant('aiAssistant.chat.newSession');
  });

  readonly selectedPropertyName = computed(() => {
    const selectedPropertyId = this.propertyId();

    if (!selectedPropertyId) {
      return this.languageService.instant('aiAssistant.properties.all');
    }

    return this.properties().find((property) => property.id === selectedPropertyId)?.name
      ?? this.languageService.instant('aiAssistant.properties.selected');
  });

  readonly renameTitle = computed(() => this.sessionToRename()?.title ?? '');

  ngOnInit(): void {
    this.loadProperties();
    this.loadSessions();
  }

  loadProperties(): void {
    this.loadingReferences.set(true);

    this.referenceDataService.loadProperties().subscribe({
      next: (properties) => {
        this.properties.set(properties);
        this.loadingReferences.set(false);
      },
      error: (error: unknown) => {
        this.loadingReferences.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.referencesError')));
      }
    });
  }

  loadSessions(): void {
    this.loadingSessions.set(true);

    this.aiAssistantService.findSessions({
      propertyId: this.propertyId() || undefined,
      page: this.sessionPage(),
      size: this.sessionSize(),
      sort: 'updatedAt,desc'
    }).subscribe({
      next: (response: PageResponse<AiChatSessionSummary>) => {
        this.sessions.set(response.content);
        this.sessionPage.set(response.page);
        this.sessionSize.set(response.size);
        this.sessionTotalElements.set(response.totalElements);
        this.sessionTotalPages.set(response.totalPages);
        this.sessionFirst.set(response.first);
        this.sessionLast.set(response.last);
        this.loadingSessions.set(false);
      },
      error: (error: unknown) => {
        this.loadingSessions.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.sessionsError')));
      }
    });
  }

  refreshByProperty(): void {
    this.sessionPage.set(0);
    this.activeSession.set(null);
    this.messages.set([]);
    this.searchResult.set(null);
    this.loadSessions();
  }

  previousSessionPage(): void {
    if (this.sessionFirst()) {
      return;
    }

    this.sessionPage.update((value) => value - 1);
    this.loadSessions();
  }

  nextSessionPage(): void {
    if (this.sessionLast()) {
      return;
    }

    this.sessionPage.update((value) => value + 1);
    this.loadSessions();
  }

  newSession(): void {
    this.activeSession.set(null);
    this.messages.set([]);
    this.searchResult.set(null);
    this.question.set('');
  }

  openSession(sessionId: string): void {
    this.loadingSession.set(true);
    this.searchResult.set(null);

    this.aiAssistantService.findSession(sessionId).subscribe({
      next: (session: AiChatSession) => {
        this.activeSession.set(session);
        this.propertyId.set(session.propertyId ?? this.propertyId());
        this.messages.set((session.messages ?? []).map((message) => this.toLocalMessage(message)));
        this.loadingSession.set(false);
        setTimeout(() => this.scrollToBottom(), 0);
      },
      error: (error: unknown) => {
        this.loadingSession.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.sessionLoadError')));
      }
    });
  }

  send(): void {
    const question = this.question().trim();

    if (!question || this.sending() || this.searching()) {
      return;
    }

    if (this.mode() === 'search') {
      this.search(question);
      return;
    }

    this.chat(question);
  }

  handleQuestionKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter' || event.shiftKey) {
      return;
    }

    event.preventDefault();
    this.send();
  }

  chat(question: string): void {
    const temporaryUserMessage: AiLocalMessage = {
      id: crypto.randomUUID(),
      role: 'USER',
      content: question,
      createdAt: new Date().toISOString()
    };

    this.messages.update((messages) => [...messages, temporaryUserMessage]);
    this.question.set('');
    this.sending.set(true);
    this.searchResult.set(null);

    const currentSession = this.activeSession();

    this.aiAssistantService.chat({
      chatSessionId: currentSession?.id ?? null,
      propertyId: currentSession?.propertyId ?? (this.propertyId() || null),
      title: currentSession?.title ?? this.createTitleFromQuestion(question),
      question,
      topK: this.topK(),
      similarityThreshold: this.similarityThreshold()
    }).subscribe({
      next: (response) => {
        const assistantMessage: AiLocalMessage = {
          id: response.assistantMessageId,
          role: 'ASSISTANT',
          content: response.answer,
          createdAt: new Date().toISOString(),
          sources: response.sources ?? [],
          grounded: response.grounded,
          toolEvidence: response.toolEvidence ?? []
        };

        this.messages.update((messages) => [...messages, assistantMessage]);
        this.sending.set(false);

        if (!currentSession || currentSession.id !== response.chatSessionId) {
          this.openSession(response.chatSessionId);
        } else {
          this.loadSessions();
          setTimeout(() => this.scrollToBottom(), 0);
        }
      },
      error: (error: unknown) => {
        this.sending.set(false);
        this.messages.update((messages) => messages.filter((message) => message.id !== temporaryUserMessage.id));
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.chatError')));
      }
    });

    setTimeout(() => this.scrollToBottom(), 0);
  }

  search(question: string): void {
    this.question.set('');
    this.searching.set(true);
    this.searchResult.set(null);

    this.aiAssistantService.search({
      question,
      propertyId: this.propertyId() || null,
      topK: this.topK(),
      similarityThreshold: this.similarityThreshold()
    }).subscribe({
      next: (response) => {
        this.searchResult.set(response);
        this.searching.set(false);
      },
      error: (error: unknown) => {
        this.searching.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.searchError')));
      }
    });
  }

  openRename(session: AiChatSessionSummary, event?: MouseEvent): void {
    event?.stopPropagation();
    this.sessionToRename.set(session);
  }

  closeRename(): void {
    if (this.renaming()) {
      return;
    }

    this.sessionToRename.set(null);
  }

  saveRename(title: string): void {
    const session = this.sessionToRename();

    if (!session) {
      return;
    }

    this.renaming.set(true);

    this.aiAssistantService.updateSessionTitle(session.id, { title }).subscribe({
      next: () => {
        this.renaming.set(false);
        this.sessionToRename.set(null);
        this.toastService.success(this.languageService.instant('aiAssistant.messages.renamed'));
        this.loadSessions();

        const activeSession = this.activeSession();

        if (activeSession?.id === session.id) {
          this.activeSession.set({ ...activeSession, title });
        }
      },
      error: (error: unknown) => {
        this.renaming.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.renameError')));
      }
    });
  }


  roleLabel(role: AiLocalMessage['role']): string {
    if (role === 'ASSISTANT') {
      return 'TAMI';
    }

    return this.languageService.instant(`aiAssistant.role.${role}`);
  }

  messageSources(message: AiLocalMessage): AiSource[] {
    return message.sources ?? [];
  }

  messageToolEvidence(message: AiLocalMessage): AiToolEvidence[] {
    return message.toolEvidence ?? [];
  }

  hasSystemEvidence(message: AiLocalMessage): boolean {
    return this.messageToolEvidence(message).length > 0;
  }

  evidenceItems(evidence: AiToolEvidence): Record<string, unknown>[] {
    return evidence.items ?? [];
  }

  evidencePreviewItems(evidence: AiToolEvidence): Record<string, unknown>[] {
    return this.evidenceItems(evidence).slice(0, 3);
  }

  evidenceExtraCount(evidence: AiToolEvidence): number {
    return Math.max(this.evidenceItems(evidence).length - this.evidencePreviewItems(evidence).length, 0);
  }

  evidenceItemEntries(item: Record<string, unknown>): Array<{ key: string; value: string }> {
    return Object.entries(item)
      .filter(([_, value]) => value !== null && value !== undefined && `${value}`.trim() !== '')
      .slice(0, 6)
      .map(([key, value]) => ({ key: this.humanizeKey(key), value: this.formatEvidenceValue(value) }));
  }

  evidenceIcon(toolName: string): string {
    if (toolName.includes('reservation')) {
      return 'bi-calendar-check';
    }

    if (toolName.includes('maintenance')) {
      return 'bi-tools';
    }

    if (toolName.includes('purchase')) {
      return 'bi-bag-check';
    }

    if (toolName.includes('task')) {
      return 'bi-check2-square';
    }

    if (toolName.includes('document') || toolName.includes('rag')) {
      return 'bi-file-earmark-text';
    }

    if (toolName.includes('property')) {
      return 'bi-house-door';
    }

    if (toolName.includes('user')) {
      return 'bi-person-circle';
    }

    if (toolName.includes('organization')) {
      return 'bi-building';
    }

    return 'bi-database-check';
  }

  trackBySession(index: number, session: AiChatSessionSummary): string {
    return session.id;
  }

  trackByMessage(index: number, message: AiLocalMessage): string {
    return message.id;
  }

  trackByEvidence(index: number, evidence: AiToolEvidence): string {
    return `${evidence.toolName}-${index}`;
  }

  trackByEvidenceItem(index: number): number {
    return index;
  }

  trackByEvidenceEntry(index: number, entry: { key: string; value: string }): string {
    return `${entry.key}-${index}`;
  }

  isActiveSession(session: AiChatSessionSummary): boolean {
    return this.activeSession()?.id === session.id;
  }

  private toLocalMessage(message: AiChatMessage): AiLocalMessage {
    return {
      id: message.id,
      role: message.role,
      content: message.content,
      createdAt: message.createdAt
    };
  }

  private createTitleFromQuestion(question: string): string {
    if (question.length <= 60) {
      return question;
    }

    return `${question.substring(0, 57)}...`;
  }

  private scrollToBottom(): void {
    const container = this.messagesContainer?.nativeElement;

    if (!container) {
      return;
    }

    container.scrollTop = container.scrollHeight;
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    const maybeHttpError = error as { error?: ApiError };
    return maybeHttpError.error?.message ?? fallback;
  }

  private humanizeKey(key: string): string {
    return key
      .replace(/([a-z])([A-Z])/g, '$1 $2')
      .replace(/_/g, ' ')
      .trim();
  }

  private formatEvidenceValue(value: unknown): string {
    if (value instanceof Date) {
      return value.toLocaleString();
    }

    if (typeof value === 'boolean') {
      return value ? 'Sí' : 'No';
    }

    return String(value);
  }
}
