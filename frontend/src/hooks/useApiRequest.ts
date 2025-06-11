import { useQuery, UseQueryOptions } from '@tanstack/react-query';

/**
 * API istekleri için TanStack Query (React Query) kullanan hook.
 * Önbellekleme, yeniden doğrulama ve durumların otomatik yönetilmesi gibi avantajlar sağlar.
 * 
 * @template TData Dönen veri tipi
 * @template TError Hata tipi
 * @param queryKey Sorgu anahtarı (önbellekleme için kullanılır)
 * @param queryFn API isteğini yapan async fonksiyon
 * @param options Ek sorgu seçenekleri
 */
export function useApiRequest<TData = unknown, TError extends Error = Error>(
  queryKey: string | string[],
  queryFn: () => Promise<TData>,
  options?: Omit<UseQueryOptions<TData, TError, TData>, 'queryKey' | 'queryFn'>
) {
  const normalizedQueryKey = Array.isArray(queryKey) ? queryKey : [queryKey];
  
  const query = useQuery<TData, TError>({
    queryKey: normalizedQueryKey,
    queryFn,
    ...options,
  });

  return {
    data: query.data,
    loading: query.isLoading,
    error: query.error?.message ?? null,
    refetch: query.refetch
  };
} 