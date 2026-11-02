import React from 'react';
import { Cloud, HardDrive, LogOut, ShieldCheck, User } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { formatBytes } from '../utils/formatters';

export const Navbar = () => {
  const { user, logout } = useAuth();

  const quota = user?.storageQuotaBytes || 5368709120;
  const used = user?.usedStorageBytes || 0;
  const percentage = Math.min(100, Math.round((used / quota) * 100));

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-800 bg-slate-900/80 backdrop-blur-md">
      <div className="flex h-16 items-center justify-between px-6">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-brand-600 to-indigo-500 shadow-lg shadow-brand-500/20">
            <Cloud className="h-6 w-6 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-base font-bold tracking-tight text-white">CloudBackup</span>
              <span className="rounded bg-brand-500/10 px-1.5 py-0.5 text-[10px] font-semibold text-brand-400 border border-brand-500/20">
                DISTRIBUTED
              </span>
            </div>
            <p className="text-xs text-slate-400">AWS S3 &bull; SHA-256 Verified</p>
          </div>
        </div>

        {/* Right side stats & profile */}
        <div className="flex items-center gap-6">
          {/* Storage Quota Bar */}
          <div className="hidden md:flex flex-col gap-1 w-48">
            <div className="flex justify-between text-xs">
              <span className="text-slate-400 flex items-center gap-1">
                <HardDrive className="w-3 h-3 text-slate-400" />
                Storage
              </span>
              <span className="font-mono text-slate-300">
                {formatBytes(used, 1)} / {formatBytes(quota, 0)}
              </span>
            </div>
            <div className="h-1.5 w-full rounded-full bg-slate-800 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  percentage > 85 ? 'bg-rose-500' : percentage > 60 ? 'bg-amber-500' : 'bg-brand-500'
                }`}
                style={{ width: `${percentage}%` }}
              />
            </div>
          </div>

          {/* User profile */}
          <div className="flex items-center gap-3 pl-4 border-l border-slate-800">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-slate-800 text-slate-300 font-semibold border border-slate-700">
              {user?.username ? user.username.charAt(0).toUpperCase() : <User className="w-4 h-4" />}
            </div>
            <div className="hidden sm:block text-left">
              <div className="text-sm font-medium text-slate-200 leading-tight">{user?.username}</div>
              <div className="text-xs text-slate-400">{user?.role}</div>
            </div>

            <button
              onClick={logout}
              title="Sign Out"
              className="p-2 ml-2 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-slate-800/80 transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
