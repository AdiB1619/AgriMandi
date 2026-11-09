interface ErrorStateProps {
  message: string;
  onRetry?: () => void;
}

/** Surfaces backend failures verbatim instead of silently showing an empty table. */
export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="bg-error-container rounded-xl p-4 flex flex-col sm:flex-row sm:items-center gap-3">
      <span className="material-symbols-outlined text-on-error-container shrink-0" style={{ fontSize: '1.5rem' }}>
        error
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-label-md uppercase tracking-wider text-on-error-container font-semibold">
          Could not load data
        </p>
        <p className="text-body-sm text-on-error-container break-words">{message}</p>
      </div>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="shrink-0 inline-flex items-center gap-1 px-3 py-1.5 rounded-lg bg-on-error-container text-error-container text-label-lg font-semibold hover:opacity-90 transition-opacity"
        >
          <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
            refresh
          </span>
          Retry
        </button>
      )}
    </div>
  );
}
