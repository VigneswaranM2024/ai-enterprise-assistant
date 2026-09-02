import React from 'react';
import { Search, FileUp, Video, ShieldCheck, UserPlus } from 'lucide-react';

interface ActivityItem {
  id: string;
  user: string;
  action: string;
  target: string;
  timestamp: string;
  type: 'SEARCH' | 'UPLOAD' | 'MEETING' | 'SECURITY' | 'USER';
}

const DUMMY_ACTIVITIES: ActivityItem[] = [
  {
    id: '1',
    user: 'Sarah Connor',
    action: 'executed hybrid RAG search',
    target: '"PostgreSQL pgvector Architecture"',
    timestamp: '2m ago',
    type: 'SEARCH',
  },
  {
    id: '2',
    user: 'Sarah Connor',
    action: 'uploaded knowledge document',
    target: 'Q3 Financial Audit Policy.pdf',
    timestamp: '15m ago',
    type: 'UPLOAD',
  },
  {
    id: '3',
    user: 'System Admin',
    action: 'generated AI meeting summary',
    target: 'Q3 Platform Strategy Sync',
    timestamp: '1h ago',
    type: 'MEETING',
  },
  {
    id: '4',
    user: 'System Admin',
    action: 'provisioned user role',
    target: 'john.wick@acme.com (ROLE_EMPLOYEE)',
    timestamp: '2h ago',
    type: 'USER',
  },
];

export const ActivityFeed: React.FC = () => {
  return (
    <div className="glass-panel p-5 rounded-xl border-slate-800 space-y-4">
      <div className="flex justify-between items-center pb-3 border-b border-slate-800">
        <h3 className="text-xs font-bold text-white uppercase tracking-wider">Recent Activity Feed</h3>
        <span className="text-[10px] text-slate-500 font-mono">Live Stream</span>
      </div>

      <div className="space-y-3">
        {DUMMY_ACTIVITIES.map((act) => (
          <div key={act.id} className="flex items-start space-x-3 text-xs">
            <div className="mt-0.5 p-1.5 rounded-lg bg-slate-900 border border-slate-800 flex-shrink-0">
              {act.type === 'SEARCH' && <Search className="w-3.5 h-3.5 text-indigo-400" />}
              {act.type === 'UPLOAD' && <FileUp className="w-3.5 h-3.5 text-emerald-400" />}
              {act.type === 'MEETING' && <Video className="w-3.5 h-3.5 text-purple-400" />}
              {act.type === 'USER' && <UserPlus className="w-3.5 h-3.5 text-amber-400" />}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-slate-300 truncate">
                <span className="font-semibold text-white">{act.user}</span> {act.action}{' '}
                <span className="font-mono text-slate-400">{act.target}</span>
              </p>
              <div className="text-[10px] text-slate-500 mt-0.5">{act.timestamp}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
