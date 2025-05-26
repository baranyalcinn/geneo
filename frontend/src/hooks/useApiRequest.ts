import { useState, useCallback, useEffect } from 'react';

/**
 * API istekleri için loading, error ve veri yönetimini kolaylaştıran hook.
 * @template T Dönen veri tipi
 * @param requestFn API isteğini yapan async fonksiyon
 * @param immediate İlk render'da otomatik çalışsın mı?
 */
export function useApiRequest<T>(requestFn: () => Promise<T>, immediate = true) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const execute = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await requestFn();
      setData(result);
    } catch (err: any) {
      setError(err.message || 'Bilinmeyen hata');
    } finally {
      setLoading(false);
    }
  }, [requestFn]);

  useEffect(() => {
    if (immediate) execute();
  }, [execute, immediate]);

  return { data, loading, error, refetch: execute };
} 