import React, { useState } from 'react';
import { Search, Download, History, ShieldCheck, RotateCcw, File, Loader2, AlertCircle } from 'lucide-react';
import { filesApi } from '../api/filesApi';
import { IntegrityBadge } from './IntegrityBadge';
import { formatBytes, formatDate, truncateHash } from '../utils/formatters';

export const FileListTable = ({
  files,
  loading,
  search,
  onSearchChange,
  onOpenVersions,
  onOpenUpload,
  onRefresh,
  onToast,
}) => {
  const [verifyingId, setVerifyingId] = useState(null);

  const handleQuickVerify = async (file) => {
    setVerifyingId(file.id);
    try {
      const res = await filesApi.verifyFileIntegrity(file.id);
      const data = res.data;
      if (data?.verified) {
        onToast?.(`SHA-256 integrity verified for ${file.filename}!`, 'success');
      } else {
        onToast?.(`CORRUPTION DETECTED in ${file.filename}! Stored hash does not match cloud object.`, 'error');
      }
      onRefresh?.();
    } catch (err) {
      onToast?.(err.message || 'Integrity check failed', 'error');
    } finally {
      setVerifyingId(null);
    }
  };

  const handleDownload = (file) => {
    filesApi.downloadFile(file.id, null, file.filename);
  };

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/50 backdrop-blur-sm overflow-hidden">
      {/* Table controls */}
      <div className="p-4 sm:p-5 flex flex-col sm:flex-row items-center justify-between gap-4 border-b border-slate-800">
        <div className="relative w-full sm:w-80">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
          <input
            type="text"
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search backed up files..."
            className="w-full pl-9 pr-4 py-2 text-sm rounded-xl bg-slate-950/80 border border-slate-800 text-slate-200 placeholder-slate-500 focus:outline-none focus:border-brand-500"
          />
        </div>

        <div className="flex items-center gap-2.5 w-full sm:w-auto justify-end">
          <button
            onClick={onOpenUpload}
            className="flex items-center gap-2 px-4 py-2 text-sm font-semibold rounded-xl bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-600/20 transition-all"
          >
            <File className="w-4 h-4" />
            Upload File
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-950/60 text-xs uppercase tracking-wider text-slate-400 border-b border-slate-800">
            <tr>
              <th className="py-3.5 px-4 font-semibold">Filename</th>
              <th className="py-3.5 px-4 font-semibold">Version</th>
              <th className="py-3.5 px-4 font-semibold">Size</th>
              <th className="py-3.5 px-4 font-semibold">SHA-256 Checksum</th>
              <th className="py-3.5 px-4 font-semibold">Status</th>
              <th className="py-3.5 px-4 font-semibold">Last Modified</th>
              <th className="py-3.5 px-4 font-semibold text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 text-slate-300">
            {loading ? (
              <tr>
                <td colSpan="7" className="py-12 text-center text-slate-500">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <Loader2 className="w-6 h-6 animate-spin text-brand-500" />
                    <span>Loading files from cloud inventory...</span>
                  </div>
                </td>
              </tr>
            ) : files.length === 0 ? (
              <tr>
                <td colSpan="7" className="py-12 text-center text-slate-500">
                  <div className="flex flex-col items-center justify-center gap-2">
                    <File className="w-8 h-8 text-slate-600" />
                    <span>{search ? 'No files match your search query.' : 'No backed-up files yet. Upload your first file!'}</span>
                  </div>
                </td>
              </tr>
            ) : (
              files.map((file) => (
                <tr key={file.id} className="hover:bg-slate-800/40 transition-colors">
                  {/* Filename */}
                  <td className="py-3.5 px-4">
                    <div className="flex items-center gap-2.5">
                      <div className="p-2 rounded-lg bg-slate-800 text-slate-400 border border-slate-700/60">
                        <File className="w-4 h-4 text-brand-400" />
                      </div>
                      <div className="truncate max-w-xs font-medium text-white" title={file.filename}>
                        {file.filename}
                      </div>
                    </div>
                  </td>

                  {/* Version */}
                  <td className="py-3.5 px-4">
                    <button
                      onClick={() => onOpenVersions(file)}
                      className="inline-flex items-center gap-1 font-mono text-xs px-2 py-0.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-brand-300 border border-slate-700 transition-colors"
                      title="View all versions"
                    >
                      v{file.currentVersionNumber}
                      <span className="text-[10px] text-slate-500">({file.totalVersions} rev)</span>
                    </button>
                  </td>

                  {/* Size */}
                  <td className="py-3.5 px-4 font-mono text-xs text-slate-300">
                    {formatBytes(file.totalSizeBytes)}
                  </td>

                  {/* SHA-256 */}
                  <td className="py-3.5 px-4 font-mono text-xs text-indigo-300" title={file.checksumSha256}>
                    {truncateHash(file.checksumSha256, 7)}
                  </td>

                  {/* Status */}
                  <td className="py-3.5 px-4">
                    <IntegrityBadge status={file.status === 'CORRUPTED' ? 'CORRUPTED' : 'VERIFIED'} />
                  </td>

                  {/* Last Modified */}
                  <td className="py-3.5 px-4 text-xs text-slate-400">
                    {formatDate(file.updatedAt)}
                  </td>

                  {/* Actions */}
                  <td className="py-3.5 px-4 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => handleQuickVerify(file)}
                        disabled={verifyingId === file.id}
                        title="Verify SHA-256 Integrity against S3"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-400 hover:bg-slate-800 transition-colors"
                      >
                        {verifyingId === file.id ? (
                          <Loader2 className="w-4 h-4 animate-spin text-indigo-400" />
                        ) : (
                          <ShieldCheck className="w-4 h-4" />
                        )}
                      </button>

                      <button
                        onClick={() => onOpenVersions(file)}
                        title="Version History &amp; Recovery"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-brand-400 hover:bg-slate-800 transition-colors"
                      >
                        <History className="w-4 h-4" />
                      </button>

                      <button
                        onClick={() => handleDownload(file)}
                        title="Download Active Version"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-emerald-400 hover:bg-slate-800 transition-colors"
                      >
                        <Download className="w-4 h-4" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
