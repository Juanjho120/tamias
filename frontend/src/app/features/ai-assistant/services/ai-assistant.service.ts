import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../../../core/models/page-response.model';
import { ApiService } from '../../../core/services/api.service';
import {
  AiAssistantFilters,
  AiChatMessage,
  AiChatRequest,
  AiChatResponse,
  AiChatSession,
  AiChatSessionCreateRequest,
  AiChatSessionSummary,
  AiChatSessionUpdateRequest,
  AiSearchRequest,
  AiSearchResponse
} from '../models/ai-assistant.model';

@Injectable({
  providedIn: 'root'
})
export class AiAssistantService {
  constructor(private readonly apiService: ApiService) {
  }

  search(request: AiSearchRequest): Observable<AiSearchResponse> {
    return this.apiService.post<AiSearchResponse>('/ai/search', request);
  }

  chat(request: AiChatRequest): Observable<AiChatResponse> {
    return this.apiService.post<AiChatResponse>('/ai/chat', request);
  }

  findSessions(filters: AiAssistantFilters): Observable<PageResponse<AiChatSessionSummary>> {
    return this.apiService.get<PageResponse<AiChatSessionSummary>>('/ai/chat-sessions', {
      propertyId: filters.propertyId,
      page: filters.page,
      size: filters.size,
      sort: filters.sort ?? 'updatedAt,desc'
    });
  }

  findSession(sessionId: string): Observable<AiChatSession> {
    return this.apiService.get<AiChatSession>(`/ai/chat-sessions/${sessionId}`);
  }

  findMessages(sessionId: string): Observable<AiChatMessage[]> {
    return this.apiService.get<AiChatMessage[]>(`/ai/chat-sessions/${sessionId}/messages`);
  }

  createSession(request: AiChatSessionCreateRequest): Observable<AiChatSession> {
    return this.apiService.post<AiChatSession>('/ai/chat-sessions', request);
  }

  updateSessionTitle(sessionId: string, request: AiChatSessionUpdateRequest): Observable<AiChatSessionSummary> {
    return this.apiService.patch<AiChatSessionSummary>(`/ai/chat-sessions/${sessionId}/title`, request);
  }
}
