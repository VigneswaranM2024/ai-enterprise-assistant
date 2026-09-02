import React, { useState, useEffect, useRef } from 'react';
import { chatService } from '../services/chatService';
import { ChatSession, ChatMessage } from '../types/chat.types';
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
} from 'lucide-react';

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

  // Auto-scroll to bottom of messages
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isSending]);

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
        // Auto-create initial session if none exists
        handleCreateSession();
      }
    } catch (err: any) {
      console.error('Failed to load chat sessions:', err);
      setError('Could not load chat sessions. Please ensure backend is running.');
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

    // Optimistically append User message
    const tempUserMsg: ChatMessage = {
      id: 'temp-' + Date.now(),
      role: 'USER',
      content: userQuery,
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, tempUserMsg]);

    try {
      const res = await chatService.sendMessage(targetSessionId, userQuery);
      // Replace optimistic message with actual backend turns
      setMessages((prev) => {
        const filtered = prev.filter((m) => m.id !== tempUserMsg.id);
        return [...filtered, res.userMessage, res.assistantMessage];
      });

      // Update session title in session list
      setSessions((prev) =>
        prev.map((s) =>
          s.id === targetSessionId ? { ...s, updatedAt: new Date().toISOString() } : s
        )
      );
    } catch (err: any) {
      console.error('Failed to send message:', err);
      const detail = err.response?.data?.message || 'Error processing AI query.';
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

  return (
    <div className="flex h-[calc(100vh-4rem)] bg-slate-950 text-slate-100 overflow-hidden">
      {/* --- Sidebar: Conversations List --- */}
      <aside className="w-64 border-r border-slate-800/80 bg-slate-900/40 flex flex-col flex-shrink-0">
        <div className="p-4 border-b border-slate-800/80">
          <button
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
              <Bot className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-white">RAG Enterprise Assistant</h2>
              <p className="text-[10px] text-slate-400">
                Grounding: Secure Document Index • Model: Groq / Llama 3
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5 animate-pulse"></span>
              Isolated Context
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
            <div className="flex flex-col items-center justify-center h-full text-center space-y-4 max-w-md mx-auto">
              <div className="w-16 h-16 rounded-2xl bg-brand-600/10 border border-brand-500/20 flex items-center justify-center text-brand-400">
                <Sparkles className="w-8 h-8" />
              </div>
              <div className="space-y-1">
                <h3 className="text-base font-semibold text-white">Ask your Enterprise Data</h3>
                <p className="text-xs text-slate-400">
                  Ask questions about ingested documents, policies, and project specs. Answers are verified with exact citations.
                </p>
              </div>
            </div>
          ) : (
            messages.map((msg) => {
              const isUser = msg.role === 'USER';
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
                  <div className="space-y-2 max-w-2xl">
                    <div
                      className={`p-4 rounded-2xl text-xs leading-relaxed whitespace-pre-wrap ${
                        isUser
                          ? 'bg-brand-600 text-white rounded-tr-none shadow-lg'
                          : 'glass-panel text-slate-200 rounded-tl-none border border-slate-800'
                      }`}
                    >
                      {msg.content}
                    </div>

                    {/* Citations Section for Assistant Turn */}
                    {!isUser && msg.citations && msg.citations.length > 0 && (
                      <div className="pt-1">
                        <button
                          onClick={() => toggleCitation(msg.id)}
                          className="flex items-center space-x-1.5 text-[11px] font-medium text-brand-400 hover:text-brand-300 transition"
                        >
                          <BookOpen className="w-3.5 h-3.5" />
                          <span>
                            {msg.citations.length} Verified {msg.citations.length === 1 ? 'Source' : 'Sources'}
                          </span>
                          {expandedCitations[msg.id] ? (
                            <ChevronDown className="w-3 h-3" />
                          ) : (
                            <ChevronRight className="w-3 h-3" />
                          )}
                        </button>

                        {expandedCitations[msg.id] && (
                          <div className="mt-2 space-y-2 pl-2 border-l-2 border-brand-500/30">
                            {msg.citations.map((cite, idx) => (
                              <div
                                key={idx}
                                className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800 text-[11px] space-y-1"
                              >
                                <div className="flex items-center justify-between font-semibold text-slate-300">
                                  <span>{cite.title || 'Document Chunk'}</span>
                                  {cite.score && (
                                    <span className="text-[10px] text-emerald-400 bg-emerald-500/10 px-1.5 py-0.5 rounded">
                                      {(cite.score * 100).toFixed(0)}% match
                                    </span>
                                  )}
                                </div>
                                {cite.snippet && (
                                  <p className="text-slate-400 italic text-[10px] line-clamp-2">
                                    "{cite.snippet}"
                                  </p>
                                )}
                              </div>
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
                <span>Running secure RAG vector search & LLM inference...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* --- Footer: Input Form --- */}
        <footer className="p-4 border-t border-slate-800/80 bg-slate-900/30 backdrop-blur-sm">
          <form onSubmit={handleSendMessage} className="max-w-4xl mx-auto flex items-center space-x-3">
            <textarea
              value={inputQuery}
              onChange={(e) => setInputQuery(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask a question about enterprise policies, codebases, or documents... (Shift+Enter for new line)"
              disabled={isSending}
              rows={1}
              className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-4 py-3 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-brand-500 transition disabled:opacity-50 resize-none overflow-hidden"
              style={{ minHeight: '44px', maxHeight: '120px' }}
            />
            <button
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
        </footer>
      </main>
    </div>
  );
};
