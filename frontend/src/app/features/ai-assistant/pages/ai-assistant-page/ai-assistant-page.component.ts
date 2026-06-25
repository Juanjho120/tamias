import { DatePipe, NgClass, TitleCasePipe } from '@angular/common';
import { Component, ElementRef, OnDestroy, OnInit, ViewChild, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/i18n/language.service';
import { ApiError } from '../../../../core/models/api-error.model';
import { PageResponse } from '../../../../core/models/page-response.model';
import { ConfirmModalComponent } from '../../../../shared/confirm-modal/confirm-modal.component';
import { ToastService } from '../../../../shared/toast/toast.service';
import { TamiBrandingService } from '../../../../shared/tami-robot/tami-branding.service';
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
import { TamiSpeechAudioService } from '../../services/tami-speech-audio.service';

type AssistantMode = 'chat' | 'search';
type AiSessionSort = 'createdAt,desc' | 'createdAt,asc';
type AiAnimatedLocalMessage = AiLocalMessage & {
  displayedContent?: string;
  typing?: boolean;
};

@Component({
  selector: 'app-ai-assistant-page',
  standalone: true,
  imports: [
    DatePipe,
    FormsModule,
    NgClass,
    TitleCasePipe,
    TranslatePipe,
    ConfirmModalComponent,
    AiSourceListComponent,
    AiSessionTitleModalComponent
  ],
  templateUrl: './ai-assistant-page.component.html',
  styles: [`
    .ai-typing-cursor {
      display: inline-block;
      margin-left: 1px;
      animation: tamiTypingCursorBlink 1s steps(2, start) infinite;
    }

    @keyframes tamiTypingCursorBlink {
      0%, 45% { opacity: 1; }
      46%, 100% { opacity: 0; }
    }
  `]
})
export class AiAssistantPageComponent implements OnInit, OnDestroy {
  @ViewChild('messagesContainer') messagesContainer?: ElementRef<HTMLElement>;
  @ViewChild('questionInput') questionInput?: ElementRef<HTMLTextAreaElement>;

  private readonly aiAssistantService = inject(AiAssistantService);
  private readonly referenceDataService = inject(AiReferenceDataService);
  private readonly toastService = inject(ToastService);
  private readonly languageService = inject(LanguageService);
  private readonly tamiBrandingService = inject(TamiBrandingService);
  private readonly tamiSpeechAudioService = inject(TamiSpeechAudioService);
  private readonly typingDelayMs = 12;
  private readonly typingTimers = new Map<string, ReturnType<typeof setInterval>>();
  private readonly tamiSpeechSpeedRatio = 0.75;
  readonly tamiMouthAnimationDurationMs = Math.round((this.typingDelayMs * 4) / this.tamiSpeechSpeedRatio);
  private readonly tamiSpeechBlipIntervalMs = this.tamiMouthAnimationDurationMs;
  private pendingTamiSpeechPreparation?: Promise<boolean>;

  readonly loadingReferences = signal(false);
  readonly loadingSessions = signal(false);
  readonly loadingSession = signal(false);
  readonly sending = signal(false);
  readonly typingAssistant = signal(false);
  readonly searching = signal(false);
  readonly renaming = signal(false);
  readonly deletingSessionId = signal<string | null>(null);
  readonly properties = signal<AiPropertyOption[]>([]);
  readonly sessions = signal<AiChatSessionSummary[]>([]);
  readonly activeSession = signal<AiChatSession | null>(null);
  readonly messages = signal<AiAnimatedLocalMessage[]>([]);
  readonly searchResult = signal<AiSearchResponse | null>(null);
  readonly sessionToRename = signal<AiChatSessionSummary | null>(null);
  readonly sessionToDelete = signal<AiChatSessionSummary | null>(null);
  readonly propertyId = signal('');
  readonly mode = signal<AssistantMode>('chat');
  readonly question = signal('');
  readonly topK = signal(5);
  readonly similarityThreshold = signal(0.3);
  readonly sessionSort = signal<AiSessionSort>('createdAt,desc');
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
  readonly deleteSessionMessage = computed(() => {
    const session = this.sessionToDelete();
    if (!session) {
      return '';
    }

    return this.languageService.instant('aiAssistant.sessions.confirmDeleteMessage', {
      title: session.title || this.languageService.instant('aiAssistant.sessions.untitled')
    });
  });

  ngOnInit(): void {
    this.loadProperties();
    this.loadSessions();
  }

  ngOnDestroy(): void {
    this.clearTypingTimers();
    this.tamiSpeechAudioService.release();
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
      sort: this.sessionSort()
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
    this.clearTypingTimers();
    this.sessionPage.set(0);
    this.activeSession.set(null);
    this.messages.set([]);
    this.searchResult.set(null);
    this.loadSessions();
  }

  changeSessionSort(value: string): void {
    const nextSort: AiSessionSort = value === 'createdAt,asc' ? 'createdAt,asc' : 'createdAt,desc';
    this.sessionSort.set(nextSort);
    this.sessionPage.set(0);
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
    this.clearTypingTimers();
    this.activeSession.set(null);
    this.messages.set([]);
    this.searchResult.set(null);
    this.question.set('');
    this.focusQuestionInput();
  }

  openSession(sessionId: string): void {
    this.clearTypingTimers();
    this.loadingSession.set(true);
    this.searchResult.set(null);
    this.aiAssistantService.findSession(sessionId).subscribe({
      next: (session: AiChatSession) => {
        this.activeSession.set(session);
        this.propertyId.set(session.propertyId ?? this.propertyId());
        this.messages.set((session.messages ?? []).map((message) => this.toLocalMessage(message)));
        this.loadingSession.set(false);
        
        setTimeout(() => {
          this.scrollToBottom();
          this.focusQuestionInput();
        }, 0);
      },
      error: (error: unknown) => {
        this.loadingSession.set(false);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.sessionLoadError')));
      }
    });
  }

  send(): void {
    const question = this.question().trim();
    if (!question || this.sending() || this.typingAssistant() || this.searching()) {
      return;
    }

    if (this.mode() === 'search') {
      this.search(question);
      return;
    }

    this.prepareTamiSpeechAudio();
    this.chat(question);
  }

  prepareTamiSpeechAudio(): void {
    if (this.mode() !== 'chat') {
      return;
    }

    this.pendingTamiSpeechPreparation ??= this.tamiSpeechAudioService.prepare();
  }

  handleQuestionKeydown(event: KeyboardEvent): void {
    this.prepareTamiSpeechAudio();

    if (event.key !== 'Enter' || event.shiftKey) {
      return;
    }

    event.preventDefault();
    this.send();
  }

  chat(question: string): void {
    const currentSession = this.activeSession();
    const requestPropertyId = currentSession?.propertyId ?? (this.propertyId() || null);
    const requestTitle = currentSession?.title ?? this.createTitleFromQuestion(question);
    const temporaryUserMessage: AiAnimatedLocalMessage = {
      id: crypto.randomUUID(),
      role: 'USER',
      content: question,
      displayedContent: question,
      createdAt: new Date().toISOString()
    };

    this.messages.update((messages) => [...messages, temporaryUserMessage]);
    this.question.set('');
    this.sending.set(true);
    this.searchResult.set(null);

    this.aiAssistantService.chat({
      chatSessionId: currentSession?.id ?? null,
      propertyId: requestPropertyId,
      title: requestTitle,
      question,
      topK: this.topK(),
      similarityThreshold: this.similarityThreshold()
    }).subscribe({
      next: (response) => {
        const now = new Date().toISOString();
        const assistantMessage: AiAnimatedLocalMessage = {
          id: response.assistantMessageId,
          role: 'ASSISTANT',
          content: response.answer,
          displayedContent: '',
          typing: false,
          createdAt: now,
          sources: response.sources ?? [],
          grounded: response.grounded,
          toolEvidence: response.toolEvidence ?? []
        };

        this.messages.update((messages) => [
          ...messages.map((message) => message.id === temporaryUserMessage.id
            ? { ...message, id: response.userMessageId }
            : message),
          assistantMessage
        ]);
        this.sending.set(false);
        if (!currentSession || currentSession.id !== response.chatSessionId) {
          this.activeSession.set({
            id: response.chatSessionId,
            propertyId: requestPropertyId,
            propertyName: null,
            title: requestTitle,
            createdBy: null,
            createdByName: null,
            createdAt: now,
            updatedAt: now,
            messages: []
          });
        }

        void this.animateAssistantMessage(response.assistantMessageId, response.answer, () => {
          this.typingAssistant.set(false);
          this.loadSessions();
          setTimeout(() => this.scrollToBottom(), 0);
        });
      },
      error: (error: unknown) => {
        this.pendingTamiSpeechPreparation = undefined;
        this.sending.set(false);
        this.typingAssistant.set(false);
        this.tamiBrandingService.setSpeaking(false);
        this.tamiSpeechAudioService.stop();
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

  requestDeleteSession(session: AiChatSessionSummary, event?: MouseEvent): void {
    event?.stopPropagation();
    this.sessionToDelete.set(session);
  }

  cancelDeleteSession(): void {
    if (this.deletingSessionId()) {
      return;
    }

    this.sessionToDelete.set(null);
  }

  confirmDeleteSession(): void {
    const session = this.sessionToDelete();
    if (!session) {
      return;
    }

    this.deletingSessionId.set(session.id);
    this.aiAssistantService.deleteSession(session.id).subscribe({
      next: () => {
        this.deletingSessionId.set(null);
        this.sessionToDelete.set(null);
        this.toastService.success(this.languageService.instant('aiAssistant.messages.sessionDeleted'));

        if (this.activeSession()?.id === session.id) {
          this.newSession();
        }

        this.loadSessions();
      },
      error: (error: unknown) => {
        this.deletingSessionId.set(null);
        this.toastService.error(this.extractErrorMessage(error, this.languageService.instant('aiAssistant.messages.sessionDeleteError')));
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

  displayedMessageContent(message: AiAnimatedLocalMessage): string {
    return message.displayedContent ?? message.content;
  }

  isTypingMessage(message: AiAnimatedLocalMessage): boolean {
    return message.role === 'ASSISTANT' && message.typing === true;
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
    if (toolName.includes('payment')) {
      return 'bi-credit-card';
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

  trackByMessage(index: number, message: AiAnimatedLocalMessage): string {
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

  private toLocalMessage(message: AiChatMessage): AiAnimatedLocalMessage {
    return {
      id: message.id,
      role: message.role,
      content: message.content,
      displayedContent: message.content,
      typing: false,
      createdAt: message.createdAt
    };
  }

  private createTitleFromQuestion(question: string): string {
    if (question.length <= 60) {
      return question;
    }

    return `${question.substring(0, 57)}...`;
  }

  private async animateAssistantMessage(messageId: string, answer: string, onComplete?: () => void): Promise<void> {
    this.stopTypingTimer(messageId);

    if (!answer) {
      this.finishTypingMessage(messageId, answer, onComplete);
      return;
    }

    await this.pendingTamiSpeechPreparation?.catch(() => false);
    this.pendingTamiSpeechPreparation = undefined;

    await this.tamiSpeechAudioService.start({ intervalMs: this.tamiSpeechBlipIntervalMs });

    if (!this.messages().some((message) => message.id === messageId)) {
      this.tamiSpeechAudioService.stop();
      this.tamiBrandingService.setSpeaking(false);
      return;
    }

    const chunkSize = this.resolveTypingChunkSize(answer);
    let currentIndex = Math.min(chunkSize, answer.length);
    const initialDisplayedContent = answer.slice(0, currentIndex);
    const initiallyDone = currentIndex >= answer.length;

    this.typingAssistant.set(true);
    this.tamiBrandingService.setSpeaking(!initiallyDone);

    this.messages.update((messages) => messages.map((message) => message.id === messageId
      ? { ...message, displayedContent: initialDisplayedContent, typing: !initiallyDone }
      : message
    ));
    setTimeout(() => this.scrollToBottom(), 0);

    if (initiallyDone) {
      this.finishTypingMessage(messageId, answer, onComplete);
      return;
    }

    const timer = setInterval(() => {
      currentIndex = Math.min(currentIndex + chunkSize, answer.length);
      const displayedContent = answer.slice(0, currentIndex);
      const done = currentIndex >= answer.length;

      this.messages.update((messages) => messages.map((message) => message.id === messageId
        ? { ...message, displayedContent, typing: !done }
        : message
      ));
      setTimeout(() => this.scrollToBottom(), 0);

      if (done) {
        this.finishTypingMessage(messageId, answer, onComplete);
      }
    }, this.typingDelayMs);

    this.typingTimers.set(messageId, timer);
  }

  private finishTypingMessage(messageId: string, answer: string, onComplete?: () => void): void {
    this.messages.update((messages) => messages.map((message) => message.id === messageId
      ? { ...message, content: answer, displayedContent: answer, typing: false }
      : message
    ));
    this.stopTypingTimer(messageId);
    this.typingAssistant.set(false);
    this.tamiSpeechAudioService.stop();
    this.tamiBrandingService.setSpeaking(false);
    onComplete?.();
    this.focusQuestionInput();
  }

  private resolveTypingChunkSize(answer: string): number {
    if (answer.length > 2_000) {
      return 8;
    }
    if (answer.length > 1_000) {
      return 5;
    }
    if (answer.length > 500) {
      return 3;
    }

    return 1;
  }

  private clearTypingTimers(): void {
    this.pendingTamiSpeechPreparation = undefined;
    this.typingAssistant.set(false);
    this.tamiSpeechAudioService.stop();
    this.tamiBrandingService.setSpeaking(false);

    for (const timer of this.typingTimers.values()) {
      clearInterval(timer);
    }

    this.typingTimers.clear();
  }

  private stopTypingTimer(messageId: string): void {
    const timer = this.typingTimers.get(messageId);
    if (!timer) {
      return;
    }

    clearInterval(timer);
    this.typingTimers.delete(messageId);

    if (this.typingTimers.size === 0) {
      this.typingAssistant.set(false);
      this.tamiSpeechAudioService.stop();
      this.tamiBrandingService.setSpeaking(false);
    }
  }

  private focusQuestionInput(): void {
    setTimeout(() => {
      if (this.mode() !== 'chat') {
        return;
      }

      if (this.sending() || this.typingAssistant() || this.searching()) {
        return;
      }

      this.questionInput?.nativeElement.focus();
    }, 0);
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
