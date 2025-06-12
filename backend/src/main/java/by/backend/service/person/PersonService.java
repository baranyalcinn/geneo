package by.backend.service.person;

import by.backend.model.dto.PersonDTO;
import by.backend.model.dto.PersonSummaryDTO;
// import by.backend.model.entity.Person; // Artık public API'de Person entity'si olmayacak (getPersonEntity hariç)
import java.util.List;
import java.util.Locale;

public interface PersonService {
    PersonDTO createPerson(PersonDTO personDTO); // Dönüş tipi PersonDTO oldu
    PersonDTO updatePerson(Long id, PersonDTO personDTO); // Dönüş tipi PersonDTO oldu, Locale eklendi
    void deletePerson(Long id);
    PersonDTO getPerson(Long id);
    PersonSummaryDTO getPersonSummary(Long id); // Yeni metot eklendi
    by.backend.model.entity.Person getPersonEntity(Long id); // Tam yoluyla entity tipini belirtelim
    List<PersonDTO> getAllPersons();
    List<PersonDTO> findByLastName(String lastName);
} 