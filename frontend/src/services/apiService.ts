import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';

const API_URL = 'http://localhost:8080/api'; // TODO: Ortam değişkenlerinden al
const MAX_RETRIES = 3;
const RETRY_DELAY = 1000; // ms

// Axios temel yapılandırması
const apiClient = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 30000, // 30 saniye (api.ts'den alındı)
  withCredentials: true, // gameApi.ts dosyasındaki ayar dikkate alınarak eklendi
  // withCredentials: true, // Gerekirse eklenecek
});

// İstek interceptor'u (api.ts'den alındı)
apiClient.interceptors.request.use(
  (config) => {
    console.log(`API İsteği: ${config.method?.toUpperCase()} ${config.url}`, config.data || {});

    return config;
  },
  (error) => {
    console.error('API İstek hatası Interceptor:', error);
    return Promise.reject(error);
  }
);

// Yanıt interceptor'u (api.ts'den alındı ve geliştirildi)
apiClient.interceptors.response.use(
  (response) => {
    console.log(`API Yanıtı (${response.status}): ${response.config.method?.toUpperCase()} ${response.config.url}`, response.data);
    return response;
  },
  async (error: AxiosError) => {
    if (axios.isAxiosError(error)) {
      const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean };
      if (error.response) {
        console.error(`API Hata Yanıtı (${error.response.status}): ${originalRequest?.method?.toUpperCase()} ${originalRequest?.url}`, error.response.data);
    
      } else if (error.request) {
        console.error(`API İstek Hatası (Yanıt Yok): ${originalRequest?.method?.toUpperCase()} ${originalRequest?.url}`);
      } else {
        console.error(`API Genel Hata: ${error.message}`);
      }
    } else {
      console.error('API Beklenmeyen Hata:', error);
    }
    return Promise.reject(error);
  }
);

// Yeniden deneme mantığı (api.ts'den alındı)
const retryRequest = async <T>(requestFn: () => Promise<T>, retries = MAX_RETRIES, delay = RETRY_DELAY): Promise<T> => {
  try {
    return await requestFn();
  } catch (error) {
    if (retries <= 0 || (axios.isAxiosError(error) && error.response && error.response.status < 500 && error.response.status !== 429)) {
      // 5xx veya 429 (Too Many Requests) olmayan hatalarda yeniden deneme yapma
      throw error;
    }
    console.log(`İstek başarısız. ${retries} deneme kaldı. ${delay}ms sonra tekrar denenecek. URL: ${(error as AxiosError).config?.url}`);
    await new Promise(resolve => setTimeout(resolve, delay));
    return retryRequest(requestFn, retries - 1, delay * 1.5); // Üstel geri çekilme
  }
};

// İstek önbelleği için simple map
const cache = new Map<string, { data: any; timestamp: number }>();
const CACHE_DURATION = 5 * 60 * 1000; // 5 dakika

// İstek gruplaması için Map
const pendingRequests = new Map<string, Promise<any>>();

// İstek kütüphanesi
export const apiService = {
  // GET istekleri için metod
  async get<T>(endpoint: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
    let queryString = '';
    if (params) {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          searchParams.append(key, value as string);
        }
      });
      queryString = searchParams.toString();
    }
    
    const url = `${endpoint}${queryString ? `?${queryString}` : ''}`;
    console.log(`GET isteği hazırlanıyor: ${url}`);
    
    const cacheKey = `GET:${url}`;
    const cachedResponse = cache.get(cacheKey);
    
    if (cachedResponse && Date.now() - cachedResponse.timestamp < CACHE_DURATION) {
      console.log(`Cache hit for ${url}`);
      return Promise.resolve(cachedResponse.data as T);
    }
    
    if (pendingRequests.has(cacheKey)) {
      console.log(`Pending request reused for ${url}`);
      return pendingRequests.get(cacheKey)! as Promise<T>;
    }
    
    const requestPromise = retryRequest(() => 
      apiClient.get<T>(url, config).then((response: AxiosResponse<T>) => {
        cache.set(cacheKey, { data: response.data, timestamp: Date.now() });
        pendingRequests.delete(cacheKey);
        return response.data;
      })
    ).catch(error => {
        pendingRequests.delete(cacheKey);
        throw error;
    });
    
    pendingRequests.set(cacheKey, requestPromise);
    return requestPromise;
  },
  
  // POST istekleri için metod
  async post<T>(endpoint: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    // POST sonrası ilgili GET önbelleklerini temizle (basit yaklaşım)
    // Daha sofistike bir cache invalidation stratejisi düşünülebilir.
    const baseEndpoint = endpoint.split('/')[0];
    if (baseEndpoint) {
        Array.from(cache.keys())
          .filter(key => key.startsWith('GET:') && key.includes(baseEndpoint))
          .forEach(key => {
            console.log(`Cache invalidated for ${key} after POST to ${endpoint}`);
            cache.delete(key);
          });
    }
    
    return retryRequest(() => apiClient.post<T>(endpoint, data, config).then((response: AxiosResponse<T>) => response.data));
  },
  
  // PUT istekleri için metod
  async put<T>(endpoint: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const baseEndpoint = endpoint.split('/')[0];
     if (baseEndpoint) {
        Array.from(cache.keys())
          .filter(key => key.startsWith('GET:') && (key.includes(baseEndpoint) || key.includes(endpoint))) // Hem liste hem de detay cache'ini temizle
          .forEach(key => {
            console.log(`Cache invalidated for ${key} after PUT to ${endpoint}`);
            cache.delete(key);
          });
    }

    return retryRequest(() => apiClient.put<T>(endpoint, data, config).then((response: AxiosResponse<T>) => response.data));
  },
  
  // DELETE istekleri için metod
  async delete<T>(endpoint: string, config?: AxiosRequestConfig): Promise<T> {
    const baseEndpoint = endpoint.split('/')[0];
    if (baseEndpoint) {
        Array.from(cache.keys())
          .filter(key => key.startsWith('GET:') && (key.includes(baseEndpoint) || key.includes(endpoint))) // Hem liste hem de detay cache'ini temizle
          .forEach(key => {
            console.log(`Cache invalidated for ${key} after DELETE to ${endpoint}`);
            cache.delete(key);
          });
    }
    
    return retryRequest(() => apiClient.delete<T>(endpoint, config).then((response: AxiosResponse<T>) => response.data));
  },
  
  // Önbelleği temizleme metodu
  clearCache(pattern?: string): void {
    if (pattern) {
      Array.from(cache.keys())
        .filter(key => key.includes(pattern))
        .forEach(key => cache.delete(key));
    } else {
      cache.clear();
    }
    console.log(pattern ? `Cache cleared for pattern: ${pattern}` : 'All cache cleared');
  }
};