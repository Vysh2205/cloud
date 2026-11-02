import React, { useState, useRef } from 'react';
import { Upload, X, FileText, CheckCircle2, ShieldCheck, AlertCircle, Loader2 } from 'lucide-react';
import { filesApi } from '../api/filesApi';
import { calculateClientSha256 } from '../utils/hash';
import { formatBytes } from '../utils/formatters';

export const FileUploadModal = ({ isOpen, onClose, onUploadSuccess }) => {
  const [file, setFile] = useState(null);
  const [changeSummary, setChangeSummary] = useState('');
  const [clientChecksum, setClientChecksum] = useState('');
  const [calculatingHash, setCalculatingHash] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  if (!isOpen) return null;

  const handleFileChange = async (selectedFile) => {
    if (!selectedFile) return;
    setFile(selectedFile);
    setError(null);
    setCalculatingHash(true);
    try {
      const hash = await calculateClientSha256(selectedFile);
      setClientChecksum(hash);
    } catch (err) {
      console.error('Failed to compute client SHA-256', err);
    } finally {
      setCalculatingHash(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileChange(e.dataTransfer.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a file to backup.');
      return;
    }

    setUploading(true);
    setProgress(0);
    setError(null);

    const formData = new FormData();
    formData.append('file', file);
    if (changeSummary.trim()) {
      formData.append('changeSummary', changeSummary.trim());
    }

    try {
      const response = await filesApi.uploadFile(formData, (p) => setProgress(p));
      onUploadSuccess(response.data || response);
      handleClose();
    } catch (err) {
      setError(err.message || 'Failed to backup file to cloud storage.');
    } finally {
      setUploading(false);
    }
  };

  const handleClose = () => {
    setFile(null);
    setChangeSummary('');
    setClientChecksum('');
    setError(null);
    setProgress(0);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900 shadow-2xl p-6">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
              <Upload className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-white">Upload &amp; Backup File</h3>
              <p className="text-xs text-slate-400">Stores directly to AWS S3 with SHA-256 integrity snapshot</p>
            </div>
          </div>
          <button
            onClick={handleClose}
            disabled={uploading}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {error && (
          <div className="mt-4 flex items-center gap-2 p-3 rounded-xl bg-rose-950/50 border border-rose-500/40 text-xs text-rose-300">
            <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          {/* Dropzone */}
          <div
            onDragOver={(e) => e.preventDefault()}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-all ${
              file
                ? 'border-brand-500/50 bg-brand-500/5'
                : 'border-slate-700 hover:border-slate-600 bg-slate-950/40 hover:bg-slate-950/70'
            }`}
          >
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => handleFileChange(e.target.files?.[0])}
              className="hidden"
            />
            {file ? (
              <div className="flex flex-col items-center gap-2">
                <FileText className="w-10 h-10 text-brand-400" />
                <div className="text-sm font-semibold text-white truncate max-w-xs">{file.name}</div>
                <div className="text-xs text-slate-400">{formatBytes(file.size)}</div>
              </div>
            ) : (
              <div className="flex flex-col items-center gap-2">
                <Upload className="w-9 h-9 text-slate-500" />
                <div className="text-sm font-medium text-slate-300">
                  Drag and drop your file here, or <span className="text-brand-400 underline">browse</span>
                </div>
                <div className="text-xs text-slate-500">Supports all file types up to 500 MB</div>
              </div>
            )}
          </div>

          {/* SHA-256 client fingerprinting display */}
          {file && (
            <div className="rounded-xl border border-slate-800 bg-slate-950 p-3 space-y-1">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span className="flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
                  Client Pre-computed SHA-256:
                </span>
                {calculatingHash && (
                  <span className="flex items-center gap-1 text-[11px] text-amber-400">
                    <Loader2 className="w-3 h-3 animate-spin" /> Hashing...
                  </span>
                )}
              </div>
              <div className="text-[11px] font-mono text-indigo-300 break-all select-all">
                {calculatingHash ? 'Calculating cryptographic hash...' : clientChecksum || '—'}
              </div>
            </div>
          )}

          {/* Change notes */}
          <div>
            <label className="block text-xs font-medium text-slate-400 mb-1">
              Version Note / Change Summary (Optional)
            </label>
            <input
              type="text"
              value={changeSummary}
              onChange={(e) => setChangeSummary(e.target.value)}
              placeholder="e.g. Updated production database configuration"
              className="w-full px-3 py-2 text-sm rounded-xl bg-slate-950 border border-slate-800 text-slate-200 placeholder-slate-600 focus:outline-none focus:border-brand-500"
            />
          </div>

          {/* Upload progress */}
          {uploading && (
            <div className="space-y-1.5">
              <div className="flex justify-between text-xs text-slate-400">
                <span>Streaming to S3 &amp; hashing...</span>
                <span className="font-mono text-brand-400">{progress}%</span>
              </div>
              <div className="h-2 w-full rounded-full bg-slate-800 overflow-hidden">
                <div
                  className="h-full bg-brand-500 rounded-full transition-all duration-200"
                  style={{ width: `${progress}%` }}
                />
              </div>
            </div>
          )}

          {/* Action buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800">
            <button
              type="button"
              onClick={handleClose}
              disabled={uploading}
              className="px-4 py-2 text-sm font-medium rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!file || uploading || calculatingHash}
              className="flex items-center gap-2 px-5 py-2 text-sm font-semibold rounded-xl bg-brand-600 hover:bg-brand-500 text-white disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-brand-600/30 transition-all"
            >
              {uploading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Uploading...
                </>
              ) : (
                <>
                  <Upload className="w-4 h-4" />
                  Back Up to S3
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
