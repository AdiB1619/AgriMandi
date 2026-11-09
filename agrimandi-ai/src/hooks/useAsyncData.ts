import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '../services/api';

interface AsyncDataState<T> {
  data: T | null;
  isLoading: boolean;
  error: string | null;
  reload: () => void;
}

/**
 * Minimal request lifecycle helper for the reporting screens.
 *
 * Every effect-driven fetch goes through here so loading, error and empty states are
 * handled uniformly and stale responses cannot overwrite newer ones.
 */
export function useAsyncData<T>(
  fetcher: () => Promise<T>,
  deps: React.DependencyList,
): AsyncDataState<T> {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [nonce, setNonce] = useState(0);

  const reload = useCallback(() => setNonce((value) => value + 1), []);

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError(null);

    fetcher()
      .then((result) => {
        if (!active) return;
        setData(result);
        setIsLoading(false);
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setData(null);
        setError(cause instanceof ApiError ? cause.message : 'Unexpected error while contacting the server');
        setIsLoading(false);
      });

    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, nonce]);

  return { data, isLoading, error, reload };
}
