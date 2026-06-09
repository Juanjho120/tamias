export type AiChatMessageRole = 'USER' | 'ASSISTANT' | 'SYSTEM';

export interface AiSource {
  vectorId: string | null;
  documentId: string | null;
  chunkId: string | null;
  propertyId: string | null;
  documentTitle: string | null;
  documentType: string | null;
  chunkIndex: number | null;
  score: number | null;
  content: string | null;
}

export interface AiSearchRequest {
  question: string;
  propertyId: string | null;
  topK: number | null;
  similarityThreshold: number | null;
}

export interface AiSearchResponse {
  question: string;
  sourceCount: number;
  sources: AiSource[];
}

export interface AiChatRequest {
  chatSessionId: string | null;
  propertyId: string | null;
  title: string | null;
  question: string;
  topK: number | null;
  similarityThreshold: number | null;
}

export interface AiChatResponse {
  chatSessionId: string;
  userMessageId: string;
  assistantMessageId: string;
  question: string;
  answer: string;
  grounded: boolean;
  sourceCount: number;
  sources: AiSource[];
}

export interface AiChatSessionSummary {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  title: string | null;
  createdBy: string | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

export interface AiChatMessage {
  id: string;
  chatSessionId: string;
  role: AiChatMessageRole;
  content: string;
  createdAt: string;
}

export interface AiChatSession {
  id: string;
  propertyId: string | null;
  propertyName: string | null;
  title: string | null;
  createdBy: string | null;
  createdByName: string | null;
  createdAt: string;
  updatedAt: string;
  messages: AiChatMessage[];
}

export interface AiChatSessionCreateRequest {
  propertyId: string | null;
  title: string | null;
}

export interface AiChatSessionUpdateRequest {
  title: string;
}

export interface AiLocalMessage {
  id: string;
  role: AiChatMessageRole;
  content: string;
  createdAt: string;
  sources?: AiSource[];
  grounded?: boolean;
}

export interface AiAssistantFilters {
  propertyId?: string;
  page: number;
  size: number;
  sort?: string;
}
