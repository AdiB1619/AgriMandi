import { useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../../services/api';
import { useAuth } from '../auth/AuthContext';
import { useAsyncData } from '../../hooks/useAsyncData';
import { PageHeader } from '../../components/ui/PageHeader';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { EmptyState } from '../../components/ui/EmptyState';
import { DataTable } from '../../components/ui/DataTable';
import { LotStatusPill } from '../../components/ui/StatusPills';
import { formatDateTime, formatNumber } from '../../utils/format';

interface Lot {
  id: string;
  lotNumber: string;
  status: string;
  quantity: number | null;
  quantityUnit: string | null;
  packagingType: string | null;
  createdAt: string;
  commodity?: { id: string; name: string; defaultPriceUnit?: string } | null;
  farmer?: { id: string; name: string; village?: string | null } | null;
  assetType?: { id: string; name: string } | null;
}

interface FarmerOption {
  id: string;
  name: string;
  village?: string | null;
  status?: string;
}

interface CommodityOption {
  id: string;
  name: string;
  defaultPriceUnit?: string | null;
  activeStatus?: boolean;
}

interface AssetTypeOption {
  id: string;
  name: string;
  code?: string;
  returnable?: boolean;
}

const STATUS_OPTIONS = [
  'PENDING_WEIGHMENT',
  'READY_FOR_AUCTION',
  'AUCTION_IN_PROGRESS',
  'TIE_REQUIRES_AGENT_DECISION',
  'WINNER_PENDING_CONFIRMATION',
  'TRANSACTION_CREATED',
];

export default function LotsManagement() {
  const [searchParams, setSearchParams] = useSearchParams();

  const showCreateForm = searchParams.get('create') === '1';
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const lots = useAsyncData<Lot[]>(() => api.get<Lot[]>('/api/lots'), []);
  const [openLotId, setOpenLotId] = useState<string | null>(null);
  const lotDetail = useAsyncData<Lot | null>(
    () => (openLotId ? api.get<Lot>(`/api/lots/${openLotId}`) : Promise.resolve(null)),
    [openLotId],
  );
  const reference = useAsyncData(
    () =>
      Promise.all([
        api.get<FarmerOption[]>('/api/farmers'),
        api.get<CommodityOption[]>('/api/commodities'),
        api.get<AssetTypeOption[]>('/api/asset-types'),
      ]).then(([farmers, commodities, assetTypes]) => ({ farmers, commodities, assetTypes })),
    [],
  );

  const visibleLots = useMemo(() => {
    const all = lots.data ?? [];
    const term = search.trim().toLowerCase();
    return all.filter((lot) => {
      if (statusFilter && lot.status !== statusFilter) return false;
      if (!term) return true;
      return [lot.lotNumber, lot.farmer?.name, lot.commodity?.name]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(term));
    });
  }, [lots.data, search, statusFilter]);

  const closeForm = () => {
    searchParams.delete('create');
    setSearchParams(searchParams, { replace: true });
  };

  const openForm = () => setSearchParams({ create: '1' }, { replace: true });

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title="Lots Management"
        subtitle="Inward lots and their weighment, straight from the market register."
        icon="inventory_2"
        actions={
          showCreateForm ? (
            <button
              type="button"
              onClick={closeForm}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg border border-outline-variant text-label-lg font-semibold text-on-surface hover:bg-surface-container-high transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                close
              </span>
              Close form
            </button>
          ) : (
            <button
              type="button"
              onClick={openForm}
              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-primary text-on-primary text-label-lg font-semibold hover:bg-primary-container transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                add_circle
              </span>
              New lot
            </button>
          )
        }
      />

      {showCreateForm && (
        <CreateLotForm
          reference={reference}
          onCreated={(lotId, lotNumber) => {
            closeForm();
            setSearch(lotNumber);
            setSearchInput(lotNumber);
            lots.reload();
            // Open the lot we just made so the weighbridge step is the next thing the agent does.
            setOpenLotId(lotId);
          }}
          onCancel={closeForm}
        />
      )}

      <div className="bg-surface-container-lowest rounded-xl shadow-sm">
        <div className="px-3 py-2 border-b border-surface-container-high flex flex-wrap items-end gap-3">
          <label className="flex flex-col gap-1">
            <span className="text-label-sm uppercase tracking-wider text-on-surface-variant font-semibold">
              Search
            </span>
            <form
              className="flex items-center gap-1"
              onSubmit={(event) => {
                event.preventDefault();
                setSearch(searchInput);
              }}
            >
              <input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Lot number, farmer, commodity"
                className="px-2 py-1.5 bg-surface-container-low rounded-lg text-body-md text-on-surface border border-outline-variant focus:outline-none focus:border-secondary"
              />
              <button
                type="submit"
                className="shrink-0 text-label-sm font-semibold text-primary hover:underline"
              >
                Go
              </button>
            </form>
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-label-sm uppercase tracking-wider text-on-surface-variant font-semibold">
              Status
            </span>
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
              className="px-2 py-1.5 bg-surface-container-low rounded-lg text-body-md text-on-surface border border-outline-variant focus:outline-none focus:border-secondary"
            >
              <option value="">All statuses</option>
              {STATUS_OPTIONS.map((status) => (
                <option key={status} value={status}>
                  {status.replace(/_/g, ' ')}
                </option>
              ))}
            </select>
          </label>

          {(search || statusFilter) && (
            <button
              type="button"
              onClick={() => {
                setSearchInput('');
                setSearch('');
                setStatusFilter('');
              }}
              className="inline-flex items-center gap-1 px-3 py-1.5 text-label-lg font-semibold rounded-lg border border-outline-variant text-on-surface hover:bg-surface-container-high transition-colors"
            >
              <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                restart_alt
              </span>
              Reset
            </button>
          )}
        </div>

        {lots.isLoading ? (
          <div className="p-4">
            <LoadingState label="Loading lots" />
          </div>
        ) : lots.error ? (
          <div className="p-4">
            <ErrorState message={lots.error} onRetry={lots.reload} />
          </div>
        ) : visibleLots.length === 0 ? (
          <div className="p-4">
            <EmptyState
              title={(lots.data ?? []).length === 0 ? 'No lots recorded yet' : 'No lots match these filters'}
              description={
                (lots.data ?? []).length === 0
                  ? 'Record the first inward lot for this market to get started.'
                  : 'Adjust or reset the search and status filters.'
              }
              icon="inventory_2"
              action={
                (lots.data ?? []).length === 0 ? (
                  <button
                    type="button"
                    onClick={openForm}
                    className="mt-2 inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-primary text-on-primary text-label-lg font-semibold hover:bg-primary-container transition-colors"
                  >
                    <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
                      add_circle
                    </span>
                    New lot
                  </button>
                ) : undefined
              }
            />
          </div>
        ) : (
          <DataTable<Lot>
            rows={visibleLots}
            rowKey={(row) => row.id}
            caption="Lots recorded in this market"
            onRowClick={(row) => setOpenLotId(row.id)}
            columns={[
              {
                key: 'lot',
                header: 'Lot',
                render: (row) => <span className="font-semibold">{row.lotNumber}</span>,
              },
              { key: 'commodity', header: 'Commodity', render: (row) => row.commodity?.name ?? '—' },
              { key: 'farmer', header: 'Farmer', render: (row) => row.farmer?.name ?? '—' },
              {
                key: 'qty',
                header: 'Quantity',
                align: 'right',
                render: (row) => `${formatNumber(row.quantity)} ${row.quantityUnit ?? ''}`.trim(),
              },
              {
                key: 'packaging',
                header: 'Packaging',
                secondary: true,
                render: (row) => row.packagingType ?? '—',
              },
              { key: 'status', header: 'Status', render: (row) => <LotStatusPill status={row.status} /> },
              {
                key: 'created',
                header: 'Inwarded',
                align: 'right',
                secondary: true,
                render: (row) => formatDateTime(row.createdAt),
              },
            ]}
          />
        )}

        {!lots.isLoading && !lots.error && visibleLots.length > 0 && (
          <div className="px-3 py-2 border-t border-surface-container-high text-body-sm text-on-surface-variant">
            Showing {visibleLots.length} of {(lots.data ?? []).length} lot
            {(lots.data ?? []).length === 1 ? '' : 's'}
          </div>
        )}
      </div>

      {openLotId && (
        <LotDetail
          state={lotDetail}
          onClose={() => setOpenLotId(null)}
          onChanged={() => {
            lots.reload();
            lotDetail.reload();
          }}
        />
      )}
    </div>
  );
}

