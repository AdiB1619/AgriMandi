import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { useAsyncData } from '../../hooks/useAsyncData';
import { PageHeader } from '../../components/ui/PageHeader';
import { LoadingState } from '../../components/ui/LoadingState';
import { ErrorState } from '../../components/ui/ErrorState';
import { EmptyState } from '../../components/ui/EmptyState';
import { LotStatusPill } from '../../components/ui/StatusPills';
import { formatCurrency, formatDateTime, formatNumber } from '../../utils/format';

interface Lot {
  id: string;
  lotNumber: string;
  status: string;
  quantity: number | null;
  quantityUnit: string | null;
  commodity?: { name?: string } | null;
  farmer?: { name?: string } | null;
}

interface Bid {
  id: string;
  buyerId: string;
  buyerName: string;
  bidAmount: number;
  priceUnit: string;
  bidTime: string;
  status: string;
}

/**
 * Lot statuses an agent still has to act on. A lot leaves this queue only once its auction has
 * been settled, so a closed auction or a tie never disappears before it can be resolved.
 */
const ACTIVE_LOT_STATUSES = [
  'READY_FOR_AUCTION',
  'AUCTION_IN_PROGRESS',
  'TIE_REQUIRES_AGENT_DECISION',
  'WINNER_PENDING_CONFIRMATION',
];

/** Bids may only be taken while the auction is open. */
const AUCTION_OPEN = 'AUCTION_IN_PROGRESS';

