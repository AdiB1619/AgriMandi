import type { ReactNode } from 'react';

type BadgeVariant =
  | 'primary'
  | 'secondary'
  | 'tertiary'
  | 'error'
  | 'surface'
  | 'success'
  | 'warning';

const variantClasses: Record<BadgeVariant, string> = {
  primary: 'bg-[#134e35] text-white',
  secondary: 'bg-[#a8efc7] text-[#296f4f]',
  tertiary: 'bg-[#dbe1ff] text-[#00174b]',
  error: 'bg-[#ffdad6] text-[#93000a]',
  surface: 'bg-[#dce9ff] text-[#0b1c30]',
  success: 'bg-[#a8efc7] text-[#246a4b]',
  warning: 'bg-[#ffdad6] text-[#93000a]',
};

interface BadgeProps {
  children: ReactNode;
  variant?: BadgeVariant;
  dot?: boolean;
  className?: string;
}

export function Badge({ children, variant = 'surface', dot, className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-[0.125rem] px-[0.25rem] py-[0.125rem] text-label-sm rounded font-semibold ${variantClasses[variant]} ${className}`}
    >
      {dot && <span className="w-1 h-1 rounded-full bg-current" />}
      {children}
    </span>
  );
}
