import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import i18n from '../config/i18n';

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
    
    // İstek parametrelerine dil kodunu ekle
    if (!config.params) {
      config.params = {};
    }
    config.params.lang = i18n.language || 'tr'; // Varsayılan olarak Türkçe
    
    // Accept-Language header'ını ekle
    // Axios v1.x için headers objesini güvenli bir şekilde kullan
    if (config.headers) {
      config.headers['Accept-Language'] = i18n.language || 'tr';
    }

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
  async get<T>(endpoint: string, options?: { params?: any; config?: AxiosRequestConfig }): Promise<T> {
    const { params, config } = options || {};
    
    // Axios'un kendi parametre işleme mantığını kullan
    const requestConfig = {
      ...config,
      params
    };
    
    // Cache için URL ve parametreleri birleştir
    const cacheKey = `GET:${endpoint}:${JSON.stringify(params || {})}:${i18n.language}`;
    const cachedResponse = cache.get(cacheKey);
    
    if (cachedResponse && Date.now() - cachedResponse.timestamp < CACHE_DURATION) {
      console.log(`Cache hit for ${endpoint}`);
      return Promise.resolve(cachedResponse.data as T);
    }
    
    if (pendingRequests.has(cacheKey)) {
      console.log(`Pending request reused for ${endpoint}`);
      return pendingRequests.get(cacheKey)! as Promise<T>;
    }
    
    const requestPromise = retryRequest(() => 
      apiClient.get<T>(endpoint, requestConfig).then((response: AxiosResponse<T>) => {
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
  async post<T>(endpoint: string, data?: any, options?: { params?: any; config?: AxiosRequestConfig }): Promise<T> {
    const { params, config } = options || {};
    
    // Axios'un kendi parametre işleme mantığını kullan
    const requestConfig = {
      ...config,
      params
    };
    
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
    
    return retryRequest(() => apiClient.post<T>(endpoint, data, requestConfig).then((response: AxiosResponse<T>) => response.data));
  },
  
  // PUT istekleri için metod
  async put<T>(endpoint: string, data?: any, options?: { params?: any; config?: AxiosRequestConfig }): Promise<T> {
    const { params, config } = options || {};
    
    // Axios'un kendi parametre işleme mantığını kullan
    const requestConfig = {
      ...config,
      params
    };
    
    const baseEndpoint = endpoint.split('/')[0];
     if (baseEndpoint) {
        Array.from(cache.keys())
          .filter(key => key.startsWith('GET:') && (key.includes(baseEndpoint) || key.includes(endpoint))) // Hem liste hem de detay cache'ini temizle
          .forEach(key => {
            console.log(`Cache invalidated for ${key} after PUT to ${endpoint}`);
            cache.delete(key);
          });
    }

    return retryRequest(() => apiClient.put<T>(endpoint, data, requestConfig).then((response: AxiosResponse<T>) => response.data));
  },
  
  // DELETE istekleri için metod
  async delete<T>(endpoint: string, options?: { params?: any; config?: AxiosRequestConfig }): Promise<T> {
    const { params, config } = options || {};
    
    // Axios'un kendi parametre işleme mantığını kullan
    const requestConfig = {
      ...config,
      params
    };
    
    const baseEndpoint = endpoint.split('/')[0];
    if (baseEndpoint) {
        Array.from(cache.keys())
          .filter(key => key.startsWith('GET:') && (key.includes(baseEndpoint) || key.includes(endpoint))) // Hem liste hem de detay cache'ini temizle
          .forEach(key => {
            console.log(`Cache invalidated for ${key} after DELETE to ${endpoint}`);
            cache.delete(key);
          });
    }
    
    return retryRequest(() => apiClient.delete<T>(endpoint, requestConfig).then((response: AxiosResponse<T>) => response.data));
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