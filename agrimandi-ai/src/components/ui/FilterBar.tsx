import type { ReactNode } from 'react';

interface FilterBarProps {
  children: ReactNode;
  onReset?: () => void;
}

/** Horizontal filter container shared by the reporting and ledger screens. */
export function FilterBar({ children, onReset }: FilterBarProps) {
  return (
    <div className="bg-surface-container-lowest rounded-xl shadow-sm p-3">
      <div className="flex flex-wrap items-end gap-3">
        {children}
        {onReset && (
          <button
            type="button"
            onClick={onReset}
            className="inline-flex items-center gap-1 px-3 py-1.5 text-label-lg font-semibold rounded-lg border border-outline-variant text-on-surface hover:bg-surface-container-high transition-colors"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
              restart_alt
            </span>
            Reset
          </button>
        )}
      </div>
    </div>
  );
}

interface FilterFieldProps {
  label: string;
  children: ReactNode;
  className?: string;
}

export function FilterField({ label, children, className = '' }: FilterFieldProps) {
  return (
    <label className={`flex flex-col gap-1 min-w-0 ${className}`}>
      <span className="text-label-sm uppercase tracking-wider text-on-surface-variant font-semibold">{label}</span>
      {children}
    </label>
  );
}

export const filterControlClass =
  'px-2 py-1.5 bg-surface-container-low rounded-lg text-body-md text-on-surface border border-outline-variant focus:outline-none focus:border-secondary min-w-0';