function LotDetail({
  state,
  onClose,
  onChanged,
}: {
  state: { data: Lot | null; isLoading: boolean; error: string | null; reload: () => void };
  onClose: () => void;
  onChanged: () => void;
}) {
  const lot = state.data;

  return (
    <div className="bg-surface-container-lowest rounded-xl shadow-sm">
      <div className="px-3 py-2 border-b border-surface-container-high flex items-center justify-between gap-2">
        <h2 className="text-headline-sm font-semibold text-on-surface">
          Lot detail {lot ? `• ${lot.lotNumber}` : ''}
        </h2>
        <button
          type="button"
          onClick={onClose}
          className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-label-lg font-semibold text-on-surface-variant hover:bg-surface-container-high transition-colors"
        >
          <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
            close
          </span>
          Close
        </button>
      </div>

      <div className="p-3">
        {state.isLoading ? (
          <LoadingState label="Loading lot" />
        ) : state.error ? (
          <ErrorState message={state.error} onRetry={state.reload} />
        ) : !lot ? (
          <EmptyState title="Lot not found" description="This lot may have been removed." icon="search_off" />
        ) : (
          <>
            <dl className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <Detail label="Lot number" value={lot.lotNumber} />
              <Detail label="Status" value={<LotStatusPill status={lot.status} />} />
              <Detail label="Commodity" value={lot.commodity?.name ?? '—'} />
              <Detail label="Farmer" value={lot.farmer?.name ?? '—'} />
              <Detail
                label="Quantity"
                value={`${formatNumber(lot.quantity)} ${lot.quantityUnit ?? ''}`.trim()}
              />
              <Detail label="Packaging" value={lot.packagingType ?? '—'} />
              <Detail label="Returnable asset" value={lot.assetType?.name ?? 'None'} />
              <Detail label="Inwarded at" value={formatDateTime(lot.createdAt)} />
              {lot.farmer?.village && <Detail label="Farmer village" value={lot.farmer.village} />}
            </dl>

            {lot.status === 'PENDING_WEIGHMENT' ? (
              <WeighmentForm
                defaultUnit={lot.quantityUnit ?? 'KG'}
                onSubmit={async (gross, tare, unit) => {
                  // The market server recomputes the net weight and moves the lot to
                  // READY_FOR_AUCTION; the UI only submits the weighbridge slip.
                  await api.post(`/api/lots/${lot.id}/weighment`, {
                    grossWeight: gross,
                    tareWeight: tare,
                    weightUnit: unit,
                  });
                }}
                onSaved={onChanged}
              />
            ) : (
              <p className="text-body-sm text-on-surface-variant mt-3">
                This lot is past weighment, so its quantity is fixed and it can proceed to auction.
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-0.5 min-w-0">
      <dt className="text-label-sm uppercase tracking-wider text-on-surface-variant">{label}</dt>
      <dd className="text-body-md text-on-surface font-semibold break-words">{value}</dd>
    </div>
  );
}

/**
 * Weighbridge slip for a lot that is still PENDING_WEIGHMENT. Net weight is computed by the
 * market server; this form only sends gross, tare and the unit.
 */
function WeighmentForm({
  defaultUnit,
  onSubmit,
  onSaved,
}: {
  defaultUnit: string;
  onSubmit: (gross: number, tare: number, unit: string) => Promise<void>;
  onSaved: () => void;
}) {
  const [gross, setGross] = useState('');
  const [tare, setTare] = useState('');
  const [unit, setUnit] = useState(defaultUnit);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const grossValue = Number(gross);
  const tareValue = Number(tare);
  const netValue = (Number.isFinite(grossValue) ? grossValue : 0) - (Number.isFinite(tareValue) ? tareValue : 0);

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (gross === '' || !Number.isFinite(grossValue) || grossValue <= 0) {
      setError('Gross weight must be a number greater than zero.');
      return;
    }
    if (tare === '' || !Number.isFinite(tareValue) || tareValue < 0) {
      setError('Tare weight must be zero or greater.');
      return;
    }
    if (tareValue >= grossValue) {
      setError('Tare weight must be less than gross weight.');
      return;
    }
    setIsSaving(true);
    try {
      await onSubmit(grossValue, tareValue, unit);
      setGross('');
      setTare('');
      onSaved();
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not save the weighment.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <form
      onSubmit={save}
      className="mt-4 p-3 bg-surface-container-low rounded-lg flex flex-col gap-3 border border-outline-variant"
    >
      <div>
        <h3 className="text-label-lg font-bold text-on-surface">Record weighment</h3>
        <p className="text-body-sm text-on-surface-variant">
          This lot cannot be auctioned until a weighbridge slip is recorded.
        </p>
      </div>

      {error && (
        <div className="p-2 bg-error-container text-on-error-container text-label-sm rounded flex items-start gap-2">
          <span className="material-symbols-outlined shrink-0" style={{ fontSize: '1.125rem' }}>
            error
          </span>
          <span className="break-words">{error}</span>
        </div>
      )}

      <div className="flex flex-col sm:flex-row gap-3 items-end">
        <div className="flex flex-col gap-1 flex-1 min-w-0">
          <label className="text-label-sm font-semibold text-on-surface" htmlFor="w-gross">
            Gross weight
          </label>
          <input
            id="w-gross"
            type="number"
            step="0.01"
            min="0"
            value={gross}
            onChange={(event) => setGross(event.target.value)}
            className="px-space-sm py-space-xs bg-surface-container-lowest rounded-lg focus:outline-none"
          />
        </div>
        <div className="flex flex-col gap-1 flex-1 min-w-0">
          <label className="text-label-sm font-semibold text-on-surface" htmlFor="w-tare">
            Tare weight
          </label>
          <input
            id="w-tare"
            type="number"
            step="0.01"
            min="0"
            value={tare}
            onChange={(event) => setTare(event.target.value)}
            className="px-space-sm py-space-xs bg-surface-container-lowest rounded-lg focus:outline-none"
          />
        </div>
        <div className="flex flex-col gap-1 w-28">
          <label className="text-label-sm font-semibold text-on-surface" htmlFor="w-unit">
            Unit
          </label>
          <input
            id="w-unit"
            value={unit}
            onChange={(event) => setUnit(event.target.value.toUpperCase())}
            className="px-space-sm py-space-xs bg-surface-container-lowest rounded-lg focus:outline-none"
          />
        </div>
        <button
          type="submit"
          disabled={isSaving}
          className="h-9 px-space-md py-space-xs bg-primary text-on-primary text-label-md font-semibold rounded-lg hover:bg-primary-container transition-colors disabled:opacity-50"
        >
          {isSaving ? 'Saving…' : 'Save weighment'}
        </button>
      </div>

      {netValue > 0 && (
        <p className="text-body-sm text-on-surface-variant">
          Net weight after tare: <strong className="text-on-surface">{formatNumber(netValue)} {unit}</strong>
        </p>
      )}
    </form>
  );
}

interface ReferenceData {
  farmers: FarmerOption[];
  commodities: CommodityOption[];
  assetTypes: AssetTypeOption[];
}

function CreateLotForm({
  reference,
  onCreated,
  onCancel,
}: {
  reference: { data: ReferenceData | null; isLoading: boolean; error: string | null; reload: () => void };
  onCreated: (lotId: string, lotNumber: string) => void;
  onCancel: () => void;
}) {
  const { user } = useAuth();

  const [farmerId, setFarmerId] = useState('');
  const [commodityId, setCommodityId] = useState('');
  const [assetTypeId, setAssetTypeId] = useState('');
  const [packagingType, setPackagingType] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  if (reference.isLoading) {
    return <LoadingState label="Loading farmers, commodities and asset types" />;
  }
  if (reference.error) {
    return <ErrorState message={reference.error} onRetry={reference.reload} />;
  }

  const { farmers, commodities, assetTypes } = reference.data ?? {
    farmers: [],
    commodities: [],
    assetTypes: [],
  };

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    if (!farmerId) errors.farmer = 'Select the farmer bringing this lot.';
    if (!commodityId) errors.commodity = 'Select a commodity.';
    if (!packagingType.trim()) errors.packaging = 'Packaging type is required by the market register.';
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    if (!validate() || !user) return;

    setIsSubmitting(true);
    try {
      // The register entry is created first. The market server sets the lot to
      // PENDING_WEIGHMENT; the weighbridge slip is recorded from the lot detail screen,
      // which is where the agent sees the lot they just created.
      const lot = await api.post<{ id: string; lotNumber: string }>('/api/lots', {
        marketId: user.marketId,
        farmerId,
        commodityId,
        packagingType: packagingType.trim(),
        assetTypeId: assetTypeId || null,
      });

      onCreated(lot.id, lot.lotNumber);
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : 'Could not create the lot.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const inputClass =
    'px-space-md py-space-sm bg-surface-container-low text-on-surface text-numeric-tabular-md rounded-lg focus:outline-none focus:ring-2 ring-primary';
  const labelClass = 'text-label-md text-on-surface font-semibold';

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-space-md bg-surface-container-lowest p-space-lg rounded-xl shadow-sm"
    >
      <div>
        <h2 className="text-headline-sm font-semibold text-on-surface">Inward lot</h2>
        <p className="text-body-sm text-on-surface-variant">
          Creates the lot register entry. The weighbridge slip is recorded from the lot itself, and
          the lot can only be auctioned once a net weight exists.
        </p>
      </div>

      {error && (
        <div className="p-space-sm bg-error-container text-on-error-container text-label-sm rounded flex items-start gap-2">
          <span className="material-symbols-outlined shrink-0" style={{ fontSize: '1.125rem' }}>
            error
          </span>
          <span className="break-words">{error}</span>
        </div>
      )}

      {farmers.length === 0 || commodities.length === 0 ? (
        <div className="p-space-sm bg-surface-container text-on-surface-variant text-label-sm rounded">
          {farmers.length === 0 && commodities.length === 0
            ? 'No farmers or commodities are registered for this market yet, so a lot cannot be created.'
            : farmers.length === 0
              ? 'No farmers are registered for this market yet.'
              : 'No commodities are registered yet.'}
        </div>
      ) : (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
            <div className="flex flex-col gap-space-xs">
              <label className={labelClass} htmlFor="lot-farmer">
                Farmer <span className="text-error">*</span>
              </label>
              <select
                id="lot-farmer"
                value={farmerId}
                onChange={(event) => setFarmerId(event.target.value)}
                className={inputClass}
              >
                <option value="">Select a farmer</option>
                {farmers.map((farmer) => (
                  <option key={farmer.id} value={farmer.id}>
                    {farmer.name}
                    {farmer.village ? ` • ${farmer.village}` : ''}
                  </option>
                ))}
              </select>
              {fieldErrors.farmer && (
                <span className="text-label-sm text-error">{fieldErrors.farmer}</span>
              )}
            </div>

            <div className="flex flex-col gap-space-xs">
              <label className={labelClass} htmlFor="lot-commodity">
                Commodity <span className="text-error">*</span>
              </label>
              <select
                id="lot-commodity"
                value={commodityId}
                onChange={(event) => setCommodityId(event.target.value)}
                className={inputClass}
              >
                <option value="">Select a commodity</option>
                {commodities.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name}
                  </option>
                ))}
              </select>
              {fieldErrors.commodity && (
                <span className="text-label-sm text-error">{fieldErrors.commodity}</span>
              )}
            </div>

            <div className="flex flex-col gap-space-xs">
              <label className={labelClass} htmlFor="lot-packaging">
                Packaging <span className="text-error">*</span>
              </label>
              <input
                id="lot-packaging"
                value={packagingType}
                onChange={(event) => setPackagingType(event.target.value)}
                placeholder="e.g. LOOSE, JUTE BAG, CRATE"
                className={inputClass}
              />
              {fieldErrors.packaging && (
                <span className="text-label-sm text-error">{fieldErrors.packaging}</span>
              )}
            </div>

            <div className="flex flex-col gap-space-xs">
              <label className={labelClass} htmlFor="lot-asset">
                Returnable asset type
              </label>
              <select
                id="lot-asset"
                value={assetTypeId}
                onChange={(event) => setAssetTypeId(event.target.value)}
                className={inputClass}
              >
                <option value="">No returnable asset</option>
                {assetTypes.map((asset) => (
                  <option key={asset.id} value={asset.id}>
                    {asset.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex justify-end gap-space-sm">
            <button
              type="button"
              onClick={onCancel}
              className="px-space-md py-space-sm rounded-lg border border-outline-variant text-label-lg font-semibold hover:bg-surface-container-high transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-space-lg py-space-sm bg-primary text-on-primary text-label-lg font-semibold rounded-lg hover:bg-primary-container hover:text-on-primary-container transition-colors disabled:opacity-50"
            >
              {isSubmitting ? 'Saving…' : 'Create lot'}
            </button>
          </div>
        </>
      )}
    </form>
  );
}
