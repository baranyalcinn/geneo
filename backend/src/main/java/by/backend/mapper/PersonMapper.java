package by.backend.mapper;

import by.backend.model.dto.PersonDTO;
import by.backend.model.dto.PersonInfoDTO;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.entity.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import by.backend.model.enums.Gender;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Kişi verilerinin entity ve DTO dönüşümlerini sağlayan mapper.
 */
@Mapper(componentModel = "spring")
@Component
public interface PersonMapper {

    /**
     * Person entity'sini PersonDTO'ya dönüştürür
     * 
     * @param person Dönüştürülecek kişi entity'si
     * @return Kişi DTO'su
     */
    @Mappings({
        @Mapping(source = "gender", target = "gender"),
        @Mapping(source = "person", target = "mother", qualifiedByName = "getMotherSummary"),
        @Mapping(source = "person", target = "father", qualifiedByName = "getFatherSummary"),
        @Mapping(source = "person", target = "spouse", qualifiedByName = "getSpouseSummary")
    })
    PersonDTO toDTO(Person person);

    /**
     * Person entity'sini PersonInfoDTO'ya dönüştürür. Oyun için gerekli temel bilgileri içerir.
     *
     * @param person Dönüştürülecek kişi entity'si
     * @return Oyun için kişi bilgi DTO'su
     */
    @Mappings({
        @Mapping(target = "name", expression = "java(person.getFirstName() + \" \" + person.getLastName())"),
        @Mapping(target = "birthYear", expression = "java(person.getBirthDate() != null ? person.getBirthDate().getYear() : null)"),
        @Mapping(target = "deathYear", expression = "java(person.getDeathDate() != null ? person.getDeathDate().getYear() : null)")
    })
    PersonInfoDTO personToPersonInfo(Person person);

    /**
     * PersonDTO'yu Person entity'sine dönüştürür
     * İlişkiler ve bağlantılı listeler ignore edilir
     * 
     * @param personDTO Dönüştürülecek kişi DTO'su
     * @return Kişi entity'si
     */
    @Mappings({
        @Mapping(source = "gender", target = "gender"),
        @Mapping(target = "familyTrees", ignore = true),
        @Mapping(target = "relationships1", ignore = true),
        @Mapping(target = "relationships2", ignore = true),
        @Mapping(target = "allRelationships", ignore = true),
        @Mapping(target = "children", ignore = true),
        @Mapping(target = "parents", ignore = true),
        @Mapping(target = "siblings", ignore = true)
    })
    Person toEntity(PersonDTO personDTO);

    /**
     * Person entity'sini özet DTO'ya dönüştürür
     * 
     * @param person Dönüştürülecek kişi entity'si
     * @return Özet kişi DTO'su
     */
    PersonSummaryDTO toSummaryDTO(Person person);

    /**
     * Person entity listesini PersonDTO listesine dönüştürür
     * 
     * @param personList Dönüştürülecek kişi entity listesi
     * @return Kişi DTO listesi
     */
    List<PersonDTO> toDTOList(List<Person> personList);

    /**
     * Person entity listesini özet DTO listesine dönüştürür
     * 
     * @param personList Dönüştürülecek kişi entity listesi
     * @return Özet kişi DTO listesi
     */
    List<PersonSummaryDTO> toSummaryDTOList(List<Person> personList);

    /**
     * PersonDTO'dan Person entity'sini günceller
     * ID korunur ve ilişkiler ignore edilir
     * 
     * @param personDTO Güncellenecek veriler
     * @param personEntity Güncellenecek entity
     */
    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(source = "gender", target = "gender"),
        @Mapping(target = "familyTrees", ignore = true),
        @Mapping(target = "relationships1", ignore = true),
        @Mapping(target = "relationships2", ignore = true),
        @Mapping(target = "allRelationships", ignore = true),
        @Mapping(target = "children", ignore = true),
        @Mapping(target = "parents", ignore = true),
        @Mapping(target = "siblings", ignore = true)
    })
    void updateEntityFromDto(PersonDTO personDTO, @MappingTarget Person personEntity);

    /**
     * Özet DTO'dan Person entity'si oluşturur
     * Birçok alan ignore edilir
     * 
     * @param summaryDTO Dönüştürülecek özet DTO
     * @return Kişi entity'si
     */
    @Mappings({
        @Mapping(target = "birthDate", ignore = true),
        @Mapping(target = "deathDate", ignore = true),
        @Mapping(target = "familyTrees", ignore = true),
        @Mapping(target = "gender", ignore = true),
        @Mapping(target = "relationships1", ignore = true),
        @Mapping(target = "relationships2", ignore = true),
        @Mapping(target = "allRelationships", ignore = true),
        @Mapping(target = "children", ignore = true),
        @Mapping(target = "parents", ignore = true),
        @Mapping(target = "siblings", ignore = true)
    })
    Person fromSummaryDTO(PersonSummaryDTO summaryDTO);

    // Helper method to get mother summary
    @Named("getMotherSummary")
    default PersonSummaryDTO getMotherSummary(Person person) {
        if (person == null) {
            return null;
        }
        return person.getParents().stream()
                .filter(p -> p.getGender() == Gender.KADIN)
                .findFirst()
                .map(this::toSummaryDTO)
                .orElse(null);
    }

    // Helper method to get father summary
    @Named("getFatherSummary")
    default PersonSummaryDTO getFatherSummary(Person person) {
        if (person == null) {
            return null;
        }
        return person.getParents().stream()
                .filter(p -> p.getGender() == Gender.ERKEK)
                .findFirst()
                .map(this::toSummaryDTO)
                .orElse(null);
    }

    // Helper method to get spouse summary
    @Named("getSpouseSummary")
    default PersonSummaryDTO getSpouseSummary(Person person) {
        if (person == null) {
            return null;
        }
        Optional<Person> spouseOptional = person.getSpouseOptional();
        return spouseOptional.map(this::toSummaryDTO).orElse(null);
    }
} 