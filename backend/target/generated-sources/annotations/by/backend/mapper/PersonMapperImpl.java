package by.backend.mapper;

import by.backend.model.dto.PersonDTO;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.entity.Person;
import by.backend.model.enums.Gender;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-24T15:08:03+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.1 (Oracle Corporation)"
)
@Component
public class PersonMapperImpl implements PersonMapper {

    @Override
    public PersonDTO toDTO(Person person) {
        if ( person == null ) {
            return null;
        }

        Gender gender = null;
        PersonSummaryDTO mother = null;
        PersonSummaryDTO father = null;
        PersonSummaryDTO spouse = null;
        Long id = null;
        String firstName = null;
        String lastName = null;
        LocalDate birthDate = null;
        LocalDate deathDate = null;

        gender = person.getGender();
        mother = getMotherSummary( person );
        father = getFatherSummary( person );
        spouse = getSpouseSummary( person );
        id = person.getId();
        firstName = person.getFirstName();
        lastName = person.getLastName();
        birthDate = person.getBirthDate();
        deathDate = person.getDeathDate();

        PersonDTO personDTO = new PersonDTO( id, firstName, lastName, birthDate, deathDate, gender, mother, father, spouse );

        return personDTO;
    }

    @Override
    public Person toEntity(PersonDTO personDTO) {
        if ( personDTO == null ) {
            return null;
        }

        Person person = new Person();

        person.setGender( personDTO.gender() );
        person.setId( personDTO.id() );
        person.setFirstName( personDTO.firstName() );
        person.setLastName( personDTO.lastName() );
        person.setBirthDate( personDTO.birthDate() );
        person.setDeathDate( personDTO.deathDate() );

        return person;
    }

    @Override
    public PersonSummaryDTO toSummaryDTO(Person person) {
        if ( person == null ) {
            return null;
        }

        Long id = null;
        String firstName = null;
        String lastName = null;

        id = person.getId();
        firstName = person.getFirstName();
        lastName = person.getLastName();

        PersonSummaryDTO personSummaryDTO = new PersonSummaryDTO( id, firstName, lastName );

        return personSummaryDTO;
    }

    @Override
    public List<PersonDTO> toDTOList(List<Person> personList) {
        if ( personList == null ) {
            return null;
        }

        List<PersonDTO> list = new ArrayList<PersonDTO>( personList.size() );
        for ( Person person : personList ) {
            list.add( toDTO( person ) );
        }

        return list;
    }

    @Override
    public List<PersonSummaryDTO> toSummaryDTOList(List<Person> personList) {
        if ( personList == null ) {
            return null;
        }

        List<PersonSummaryDTO> list = new ArrayList<PersonSummaryDTO>( personList.size() );
        for ( Person person : personList ) {
            list.add( toSummaryDTO( person ) );
        }

        return list;
    }

    @Override
    public void updateEntityFromDto(PersonDTO personDTO, Person personEntity) {
        if ( personDTO == null ) {
            return;
        }

        personEntity.setGender( personDTO.gender() );
        personEntity.setFirstName( personDTO.firstName() );
        personEntity.setLastName( personDTO.lastName() );
        personEntity.setBirthDate( personDTO.birthDate() );
        personEntity.setDeathDate( personDTO.deathDate() );
    }

    @Override
    public Person fromSummaryDTO(PersonSummaryDTO summaryDTO) {
        if ( summaryDTO == null ) {
            return null;
        }

        Person person = new Person();

        person.setId( summaryDTO.id() );
        person.setFirstName( summaryDTO.firstName() );
        person.setLastName( summaryDTO.lastName() );

        return person;
    }
}
