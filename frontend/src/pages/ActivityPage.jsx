import React, { useState, useEffect } from 'react';
import { ActivityLogsTable } from '../components/ActivityLogsTable';
import { auditApi } from '../api/auditApi';

export const ActivityPage = ({ onToast }) => {
  const [backupLogs, setBackupLogs] = useState([]);
  const [recoveryLogs, setRecoveryLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadLogs();
  }, []);

  const loadLogs = async () => {
    setLoading(true);
    try {
      const [backupRes, recoveryRes] = await Promise.all([
        auditApi.getBackupHistory(),
        auditApi.getRecoveryHistory(),
      ]);
      setBackupLogs(backupRes.data || []);
      setRecoveryLogs(recoveryRes.data || []);
    } catch (err) {
      console.error('Failed to load audit history', err);
      onToast?.(err.message || 'Error fetching activity logs', 'error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold tracking-tight text-white">Cloud Audit &amp; Recovery Trail</h2>
        <p className="text-xs text-slate-400 mt-0.5">
          Tamper-evident audit trail of all distributed upload, retry, version promotion, and recovery events
        </p>
      </div>

      <ActivityLogsTable
        backupLogs={backupLogs}
        recoveryLogs={recoveryLogs}
        loading={loading}
        onRefresh={loadLogs}
      />
    </div>
  );
};
