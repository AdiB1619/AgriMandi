import type { ReactNode } from 'react';

interface StatCardProps {
  title: string;
  icon: string;
  iconColor?: string;
  value: ReactNode;
  subValue?: ReactNode;
  progress?: number;
  progressColor?: string;
  footer?: ReactNode;
}

export function StatCard({
  title,
  icon,
  iconColor = 'text-[#246a4b]',
  value,
  subValue,
  progress,
  progressColor = 'bg-[#134e35]',
  footer,
}: StatCardProps) {
  return (
    <div className="bg-white p-[0.75rem] rounded-xl shadow-sm flex flex-col justify-between">
      <div className="flex items-start justify-between">
        <span className="text-label-sm uppercase tracking-wider text-[#404943]">{title}</span>
        <span className={`material-symbols-outlined ${iconColor}`} style={{ fontSize: '1.25rem' }}>
          {icon}
        </span>
      </div>
      <div className="my-[0.25rem]">
        <div className="flex items-baseline gap-[0.25rem]">{value}</div>
        {subValue && <div className="mt-[0.125rem]">{subValue}</div>}
        {progress !== undefined && (
          <div className="w-full bg-[#eff4ff] h-1.5 rounded-full overflow-hidden mt-[0.25rem]">
            <div
              className={`${progressColor} h-full rounded-full transition-all duration-500`}
              style={{ width: `${Math.min(progress, 100)}%` }}
            />
          </div>
        )}
      </div>
      {footer && (
        <div className="flex items-center justify-between text-body-sm text-[#404943] pt-[0.25rem] bg-[#eff4ff]/50 px-[0.25rem] rounded">
          {footer}
        </div>
      )}
    </div>
  );
}
