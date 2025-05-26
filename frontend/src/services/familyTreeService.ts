import { apiService } from './apiService';
import { Family, FamilyMember } from '../types/family'; // Varsayımsal tip adları, types/family.ts'de tanımlanmalı

// Aile ağacı API servisi

// Örnek Aile Ağacı Tipi (types/family.ts dosyasında olmalı)
// export interface FamilyTree {
//   id: number;
//   name: string;
//   members?: Person[]; // types/Person.ts'den Person tipi kullanılabilir
// }

// Örnek Aile Üyesi Ekleme/Çıkarma Tipi (types/family.ts dosyasında olmalı)
// export interface FamilyMemberRequestBody {
//   personId: number;
// }

export const familyTreeService = {
  /**
   * Yeni bir aile ağacı oluşturur.
   * @param name Aile ağacının adı.
   * @returns Oluşturulan aile ağacı nesnesi.
   */
  createFamilyTree: async (name: string): Promise<Family> => {
    // Backend'in { name: string } gibi bir body beklediğini varsayıyoruz.
    return apiService.post<Family>('family-trees', { name });
  },

  /**
   * Belirli bir ID'ye sahip aile ağacını getirir.
   * @param id Getirilecek aile ağacının ID'si. family.ts'de id: string, burada number. Backend ile tutarlı olmalı.
   * @returns Aile ağacı nesnesi.
   */
  getFamilyTree: async (id: string | number): Promise<Family> => { // id tipini string | number yaptım
    return apiService.get<Family>(`family-trees/${id}`);
  },

  /**
   * Bir aile ağacına üye ekler.
   * @param treeId Aile ağacının ID'si.
   * @param personId Eklenecek kişinin ID'si (FamilyMember tipinde id: string).
   * @returns İşlem başarılı olursa Promise<void> veya güncellenmiş ağaç.
   */
  addMember: async (treeId: string | number, personId: string): Promise<void> => {
    // Backend'in nasıl bir request beklediğine göre body değişebilir.
    // Örneğin: return apiService.post<void>(`family-trees/${treeId}/members`, { personId });
    return apiService.post<void>(`family-trees/${treeId}/members/${personId}`, {}); // Path param olarak gönderiliyorsa
  },

  /**
   * Bir aile ağacından üye çıkarır.
   * @param treeId Aile ağacının ID'si.
   * @param personId Çıkarılacak kişinin ID'si.
   * @returns İşlem başarılı olursa Promise<void>.
   */
  removeMember: async (treeId: string | number, personId: string): Promise<void> => {
    return apiService.delete<void>(`family-trees/${treeId}/members/${personId}`);
  },

  // İleride eklenebilecek diğer aile ağacı ile ilgili fonksiyonlar:
  // updateFamilyTreeName: async (treeId: number, newName: string): Promise<FamilyTree> => { ... },
  // getAllFamilyTrees: async (): Promise<FamilyTree[]> => { ... },
}; 