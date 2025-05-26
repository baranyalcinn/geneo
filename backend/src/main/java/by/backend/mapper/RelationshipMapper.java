package by.backend.mapper;

import by.backend.model.dto.RelationshipDTO;
// import by.backend.model.dto.RelationshipDescriptionResult; // Kullanılmadığı için yorum satırına alındı.
import by.backend.model.entity.Relationship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * İlişki verilerinin entity ve DTO dönüşümlerini sağlayan mapper.
 */
@Mapper(componentModel = "spring", uses = PersonMapper.class)
public interface RelationshipMapper {

    /**
     * İlişki entity'sini DTO'ya dönüştürür
     * 
     * @param relationship Dönüştürülecek ilişki entity'si
     * @return İlişki DTO'su
     */
    @Mapping(source = "person1", target = "person1")
    @Mapping(source = "person2", target = "person2")
    @Mapping(source = "type", target = "type")
    @Mapping(target = "isActive", ignore = true)
    RelationshipDTO toDTO(Relationship relationship);

    /**
     * İlişki DTO'sundan entity oluşturur
     * 
     * @param relationshipDTO Dönüştürülecek ilişki DTO'su
     * @return İlişki entity'si
     */
    @Mapping(source = "person1", target = "person1") 
    @Mapping(source = "person2", target = "person2") 
    @Mapping(source = "type", target = "type")
    @Mapping(target = "id", ignore = true) // ID otomatik atanır
    Relationship toEntity(RelationshipDTO relationshipDTO);

    /**
     * İlişki entity listesini DTO listesine dönüştürür
     * 
     * @param relationships Dönüştürülecek ilişki entity listesi
     * @return İlişki DTO listesi
     */
    List<RelationshipDTO> toDTOList(List<Relationship> relationships);

    // RelationshipStepDTO ve RelationshipDescriptionResult için entity olmadığı için
    // doğrudan entity'den dönüşüm metotları burada tanımlanmıyor.
    // Bu DTO'lar genellikle servis katmanında özel mantıklarla oluşturulur.
    // İhtiyaç duyulursa, bu DTO'ları oluşturan metotlara özel mappinngler eklenebilir.
} 