export default function AuctionDesk() {
  const navigate = useNavigate();

  const lots = useAsyncData<Lot[]>(() => api.get<Lot[]>('/api/lots'), []);
  const buyers = useAsyncData(() => api.get<{ id: string; name: string }[]>('/api/buyers'), []);

  const [selectedLotId, setSelectedLotId] = useState<string>('');
  const [buyerId, setBuyerId] = useState('');
  const [bidAmount, setBidAmount] = useState('');
  const [actionError, setActionError] = useState<string | null>(null);
  const [pendingAction, setPendingAction] = useState<string | null>(null);
  const [bidError, setBidError] = useState<string | null>(null);

  const bids = useAsyncData<Bid[]>(
    () => (selectedLotId ? api.get<Bid[]>(`/api/lots/${selectedLotId}/bids`) : Promise.resolve([])),
    [selectedLotId],
  );
  const highestBids = useAsyncData<Bid[]>(
    () =>
      selectedLotId
        ? api.get<Bid[]>(`/api/lots/${selectedLotId}/auction/highest-bids`)
        : Promise.resolve([]),
    [selectedLotId],
  );

  const queue = (lots.data ?? []).filter((lot) => ACTIVE_LOT_STATUSES.includes(lot.status));
  const selectedLot = queue.find((lot) => lot.id === selectedLotId) ?? null;

  const selectLot = (id: string) => {
    setSelectedLotId(id);
    setActionError(null);
    setBidError(null);
    setBidAmount('');
  };

  const runAction = async (label: string, action: () => Promise<unknown>) => {
    setActionError(null);
    setPendingAction(label);
    try {
      await action();
      lots.reload();
      bids.reload();
      highestBids.reload();
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : `${label} failed.`);
    } finally {
      setPendingAction(null);
    }
  };

  const startAuction = () =>
    runAction('Start auction', () => api.post(`/api/lots/${selectedLotId}/auction/start`));

  const closeAuction = () =>
    runAction('Close auction', () => api.post(`/api/lots/${selectedLotId}/auction/close`));

  const recordBid = async (event: React.FormEvent) => {
    event.preventDefault();
    setBidError(null);
    const amount = Number(bidAmount);
    if (!buyerId) {
      setBidError('Select the buyer placing this bid.');
      return;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      setBidError('Enter a bid rate greater than zero.');
      return;
    }
    if (!selectedLot) return;

    setPendingAction('Record bid');
    try {
      await api.post(`/api/lots/${selectedLotId}/bids`, {
        buyerId,
        bidAmount: amount,
        priceUnit: selectedLot.quantityUnit,
      });
      setBidAmount('');
      bids.reload();
      highestBids.reload();
    } catch (cause: unknown) {
      setBidError(cause instanceof Error ? cause.message : 'Could not record the bid.');
    } finally {
      setPendingAction(null);
    }
  };

  const confirmWinner = async (bidId: string) => {
    setActionError(null);
    setPendingAction(`confirm-${bidId}`);
    try {
      // The backend owns winner selection and transaction generation; the UI only sends the
      // agent's choice and follows whatever transaction comes back.
      const transaction = await api.post<{ id: string }>(
        `/api/lots/${selectedLotId}/auction/confirm-winner`,
        { winningBidId: bidId },
      );
      navigate(`/transactions-billing/${transaction.id}`);
    } catch (cause: unknown) {
      setActionError(cause instanceof Error ? cause.message : 'Could not confirm the winner.');
      setPendingAction(null);
      highestBids.reload();
      lots.reload();
    }
  };

  const isOpen = selectedLot?.status === AUCTION_OPEN;
  const canResolve = highestBids.data && highestBids.data.length > 0 && !isOpen;
  const isTie = (highestBids.data?.length ?? 0) > 1;

  return (
    <div className="flex flex-col w-full pb-space-2xl gap-space-lg">
      <PageHeader
        title="Auction Desk"
        subtitle="Record floor bids and settle lots. The market server decides the winning bid."
        icon="gavel"
        actions={
          <button
            type="button"
            onClick={() => {
              lots.reload();
              bids.reload();
              highestBids.reload();
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-space-lg">
        <div className="flex flex-col gap-space-sm bg-surface-container-lowest p-space-md rounded-xl shadow-sm">
          <h2 className="text-headline-sm text-on-surface font-semibold">Lots to settle</h2>

          {lots.isLoading ? (
            <LoadingState label="Loading lots" />
          ) : lots.error ? (
            <ErrorState message={lots.error} onRetry={lots.reload} />
          ) : queue.length === 0 ? (
            <EmptyState
              title="Nothing in the auction queue"
              description="Lots appear here once they are weighed, and disappear once a winner is confirmed."
              icon="gavel"
            />
          ) : (
            <div className="flex flex-col gap-space-xs max-h-[32rem] overflow-y-auto">
              {queue.map((lot) => (
                <button
                  key={lot.id}
                  type="button"
                  onClick={() => selectLot(lot.id)}
                  className={`flex flex-col p-space-sm rounded-lg border text-left transition-colors ${
                    selectedLotId === lot.id
                      ? 'bg-primary-container border-primary'
                      : 'bg-surface-container-low border-outline-variant hover:bg-surface-container-high'
                  }`}
                >
                  <div className="flex justify-between items-center gap-2 mb-space-xxs">
                    <span className="text-label-md font-bold">{lot.lotNumber}</span>
                    <LotStatusPill status={lot.status} />
                  </div>
                  <span className="text-body-sm text-on-surface-variant">
                    {lot.commodity?.name ?? '—'} • {formatNumber(lot.quantity)}{' '}
                    {lot.quantityUnit ?? ''}
                  </span>
                  <span className="text-body-sm text-on-surface-variant truncate">
                    Farmer: {lot.farmer?.name ?? '—'}
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="lg:col-span-2 flex flex-col gap-space-md bg-surface-container-lowest p-space-lg rounded-xl shadow-sm">
          {!selectedLot ? (
            <EmptyState
              title="Select a lot"
              description="Choose a lot from the queue to start, close or resolve its auction."
              icon="touch_app"
            />
          ) : (
            <>
              <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-3">
                <dl className="grid grid-cols-2 sm:grid-cols-4 gap-3 flex-1">
                  <Summary label="Lot" value={selectedLot.lotNumber} />
                  <Summary label="Farmer" value={selectedLot.farmer?.name ?? '—'} />
                  <Summary label="Commodity" value={selectedLot.commodity?.name ?? '—'} />
                  <Summary
                    label="Quantity"
                    value={`${formatNumber(selectedLot.quantity)} ${selectedLot.quantityUnit ?? ''}`.trim()}
                  />
                </dl>

                <div className="flex gap-space-sm shrink-0">
                  {selectedLot.status === 'READY_FOR_AUCTION' && (
                    <button
                      type="button"
                      onClick={startAuction}
                      disabled={pendingAction !== null}
                      className="px-space-md py-space-sm bg-primary text-on-primary text-label-md font-semibold rounded-lg hover:bg-primary-container transition-colors disabled:opacity-50"
                    >
                      {pendingAction === 'Start auction' ? 'Starting…' : 'Start auction'}
                    </button>
                  )}
                  {isOpen && (
                    <button
                      type="button"
                      onClick={closeAuction}
                      disabled={pendingAction !== null}
                      className="px-space-md py-space-sm bg-error text-on-error text-label-md font-semibold rounded-lg hover:bg-error-container transition-colors disabled:opacity-50"
                    >
                      {pendingAction === 'Close auction' ? 'Closing…' : 'Close auction'}
                    </button>
                  )}
                </div>
              </div>

              {actionError && (
                <div className="p-space-sm bg-error-container text-on-error-container text-label-sm rounded flex items-start gap-2">
                  <span className="material-symbols-outlined shrink-0" style={{ fontSize: '1.125rem' }}>
                    error
                  </span>
                  <span className="break-words">{actionError}</span>
                </div>
              )}

              {/* Bid entry, only while the auction is actually open. */}
              {isOpen && (
                <form onSubmit={recordBid} className="flex flex-col sm:flex-row gap-space-sm items-end">
                  <div className="flex flex-col gap-space-xs flex-1 min-w-0">
                    <label className="text-label-sm text-on-surface font-semibold" htmlFor="bid-buyer">
                      Buyer
                    </label>
                    <select
                      id="bid-buyer"
                      value={buyerId}
                      onChange={(event) => setBuyerId(event.target.value)}
                      className="px-space-sm py-space-xs bg-surface-container-low rounded-lg focus:outline-none"
                    >
                      <option value="">Select a buyer</option>
                      {(buyers.data ?? []).map((buyer) => (
                        <option key={buyer.id} value={buyer.id}>
                          {buyer.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="flex flex-col gap-space-xs flex-1 min-w-0">
                    <label className="text-label-sm text-on-surface font-semibold" htmlFor="bid-amount">
                      Bid rate ({selectedLot.quantityUnit} ₹)
                    </label>
                    <input
                      id="bid-amount"
                      type="number"
                      step="0.01"
                      min="0"
                      value={bidAmount}
                      onChange={(event) => setBidAmount(event.target.value)}
                      className="px-space-sm py-space-xs bg-surface-container-low rounded-lg focus:outline-none"
                    />
                  </div>
                  <button
                    type="submit"
                    disabled={pendingAction !== null}
                    className="h-9 px-space-md py-space-xs bg-secondary text-on-secondary text-label-md font-semibold rounded-lg hover:bg-secondary-container transition-colors disabled:opacity-50"
                  >
                    {pendingAction === 'Record bid' ? 'Recording…' : 'Record bid'}
                  </button>
                </form>
              )}

              {bidError && (
                <div className="p-space-sm bg-error-container text-on-error-container text-label-sm rounded">
                  {bidError}
                </div>
              )}

              {!isOpen && selectedLot.status === 'READY_FOR_AUCTION' && (
                <p className="text-body-sm text-on-surface-variant">
                  Start the auction to begin accepting bids.
                </p>
              )}

              {/* Recorded bids */}
              <div>
                <h3 className="text-label-lg text-on-surface font-semibold mb-space-xs">
                  Recorded bids
                </h3>
                {bids.isLoading ? (
                  <LoadingState label="Loading bids" />
                ) : bids.error ? (
                  <ErrorState message={bids.error} onRetry={bids.reload} />
                ) : (bids.data ?? []).length === 0 ? (
                  <p className="text-body-sm text-on-surface-variant">
                    No bids have been recorded for this lot.
                  </p>
                ) : (
                  <div className="flex flex-col gap-space-xs">
                    {(bids.data ?? []).map((bid) => {
                      const isCandidate =
                        (highestBids.data ?? []).some((candidate) => candidate.id === bid.id);
                      return (
                        <div
                          key={bid.id}
                          className={`flex flex-wrap justify-between items-center gap-2 p-space-xs rounded ${
                            isCandidate ? 'bg-secondary-container' : 'bg-surface-container-low'
                          }`}
                        >
                          <span className="text-body-sm font-semibold">{bid.buyerName}</span>
                          <span className="text-label-md font-bold text-primary">
                            {formatCurrency(bid.bidAmount)} /{bid.priceUnit}
                          </span>
                          <span className="text-body-sm text-on-surface-variant">
                            {formatDateTime(bid.bidTime)}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Resolution */}
              {highestBids.isLoading ? (
                <LoadingState label="Checking auction outcome" />
              ) : highestBids.error ? (
                <ErrorState message={highestBids.error} onRetry={highestBids.reload} />
              ) : canResolve ? (
                <div className="p-space-md bg-secondary-container text-on-secondary-container rounded-lg flex flex-col gap-space-sm border border-secondary">
                  <div className="flex justify-between items-center gap-2">
                    <h3 className="text-label-lg font-bold">Candidate winner(s)</h3>
                    {isTie && (
                      <span className="px-space-xs py-space-xxs bg-error text-on-error text-label-sm font-bold rounded">
                        TIE — AGENT DECISION REQUIRED
                      </span>
                    )}
                  </div>
                  <div className="flex flex-col gap-space-sm">
                    {(highestBids.data ?? []).map((candidate) => (
                      <div
                        key={candidate.id}
                        className="flex flex-wrap justify-between items-center gap-2 bg-surface-container-lowest p-space-xs rounded"
                      >
                        <span className="font-semibold text-on-surface">{candidate.buyerName}</span>
                        <span className="font-bold text-secondary">
                          {formatCurrency(candidate.bidAmount)} /{candidate.priceUnit}
                        </span>
                        <button
                          type="button"
                          onClick={() => confirmWinner(candidate.id)}
                          disabled={pendingAction !== null}
                          className="px-space-sm py-space-xxs bg-primary text-on-primary text-label-sm font-bold rounded hover:bg-primary-container transition-colors disabled:opacity-50"
                        >
                          {pendingAction === `confirm-${candidate.id}` ? 'Confirming…' : 'Confirm winner'}
                        </button>
                      </div>
                    ))}
                  </div>
                  <p className="text-body-sm text-on-secondary-container">
                    Confirming records the transaction and raises it on the payment and asset
                    ledgers. The resulting amount is calculated by the market server.
                  </p>
                </div>
              ) : !isOpen && selectedLot.status === 'TIE_REQUIRES_AGENT_DECISION' ? (
                <p className="text-body-sm text-on-surface-variant">
                  This lot needs a tie decision. Reload the outcome above if no candidate is shown.
                </p>
              ) : null}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function Summary({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-0.5 min-w-0">
      <dt className="text-label-sm uppercase tracking-wider text-on-surface-variant">{label}</dt>
      <dd className="text-body-md text-on-surface font-semibold truncate">{value}</dd>
    </div>
  );
}
