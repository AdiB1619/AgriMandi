import { api } from '../../services/api';
import { useAsyncData } from '../../hooks/useAsyncData';
import { PageHeader } from '../../components/ui/PageHeader';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { EmptyState } from '../../components/ui/EmptyState';
import { DataTable } from '../../components/ui/DataTable';
import { PaymentStatusPill, AssetStatusPill } from '../../components/ui/StatusPills';
import { formatCurrency, formatDateTime, formatNumber, titleCase } from '../../utils/format';

interface BuyerProfile {
  id: string;
  buyerCode: string | null;
  name: string;
  businessName: string | null;
  phone: string | null;
  address: string | null;
  licenseReference: string | null;
  status: string | null;
}

/** Same shape as TransactionResponseDTO, as returned for the signed-in buyer only. */
interface BuyerPurchase {
  id: string;
  transactionNumber: string;
  transactionDate: string;
  lot: { id: string; lotNumber: string; commodity: string; quantity: number | null; unit: string | null };
  farmer: { id: string; name: string };
  winningBid: { id: string; rate: number | null; priceUnit: string | null };
  amount: { gross: number | null; net: number | null; paid: number | null; pending: number | null };
  paymentStatus: string;
  assets: { assetTypeId: string; assetTypeName: string; issued: number; returned: number; outstanding: number } | null;
}

function sum(values: (number | null)[]): number {
  return values.reduce<number>((total, value) => total + (value ?? 0), 0);
}

/**
 * Read-only purchase statement for the signed-in buyer, backed by
 * `GET /api/buyer/me/purchases`. Outstanding dues come from the transactions the
 * backend returns; this screen does not derive a credit limit or a running balance
 * that the market server does not expose.
 */
