import React, { useState, useEffect, useRef, useCallback } from 'react';
import { chatService } from '../services/chatService';
import { ChatSession, ChatMessage, Citation } from '../types/chat.types';
import {
  MessageSquare,
  Plus,
  Send,
  Trash2,
  Bot,
  User,
  Sparkles,
  BookOpen,
  ChevronDown,
  ChevronRight,
  AlertCircle,
  Loader2,
  FileText,
  Zap,
  Brain,
} from 'lucide-react';

// ---------------------------------------------------------------------------
// Simple Markdown renderer (no external dependency)
// Supports: headings, bold, italic, inline code, code blocks, bullets, numbers
// ---------------------------------------------------------------------------
function renderMarkdown(text: string): React.ReactNode {
  if (!text) return null;

  // Split by code blocks first
  const parts = text.split(/(```[\s\S]*?```)/g);

  return parts.map((part, partIdx) => {
    // Fenced code block
    if (part.startsWith('```') && part.endsWith('```')) {
      const inner = part.slice(3, -3);
      const newlineIdx = inner.indexOf('\n');
      const lang = newlineIdx !== -1 ? inner.slice(0, newlineIdx).trim() : '';
      const code = newlineIdx !== -1 ? inner.slice(newlineIdx + 1) : inner;
      return (
        <pre
          key={partIdx}
          className="bg-slate-950 border border-slate-800 rounded-lg p-3 overflow-x-auto my-2 text-[11px] font-mono text-emerald-300"
        >
          {lang && (
            <div className="text-slate-500 text-[10px] mb-2 font-sans">{lang}</div>
          )}
          <code>{code}</code>
        </pre>
      );
    }

    // Process inline content line by line
    const lines = part.split('\n');
    const elements: React.ReactNode[] = [];
    let i = 0;

    while (i < lines.length) {
      const line = lines[i];

      // Heading H3
      if (line.startsWith('### ')) {
        elements.push(
          <h3 key={`${partIdx}-${i}`} className="text-sm font-semibold text-white mt-3 mb-1">
            {renderInline(line.slice(4))}
          </h3>
        );
        i++;
        continue;
      }
      // Heading H2
      if (line.startsWith('## ')) {
        elements.push(
          <h2 key={`${partIdx}-${i}`} className="text-sm font-bold text-white mt-4 mb-1 border-b border-slate-700 pb-1">
            {renderInline(line.slice(3))}
          </h2>
        );
        i++;
        continue;
      }
      // Heading H1
      if (line.startsWith('# ')) {
        elements.push(
          <h1 key={`${partIdx}-${i}`} className="text-base font-bold text-white mt-4 mb-2">
            {renderInline(line.slice(2))}
          </h1>
        );
        i++;
        continue;
      }
      // Unordered list item
      if (line.match(/^[-*+] /)) {
        const listItems: React.ReactNode[] = [];
        while (i < lines.length && lines[i].match(/^[-*+] /)) {
          listItems.push(
            <li key={`${partIdx}-${i}`} className="ml-4 text-slate-200">
              {renderInline(lines[i].slice(2))}
            </li>
          );
          i++;
        }
        elements.push(
          <ul key={`${partIdx}-ul-${i}`} className="list-disc list-inside my-1 space-y-0.5">
            {listItems}
          </ul>
        );
        continue;
      }
      // Ordered list item
      if (line.match(/^\d+\. /)) {
        const listItems: React.ReactNode[] = [];
        while (i < lines.length && lines[i].match(/^\d+\. /)) {
          const content = lines[i].replace(/^\d+\. /, '');
          listItems.push(
            <li key={`${partIdx}-${i}`} className="ml-4 text-slate-200">
              {renderInline(content)}
            </li>
          );
          i++;
        }
        elements.push(
          <ol key={`${partIdx}-ol-${i}`} className="list-decimal list-inside my-1 space-y-0.5">
            {listItems}
          </ol>
        );
        continue;
      }
      // Horizontal rule
      if (line.match(/^---+$/) || line.match(/^\*\*\*+$/)) {
        elements.push(<hr key={`${partIdx}-${i}`} className="border-slate-700 my-2" />);
        i++;
        continue;
      }
      // Empty line — paragraph break
      if (line.trim() === '') {
        elements.push(<div key={`${partIdx}-${i}`} className="h-1" />);
        i++;
        continue;
      }
      // Regular paragraph line
      elements.push(
        <p key={`${partIdx}-${i}`} className="text-slate-200 leading-relaxed">
          {renderInline(line)}
        </p>
      );
      i++;
    }

    return <React.Fragment key={partIdx}>{elements}</React.Fragment>;
  });
}

