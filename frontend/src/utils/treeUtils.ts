import { Person } from '../types/Person'; // Person tipini import etmeyi unutmayalım

// Dairesel yapıları güvenli bir şekilde JSON'a dönüştürmek için yardımcı fonksiyon
// Bu fonksiyon objenin sadece belirli alanlarını (id, firstName, lastName) döndürür
// Eğer tüm alanlar gerekiyorsa, döndürülen objeyi genişletmek gerekir.
export const removeCycles = (obj: any, cache = new Set()): Person | any => { // Dönüş tipini Person | any olarak güncelleyebiliriz
  if (obj === null || typeof obj !== 'object') return obj;

  // Dairesel referansı kontrol et
  // Basitleştirilmiş dönüş yerine sadece null döndürmek veya bir ID döndürmek daha güvenli olabilir.
  // Şimdilik orijinal mantığı koruyalım.
  if (cache.has(obj)) return { id: obj.id, firstName: obj.firstName, lastName: obj.lastName }; 
  cache.add(obj);

  if (Array.isArray(obj)) {
    // Haritalama sırasında cache'in kopyasını geçmek önemli
    return obj.map(item => removeCycles(item, new Set(cache))); 
  }

  const result: any = {};
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
        // Cache'in kopyasını geç
        result[key] = removeCycles(obj[key], new Set(cache));
    }
  }
  // Sonucu Person tipine cast etmeye çalışabiliriz, ancak bu her zaman doğru olmayabilir.
  return result as Person; 
}; 