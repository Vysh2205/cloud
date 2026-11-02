import api from './client';
import axios from 'axios';

export const filesApi = {
  uploadFile: async (formData, onProgress) => {
    return await api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(percentCompleted);
        }
      },
    });
  },

  getFiles: async (search = '') => {
    const params = search ? { search } : {};
    return await api.get('/files', { params });
  },

  getFileById: async (fileId) => {
    return await api.get(`/files/${fileId}`);
  },

  getFileVersions: async (fileId) => {
    return await api.get(`/files/${fileId}/versions`);
  },

  verifyFileIntegrity: async (fileId, versionId = null) => {
    const params = versionId ? { versionId } : {};
    return await api.post(`/files/${fileId}/verify`, null, { params });
  },

  restoreVersion: async (fileId, versionId, data = { promoteToCurrentVersion: true, restoreReason: 'User restore request' }) => {
    return await api.post(`/files/${fileId}/restore/${versionId}`, data);
  },

  downloadFile: async (fileId, versionId = null, filename = 'download') => {
    const token = localStorage.getItem('backup_platform_token');
    const url = `/api/files/${fileId}/download${versionId ? `?versionId=${versionId}` : ''}`;

    const response = await axios.get(url, {
      responseType: 'blob',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });

    const blob = new Blob([response.data]);
    const link = document.createElement('a');
    link.href = window.URL.createObjectURL(blob);
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(link.href);
  },
};