export function BuyerPortal() {
  const profile = useAsyncData<BuyerProfile>(() => api.get<BuyerProfile>('/api/buyer/me'), []);
  const purchases = useAsyncData<BuyerPurchase[]>(
    () => api.get<BuyerPurchase[]>('/api/buyer/me/purchases'),
    [],
  );

  const rows = purchases.data ?? [];
  const totalOutstanding = sum(rows.map((row) => row.amount.pending));
  const withAssets = rows.filter((row) => row.assets);

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title={profile.data?.name ?? 'Purchase Statement'}
        subtitle={
          profile.data
            ? `${profile.data.buyerCode ?? 'No code'}${
                profile.data.businessName ? ` • ${profile.data.businessName}` : ''
              }`
            : 'Your official APMC purchase record'
        }
        icon="storefront"
        actions={
          <button
            type="button"
            onClick={() => {
              profile.reload();
              purchases.reload();
            }}
            className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant text-label-lg font-semibold text-on-surface hover:bg-surface-container-high transition-colors"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
              refresh
            </span>
            Refresh
          </button>
        }
      />

      <div className="p-3 bg-secondary-container text-on-secondary-container rounded-lg flex items-start gap-2">
        <span className="material-symbols-outlined shrink-0" style={{ fontSize: '1.25rem' }}>
          info
        </span>
        <p className="text-body-sm">
          Read-only statement. Payments are recorded by the market office, and the market does not
          publish a credit limit for buyers, so no limit is shown here.
        </p>
      </div>

      {profile.isLoading ? (
        <LoadingState label="Loading your profile" />
      ) : profile.error ? (
        <ErrorState message={profile.error} onRetry={profile.reload} />
      ) : null}

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-space-md">
        <Kpi
          label="Lots purchased"
          value={formatNumber(rows.length)}
          icon="shopping_cart"
          tone="text-primary"
        />
        <Kpi
          label="Total purchased"
          value={formatCurrency(sum(rows.map((row) => row.amount.net)))}
          icon="currency_rupee"
          tone="text-primary"
        />
        <Kpi
          label="Paid"
          value={formatCurrency(sum(rows.map((row) => row.amount.paid)))}
          icon="task_alt"
          tone="text-secondary"
        />
        <Kpi
          label="Outstanding"
          value={formatCurrency(totalOutstanding)}
          icon="pending"
          tone={totalOutstanding > 0 ? 'text-error' : 'text-secondary'}
        />
      </div>

      <div className="bg-surface-container-lowest rounded-xl shadow-sm">
        <div className="p-4 border-b border-surface-container-high">
          <h2 className="text-headline-sm font-bold">Purchase register</h2>
          <p className="text-body-sm text-on-surface-variant">
            {withAssets.length} purchase{withAssets.length === 1 ? '' : 's'} with returnable assets
          </p>
        </div>

        {purchases.isLoading ? (
          <div className="p-4">
            <LoadingState label="Loading your purchases" />
          </div>
        ) : purchases.error ? (
          <div className="p-4">
            <ErrorState message={purchases.error} onRetry={purchases.reload} />
          </div>
        ) : rows.length === 0 ? (
          <div className="p-4">
            <EmptyState
              title="No purchases yet"
              description="When you win an auction at this market, the resulting purchase will appear here."
              icon="shopping_cart"
            />
          </div>
        ) : (
          <DataTable<BuyerPurchase>
            rows={rows}
            rowKey={(row) => row.id}
            caption="Your settled purchases"
            columns={[
              {
                key: 'txn',
                header: 'Transaction',
                render: (row) => (
                  <div className="min-w-0">
                    <div className="font-semibold">{row.transactionNumber}</div>
                    <div className="text-body-sm text-on-surface-variant">
                      {formatDateTime(row.transactionDate)}
                    </div>
                  </div>
                ),
              },
              { key: 'lot', header: 'Lot', render: (row) => row.lot.lotNumber },
              {
                key: 'commodity',
                header: 'Commodity',
                secondary: true,
                render: (row) => (
                  <div className="min-w-0">
                    <div>{row.lot.commodity}</div>
                    <div className="text-body-sm text-on-surface-variant">
                      {formatNumber(row.lot.quantity)} {row.lot.unit ?? ''}
                    </div>
                  </div>
                ),
              },
              { key: 'farmer', header: 'Farmer', secondary: true, render: (row) => row.farmer.name },
              {
                key: 'rate',
                header: 'Rate',
                align: 'right',
                secondary: true,
                render: (row) => (
                  <span className="font-semibold text-primary">
                    {formatCurrency(row.winningBid.rate)}
                    {row.winningBid.priceUnit ? `/${row.winningBid.priceUnit}` : ''}
                  </span>
                ),
              },
              {
                key: 'net',
                header: 'Net total',
                align: 'right',
                render: (row) => formatCurrency(row.amount.net),
              },
              {
                key: 'pending',
                header: 'Outstanding',
                align: 'right',
                render: (row) => (
                  <span className={Number(row.amount.pending) > 0 ? 'text-error font-semibold' : 'text-secondary'}>
                    {formatCurrency(row.amount.pending)}
                  </span>
                ),
              },
              {
                key: 'assets',
                header: 'Assets',
                align: 'right',
                secondary: true,
                render: (row) =>
                  row.assets ? (
                    <span>
                      {formatNumber(row.assets.returned)}/{formatNumber(row.assets.issued)}{' '}
                      <span className="text-body-sm text-on-surface-variant">
                        ({row.assets.assetTypeName})
                      </span>
                    </span>
                  ) : (
                    <span className="text-on-surface-variant">None</span>
                  ),
              },
              {
                key: 'status',
                header: 'Status',
                render: (row) => (
                  <div className="flex flex-col gap-1 items-start">
                    <PaymentStatusPill status={row.paymentStatus} />
                    {row.assets && <AssetStatusPill status={assetStatus(row)} />}
                  </div>
                ),
              },
            ]}
          />
        )}
      </div>

      {profile.data && (
        <div className="bg-surface-container-lowest rounded-xl shadow-sm p-4">
          <h2 className="text-headline-sm font-bold mb-3">Registered details</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
            <Field label="Buyer code" value={profile.data.buyerCode} />
            <Field label="Business" value={profile.data.businessName} />
            <Field label="Phone" value={profile.data.phone} />
            <Field label="Status" value={profile.data.status ? titleCase(profile.data.status) : null} />
            <Field label="Address" value={profile.data.address} />
            <Field label="Licence reference" value={profile.data.licenseReference} />
          </div>
          <p className="text-body-sm text-on-surface-variant mt-3">
            To correct a registered detail, contact the market office.
          </p>
        </div>
      )}
    </div>
  );
}

function assetStatus(row: BuyerPurchase): string {
  if (!row.assets) return 'NO_ISSUE_RECORDED';
  if (row.assets.issued === 0) return 'NO_ISSUE_RECORDED';
  if (row.assets.outstanding === 0) return 'RETURNED';
  return row.assets.returned > 0 ? 'PARTIALLY_RETURNED' : 'OUTSTANDING';
}

function Kpi({
  label,
  value,
  icon,
  tone,
}: {
  label: string;
  value: string;
  icon: string;
  tone: string;
}) {
  return (
    <div className="bg-surface-container-lowest rounded-xl shadow-sm p-3 flex items-center gap-2">
      <span className={`material-symbols-outlined ${tone}`} style={{ fontSize: '1.75rem' }}>
        {icon}
      </span>
      <div className="min-w-0">
        <div className="text-headline-sm font-bold text-on-surface truncate">{value}</div>
        <div className="text-label-sm text-on-surface-variant">{label}</div>
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="flex flex-col gap-0.5 min-w-0">
      <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">{label}</span>
      <span className="text-body-md text-on-surface font-semibold break-words">
        {value ?? '—'}
      </span>
    </div>
  );
}
