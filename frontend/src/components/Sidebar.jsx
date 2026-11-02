import React from 'react';
import { LayoutDashboard, Files, History, Shield, Server, Activity } from 'lucide-react';

export const Sidebar = ({ activeTab, onTabChange }) => {
  const navItems = [
    { id: 'dashboard', label: 'Overview', icon: LayoutDashboard },
    { id: 'files', label: 'Files & Backups', icon: Files },
    { id: 'activity', label: 'Recovery & Audit Logs', icon: History },
  ];

  return (
    <aside className="w-64 border-r border-slate-800 bg-slate-900/40 p-4 flex flex-col justify-between hidden md:flex shrink-0">
      <div className="space-y-6">
        <div>
          <div className="px-3 mb-2 text-xs font-semibold uppercase tracking-wider text-slate-500">
            Platform
          </div>
          <nav className="space-y-1">
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => onTabChange(item.id)}
                  className={`flex w-full items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-brand-600/15 text-brand-400 border border-brand-500/20 shadow-sm'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                  }`}
                >
                  <Icon className={`w-4 h-4 ${isActive ? 'text-brand-400' : 'text-slate-400'}`} />
                  {item.label}
                </button>
              );
            })}
          </nav>
        </div>

        {/* Distributed Architecture Status Card */}
        <div className="rounded-xl border border-slate-800/80 bg-slate-900/60 p-4 text-xs space-y-3">
          <div className="flex items-center gap-2 font-semibold text-slate-300">
            <Server className="w-4 h-4 text-brand-400" />
            Distributed Node
          </div>
          <div className="space-y-2 text-slate-400">
            <div className="flex justify-between items-center">
              <span>Cloud Storage:</span>
              <span className="flex items-center gap-1 font-mono text-emerald-400">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                AWS S3
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span>Integrity Engine:</span>
              <span className="font-mono text-indigo-400">SHA-256</span>
            </div>
            <div className="flex justify-between items-center">
              <span>Versioning:</span>
              <span className="font-mono text-slate-300">Immutable</span>
            </div>
          </div>
        </div>
      </div>

      {/* Footer Info */}
      <div className="px-3 py-2 text-[11px] text-slate-500 border-t border-slate-800/80">
        <div>v1.0.0 Enterprise Release</div>
        <div>Kubernetes &bull; PostgreSQL</div>
      </div>
    </aside>
  );
};
