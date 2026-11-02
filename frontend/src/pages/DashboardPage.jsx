import React, { useState, useEffect } from 'react';
import { Files, Database, ShieldCheck, HardDrive, Upload, RefreshCw } from 'lucide-react';
import { StatCard } from '../components/StatCard';
import { FileListTable } from '../components/FileListTable';
import { FileUploadModal } from '../components/FileUploadModal';
import { VersionHistoryModal } from '../components/VersionHistoryModal';
import { filesApi } from '../api/filesApi';
import { auditApi } from '../api/auditApi';
import { useAuth } from '../context/AuthContext';
import { formatBytes } from '../utils/formatters';

export const DashboardPage = ({ onToast }) => {
  const { user, refreshUser } = useAuth();
  const [files, setFiles] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [isUploadOpen, setIsUploadOpen] = useState(false);
  const [selectedFileForVersions, setSelectedFileForVersions] = useState(null);

  useEffect(() => {
    loadDashboardData();
  }, [search]);

  const loadDashboardData = async () => {
    setLoading(true);
    try {
      const [filesRes, statsRes] = await Promise.all([
        filesApi.getFiles(search),
        auditApi.getDashboardStats().catch(() => ({ data: null })),
      ]);
      setFiles(filesRes.data || []);
      if (statsRes.data) {
        setStats(statsRes.data);
      }
      refreshUser();
    } catch (err) {
      console.error('Failed to load dashboard data', err);
      onToast?.(err.message || 'Error syncing with cloud storage', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleUploadSuccess = (newFile) => {
    onToast?.(`File '${newFile.filename}' backed up to S3 successfully!`, 'success');
    loadDashboardData();
  };

  const handleRestoreSuccess = () => {
    loadDashboardData();
  };

  const totalRevisions = files.reduce((acc, f) => acc + (f.totalVersions || 1), 0);
  const corruptedFiles = files.filter((f) => f.status === 'CORRUPTED').length;

  return (
    <div className="space-y-6">
      {/* Welcome & quick action banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold tracking-tight text-white">Cloud Storage &amp; Backup Overview</h2>
          <p className="text-xs text-slate-400 mt-0.5">
            Real-time multi-version S3 inventory with automated SHA-256 cryptographic verification
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={loadDashboardData}
            className="flex items-center gap-2 px-3 py-2 text-xs font-medium rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Sync Cloud
          </button>

          <button
            onClick={() => setIsUploadOpen(true)}
            className="flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-xl bg-brand-600 hover:bg-brand-500 text-white shadow-lg shadow-brand-600/25 transition-all"
          >
            <Upload className="w-3.5 h-3.5" />
            New Backup
          </button>
        </div>
      </div>

      {/* 4 Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Active Files"
          value={files.length}
          subtitle="Unique file lineages"
          icon={Files}
          color="brand"
        />
        <StatCard
          title="Total Revisions"
          value={stats?.totalBackups || totalRevisions}
          subtitle="Immutable backup versions"
          icon={Database}
          color="indigo"
        />
        <StatCard
          title="Cloud Storage"
          value={formatBytes(user?.usedStorageBytes || stats?.totalStorageBytes || 0)}
          subtitle={`Limit: ${formatBytes(user?.storageQuotaBytes || 5368709120)}`}
          icon={HardDrive}
          color="emerald"
        />
        <StatCard
          title="Integrity Health"
          value={corruptedFiles === 0 ? '100% Intact' : `${corruptedFiles} Corrupted`}
          subtitle={corruptedFiles === 0 ? 'All hashes verified' : 'Action required'}
          icon={ShieldCheck}
          color={corruptedFiles === 0 ? 'emerald' : 'rose'}
        />
      </div>

      {/* Main File Management Table */}
      <FileListTable
        files={files}
        loading={loading}
        search={search}
        onSearchChange={setSearch}
        onOpenVersions={(file) => setSelectedFileForVersions(file)}
        onOpenUpload={() => setIsUploadOpen(true)}
        onRefresh={loadDashboardData}
        onToast={onToast}
      />

      {/* Modals */}
      <FileUploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onUploadSuccess={handleUploadSuccess}
      />

      <VersionHistoryModal
        file={selectedFileForVersions}
        isOpen={!!selectedFileForVersions}
        onClose={() => setSelectedFileForVersions(null)}
        onRestoreSuccess={handleRestoreSuccess}
        onToast={onToast}
      />
    </div>
  );
};
