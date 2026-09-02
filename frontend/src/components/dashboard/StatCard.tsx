import React from 'react';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  changeText?: string;
  isPositive?: boolean;
  icon: LucideIcon;
  iconColor?: string;
  iconBg?: string;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  changeText,
  isPositive = true,
  icon: Icon,
  iconColor = 'text-indigo-400',
  iconBg = 'bg-indigo-600/20',
}) => {
  return (
    <div className="glass-panel p-5 rounded-xl border-slate-800 space-y-3 hover:border-slate-700 transition">
      <div className="flex justify-between items-center text-slate-400">
        <span className="text-xs font-medium">{title}</span>
        <div className={`w-8 h-8 rounded-lg ${iconBg} ${iconColor} flex items-center justify-center`}>
          <Icon className="w-4 h-4" />
        </div>
      </div>
      <div>
        <div className="text-2xl font-bold text-white tracking-tight">{value}</div>
        {changeText && (
          <div className={`text-[10px] font-semibold mt-1 ${isPositive ? 'text-emerald-400' : 'text-amber-400'}`}>
            {changeText}
          </div>
        )}
      </div>
    </div>
  );
};
