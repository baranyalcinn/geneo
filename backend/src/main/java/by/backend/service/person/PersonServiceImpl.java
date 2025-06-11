package by.backend.service.person;

import by.backend.model.dto.PersonDTO;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.entity.Person;
import by.backend.repository.PersonRepository;
import by.backend.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;

    @Override
    @Transactional
    public PersonDTO createPerson(PersonDTO personDTO) {
        try {
            log.info("Kişi oluşturma DTO: {}", personDTO);
            Person person = personMapper.toEntity(personDTO);
            
            log.info("Kişi entity oluşturuluyor (mapper sonrası): {}", person);
            Person savedPerson = personRepository.save(person);
            log.info("Kişi kaydedildi: {}", savedPerson);
            return personMapper.toDTO(savedPerson);
        } catch (Exception e) {
            log.error("Kişi oluşturma hatası: ", e);
            throw new RuntimeException("Kişi oluşturulamadı: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PersonDTO updatePerson(Long id, PersonDTO personDTO) {
        Person personEntity = personRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Güncellenecek kişi bulunamadı: " + id));
        
        personMapper.updateEntityFromDto(personDTO, personEntity);
        
        Person updatedPerson = personRepository.save(personEntity);
        log.info("Kişi güncellendi: {}", updatedPerson);
        return personMapper.toDTO(updatedPerson);
    }

    @Override
    @Transactional
    public void deletePerson(Long id) {
        personRepository.deleteById(id);
        log.info("Kişi silindi: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonDTO getPerson(Long id) {
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Kişi bulunamadı: " + id));
        return personMapper.toDTO(person);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonDTO> getAllPersons() {
        return personRepository.findAll().stream()
            .map(personMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonDTO> findByLastName(String lastName) {
        return personRepository.findByLastName(lastName).stream()
            .map(personMapper::toDTO)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Person getPersonEntity(Long id) {
        return personRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Kişi entity bulunamadı: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonSummaryDTO getPersonSummary(Long id) {
        Person person = personRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Kişi özeti için kişi bulunamadı: " + id));
        return personMapper.toSummaryDTO(person);
    }
} 