import React, { useState } from 'react';
import { History, RotateCcw, CheckCircle2, XCircle, AlertTriangle, RefreshCw } from 'lucide-react';
import { formatBytes, formatDate, truncateHash } from '../utils/formatters';

export const ActivityLogsTable = ({ backupLogs, recoveryLogs, loading, onRefresh }) => {
  const [activeSubTab, setActiveSubTab] = useState('backups');

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/50 backdrop-blur-sm overflow-hidden">
      {/* Header with tabs and refresh button */}
      <div className="p-4 sm:p-5 flex flex-col sm:flex-row items-center justify-between gap-4 border-b border-slate-800">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveSubTab('backups')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
              activeSubTab === 'backups'
                ? 'bg-brand-600 text-white shadow-md shadow-brand-600/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            <History className="w-3.5 h-3.5" />
            Backup Activity ({backupLogs.length})
          </button>

          <button
            onClick={() => setActiveSubTab('recovery')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
              activeSubTab === 'recovery'
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/20'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
            }`}
          >
            <RotateCcw className="w-3.5 h-3.5" />
            Recovery &amp; Restore Activity ({recoveryLogs.length})
          </button>
        </div>

        <button
          onClick={onRefresh}
          className="p-1.5 text-slate-400 hover:text-slate-200 hover:bg-slate-800 rounded-lg transition-colors"
          title="Refresh logs"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Content */}
      <div className="overflow-x-auto">
        {activeSubTab === 'backups' ? (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="py-3 px-4 font-semibold">Event ID</th>
                <th className="py-3 px-4 font-semibold">File</th>
                <th className="py-3 px-4 font-semibold">Version</th>
                <th className="py-3 px-4 font-semibold">Size</th>
                <th className="py-3 px-4 font-semibold">SHA-256</th>
                <th className="py-3 px-4 font-semibold">Status</th>
                <th className="py-3 px-4 font-semibold">Retries</th>
                <th className="py-3 px-4 font-semibold">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300 text-xs">
              {backupLogs.length === 0 ? (
                <tr>
                  <td colSpan="8" className="py-10 text-center text-slate-500">
                    No backup activities logged yet.
                  </td>
                </tr>
              ) : (
                backupLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/30">
                    <td className="py-3 px-4 font-mono text-slate-500">#{log.id}</td>
                    <td className="py-3 px-4 font-medium text-white truncate max-w-xs">{log.filename}</td>
                    <td className="py-3 px-4 font-mono">v{log.versionNumber}</td>
                    <td className="py-3 px-4 font-mono">{formatBytes(log.fileSizeBytes)}</td>
                    <td className="py-3 px-4 font-mono text-indigo-300">{truncateHash(log.checksumSha256, 6)}</td>
                    <td className="py-3 px-4">
                      {log.status === 'COMPLETED' ? (
                        <span className="inline-flex items-center gap-1 text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                          <CheckCircle2 className="w-3 h-3" /> Completed
                        </span>
                      ) : log.status === 'FAILED' ? (
                        <span className="inline-flex items-center gap-1 text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded-full border border-rose-500/20" title={log.failureReason}>
                          <XCircle className="w-3 h-3" /> Failed
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-full border border-amber-500/20">
                          <AlertTriangle className="w-3 h-3" /> {log.status}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 font-mono text-slate-400">{log.retryCount || 0}</td>
                    <td className="py-3 px-4 text-slate-400">{formatDate(log.createdAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-950/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
              <tr>
                <th className="py-3 px-4 font-semibold">Recovery ID</th>
                <th className="py-3 px-4 font-semibold">File</th>
                <th className="py-3 px-4 font-semibold">Restored Version</th>
                <th className="py-3 px-4 font-semibold">Integrity Verified</th>
                <th className="py-3 px-4 font-semibold">Status</th>
                <th className="py-3 px-4 font-semibold">Timestamp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60 text-slate-300 text-xs">
              {recoveryLogs.length === 0 ? (
                <tr>
                  <td colSpan="6" className="py-10 text-center text-slate-500">
                    No recovery activities logged yet.
                  </td>
                </tr>
              ) : (
                recoveryLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-800/30">
                    <td className="py-3 px-4 font-mono text-slate-500">#{log.id}</td>
                    <td className="py-3 px-4 font-medium text-white truncate max-w-xs">{log.filename}</td>
                    <td className="py-3 px-4 font-mono font-bold text-brand-300">v{log.restoredVersionNumber}</td>
                    <td className="py-3 px-4">
                      {log.checksumVerified ? (
                        <span className="inline-flex items-center gap-1 text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                          <CheckCircle2 className="w-3 h-3" /> Pre-Verified Match
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded-full border border-rose-500/20">
                          <XCircle className="w-3 h-3" /> Mismatch
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      {log.status === 'SUCCESS' ? (
                        <span className="inline-flex items-center gap-1 text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded-full border border-emerald-500/20">
                          <CheckCircle2 className="w-3 h-3" /> Success
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 text-rose-400 bg-rose-500/10 px-2 py-0.5 rounded-full border border-rose-500/20" title={log.failureReason}>
                          <XCircle className="w-3 h-3" /> {log.status}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-400">{formatDate(log.restoredAt)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
