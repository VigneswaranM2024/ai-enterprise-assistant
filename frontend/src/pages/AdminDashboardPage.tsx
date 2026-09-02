import React from 'react';
import { useAuth } from '../hooks/useAuth';
import { StatCard } from '../components/dashboard/StatCard';
import { TokenSpendChart } from '../components/dashboard/TokenSpendChart';
import { ActivityFeed } from '../components/dashboard/ActivityFeed';
import { Users, Database, Cpu, DollarSign, ShieldAlert } from 'lucide-react';
import { Link } from 'react-router-dom';

export const AdminDashboardPage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Admin Governance Dashboard</h1>
          <p className="text-xs text-slate-400 mt-1">
            System metrics, vector store health, and user provisioning for <span className="text-indigo-400 font-semibold">{user?.tenantSlug}</span>
          </p>
        </div>
        <div className="flex space-x-2">
          <Link
            to="/app/admin/users"
            className="px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs rounded-lg transition inline-flex items-center space-x-1.5"
          >
            <Users className="w-3.5 h-3.5" />
            <span>Manage Users</span>
          </Link>
        </div>
      </div>

      {/* Metric Cards Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Total Active Users"
          value={128}
          changeText="+12% from last month"
          isPositive={true}
          icon={Users}
          iconColor="text-indigo-400"
          iconBg="bg-indigo-600/20"
        />
        <StatCard
          title="Indexed Vector Chunks"
          value="184,500"
          changeText="pgvector HNSW 1536-dim"
          isPositive={true}
          icon={Database}
          iconColor="text-emerald-400"
          iconBg="bg-emerald-600/20"
        />
        <StatCard
          title="Avg Search Latency"
          value="42.5 ms"
          changeText="Within 100ms SLA"
          isPositive={true}
          icon={Cpu}
          iconColor="text-purple-400"
          iconBg="bg-purple-600/20"
        />
        <StatCard
          title="Token Spend (Today)"
          value="$14.25"
          changeText="1.42M tokens generated"
          isPositive={true}
          icon={DollarSign}
          iconColor="text-amber-400"
          iconBg="bg-amber-600/20"
        />
      </div>

      {/* Main Grid: Chart & Activity Feed */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <TokenSpendChart />

          {/* System Health Panel */}
          <div className="glass-panel p-5 rounded-xl border-slate-800 space-y-3">
            <h3 className="text-xs font-bold text-white uppercase tracking-wider flex items-center space-x-2">
              <ShieldAlert className="w-4 h-4 text-indigo-400" />
              <span>System Infrastructure Status</span>
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
              <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
                <div className="text-slate-400 text-[10px]">PostgreSQL + pgvector</div>
                <div className="font-semibold text-emerald-400 flex items-center space-x-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                  <span>Healthy (QPS: 420)</span>
                </div>
              </div>

              <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
                <div className="text-slate-400 text-[10px]">Redis Cache & Rate Limiter</div>
                <div className="font-semibold text-emerald-400 flex items-center space-x-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                  <span>99.99% Cache Hit</span>
                </div>
              </div>

              <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
                <div className="text-slate-400 text-[10px]">Spring Boot Virtual Threads</div>
                <div className="font-semibold text-emerald-400 flex items-center space-x-1">
                  <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                  <span>Active Threads: 18</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div>
          <ActivityFeed />
        </div>
      </div>
    </div>
  );
};
