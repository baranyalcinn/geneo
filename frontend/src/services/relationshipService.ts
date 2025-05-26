import { apiService } from './apiService';
import { Relationship } from '../types/relationship';
// Varsayımsal Relationship tipi, types/relationship.ts veya types/common.ts gibi bir yerde tanımlanmalı
// import { Relationship } from '../types/relationship';

// Örnek Relationship Tipi (types/relationship.ts dosyasında olmalı)
// export interface Relationship {
//   id: number | string;
//   person1Id: number | string;
//   person2Id: number | string;
//   type: string; // Örneğin 'SPOUSE', 'PARENT_OF', 'CHILD_OF' vb.
//   startDate?: string;
//   endDate?: string;
// }

// Tip için geçici bir any kullanıyorum, uygun tip tanımlanmalı.
interface RelationshipRequestBody {
  person1Id: number | string;
  person2Id: number | string;
  type: string;
  startDate?: string; // Opsiyonel olarak request body'de de olabilir
  endDate?: string;   // Opsiyonel olarak request body'de de olabilir
}

export const relationshipService = {
  /**
   * Tüm ilişkileri getirir.
   * @returns İlişki nesnelerinin bir dizisi.
   */
  getAllRelationships: async (): Promise<Relationship[]> => {
    return apiService.get<Relationship[]>('relationships');
  },

  /**
   * Yeni bir ilişki oluşturur.
   * @param relationshipData Oluşturulacak ilişkinin verileri.
   * @returns Oluşturulan ilişki nesnesi.
   */
  createRelationship: async (relationshipData: RelationshipRequestBody): Promise<Relationship> => {
    return apiService.post<Relationship>('relationships', relationshipData);
  },

  // İleride eklenebilecek diğer ilişki ile ilgili fonksiyonlar:
  // getRelationshipById: async (id: number | string): Promise<Relationship> => { ... },
  // updateRelationship: async (id: number | string, data: Partial<Relationship>): Promise<Relationship> => { ... },
  // deleteRelationship: async (id: number | string): Promise<void> => { ... },
}; 