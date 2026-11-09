interface PaginationProps {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onSizeChange?: (size: number) => void;
}

const PAGE_SIZES = [10, 20, 50, 100];

/** Server-side pagination controls. Page state lives in the query string of the caller. */
export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onSizeChange,
}: PaginationProps) {
  const firstRow = totalElements === 0 ? 0 : page * size + 1;
  const lastRow = Math.min((page + 1) * size, totalElements);

  const buttonClass =
    'inline-flex items-center justify-center gap-1 px-2.5 py-1.5 rounded-lg text-label-lg font-semibold border transition-colors disabled:opacity-40 disabled:cursor-not-allowed';

  return (
    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
      <span className="text-body-sm text-on-surface-variant">
        {totalElements === 0 ? 'No records' : `Showing ${firstRow}–${lastRow} of ${totalElements}`}
      </span>

      <div className="flex items-center gap-2 flex-wrap">
        {onSizeChange && (
          <label className="flex items-center gap-1 text-body-sm text-on-surface-variant">
            <span className="text-label-sm uppercase tracking-wider">Rows</span>
            <select
              value={size}
              onChange={(event) => onSizeChange(Number(event.target.value))}
              className="px-2 py-1 bg-surface-container-low rounded-lg text-body-sm text-on-surface border border-outline-variant focus:outline-none"
            >
              {PAGE_SIZES.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        )}

        <button
          type="button"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
          className={`${buttonClass} border-outline-variant text-on-surface hover:bg-surface-container-high`}
        >
          <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
            chevron_left
          </span>
          Prev
        </button>

        <span className="text-label-sm text-on-surface-variant whitespace-nowrap">
          Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
        </span>

        <button
          type="button"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
          className={`${buttonClass} border-outline-variant text-on-surface hover:bg-surface-container-high`}
        >
          Next
          <span className="material-symbols-outlined" style={{ fontSize: '1rem' }}>
            chevron_right
          </span>
        </button>
      </div>
    </div>
  );
}
