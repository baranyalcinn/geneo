export interface FamilyMember {
  id: string;
  name: string;
  gender: 'male' | 'female';
  birthDate?: string;
  deathDate?: string;
  parentId?: string;
  spouse?: {
    id: string;
    name: string;
    gender: 'male' | 'female';
    birthDate?: string;
    deathDate?: string;
  };
  children?: FamilyMember[];
}

export interface Family {
  id: string;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
} 