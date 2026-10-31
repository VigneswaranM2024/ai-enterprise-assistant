/**
 * Citation referencing an authoritative enterprise document chunk matched during RAG retrieval.
 * Field names match the backend CitationDTO record exactly.
 */
export interface Citation {
  citationId: string;
  documentId: string;
  chunkId: string;
  documentTitle: string;
  fileName: string;
  chunkIndex?: number | null;
  similarityScore?: number | null;
}

export interface ChatMessage {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  citations?: Citation[];
  createdAt: string;
}

export interface ChatSession {
  id: string;
  title: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatSessionDetail extends ChatSession {
  messages: ChatMessage[];
}

export interface SendMessageResponse {
  sessionId: string;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
  citations: Citation[];
  sourcesUsed: number;
  model: string;
}
