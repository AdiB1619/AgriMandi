import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  icon?: string;
  badge?: ReactNode;
  actions?: ReactNode;
}

/**
 * Shared page title block. Keeps the institutional header treatment consistent
 * across the operational, reporting and oversight screens.
 */
export function PageHeader({ title, subtitle, icon, badge, actions }: PageHeaderProps) {
  return (
    <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-3 bg-surface-container-lowest p-4 rounded-xl shadow-sm">
      <div className="flex items-center gap-3 min-w-0">
        {icon && (
          <span className="material-symbols-outlined text-primary shrink-0" style={{ fontSize: '2rem' }}>
            {icon}
          </span>
        )}
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="text-headline-md font-bold text-on-surface truncate">{title}</h1>
            {badge}
          </div>
          {subtitle && <p className="text-body-sm text-on-surface-variant mt-0.5">{subtitle}</p>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2 shrink-0">{actions}</div>}
    </div>
  );
}
