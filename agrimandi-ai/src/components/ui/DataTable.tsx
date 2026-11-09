import type { ReactNode } from 'react';

export interface DataTableColumn<T> {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
  align?: 'left' | 'right' | 'center';
  /** Hide on narrow viewports to keep the dense layout readable. */
  secondary?: boolean;
}

interface DataTableProps<T> {
  columns: DataTableColumn<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  onRowClick?: (row: T) => void;
  caption?: string;
}

/**
 * Dense, enterprise-oriented table. Rows are real records from the backend;
 * an empty `rows` array renders the caller's empty state instead of fake rows.
 */
export function DataTable<T>({ columns, rows, rowKey, onRowClick, caption }: DataTableProps<T>) {
  const alignClass = (align?: DataTableColumn<T>['align']) =>
    align === 'right' ? 'text-right' : align === 'center' ? 'text-center' : 'text-left';

  return (
    <div className="w-full overflow-x-auto">
      <table className="w-full border-collapse text-body-md">
        {caption && <caption className="sr-only">{caption}</caption>}
        <thead>
          <tr className="bg-surface-container-low">
            {columns.map((column) => (
              <th
                key={column.key}
                scope="col"
                className={`px-3 py-2 text-label-sm uppercase tracking-wider text-on-surface-variant font-semibold whitespace-nowrap ${alignClass(column.align)} ${column.secondary ? 'hidden lg:table-cell' : ''}`}
              >
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              className={`border-b border-surface-container-high last:border-0 ${
                onRowClick ? 'cursor-pointer hover:bg-surface-container-low' : ''
              }`}
            >
              {columns.map((column) => (
                <td
                  key={column.key}
                  className={`px-3 py-2 text-on-surface align-middle ${alignClass(column.align)} ${column.secondary ? 'hidden lg:table-cell' : ''}`}
                >
                  {column.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
