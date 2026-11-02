import api from './client';

export const auditApi = {
  getBackupHistory: async () => {
    return await api.get('/backup/history');
  },

  getRecoveryHistory: async () => {
    return await api.get('/recovery/history');
  },

  getDashboardStats: async () => {
    return await api.get('/dashboard/stats');
  },
};
