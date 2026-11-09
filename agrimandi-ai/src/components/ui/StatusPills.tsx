import type { ReactNode } from 'react';
import { Badge } from './Badge';

type Variant = 'primary' | 'secondary' | 'tertiary' | 'error' | 'surface' | 'success' | 'warning';

const PAYMENT_STATUS_VARIANTS: Record<string, Variant> = {
  PAID: 'success',
  PARTIALLY_PAID: 'warning',
  CREATED: 'surface',
  CANCELLED: 'error',
};

const ASSET_STATUS_VARIANTS: Record<string, Variant> = {
  RETURNED: 'success',
  PARTIALLY_RETURNED: 'warning',
  OUTSTANDING: 'error',
  NO_ISSUE_RECORDED: 'surface',
};

const LOT_STATUS_VARIANTS: Record<string, Variant> = {
  PENDING_WEIGHMENT: 'surface',
  READY_FOR_AUCTION: 'tertiary',
  AUCTION_IN_PROGRESS: 'warning',
  TIE_REQUIRES_AGENT_DECISION: 'error',
  WINNER_PENDING_CONFIRMATION: 'warning',
  TRANSACTION_CREATED: 'success',
};

const EXCEPTION_VARIANTS: Record<string, Variant> = {
  TIE_REQUIRES_AGENT_DECISION: 'error',
  'WINNER_PENDING_CONFIRMATION': 'warning',
  'PARTIALLY_PAID': 'warning',
  CREATED: 'surface',
};

function label(value: string | null | undefined): string {
  if (!value) return 'Unknown';
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

export function PaymentStatusPill({ status }: { status: string | null | undefined }) {
  const key = status ?? '';
  return <Badge variant={PAYMENT_STATUS_VARIANTS[key] ?? 'surface'} dot>{label(status)}</Badge>;
}

export function AssetStatusPill({ status }: { status: string | null | undefined }) {
  const key = status ?? '';
  return <Badge variant={ASSET_STATUS_VARIANTS[key] ?? 'surface'} dot>{label(status)}</Badge>;
}

export function LotStatusPill({ status }: { status: string | null | undefined }) {
  const key = status ?? '';
  return <Badge variant={LOT_STATUS_VARIANTS[key] ?? 'surface'}>{label(status)}</Badge>;
}

export function ActionPill({ action }: { action: string | null | undefined }): ReactNode {
  return <Badge variant={EXCEPTION_VARIANTS[action ?? ''] ?? 'tertiary'}>{label(action)}</Badge>;
}
