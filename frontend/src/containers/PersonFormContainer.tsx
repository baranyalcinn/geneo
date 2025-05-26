import React, { useEffect, useCallback } from 'react';
import { usePersonStore } from '../store/usePersonStore';
import PersonForm from '../components/PersonForm/PersonForm';
import { Person } from '../types/Person';
import { useApiRequest } from '../hooks/useApiRequest';
import { personService } from '../services/personService';
import EmptyState from '../components/ui/EmptyState';
import LoadingIndicator from '../components/ui/LoadingIndicator';
import ErrorMessage from '../components/ui/ErrorMessage';
import { useLanguage } from '../context/LanguageContext';

interface Props {
  personId?: number;
  onSave: () => void;
  onCancel: () => void;
}

export const PersonFormContainer: React.FC<Props> = ({ personId, onSave, onCancel }) => {
  const {
    person, setPerson,
    availableFathers, setAvailableFathers,
    availableMothers, setAvailableMothers,
    availableSpouses, setAvailableSpouses,
  } = usePersonStore();

  // useCallback ile referans sabitleniyor
  const getPersonRequest = useCallback(
    () => personId ? personService.getPerson(personId.toString()) : Promise.resolve(null),
    [personId]
  );
  const getAllPersonsRequest = useCallback(
    () => personService.getAllPersons().then(response => {
      // API yanıtı doğrudan dizi veya sayfalanmış içerik olabilir
      if (Array.isArray(response)) {
        return response;
      } else if (response && response.content) {
        return response.content;
      } else {
        console.warn('Beklenmeyen API yanıt formatı:', response);
        return [];
      }
    }),
    []
  );

  // Kişi detayını ve tüm kişileri custom hook ile çek
  const { data: personData, loading: loadingPerson, error: errorPerson, refetch: refetchPerson } = useApiRequest(
    getPersonRequest,
    !!personId
  );
  const { data: allPersonsRaw, loading: loadingAll, error: errorAll, refetch: refetchAll } = useApiRequest(
    getAllPersonsRequest
  );
  const allPersons = allPersonsRaw || [];

  // Yeni kişi eklerken person state'ini sıfırla
  useEffect(() => {
    if (!personId) setPerson({});
  }, [personId, setPerson]);

  // Kişi ve ilişkili kişileri yükle (gerekirse Zustand ile senkronize et)
  useEffect(() => {
    if (personData) setPerson(personData);
  }, [personData, setPerson]);

  useEffect(() => {
    if (allPersons.length > 0) {
      setAvailableFathers(allPersons.filter((p: Person) => p.gender === 'ERKEK' && p.id !== personId));
      setAvailableMothers(allPersons.filter((p: Person) => p.gender === 'KADIN' && p.id !== personId));
      setAvailableSpouses(allPersons.filter((p: Person) => p.id !== personId));
    }
  }, [allPersons, personId, setAvailableFathers, setAvailableMothers, setAvailableSpouses]);

  // Kaydetme işlemi
  const handleSave = async () => {
    try {
      // Veri validasyonu
      if (!person.firstName || !person.lastName || !person.gender) {
        console.error('Zorunlu alanlar eksik', { person });
        return;
      }

      console.log('Kaydetme işlemi başlıyor', { person });

      if (personId) {
        // Güncelleme işlemi
        const updateData: Partial<Person> = {
          id: personId,
          firstName: person.firstName,
          lastName: person.lastName,
          gender: person.gender,
          birthDate: person.birthDate || null,
          deathDate: person.deathDate || null,
          father: person.father || null,
          mother: person.mother || null,
          spouse: person.spouse || null,
          photoUrl: person.photoUrl || null,
          placeOfBirth: person.placeOfBirth || null,
          placeOfDeath: person.placeOfDeath || null,
          notes: person.notes || null,
          relationships1Ids: person.relationships1Ids || [],
          relationships2Ids: person.relationships2Ids || [],
        };
        
        console.log('Güncellenecek veri:', updateData);
        await personService.updatePerson(personId.toString(), updateData);
      } else {
        // Yeni kişi ekleme
        const newPersonData: Omit<Person, 'id'> = {
          firstName: person.firstName!,
          lastName: person.lastName!,
          gender: person.gender!,
          birthDate: person.birthDate || null,
          deathDate: person.deathDate || null,
          father: person.father || null,
          mother: person.mother || null,
          spouse: person.spouse || null,
          photoUrl: person.photoUrl || null,
          placeOfBirth: person.placeOfBirth || null,
          placeOfDeath: person.placeOfDeath || null,
          notes: person.notes || null,
          relationships1Ids: [],
          relationships2Ids: [],
          children: [],
        };
        
        console.log('Eklenecek veri:', newPersonData);
        await personService.createPerson(newPersonData);
      }
      
      onSave();
    } catch (error) {
      console.error('Kişi kaydedilirken hata:', error);
    }
  };

  // Loading ve error durumlarını birleştir
  const loading = loadingPerson || loadingAll;
  const error = errorPerson || errorAll || null;

  if (loading) {
    return <LoadingIndicator />;
  }
  if (error) {
    return <ErrorMessage message={error} />;
  }

  return (
    <PersonForm
      person={person}
      setPerson={setPerson}
      availableFathers={availableFathers}
      availableMothers={availableMothers}
      availableSpouses={availableSpouses}
      loading={loading}
      error={error}
      onSave={handleSave}
      onCancel={onCancel}
    />
  );
}; 