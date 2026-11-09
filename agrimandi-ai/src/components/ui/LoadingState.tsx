export function LoadingState({ label = 'Loading data' }: { label?: string }) {
  return (
    <div className="bg-surface-container-lowest rounded-xl p-8 flex items-center justify-center gap-3">
      <span className="material-symbols-outlined animate-spin text-secondary" style={{ fontSize: '1.5rem' }}>
        sync
      </span>
      <span className="text-body-md text-on-surface-variant">{label}…</span>
    </div>
  );
}
