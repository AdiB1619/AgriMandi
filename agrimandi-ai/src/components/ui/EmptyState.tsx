import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description?: string;
  icon?: string;
  action?: ReactNode;
}

/** Shown whenever the backend returns no records. Never a placeholder row. */
export function EmptyState({ title, description, icon = 'inbox', action }: EmptyStateProps) {
  return (
    <div className="bg-surface-container-lowest rounded-xl p-8 flex flex-col items-center justify-center text-center gap-2">
      <span className="material-symbols-outlined text-on-surface-variant" style={{ fontSize: '2.5rem' }}>
        {icon}
      </span>
      <p className="text-headline-sm font-semibold text-on-surface">{title}</p>
      {description && <p className="text-body-sm text-on-surface-variant max-w-md">{description}</p>}
      {action}
    </div>
  );
}
