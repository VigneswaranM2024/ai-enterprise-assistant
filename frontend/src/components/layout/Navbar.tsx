import React from 'react';
import { useAuth } from '../../hooks/useAuth';
import { NotificationPopover } from '../notifications/NotificationPopover';
import { LogOut, User, Building, ShieldCheck } from 'lucide-react';

interface NavbarProps {
  onMobileMenuToggle: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onMobileMenuToggle }) => {
  const { user, logout } = useAuth();

  return (
    <header className="h-16 border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md sticky top-0 z-30 px-4 md:px-6 flex items-center justify-between">
      {/* Left Branding & Mobile Toggle */}
      <div className="flex items-center space-x-4">
        <button
          onClick={onMobileMenuToggle}
          className="md:hidden text-slate-400 hover:text-white p-1 focus:outline-none"
        >
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
        </button>
        <div className="flex items-center space-x-2">
          <div className="w-8 h-8 rounded-lg bg-indigo-600/30 text-indigo-400 flex items-center justify-center font-bold">
            AI
          </div>
          <span className="font-bold text-base tracking-tight hidden sm:inline-block">
            Enterprise Assistant
          </span>
        </div>
      </div>

      {/* Center Tenant Badge */}
      <div className="hidden md:flex items-center space-x-2 text-xs text-slate-400 bg-slate-900 border border-slate-800 px-3 py-1.5 rounded-full">
        <Building className="w-3.5 h-3.5 text-indigo-400" />
        <span>Tenant:</span>
        <span className="font-semibold text-slate-200">{user?.tenantSlug || 'acme-corp'}</span>
      </div>

      {/* Right User Controls */}
      <div className="flex items-center space-x-3">
        <NotificationPopover />

        {user && (
          <div className="flex items-center space-x-3 text-right">
            <div className="hidden sm:block">
              <div className="text-xs font-semibold text-slate-200">{user.fullName}</div>
              <div className="text-[10px] text-slate-400 flex items-center justify-end space-x-1">
                <ShieldCheck className="w-3 h-3 text-emerald-400 inline" />
                <span>{user.roles.includes('ROLE_ADMIN') ? 'ADMIN' : 'EMPLOYEE'}</span>
              </div>
            </div>
            <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300">
              <User className="w-4 h-4" />
            </div>
          </div>
        )}

        <button
          onClick={logout}
          title="Sign Out"
          className="p-2 rounded-lg text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
};
