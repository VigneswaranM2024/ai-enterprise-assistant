import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { LayoutDashboard, MessageSquare, FolderKanban, Users, ShieldAlert, FileText, Terminal } from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { isAdmin } = useAuth();

  const navItems = [
    { label: 'Dashboard', path: '/app/dashboard', icon: LayoutDashboard },
    { label: 'AI Chat Assistant', path: '/app/chat', icon: MessageSquare },
    { label: 'Knowledge Base', path: '/app/knowledge', icon: FolderKanban },
    { label: 'Meetings', path: '/app/meetings', icon: Users },
    { label: 'AI Utilities', path: '/app/utilities', icon: Terminal },
  ];

  const adminItems = [
    { label: 'User Governance', path: '/app/admin/users', icon: Users },
    { label: 'Audit Security Logs', path: '/app/admin/audit-logs', icon: ShieldAlert },
  ];

  return (
    <aside className="w-64 border-r border-slate-800/80 bg-slate-950 flex flex-col justify-between p-4 hidden md:flex">
      <div className="space-y-6">
        <div>
          <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 mb-3 px-3">
            Core Workspace
          </div>
          <nav className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  className={({ isActive }) =>
                    `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-xs font-medium transition ${
                      isActive
                        ? 'bg-indigo-600/20 text-indigo-400 border border-indigo-500/30'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
                    }`
                  }
                >
                  <Icon className="w-4 h-4" />
                  <span>{item.label}</span>
                </NavLink>
              );
            })}
          </nav>
        </div>

        {isAdmin && (
          <div>
            <div className="text-[10px] font-bold uppercase tracking-wider text-slate-500 mb-3 px-3">
              Admin Governance
            </div>
            <nav className="space-y-1">
              {adminItems.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.path}
                    to={item.path}
                    className={({ isActive }) =>
                      `flex items-center space-x-3 px-3 py-2.5 rounded-lg text-xs font-medium transition ${
                        isActive
                          ? 'bg-indigo-600/20 text-indigo-400 border border-indigo-500/30'
                          : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
                      }`
                    }
                  >
                    <Icon className="w-4 h-4" />
                    <span>{item.label}</span>
                  </NavLink>
                );
              })}
            </nav>
          </div>
        )}
      </div>

      <div className="p-3 glass-panel rounded-xl text-xs space-y-1 border-slate-800">
        <div className="font-semibold text-slate-200 flex items-center space-x-1.5">
          <FileText className="w-3.5 h-3.5 text-indigo-400" />
          <span>PostgreSQL RAG</span>
        </div>
        <div className="text-[10px] text-slate-400">pgvector HNSW Connected</div>
      </div>
    </aside>
  );
};
