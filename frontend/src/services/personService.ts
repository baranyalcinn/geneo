import { apiService } from './apiService';
import { Person } from '../types/Person';

// Sayfalama ve filtreleme için tipler
interface PaginatedPersonsResponse {
  content: Person[];
  totalElements: number;
  // backend'den gelen diğer sayfalama bilgileri eklenebilir
}

export const personService = {
  getAllPersons: async (page = 0, size = 20, filters?: Record<string, any>): Promise<PaginatedPersonsResponse> => {
    return apiService.get<PaginatedPersonsResponse>('persons', {
      params: {
        page,
        size,
        ...filters
      }
    });
  },

  getPerson: async (id: string): Promise<Person> => {
    return apiService.get<Person>(`persons/${id}`);
  },

  createPerson: async (personData: Omit<Person, 'id'>): Promise<Person> => {
    return apiService.post<Person>('persons', personData);
  },

  updatePerson: async (id: string, personData: Partial<Person>): Promise<Person> => {
    return apiService.put<Person>(`persons/${id}`, personData);
  },

  deletePerson: async (id: string): Promise<void> => {
    return apiService.delete<void>(`persons/${id}`);
  }
  // findByLastName gibi özel endpoint'ler gerekirse eklenebilir
  // Örnek:
  // findByLastName: async (lastName: string): Promise<Person[]> => {
  //   return apiService.get<Person[]>(`persons/by-lastname/${lastName}`);
  // }
}; 