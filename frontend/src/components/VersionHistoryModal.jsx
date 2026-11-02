import React, { useState, useEffect } from 'react';
import { History, X, Download, RotateCcw, ShieldCheck, CheckCircle2, AlertTriangle, Loader2 } from 'lucide-react';
import { filesApi } from '../api/filesApi';
import { IntegrityBadge } from './IntegrityBadge';
import { formatBytes, formatDate, truncateHash } from '../utils/formatters';

export const VersionHistoryModal = ({ file, isOpen, onClose, onRestoreSuccess, onToast }) => {
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [verifyingId, setVerifyingId] = useState(null);
  const [restoringId, setRestoringId] = useState(null);

  useEffect(() => {
    if (file && isOpen) {
      loadVersions();
    }
  }, [file, isOpen]);

  const loadVersions = async () => {
    setLoading(true);
    try {
      const res = await filesApi.getFileVersions(file.id);
      setVersions(res.data || []);
    } catch (err) {
      onToast?.(err.message || 'Failed to fetch version history', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (version) => {
    setVerifyingId(version.id);
    try {
      const res = await filesApi.verifyFileIntegrity(file.id, version.id);
      const data = res.data;
      if (data?.verified) {
        onToast?.(`Version ${version.versionNumber} verified: SHA-256 match!`, 'success');
      } else {
        onToast?.(`ALERT: Version ${version.versionNumber} checksum mismatch!`, 'error');
      }
      loadVersions();
    } catch (err) {
      onToast?.(err.message || 'Integrity verification failed', 'error');
    } finally {
      setVerifyingId(null);
    }
  };

  const handleRestore = async (version) => {
    if (!window.confirm(`Are you sure you want to restore Version ${version.versionNumber} as the active current version?`)) {
      return;
    }
    setRestoringId(version.id);
    try {
      const res = await filesApi.restoreVersion(file.id, version.id, {
        promoteToCurrentVersion: true,
        restoreReason: `Rollback to version ${version.versionNumber}`,
      });
      onToast?.(`File successfully restored to version ${version.versionNumber}!`, 'success');
      onRestoreSuccess?.(res.data || res);
      onClose();
    } catch (err) {
      onToast?.(err.message || 'Failed to restore version', 'error');
    } finally {
      setRestoringId(null);
    }
  };

  const handleDownload = (version) => {
    filesApi.downloadFile(file.id, version.id, `${version.versionNumber}_${file.filename}`);
  };

  if (!isOpen || !file) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-3xl rounded-2xl border border-slate-800 bg-slate-900 shadow-2xl p-6 max-h-[85vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <History className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-white">Version Lineage: {file.filename}</h3>
              <p className="text-xs text-slate-400">
                Total revisions: {versions.length} &bull; Never overwrites historical snapshots
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Versions List */}
        <div className="mt-4 overflow-y-auto flex-1 space-y-3 pr-1">
          {loading ? (
            <div className="py-12 flex flex-col items-center justify-center text-slate-400 gap-2">
              <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
              <span className="text-xs">Loading immutable version ledger...</span>
            </div>
          ) : versions.length === 0 ? (
            <div className="py-8 text-center text-slate-500 text-sm">No versions recorded.</div>
          ) : (
            versions.map((ver, idx) => {
              const isCurrent = ver.versionNumber === file.currentVersionNumber;
              return (
                <div
                  key={ver.id}
                  className={`rounded-xl border p-4 transition-all ${
                    isCurrent
                      ? 'border-brand-500/40 bg-brand-500/5'
                      : 'border-slate-800/80 bg-slate-950/60 hover:border-slate-700'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-sm font-bold text-white px-2 py-0.5 rounded bg-slate-800 border border-slate-700">
                          v{ver.versionNumber}
                        </span>
                        {isCurrent && (
                          <span className="text-[10px] font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-full">
                            Active Current
                          </span>
                        )}
                        <IntegrityBadge status={ver.verificationStatus} />
                      </div>
                      <div className="text-xs text-slate-300 font-medium">
                        {ver.changeSummary || 'Standard backup revision'}
                      </div>
                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-slate-400">
                        <span>Size: <strong className="text-slate-200 font-mono">{formatBytes(ver.fileSizeBytes)}</strong></span>
                        <span>Date: <span className="text-slate-300">{formatDate(ver.createdAt)}</span></span>
                        <span>SHA-256: <code className="text-indigo-300 font-mono">{truncateHash(ver.checksumSha256, 6)}</code></span>
                      </div>
                    </div>

                    {/* Actions */}
                    <div className="flex items-center gap-2 shrink-0">
                      <button
                        onClick={() => handleVerify(ver)}
                        disabled={verifyingId === ver.id}
                        title="Verify SHA-256 against S3"
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 transition-colors"
                      >
                        {verifyingId === ver.id ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin text-brand-400" />
                        ) : (
                          <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
                        )}
                        Verify
                      </button>

                      {!isCurrent && (
                        <button
                          onClick={() => handleRestore(ver)}
                          disabled={restoringId === ver.id}
                          title="Restore and promote as current version"
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-brand-600/20 hover:bg-brand-600/30 text-brand-300 border border-brand-500/30 transition-colors"
                        >
                          {restoringId === ver.id ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                            <RotateCcw className="w-3.5 h-3.5" />
                          )}
                          Restore
                        </button>
                      )}

                      <button
                        onClick={() => handleDownload(ver)}
                        title="Download this version"
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition-colors"
                      >
                        <Download className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
