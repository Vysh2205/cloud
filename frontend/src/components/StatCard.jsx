import React from 'react';

export const StatCard = ({ title, value, subtitle, icon: Icon, color = 'brand' }) => {
  const colorStyles = {
    brand: 'bg-brand-500/10 text-brand-400 border-brand-500/20',
    emerald: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    indigo: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
    rose: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
  }[color] || 'bg-slate-800 text-slate-300 border-slate-700';

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/50 p-5 shadow-sm backdrop-blur-sm transition-all hover:border-slate-700">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium uppercase tracking-wider text-slate-400">{title}</span>
        <div className={`flex h-10 w-10 items-center justify-center rounded-xl border ${colorStyles}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
      <div className="mt-3">
        <div className="text-2xl font-bold tracking-tight text-white font-mono">{value}</div>
        {subtitle && <p className="mt-1 text-xs text-slate-400">{subtitle}</p>}
      </div>
    </div>
  );
};
