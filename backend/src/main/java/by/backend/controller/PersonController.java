package by.backend.controller;

import by.backend.model.dto.PersonDTO;
import by.backend.service.person.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@Slf4j
public class PersonController {
    
    private final PersonService personService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PersonDTO> createPerson(@Valid @RequestBody PersonDTO personDTO) {
        log.info("Kişi oluşturma isteği alındı: {}", personDTO);
        PersonDTO createdPersonDTO = personService.createPerson(personDTO);
        return ResponseEntity.ok(createdPersonDTO);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PersonDTO> updatePerson(
            @PathVariable Long id,
            @Valid @RequestBody PersonDTO personDTO) {
        log.info("Kişi güncelleme isteği alındı. ID: {}, Veri: {}", id, personDTO);
        PersonDTO updatedPersonDTO = personService.updatePerson(id, personDTO);
        return ResponseEntity.ok(updatedPersonDTO);
    }

    @DeleteMapping("/{id}")    
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {        
        log.info("Kişi silme isteği alındı. ID: {}", id);        
        personService.deletePerson(id);        
        return ResponseEntity.noContent().build();    
    }    
    
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)    
    public ResponseEntity<PersonDTO> getPerson(@PathVariable Long id) {        
        log.info("Kişi detayı isteği alındı. ID: {}", id);        
        PersonDTO personDTO = personService.getPerson(id);        
        return ResponseEntity.ok(personDTO);    
    }    
    
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)    
    public ResponseEntity<List<PersonDTO>> getAllPersons() {        
        log.info("Tüm kişiler isteği alındı");        
        List<PersonDTO> persons = personService.getAllPersons();        
        return ResponseEntity.ok(persons);    
    }

    @GetMapping(value = "/by-lastname/{lastName}", produces = MediaType.APPLICATION_JSON_VALUE)    
    public ResponseEntity<List<PersonDTO>> findByLastName(@PathVariable String lastName) {        
        log.info("Soyada göre kişi arama isteği alındı. Soyad: {}", lastName);        
        List<PersonDTO> persons = personService.findByLastName(lastName);        
        return ResponseEntity.ok(persons);    
    }
} 