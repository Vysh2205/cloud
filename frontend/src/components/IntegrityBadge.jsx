import React from 'react';
import { ShieldCheck, ShieldAlert, Clock } from 'lucide-react';

export const IntegrityBadge = ({ status, size = 'sm' }) => {
  const isLarge = size === 'lg';

  if (status === 'VERIFIED') {
    return (
      <span className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 ${isLarge ? 'px-3 py-1 text-sm' : 'px-2.5 py-0.5 text-xs'}`}>
        <ShieldCheck className={isLarge ? "w-4 h-4" : "w-3.5 h-3.5"} />
        Verified
      </span>
    );
  }

  if (status === 'CORRUPTED') {
    return (
      <span className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/30 ${isLarge ? 'px-3 py-1 text-sm' : 'px-2.5 py-0.5 text-xs'}`}>
        <ShieldAlert className={isLarge ? "w-4 h-4" : "w-3.5 h-3.5"} />
        Corrupted
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center gap-1.5 font-medium rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 ${isLarge ? 'px-3 py-1 text-sm' : 'px-2.5 py-0.5 text-xs'}`}>
      <Clock className={isLarge ? "w-4 h-4" : "w-3.5 h-3.5"} />
      Unverified
    </span>
  );
};
