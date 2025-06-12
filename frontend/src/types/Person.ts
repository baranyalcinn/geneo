export enum Gender {
  MALE = 'ERKEK',
  FEMALE = 'KADIN',
}

// Ebeveyn ve eş gibi ilişkili kişiler için özet bilgi tipi
export interface PersonSummary {
  id: number;
  firstName: string;
  lastName: string;
}

export interface Person {
    id: number;
    firstName: string;
    lastName: string;
    gender: Gender;
    birthDate?: string | null;
    placeOfBirth?: string | null;
    deathDate?: string | null;
    placeOfDeath?: string | null;
    age?: number | null;
    mother?: PersonSummary | null; // motherId yerine PersonSummary kullandık
    father?: PersonSummary | null; // fatherId yerine PersonSummary kullandık
    spouse?: PersonSummary | null; // spouseId ve Person yerine PersonSummary kullandık
    children?: Person[] | null; // Çocuklar tam Person objesi olabilir veya PersonSummary
    avatarUrl?: string | null;
    photoUrl?: string | null;
    // FamilyTreeView bileşeni için gerekli ek alanlar
    relationships1Ids?: number[] | null;
    relationships2Ids?: number[] | null;
    notes?: string | null;
    // Tüm aileler görünümü için gerekli ek alanlar
    familyTreeId?: number | null;
    familyTreeName?: string | null;
}

// react-flow için özel düğüm verisi tipi
export interface PersonNodeData extends Record<string, unknown> {
  person: Person;
} 