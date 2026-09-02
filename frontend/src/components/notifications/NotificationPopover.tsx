import React, { useState } from 'react';
import { Bell, CheckCircle2, FileText, Video, AlertTriangle } from 'lucide-react';

interface NotificationItem {
  id: string;
  title: string;
  message: string;
  time: string;
  type: 'DOC' | 'MEETING' | 'SECURITY';
  read: boolean;
}

const DUMMY_NOTIFICATIONS: NotificationItem[] = [
  {
    id: '1',
    title: 'Document Ingestion Complete',
    message: 'Q3 Financial Audit Policy.pdf is indexed into pgvector with 42 chunks.',
    time: '5m ago',
    type: 'DOC',
    read: false,
  },
  {
    id: '2',
    title: 'Meeting Transcript Ready',
    message: 'AI Platform Strategy Sync summary & action items are generated.',
    time: '1h ago',
    type: 'MEETING',
    read: false,
  },
  {
    id: '3',
    title: 'Security ACL Updated',
    message: 'Your role permissions were updated to include RESTRICTED classification access.',
    time: '3h ago',
    type: 'SECURITY',
    read: true,
  },
];

export const NotificationPopover: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState(DUMMY_NOTIFICATIONS);

  const unreadCount = notifications.filter((n) => !n.read).length;

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-900 transition relative"
        title="Notifications"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-indigo-500 ring-2 ring-slate-950"></span>
        )}
      </button>

      {isOpen && (
        <>
          <div className="fixed inset-0 z-40" onClick={() => setIsOpen(false)}></div>
          <div className="absolute right-0 mt-2 w-80 sm:w-96 glass-panel rounded-xl shadow-2xl border border-slate-800 z-50 overflow-hidden space-y-0">
            <div className="p-3 bg-slate-900/80 border-b border-slate-800 flex justify-between items-center">
              <span className="text-xs font-bold text-white flex items-center space-x-2">
                <span>Notifications</span>
                {unreadCount > 0 && (
                  <span className="px-1.5 py-0.5 rounded-full text-[10px] bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                    {unreadCount} new
                  </span>
                )}
              </span>
              <button
                onClick={markAllAsRead}
                className="text-[10px] text-slate-400 hover:text-indigo-400 transition"
              >
                Mark all read
              </button>
            </div>

            <div className="max-h-80 overflow-y-auto divide-y divide-slate-800/60">
              {notifications.map((n) => (
                <div
                  key={n.id}
                  className={`p-3 text-xs transition flex items-start space-x-3 ${
                    n.read ? 'opacity-60 bg-transparent' : 'bg-indigo-500/5'
                  }`}
                >
                  <div className="mt-0.5 flex-shrink-0">
                    {n.type === 'DOC' && <FileText className="w-4 h-4 text-emerald-400" />}
                    {n.type === 'MEETING' && <Video className="w-4 h-4 text-purple-400" />}
                    {n.type === 'SECURITY' && <AlertTriangle className="w-4 h-4 text-amber-400" />}
                  </div>
                  <div className="flex-1 space-y-1">
                    <div className="flex justify-between items-center">
                      <span className="font-semibold text-slate-200">{n.title}</span>
                      <span className="text-[10px] text-slate-500">{n.time}</span>
                    </div>
                    <p className="text-[11px] text-slate-400 leading-relaxed">{n.message}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
};
