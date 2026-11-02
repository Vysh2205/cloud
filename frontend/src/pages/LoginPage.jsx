import React, { useState } from 'react';
import { Cloud, Lock, User, AlertCircle, Loader2, ArrowRight, KeyRound } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const LoginPage = ({ onSwitchToRegister }) => {
  const [identifier, setIdentifier] = useState('cloud_architect');
  const [password, setPassword] = useState('SecurePassword123!');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!identifier || !password) {
      setError('Please fill in all fields.');
      return;
    }

    setError('');
    setSubmitting(true);
    try {
      await login(identifier, password);
    } catch (err) {
      setError(err.message || 'Login failed. Please check your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  const fillDemoCredentials = () => {
    setIdentifier('cloud_architect');
    setPassword('SecurePassword123!');
    setError('');
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-6 bg-radial from-slate-900 to-slate-950">
      <div className="w-full max-w-md">
        {/* Card */}
        <div className="rounded-3xl border border-slate-800 bg-slate-900/70 p-8 shadow-2xl backdrop-blur-xl">
          {/* Logo */}
          <div className="flex flex-col items-center text-center mb-6">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-to-tr from-brand-600 to-indigo-500 shadow-xl shadow-brand-500/25 mb-4">
              <Cloud className="h-8 w-8 text-white" />
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-white">CloudBackup Platform</h1>
            <p className="text-xs text-slate-400 mt-1">Distributed S3 Versioning &amp; Disaster Recovery</p>
          </div>

          {/* Quick Demo Credentials Pill / Button */}
          <div className="mb-6 p-3.5 rounded-2xl bg-brand-500/10 border border-brand-500/20 text-xs">
            <div className="flex items-center justify-between font-semibold text-brand-300 mb-1">
              <span className="flex items-center gap-1.5">
                <KeyRound className="w-3.5 h-3.5 text-brand-400" />
                Default Demo Credentials:
              </span>
              <button
                type="button"
                onClick={fillDemoCredentials}
                className="text-[11px] underline text-brand-400 hover:text-brand-300 font-bold"
              >
                Auto-Fill
              </button>
            </div>
            <div className="text-slate-300 font-mono space-y-0.5 text-[11px]">
              <div>Username: <strong className="text-white">cloud_architect</strong></div>
              <div>Password: <strong className="text-white">SecurePassword123!</strong></div>
            </div>
          </div>

          {error && (
            <div className="mb-6 flex items-center gap-2.5 p-3 rounded-xl bg-rose-950/60 border border-rose-500/30 text-xs text-rose-300">
              <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">
                Username or Email
              </label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="text"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder="cloud_architect"
                  className="w-full pl-10 pr-4 py-2.5 text-sm rounded-xl bg-slate-950/80 border border-slate-800 text-slate-100 placeholder-slate-600 focus:outline-none focus:border-brand-500 transition-colors"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-300 mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="SecurePassword123!"
                  className="w-full pl-10 pr-4 py-2.5 text-sm rounded-xl bg-slate-950/80 border border-slate-800 text-slate-100 placeholder-slate-600 focus:outline-none focus:border-brand-500 transition-colors"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full mt-2 flex items-center justify-center gap-2 py-3 px-4 rounded-xl bg-brand-600 hover:bg-brand-500 text-white font-semibold text-sm shadow-lg shadow-brand-600/30 disabled:opacity-50 transition-all"
            >
              {submitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Authenticating...
                </>
              ) : (
                <>
                  Sign In
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Switch to Register */}
          <div className="mt-6 text-center text-xs text-slate-400">
            Don't have an account?{' '}
            <button
              onClick={onSwitchToRegister}
              className="text-brand-400 font-semibold hover:underline"
            >
              Register here
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
