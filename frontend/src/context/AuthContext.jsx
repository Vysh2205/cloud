import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const cached = localStorage.getItem('backup_platform_user');
    return cached ? JSON.parse(cached) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('backup_platform_token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      authApi.getProfile()
        .then((res) => {
          if (res.data) {
            setUser(res.data);
            localStorage.setItem('backup_platform_user', JSON.stringify(res.data));
          }
        })
        .catch(() => {
          logout();
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  const login = async (usernameOrEmail, password) => {
    const res = await authApi.login({ usernameOrEmail, password });
    if (res.data?.token) {
      const userData = res.data;
      setToken(userData.token);
      setUser(userData);
      localStorage.setItem('backup_platform_token', userData.token);
      localStorage.setItem('backup_platform_user', JSON.stringify(userData));
      return userData;
    }
  };

  const register = async (username, email, password) => {
    const res = await authApi.register({ username, email, password });
    if (res.data?.token) {
      const userData = res.data;
      setToken(userData.token);
      setUser(userData);
      localStorage.setItem('backup_platform_token', userData.token);
      localStorage.setItem('backup_platform_user', JSON.stringify(userData));
      return userData;
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('backup_platform_token');
    localStorage.removeItem('backup_platform_user');
  };

  const refreshUser = async () => {
    try {
      const res = await authApi.getProfile();
      if (res.data) {
        setUser(res.data);
        localStorage.setItem('backup_platform_user', JSON.stringify(res.data));
      }
    } catch (err) {
      console.error('Failed to refresh user', err);
    }
  };

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated: !!token, loading, login, register, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
