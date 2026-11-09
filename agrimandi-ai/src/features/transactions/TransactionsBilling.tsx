import { useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { api } from '../../services/api';
import { reportsApi, PERIOD_OPTIONS, type PaymentLedgerRow, type ReportPeriod } from '../../services/reportsApi';
import { useAsyncData } from '../../hooks/useAsyncData';
import { PageHeader } from '../../components/ui/PageHeader';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { EmptyState } from '../../components/ui/EmptyState';
import { DataTable } from '../../components/ui/DataTable';
import { PaymentStatusPill, AssetStatusPill } from '../../components/ui/StatusPills';
import { FilterBar, FilterField, filterControlClass } from '../../components/ui/FilterBar';
import { Pagination } from '../../components/ui/Pagination';
import { formatCurrency, formatDateTime, formatNumber } from '../../utils/format';

interface TransactionDetail {
  id: string;
  transactionNumber: string;
  transactionDate: string;
  lot: { id: string; lotNumber: string; commodity: string; quantity: number | null; unit: string | null };
  farmer: { id: string; name: string };
  buyer: { id: string; name: string };
  winningBid: { id: string; rate: number | null; priceUnit: string | null };
  amount: { gross: number | null; net: number | null; paid: number | null; pending: number | null };
  paymentStatus: string;
  assets: { assetTypeId: string; assetTypeName: string; issued: number; returned: number; outstanding: number } | null;
}

export default function TransactionsBilling() {
  const { id } = useParams<{ id: string }>();
  return id ? <TransactionDetailView id={id} /> : <TransactionList />;
}

function TransactionList() {
  const navigate = useNavigate();
  const [period, setPeriod] = useState<ReportPeriod>('CURRENT_MONTH');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const range = { period, from: from || undefined, to: to || undefined };
  const ledger = useAsyncData(
    () => reportsApi.paymentLedger({ ...range, search, page, size }),
    [period, from, to, search, page, size],
  );

  const rows = ledger.data?.content ?? [];
  const customIncomplete = period === 'CUSTOM' && (!from || !to);

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title="Transactions &amp; Billing"
        subtitle="Every settled transaction in your market with its payment position."
        icon="receipt_long"
      />

      <FilterBar
        onReset={() => {
          setPeriod('CURRENT_MONTH');
          setFrom('');
          setTo('');
          setSearchInput('');
          setSearch('');
          setPage(0);
        }}
      >
        <FilterField label="Period" className="w-40">
          <select
            value={period}
            onChange={(event) => {
              setPeriod(event.target.value as ReportPeriod);
              setPage(0);
            }}
            className={filterControlClass}
          >
            {PERIOD_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </FilterField>

        {period === 'CUSTOM' && (
          <>
            <FilterField label="From" className="w-36">
              <input
                type="date"
                value={from}
                max={to || undefined}
                onChange={(event) => {
                  setFrom(event.target.value);
                  setPage(0);
                }}
                className={filterControlClass}
              />
            </FilterField>
            <FilterField label="To" className="w-36">
              <input
                type="date"
                value={to}
                min={from || undefined}
                onChange={(event) => {
                  setTo(event.target.value);
                  setPage(0);
                }}
                className={filterControlClass}
              />
            </FilterField>
          </>
        )}

        <FilterField label="Search" className="w-64">
          <form
            className="flex items-center gap-1"
            onSubmit={(event) => {
              event.preventDefault();
              setSearch(searchInput);
              setPage(0);
            }}
          >
            <input
              value={searchInput}
              onChange={(event) => setSearchInput(event.target.value)}
              placeholder="Transaction, lot, farmer, buyer"
              className={`${filterControlClass} w-full`}
            />
            <button type="submit" className="shrink-0 text-label-sm font-semibold text-primary hover:underline">
              Go
            </button>
          </form>
        </FilterField>
      </FilterBar>

      {customIncomplete && (
        <div className="p-3 bg-surface-container text-on-surface-variant text-label-sm rounded-lg flex items-center gap-2">
          <span className="material-symbols-outlined" style={{ fontSize: '1.125rem' }}>
            info
          </span>
          Choose both a start and an end date to run a custom range.
        </div>
      )}

      <div className="bg-surface-container-lowest rounded-xl shadow-sm">
        {ledger.isLoading ? (
          <div className="p-4">
            <LoadingState label="Loading transactions" />
          </div>
        ) : ledger.error ? (
          <div className="p-4">
            <ErrorState message={ledger.error} onRetry={ledger.reload} />
          </div>
        ) : rows.length === 0 ? (
          <div className="p-4">
            <EmptyState
              title="No transactions for this selection"
              description="Adjust the period or search, or settle an auction to create the first transaction."
              icon="receipt_long"
            />
          </div>
        ) : (
          <>
            <DataTable<PaymentLedgerRow>
              rows={rows}
              rowKey={(row) => row.transactionId}
              caption="Transactions and settlement position"
              onRowClick={(row) => navigate(`/transactions-billing/${row.transactionId}`)}
              columns={[
                {
                  key: 'txn',
                  header: 'Transaction',
                  render: (row) => <span className="font-semibold">{row.transactionNumber}</span>,
                },
                { key: 'lot', header: 'Lot', render: (row) => row.lotNumber, secondary: true },
                { key: 'commodity', header: 'Commodity', render: (row) => row.commodity, secondary: true },
                { key: 'farmer', header: 'Farmer', render: (row) => row.farmerName },
                { key: 'buyer', header: 'Buyer', render: (row) => row.buyerName },
                {
                  key: 'net',
                  header: 'Net Amount',
                  align: 'right',
                  render: (row) => formatCurrency(row.totalAmount),
                },
                {
                  key: 'paid',
                  header: 'Paid',
                  align: 'right',
                  render: (row) => formatCurrency(row.paidAmount),
                },
                {
                  key: 'outstanding',
                  header: 'Outstanding',
                  align: 'right',
                  render: (row) => formatCurrency(row.outstandingAmount),
                },
                {
                  key: 'status',
                  header: 'Status',
                  render: (row) => <PaymentStatusPill status={row.paymentStatus} />,
                },
                {
                  key: 'date',
                  header: 'Date',
                  align: 'right',
                  secondary: true,
                  render: (row) => formatDateTime(row.transactionDate),
                },
              ]}
            />
            <div className="px-3 py-2 border-t border-surface-container-high">
              <Pagination
                page={ledger.data?.page ?? page}
                size={ledger.data?.size ?? size}
                totalElements={ledger.data?.totalElements ?? 0}
                totalPages={ledger.data?.totalPages ?? 0}
                onPageChange={setPage}
                onSizeChange={(next) => {
                  setSize(next);
                  setPage(0);
                }}
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function TransactionDetailView({ id }: { id: string }) {
  const navigate = useNavigate();
  const [paymentAmount, setPaymentAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [assetQuantity, setAssetQuantity] = useState('');
  const [isPaying, setIsPaying] = useState(false);
  const [isAssetBusy, setIsAssetBusy] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const detail = useAsyncData<TransactionDetail>(
    () => api.get<TransactionDetail>(`/api/transactions/${id}`),
    [id],
  );
  const pending = useAsyncData(
    () => api.get<{ pendingAmount?: number | null }>(`/api/transactions/${id}/payments/pending-amount`),
    [id],
  );

  const transaction = detail.data;
  const outstanding = pending.data?.pendingAmount ?? transaction?.amount.pending ?? 0;

  const recordPayment = async (event: React.FormEvent) => {
    event.preventDefault();
    setActionError(null);
    const amount = Number(paymentAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
      setActionError('Enter a payment amount greater than zero.');
      return;
    }
    setIsPaying(true);
    try {
      await api.post(`/api/transactions/${id}/payments`, { amount, paymentMethod });
      // Only clear the field and report success once the server has accepted it.
      setPaymentAmount('');
      detail.reload();
      pending.reload();
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not record the payment.');
    } finally {
      setIsPaying(false);
    }
  };

  const moveAssets = async (direction: 'issue' | 'return') => {
    setActionError(null);
    const quantity = Number(assetQuantity);
    if (!transaction?.assets) return;
    if (!Number.isInteger(quantity) || quantity <= 0) {
      setActionError('Enter a whole number of items greater than zero.');
      return;
    }
    setIsAssetBusy(true);
    try {
      await api.post(`/api/transactions/${id}/assets/${direction}`, {
        assetTypeId: transaction.assets.assetTypeId,
        quantity,
      });
      setAssetQuantity('');
      detail.reload();
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : `Could not ${direction} assets.`);
    } finally {
      setIsAssetBusy(false);
    }
  };

  if (detail.isLoading) {
    return <LoadingState label="Loading transaction" />;
  }

  if (detail.error) {
    return (
      <div className="flex flex-col gap-4">
        <ErrorState message={detail.error} onRetry={detail.reload} />
        <Link
          to="/transactions-billing"
          className="inline-flex items-center gap-1 text-label-lg font-semibold text-primary hover:underline self-start"
        >
          <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
            arrow_back
          </span>
          Back to all transactions
        </Link>
      </div>
    );
  }

  if (!transaction) {
    return <EmptyState title="Transaction not found" icon="search_off" />;
  }

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title={`Transaction ${transaction.transactionNumber}`}
        subtitle={`Settled on ${formatDateTime(transaction.transactionDate)}`}
        icon="receipt_long"
        badge={<PaymentStatusPill status={transaction.paymentStatus} />}
        actions={
          <>
            <button
              type="button"
              onClick={() => navigate('/transactions-billing')}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant text-label-lg font-semibold text-on-surface hover:bg-surface-container-high transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                arrow_back
              </span>
              All transactions
            </button>
            <button
              type="button"
              onClick={() => {
                detail.reload();
                pending.reload();
              }}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant text-label-lg font-semibold text-on-surface hover:bg-surface-container-high transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                refresh
              </span>
              Refresh
            </button>
          </>
        }
      />

      {actionError && (
        <div className="p-3 bg-error-container text-on-error-container text-label-sm rounded-lg flex items-start gap-2">
          <span className="material-symbols-outlined shrink-0" style={{ fontSize: '1.125rem' }}>
            error
          </span>
          <span className="break-words">{actionError}</span>
        </div>
      )}

      <div className="bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
        <h2 className="text-headline-sm font-semibold mb-3">Settlement summary</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <Field label="Lot" value={transaction.lot.lotNumber} />
          <Field label="Farmer" value={transaction.farmer.name} />
          <Field label="Buyer" value={transaction.buyer.name} />
          <Field
            label="Commodity"
            value={`${transaction.lot.commodity} (${formatNumber(transaction.lot.quantity)} ${transaction.lot.unit ?? ''})`.trim()}
          />
          <Field
            label="Winning rate"
            value={`${formatCurrency(transaction.winningBid.rate)} / ${transaction.winningBid.priceUnit ?? ''}`.trim()}
          />
          <Field label="Gross amount" value={formatCurrency(transaction.amount.gross)} />
          <Field label="Net amount" value={formatCurrency(transaction.amount.net)} />
          <Field label="Paid to date" value={formatCurrency(transaction.amount.paid)} />
          <Field
            label="Outstanding"
            value={
              <span className={Number(outstanding) > 0 ? 'text-error' : 'text-secondary'}>
                {formatCurrency(outstanding)}
              </span>
            }
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-space-lg">
        <form
          onSubmit={recordPayment}
          className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col gap-space-md"
        >
          <h2 className="text-headline-sm font-semibold">Record payment</h2>
          <p className="text-body-sm text-on-surface-variant">
            Outstanding balance is {formatCurrency(outstanding)}.
          </p>
          <div className="flex flex-col sm:flex-row gap-space-sm items-end">
            <div className="flex flex-col gap-space-xs flex-1 min-w-0">
              <label className="text-label-sm font-semibold" htmlFor="pay-amount">
                Amount
              </label>
              <input
                id="pay-amount"
                type="number"
                step="0.01"
                min="0"
                value={paymentAmount}
                onChange={(event) => setPaymentAmount(event.target.value)}
                className="px-space-sm py-space-xs bg-surface-container-low rounded-lg focus:outline-none"
              />
            </div>
            <div className="flex flex-col gap-space-xs w-40">
              <label className="text-label-sm font-semibold" htmlFor="pay-method">
                Method
              </label>
              <select
                id="pay-method"
                value={paymentMethod}
                onChange={(event) => setPaymentMethod(event.target.value)}
                className="px-space-sm py-space-xs bg-surface-container-low rounded-lg focus:outline-none"
              >
                {['CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE'].map((method) => (
                  <option key={method} value={method}>
                    {method.replace('_', ' ')}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="submit"
              disabled={isPaying || Number(outstanding) <= 0}
              className="h-9 px-space-md py-space-xs bg-secondary text-on-secondary text-label-md font-semibold rounded-lg hover:bg-secondary-container transition-colors disabled:opacity-50"
            >
              {isPaying ? 'Recording…' : 'Record payment'}
            </button>
          </div>
          {Number(outstanding) <= 0 && (
            <p className="text-body-sm text-on-surface-variant">This transaction is fully settled.</p>
          )}
        </form>

        <div className="bg-surface-container-lowest p-space-md rounded-xl shadow-sm flex flex-col gap-space-md">
          <h2 className="text-headline-sm font-semibold">Returnable assets</h2>
          {!transaction.assets ? (
            <p className="text-body-sm text-on-surface-variant">
              No returnable asset type is linked to this lot.
            </p>
          ) : (
            <>
              <div className="flex flex-wrap justify-between items-center gap-2">
                <span className="text-body-md font-semibold">{transaction.assets.assetTypeName}</span>
                <AssetStatusPill
                  status={
                    transaction.assets.issued === 0
                      ? 'NO_ISSUE_RECORDED'
                      : transaction.assets.outstanding === 0
                        ? 'RETURNED'
                        : transaction.assets.returned > 0
                          ? 'PARTIALLY_RETURNED'
                          : 'OUTSTANDING'
                  }
                />
              </div>
              <div className="grid grid-cols-3 gap-3">
                <Field label="Issued" value={formatNumber(transaction.assets.issued)} />
                <Field label="Returned" value={formatNumber(transaction.assets.returned)} />
                <Field
                  label="Outstanding"
                  value={
                    <span className={transaction.assets.outstanding > 0 ? 'text-error' : 'text-secondary'}>
                      {formatNumber(transaction.assets.outstanding)}
                    </span>
                  }
                />
              </div>
              <div className="flex flex-col sm:flex-row gap-space-sm items-end">
                <div className="flex flex-col gap-space-xs flex-1 min-w-0">
                  <label className="text-label-sm font-semibold" htmlFor="asset-qty">
                    Quantity
                  </label>
                  <input
                    id="asset-qty"
                    type="number"
                    min="1"
                    step="1"
                    value={assetQuantity}
                    onChange={(event) => setAssetQuantity(event.target.value)}
                    className="px-space-sm py-space-xs bg-surface-container-low rounded-lg focus:outline-none"
                  />
                </div>
                <button
                  type="button"
                  onClick={() => moveAssets('issue')}
                  disabled={isAssetBusy}
                  className="h-9 px-space-md py-space-xs bg-primary text-on-primary text-label-md font-semibold rounded-lg hover:bg-primary-container transition-colors disabled:opacity-50"
                >
                  Issue
                </button>
                <button
                  type="button"
                  onClick={() => moveAssets('return')}
                  disabled={isAssetBusy}
                  className="h-9 px-space-md py-space-xs border border-outline-variant text-on-surface text-label-md font-semibold rounded-lg hover:bg-surface-container-high transition-colors disabled:opacity-50"
                >
                  Return
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5 min-w-0">
      <span className="text-label-sm uppercase tracking-wider text-on-surface-variant">{label}</span>
      <span className="text-body-md text-on-surface font-semibold break-words">{value}</span>
    </div>
  );
}
