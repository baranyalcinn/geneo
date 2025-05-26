export interface Relationship {
  id: number | string;
  person1Id: number | string; // İlk kişinin ID'si
  person2Id: number | string; // İkinci kişinin ID'si
  type: string; // İlişki türü, örn: 'SPOUSE', 'PARENT_OF', 'CHILD_OF', 'SIBLING_OF'
  startDate?: string; // İlişkinin başlangıç tarihi (opsiyonel)
  endDate?: string;   // İlişkinin bitiş tarihi (opsiyonel)
} 