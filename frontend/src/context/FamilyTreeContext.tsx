import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react';
import { Person } from '../types/Person';
import { personService } from '../services/personService';
import { useLanguage } from './LanguageContext';
import { removeCycles } from '../utils/treeUtils';

// FamilyTreeContext için state ve fonksiyonların tipleri
interface FamilyTreeContextType {
  allPersons: Person[];
  selectedPerson: Person | null;
  treeData: Person | null;
  loading: boolean;
  error: string | null;
  fetchPersons: () => Promise<void>;
  selectPersonById: (id: number | null) => void;
  // Düzenleme Modalı için state ve fonksiyonlar
  isEditModalOpen: boolean;
  personToEdit: Person | null;
  openEditModal: (person: Person) => void;
  closeEditModal: () => void;
  // Detay Modalı için state ve fonksiyonlar
  isDetailModalOpen: boolean;
  personForDetails: Person | null;
  openDetailModal: (person: Person) => void;
  closeDetailModal: () => void;
}

// Context oluşturma
const FamilyTreeContext = createContext<FamilyTreeContextType | undefined>(undefined);

// FamilyTreeProvider bileşeni
interface FamilyTreeProviderProps {
  children: ReactNode;
}

export const FamilyTreeProvider: React.FC<FamilyTreeProviderProps> = ({ children }) => {
  const { t } = useLanguage(); // Dil context'inden t fonksiyonunu alıyoruz
  const [allPersons, setAllPersons] = useState<Person[]>([]);
  const [selectedPersonId, setSelectedPersonId] = useState<number | null>(null);
  const [selectedPerson, setSelectedPerson] = useState<Person | null>(null);
  const [treeData, setTreeData] = useState<Person | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Düzenleme Modalı state'leri
  const [isEditModalOpen, setIsEditModalOpen] = useState<boolean>(false);
  const [personToEdit, setPersonToEdit] = useState<Person | null>(null);

  // Detay Modalı state'leri
  const [isDetailModalOpen, setIsDetailModalOpen] = useState<boolean>(false);
  const [personForDetails, setPersonForDetails] = useState<Person | null>(null);

  const fetchPersons = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await personService.getAllPersons();
      // API yanıtı kontrolü düzeltildi - yanıt doğrudan dizi olabilir
      let persons: Person[] = [];
      
      // Doğrudan bir dizi gelirse
      if (Array.isArray(response)) {
        persons = response;
      }
      // PaginatedPersonsResponse şeklinde gelirse
      else if (response && response.content && Array.isArray(response.content)) {
        persons = response.content;
      }
      // Geçersiz yanıt
      else {
        console.error('Invalid API response format:', response);
        setError(t('errorInvalidApiResponse'));
        setAllPersons([]);
        setLoading(false);
        return;
      }
      
      setAllPersons(persons);
      
      if (persons.length > 0 && selectedPersonId === null) {
        // Eğer seçili kişi yoksa ve kişiler yüklendiyse, ilk kişiyi varsayılan olarak seç
        // Veya bu seçimi FamilyTreePage gibi bir üst bileşene bırakabiliriz.
        // Şimdilik otomatik seçim yapmayalım, sayfa karar versin.
      }
    } catch (err) {
      console.error('Error fetching persons:', err);
      setError(t('errorLoadingPersons'));
      setAllPersons([]); // Hata durumunda boş dizi olarak ayarla
    } finally {
      setLoading(false);
    }
  }, [t, selectedPersonId]); // selectedPersonId bağımlılığını ekledim, ilk yüklemede kullanılabilir.

  useEffect(() => {
    fetchPersons();
  }, [fetchPersons]);

  const processTreeData = useCallback((rootId: number | null, persons: Person[]): Person | null => {
    if (rootId === null || persons.length === 0) return null;

    const personMap = new Map<number, Person>();
    persons.forEach(p => personMap.set(p.id, { ...p, children: [], spouse: undefined })); // spouse'u başlangıçta undefined yap

    const rootPerson = personMap.get(rootId);
    if (!rootPerson) return null;

    personMap.forEach(person => {
      // Eş ilişkisini kur
      if (person.spouseId && personMap.has(person.spouseId)) {
        const spouse = personMap.get(person.spouseId);
        if (spouse) {
          person.spouse = spouse;
          // Karşılıklı eş ilişkisini de kurabiliriz, ancak bu removeCycles'da sorun yaratabilir.
          // Şimdilik tek yönlü bırakalım veya removeCycles'ı buna göre güncelleyelim.
          // spouse.spouse = person; // Dikkat: Dairesel referans
        }
      }

      // Çocuk ilişkilerini kur (hem anne hem baba üzerinden)
      persons.forEach(p => {
        if (p.fatherId === person.id || p.motherId === person.id) {
          const childFromMap = personMap.get(p.id);
          if (childFromMap) {
            person.children = person.children || [];
            if (!person.children.some(child => child.id === childFromMap.id)) {
              person.children.push(childFromMap);
            }
          }
        }
      });
    });
    
    return removeCycles(rootPerson);
  }, []);


  useEffect(() => {
    // null veya undefined kontrolü ekle
    if (selectedPersonId && Array.isArray(allPersons) && allPersons.length > 0) {
      const processedTree = processTreeData(selectedPersonId, allPersons);
      setTreeData(processedTree);
      setSelectedPerson(allPersons.find(p => p.id === selectedPersonId) || null);
    } else if (Array.isArray(allPersons) && allPersons.length > 0 && !selectedPersonId) {
      // Eğer seçili kişi ID'si yoksa ama kişiler varsa, ağacı null yap veya ilk kişiyi seç
      // Bu mantık FamilyTreePage'de yönetiliyordu, buraya taşıyabiliriz.
      // Örneğin, ilk kişiyi seç:
      // const firstPersonId = allPersons[0].id;
      // setSelectedPersonId(firstPersonId); 
      // const processedTree = processTreeData(firstPersonId, allPersons);
      // setTreeData(processedTree);
      // setSelectedPerson(allPersons[0]);
      // Şimdilik sadece treeData'yı null yapalım, seçimi sayfaya bırakalım.
      setTreeData(null);
      setSelectedPerson(null);
    } else {
      setTreeData(null);
      setSelectedPerson(null);
    }
  }, [selectedPersonId, allPersons, processTreeData]);
  
  const selectPersonById = (id: number | null) => {
    setSelectedPersonId(id);
  };

  // Düzenleme Modalı fonksiyonları
  const openEditModal = (person: Person) => {
    setPersonToEdit(person);
    setIsEditModalOpen(true);
  };

  const closeEditModal = () => {
    setIsEditModalOpen(false);
    setPersonToEdit(null);
  };

  // Detay Modalı fonksiyonları
  const openDetailModal = (person: Person) => {
    setPersonForDetails(person);
    setIsDetailModalOpen(true);
  };

  const closeDetailModal = () => {
    setIsDetailModalOpen(false);
    setPersonForDetails(null);
  };

  const contextValue: FamilyTreeContextType = {
    allPersons,
    selectedPerson,
    treeData,
    loading,
    error,
    fetchPersons,
    selectPersonById,
    // Modal state ve fonksiyonlarını ekle
    isEditModalOpen,
    personToEdit,
    openEditModal,
    closeEditModal,
    // Detay Modalı state ve fonksiyonlarını ekle
    isDetailModalOpen,
    personForDetails,
    openDetailModal,
    closeDetailModal,
  };

  return (
    <FamilyTreeContext.Provider value={contextValue}>
      {children}
    </FamilyTreeContext.Provider>
  );
};

// Custom hook for using FamilyTreeContext
export const useFamilyTree = (): FamilyTreeContextType => {
  const context = useContext(FamilyTreeContext);
  if (!context) {
    throw new Error('useFamilyTree must be used within a FamilyTreeProvider');
  }
  return context;
}; 