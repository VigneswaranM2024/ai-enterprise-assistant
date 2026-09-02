import React from 'react';
import { useAuth } from '../hooks/useAuth';
import { StatCard } from '../components/dashboard/StatCard';
import { ActivityFeed } from '../components/dashboard/ActivityFeed';
import { MessageSquare, FolderUp, Video, Mail, ShieldCheck, Database, Cpu, FileText } from 'lucide-react';
import { Link } from 'react-router-dom';

export const EmployeeDashboardPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="glass-panel p-6 rounded-2xl border-slate-800 flex flex-col md:flex-row justify-between items-start md:items-center space-y-4 md:space-y-0">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">
            Welcome back, {user?.fullName || 'Employee'}! 👋
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Connected to tenant <span className="font-semibold text-indigo-400">{user?.tenantSlug}</span> under security classification <span className="font-mono text-emerald-400">{user?.securityClassification}</span>.
          </p>
        </div>
        <div className="flex items-center space-x-2 bg-emerald-500/10 text-emerald-400 px-3 py-1.5 rounded-full text-xs font-medium border border-emerald-500/20">
          <ShieldCheck className="w-4 h-4" />
          <span>Active OAuth2 Session</span>
        </div>
      </div>

      {/* Statistics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="My Queries Today"
          value={18}
          changeText="+4 from yesterday"
          isPositive={true}
          icon={MessageSquare}
          iconColor="text-indigo-400"
          iconBg="bg-indigo-600/20"
        />
        <StatCard
          title="Documents Accessible"
          value={142}
          changeText="RBAC Pre-Filtered"
          isPositive={true}
          icon={FileText}
          iconColor="text-emerald-400"
          iconBg="bg-emerald-600/20"
        />
        <StatCard
          title="Vector Store Status"
          value="Online"
          changeText="pgvector HNSW HITS"
          isPositive={true}
          icon={Database}
          iconColor="text-purple-400"
          iconBg="bg-purple-600/20"
        />
        <StatCard
          title="Average TTFT"
          value="740 ms"
          changeText="Streaming Token Rate"
          isPositive={true}
          icon={Cpu}
          iconColor="text-amber-400"
          iconBg="bg-amber-600/20"
        />
      </div>

      {/* Main Grid: Quick Actions & Live Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Quick Action Portal</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Link
              to="/app/chat"
              className="glass-panel p-5 rounded-xl hover:border-indigo-500/50 transition group space-y-3"
            >
              <div className="w-10 h-10 rounded-lg bg-indigo-600/20 text-indigo-400 flex items-center justify-center group-hover:scale-105 transition">
                <MessageSquare className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-semibold text-sm text-white">AI Search Chat</h3>
                <p className="text-xs text-slate-400 mt-0.5">Query internal enterprise RAG context</p>
              </div>
            </Link>

            <Link
              to="/app/knowledge"
              className="glass-panel p-5 rounded-xl hover:border-indigo-500/50 transition group space-y-3"
            >
              <div className="w-10 h-10 rounded-lg bg-emerald-600/20 text-emerald-400 flex items-center justify-center group-hover:scale-105 transition">
                <FolderUp className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-semibold text-sm text-white">Upload Knowledge</h3>
                <p className="text-xs text-slate-400 mt-0.5">Ingest PDFs & DOCX into vector store</p>
              </div>
            </Link>

            <Link
              to="/app/meetings"
              className="glass-panel p-5 rounded-xl hover:border-indigo-500/50 transition group space-y-3"
            >
              <div className="w-10 h-10 rounded-lg bg-purple-600/20 text-purple-400 flex items-center justify-center group-hover:scale-105 transition">
                <Video className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-semibold text-sm text-white">Meetings & Transcripts</h3>
                <p className="text-xs text-slate-400 mt-0.5">View AI action items & summaries</p>
              </div>
            </Link>

            <Link
              to="/app/utilities"
              className="glass-panel p-5 rounded-xl hover:border-indigo-500/50 transition group space-y-3"
            >
              <div className="w-10 h-10 rounded-lg bg-amber-600/20 text-amber-400 flex items-center justify-center group-hover:scale-105 transition">
                <Mail className="w-5 h-5" />
              </div>
              <div>
                <h3 className="font-semibold text-sm text-white">AI Utilities</h3>
                <p className="text-xs text-slate-400 mt-0.5">Email, SQL, & Code generator</p>
              </div>
            </Link>
          </div>
        </div>

        <div>
          <ActivityFeed />
        </div>
      </div>
    </div>
  );
};
