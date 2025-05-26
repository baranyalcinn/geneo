package by.backend.mapper;

import by.backend.model.dto.FamilyTreeDTO; // FamilyTree DTO sınıfı
import by.backend.model.entity.FamilyTree; // FamilyTree entity'si aktif edildi
import by.backend.model.dto.PersonDTO;
import by.backend.model.dto.RelationshipDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Aile ağacı verilerinin DTO dönüşümlerini sağlayan mapper.
 * FamilyTreeDTO, kişiler ve ilişkiler gibi çeşitli bileşenlerden oluşur.
 */
@Mapper(componentModel = "spring", uses = {PersonMapper.class, RelationshipMapper.class})
public interface FamilyTreeMapper {

    /**
     * Kişiler ve ilişkiler listesinden FamilyTreeDTO oluşturur
     * 
     * @param persons Aile ağacındaki kişiler listesi
     * @param relationships Aile ağacındaki ilişkiler listesi
     * @param rootPerson Aile ağacının kök kişisi için DTO
     * @return Derlenmiş aile ağacı DTO'su
     */
    @Mapping(source = "persons", target = "persons")
    @Mapping(source = "relationships", target = "relationships")
    @Mapping(source = "rootPerson", target = "rootPerson")
    @Mapping(target = "name", ignore = true)
    FamilyTreeDTO createFamilyTreeDTO(List<PersonDTO> persons, List<RelationshipDTO> relationships, PersonDTO rootPerson);
    
    /**
     * FamilyTree entity'sinden FamilyTreeDTO'ya dönüşüm
     */
    @Mapping(source = "rootMember", target = "rootPerson")
    @Mapping(source = "members", target = "persons")
    @Mapping(target = "relationships", ignore = true) // İlişkiler ayrıca yüklenmeli
    @Mapping(source = "name", target = "name")
    FamilyTreeDTO toDTO(FamilyTree familyTree);
    
    /**
     * FamilyTreeDTO'dan FamilyTree entity'sine dönüşüm
     */
    @Mapping(source = "rootPerson", target = "rootMember")
    @Mapping(source = "persons", target = "members")
    @Mapping(target = "id", ignore = true)
    FamilyTree toEntity(FamilyTreeDTO dto);
} 