/** Renders inline markdown: bold, italic, inline code, citation tags. */
function renderInline(text: string): React.ReactNode {
  // Split on bold (**...**), italic (*...*), inline code (`...`), and citation tags [S1]
  const tokens = text.split(/(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`|\[S\d+\])/g);
  return tokens.map((token, idx) => {
    if (token.startsWith('**') && token.endsWith('**')) {
      return <strong key={idx} className="font-semibold text-white">{token.slice(2, -2)}</strong>;
    }
    if (token.startsWith('*') && token.endsWith('*')) {
      return <em key={idx} className="italic">{token.slice(1, -1)}</em>;
    }
    if (token.startsWith('`') && token.endsWith('`')) {
      return (
        <code key={idx} className="bg-slate-800 text-emerald-300 px-1 py-0.5 rounded text-[10px] font-mono">
          {token.slice(1, -1)}
        </code>
      );
    }
    // Citation tag e.g. [S1]
    if (token.match(/^\[S\d+\]$/)) {
      return (
        <span key={idx} className="text-brand-400 text-[10px] font-semibold bg-brand-500/10 px-1 rounded">
          {token}
        </span>
      );
    }
    return <React.Fragment key={idx}>{token}</React.Fragment>;
  });
}

// ---------------------------------------------------------------------------
// Citation card component
// ---------------------------------------------------------------------------
function CitationCard({ citation }: { citation: Citation }) {
  const title = citation.documentTitle || citation.fileName || 'Document';
  const score = citation.similarityScore;

  return (
    <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800 text-[11px] space-y-1">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 min-w-0">
          <FileText className="w-3 h-3 text-brand-400 flex-shrink-0" />
          <span className="font-semibold text-slate-300 truncate">{title}</span>
        </div>
        <div className="flex items-center gap-1.5 flex-shrink-0">
          {citation.citationId && (
            <span className="text-[10px] text-brand-400 bg-brand-500/10 px-1.5 py-0.5 rounded font-semibold">
              [{citation.citationId}]
            </span>
          )}
          {score != null && (
            <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded">
              {(score * 100).toFixed(0)}% match
            </span>
          )}
        </div>
      </div>
      {citation.fileName && citation.fileName !== title && (
        <p className="text-slate-500 text-[10px]">📄 {citation.fileName}</p>
      )}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Intent badge — shown in header area during sending
// ---------------------------------------------------------------------------
function getLoadingMessage(isSending: boolean): string {
  return isSending ? 'Processing your message...' : '';
}

// ---------------------------------------------------------------------------
// Main ChatPage Component
// ---------------------------------------------------------------------------
export const ChatPage: React.FC = () => {
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputQuery, setInputQuery] = useState('');
  const [isLoadingSessions, setIsLoadingSessions] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [expandedCitations, setExpandedCitations] = useState<Record<string, boolean>>({});
  const [error, setError] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Auto-scroll to bottom of messages
  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, isSending, scrollToBottom]);

  // Auto-resize textarea
  useEffect(() => {
    const ta = textareaRef.current;
    if (ta) {
      ta.style.height = 'auto';
      ta.style.height = Math.min(ta.scrollHeight, 120) + 'px';
    }
  }, [inputQuery]);

  // Load chat sessions on mount
  useEffect(() => {
    fetchSessions();
  }, []);

  // Load session messages whenever activeSessionId changes
  useEffect(() => {
    if (activeSessionId) {
      loadSessionDetails(activeSessionId);
    } else {
      setMessages([]);
    }
  }, [activeSessionId]);

  const fetchSessions = async () => {
    setIsLoadingSessions(true);
    setError(null);
    try {
      const data = await chatService.getSessions();
      setSessions(data);
      if (data.length > 0) {
        setActiveSessionId(data[0].id);
      } else {
        handleCreateSession();
      }
    } catch (err: any) {
      console.error('Failed to load chat sessions:', err);
      setError('Could not load chat sessions. Please ensure the backend is running.');
    } finally {
      setIsLoadingSessions(false);
    }
  };

  const loadSessionDetails = async (sessionId: string) => {
    setIsLoadingMessages(true);
    setError(null);
    try {
      const detail = await chatService.getSessionDetails(sessionId);
      setMessages(detail.messages || []);
    } catch (err: any) {
      console.error('Failed to load session messages:', err);
      setError('Failed to load conversation history.');
    } finally {
      setIsLoadingMessages(false);
    }
  };

  const handleCreateSession = async () => {
    setError(null);
    try {
      const newSession = await chatService.createSession();
      setSessions((prev) => [newSession, ...prev]);
      setActiveSessionId(newSession.id);
      setMessages([]);
    } catch (err: any) {
      console.error('Failed to create session:', err);
      setError('Failed to create a new chat session.');
    }
  };

  const handleDeleteSession = async (e: React.MouseEvent, sessionId: string) => {
    e.stopPropagation();
    try {
      await chatService.deleteSession(sessionId);
      const updated = sessions.filter((s) => s.id !== sessionId);
      setSessions(updated);
      if (activeSessionId === sessionId) {
        if (updated.length > 0) {
          setActiveSessionId(updated[0].id);
        } else {
          setActiveSessionId(null);
          setMessages([]);
        }
      }
    } catch (err: any) {
      console.error('Failed to delete session:', err);
      setError('Failed to delete session.');
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputQuery.trim() || isSending) return;

    let targetSessionId = activeSessionId;
    if (!targetSessionId) {
      try {
        const created = await chatService.createSession();
        setSessions([created]);
        targetSessionId = created.id;
        setActiveSessionId(created.id);
      } catch (err) {
        setError('Failed to initialize session.');
        return;
      }
    }

    const userQuery = inputQuery.trim();
    setInputQuery('');
    setIsSending(true);
    setError(null);

    // Optimistically append user message
    const tempUserMsg: ChatMessage = {
      id: 'temp-' + Date.now(),
      role: 'USER',
      content: userQuery,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMsg]);

    try {
      const res = await chatService.sendMessage(targetSessionId, userQuery);
      setMessages((prev) => {
        const filtered = prev.filter((m) => m.id !== tempUserMsg.id);
        return [...filtered, res.userMessage, res.assistantMessage];
      });

      // Reflect the backend's auto-generated title immediately, without a page reload
      const autoTitle: string =
        userQuery.length > 50 ? `${userQuery.slice(0, 50)}...` : userQuery;
      setSessions((prev) =>
        prev.map((s) =>
          s.id === targetSessionId
            ? {
                ...s,
                updatedAt: new Date().toISOString(),
                title: s.title === 'New Chat' ? autoTitle : s.title,
              }
            : s
        )
      );
    } catch (err: any) {
      console.error('Failed to send message:', err);
      // Remove optimistic message on failure
      setMessages((prev) => prev.filter((m) => m.id !== tempUserMsg.id));
      const detail =
        err.response?.data?.message ||
        err.response?.data?.error ||
        'Something went wrong processing your message. Please try again.';
      setError(detail);
    } finally {
      setIsSending(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage(e as any);
    }
  };

  const toggleCitation = (msgId: string) => {
    setExpandedCitations((prev) => ({
      ...prev,
      [msgId]: !prev[msgId],
    }));
  };

  // ---------------------------------------------------------------------------
  // Render
  // ---------------------------------------------------------------------------
  return (
    <div className="flex h-[calc(100vh-4rem)] bg-slate-950 text-slate-100 overflow-hidden">
      {/* --- Sidebar: Conversations List --- */}
      <aside className="w-64 border-r border-slate-800/80 bg-slate-900/40 flex flex-col flex-shrink-0">
        <div className="p-4 border-b border-slate-800/80">
          <button
            id="btn-new-conversation"
            onClick={handleCreateSession}
            className="w-full py-2.5 px-4 bg-brand-600 hover:bg-brand-500 text-white font-medium text-xs rounded-xl shadow-lg transition flex items-center justify-center space-x-2"
          >
            <Plus className="w-4 h-4" />
            <span>New Conversation</span>
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-3 space-y-1">
          <div className="px-2 pb-2 text-[10px] font-semibold text-slate-500 uppercase tracking-wider">
            History ({sessions.length})
          </div>

          {isLoadingSessions ? (
            <div className="p-4 text-center text-xs text-slate-500 flex items-center justify-center space-x-2">
              <Loader2 className="w-4 h-4 animate-spin text-brand-500" />
              <span>Loading sessions...</span>
            </div>
          ) : sessions.length === 0 ? (
            <div className="p-4 text-center text-xs text-slate-500">
              No conversations yet. Start a new chat!
            </div>
          ) : (
            sessions.map((session) => {
              const isActive = session.id === activeSessionId;
              return (
                <div
                  key={session.id}
                  onClick={() => setActiveSessionId(session.id)}
                  className={`group relative flex items-center justify-between p-2.5 rounded-xl cursor-pointer transition text-xs ${
                    isActive
                      ? 'bg-brand-600/20 text-brand-300 border border-brand-500/30'
                      : 'hover:bg-slate-800/60 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <div className="flex items-center space-x-2 min-w-0 pr-6">
                    <MessageSquare className="w-3.5 h-3.5 flex-shrink-0" />
                    <span className="truncate font-medium">{session.title}</span>
                  </div>

                  <button
                    onClick={(e) => handleDeleteSession(e, session.id)}
                    className="opacity-0 group-hover:opacity-100 p-1 hover:text-red-400 text-slate-500 transition rounded"
                    title="Delete Session"
                    id={`btn-delete-session-${session.id}`}
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              );
            })
          )}
        </div>
      </aside>

      {/* --- Main Area: Active Chat Window --- */}
      <main className="flex-1 flex flex-col h-full bg-slate-950 relative">
        {/* Chat Window Header */}
        <header className="h-14 border-b border-slate-800/80 px-6 flex items-center justify-between bg-slate-900/20 backdrop-blur-sm">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-lg bg-brand-600/20 text-brand-400 flex items-center justify-center">
              <Brain className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-white">IntelliAssist</h2>
              <p className="text-[10px] text-slate-400">
                Natural conversation · General knowledge · Enterprise RAG · Contextual memory
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5 animate-pulse"></span>
              Tenant Isolated
            </span>
          </div>
        </header>

        {/* Global Error Banner */}
        {error && (
          <div className="mx-6 mt-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
            <button
              onClick={() => setError(null)}
              className="text-xs hover:underline text-slate-400"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Message Stream */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {isLoadingMessages ? (
            <div className="flex items-center justify-center h-full text-slate-500 text-xs space-x-2">
              <Loader2 className="w-5 h-5 animate-spin text-brand-500" />
              <span>Fetching conversation memory...</span>
            </div>
          ) : messages.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-center space-y-6 max-w-lg mx-auto">
              <div className="w-16 h-16 rounded-2xl bg-brand-600/10 border border-brand-500/20 flex items-center justify-center text-brand-400">
                <Sparkles className="w-8 h-8" />
              </div>
              <div className="space-y-2">
                <h3 className="text-base font-semibold text-white">IntelliAssist is ready</h3>
                <p className="text-xs text-slate-400 leading-relaxed">
                  Ask anything — casual conversation, general knowledge, technical explanations,
                  or questions about your organization's documents. I'll respond intelligently.
                </p>
              </div>
              {/* Quick-start prompts */}
              <div className="grid grid-cols-1 gap-2 w-full max-w-sm">
                {[
                  { icon: <Zap className="w-3 h-3" />, label: 'Explain Spring Boot', query: 'Explain Spring Boot with an example' },
                  { icon: <BookOpen className="w-3 h-3" />, label: 'List my documents', query: 'List all my uploaded documents' },
                  { icon: <Brain className="w-3 h-3" />, label: 'What is RAG?', query: 'Explain RAG architecture in detail' },
                ].map((item) => (
                  <button
                    key={item.query}
                    onClick={() => setInputQuery(item.query)}
                    className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-900 border border-slate-800 hover:border-brand-500/40 hover:bg-slate-800/80 text-xs text-slate-400 hover:text-slate-200 transition text-left"
                  >
                    <span className="text-brand-400">{item.icon}</span>
                    {item.label}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((msg) => {
              const isUser = msg.role === 'USER';
              const hasCitations = !isUser && msg.citations && msg.citations.length > 0;
              return (
                <div
                  key={msg.id}
                  className={`flex space-x-4 max-w-3xl ${
                    isUser ? 'ml-auto flex-row-reverse space-x-reverse' : ''
                  }`}
                >
                  {/* Avatar */}
                  <div
                    className={`w-8 h-8 rounded-xl flex items-center justify-center flex-shrink-0 ${
                      isUser
                        ? 'bg-brand-600 text-white'
                        : 'bg-slate-800 text-brand-400 border border-slate-700'
                    }`}
                  >
                    {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                  </div>

                  {/* Bubble Content */}
                  <div className="space-y-2 max-w-2xl min-w-0">
                    <div
                      className={`p-4 rounded-2xl text-xs leading-relaxed ${
                        isUser
                          ? 'bg-brand-600 text-white rounded-tr-none shadow-lg whitespace-pre-wrap'
                          : 'glass-panel text-slate-200 rounded-tl-none border border-slate-800'
                      }`}
                    >
                      {isUser ? msg.content : renderMarkdown(msg.content)}
                    </div>

                    {/* Citations Section for Assistant Turn */}
                    {hasCitations && (
                      <div className="pt-1">
                        <button
                          id={`btn-citations-${msg.id}`}
                          onClick={() => toggleCitation(msg.id)}
                          className="flex items-center space-x-1.5 text-[11px] font-medium text-brand-400 hover:text-brand-300 transition"
                        >
                          <BookOpen className="w-3.5 h-3.5" />
                          <span>
                            {msg.citations!.length} Verified{' '}
                            {msg.citations!.length === 1 ? 'Source' : 'Sources'}
                          </span>
                          {expandedCitations[msg.id] ? (
                            <ChevronDown className="w-3 h-3" />
                          ) : (
                            <ChevronRight className="w-3 h-3" />
                          )}
                        </button>

                        {expandedCitations[msg.id] && (
                          <div className="mt-2 space-y-2 pl-2 border-l-2 border-brand-500/30">
                            {msg.citations!.map((cite, idx) => (
                              <CitationCard key={idx} citation={cite} />
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}

          {/* Pending Response Indicator */}
          {isSending && (
            <div className="flex space-x-4 max-w-3xl">
              <div className="w-8 h-8 rounded-xl bg-slate-800 text-brand-400 border border-slate-700 flex items-center justify-center flex-shrink-0">
                <Bot className="w-4 h-4" />
              </div>
              <div className="glass-panel p-4 rounded-2xl rounded-tl-none border border-slate-800 text-xs text-slate-400 flex items-center space-x-2">
                <Loader2 className="w-4 h-4 animate-spin text-brand-500" />
                <span>Thinking...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* --- Footer: Input Form --- */}
        <footer className="p-4 border-t border-slate-800/80 bg-slate-900/30 backdrop-blur-sm">
          <form onSubmit={handleSendMessage} className="max-w-4xl mx-auto flex items-end space-x-3">
            <textarea
              ref={textareaRef}
              id="chat-input"
              value={inputQuery}
              onChange={(e) => setInputQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask anything — chat, technical questions, or enterprise documents... (Shift+Enter for new line)"
              disabled={isSending}
              rows={1}
              className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-brand-500 transition disabled:opacity-50 resize-none"
              style={{ minHeight: '44px', maxHeight: '120px' }}
            />
            <button
              id="btn-send-message"
              type="submit"
              disabled={!inputQuery.trim() || isSending}
              className="py-3 px-5 bg-brand-600 hover:bg-brand-500 text-white font-medium text-xs rounded-xl shadow-lg transition flex items-center space-x-2 disabled:opacity-50 flex-shrink-0"
            >
              {isSending ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <>
                  <span>Send</span>
                  <Send className="w-3.5 h-3.5" />
                </>
              )}
            </button>
          </form>
          <p className="text-center text-[10px] text-slate-600 mt-2">
            IntelliAssist · Intelligent routing · Enterprise-grounded answers · Contextual memory
          </p>
        </footer>
      </main>
    </div>
  );
};
