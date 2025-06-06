package by.backend.model.dto;

// import by.backend.model.entity.Person; // Person importu kaldırıldı
import by.backend.model.enums.RelationshipStatus;
import by.backend.model.enums.RelationshipType;
import lombok.Builder;
import lombok.Data;


import java.util.List;

@Data
@Builder
// @AllArgsConstructor anotasyonunu kaldırıyorum çünkü @Builder zaten constructor oluşturuyor
public class RelationshipDescriptionResult {
    private final RelationshipStatus status;
    private final String messageKey;
    private final List<String> acceptableMessageKeys;
    private final String localizedDescription;
    private final List<RelationshipStepDTO> path;
    private final PersonSummaryDTO person1; // PersonSummaryDTO olarak değiştirildi
    private final PersonSummaryDTO person2; // PersonSummaryDTO olarak değiştirildi
    private final RelationshipType directTypeIfApplicable;
    private final RelationshipPathDTO relationshipPath;
}

/* Java 16+ Record alternative:
public record RelationshipDescriptionResult(
    String localizedDescription,
    String messageKey,
    RelationshipStatus status,
    PersonSummaryDTO person1, // PersonSummaryDTO olarak değiştirildi
    PersonSummaryDTO person2, // PersonSummaryDTO olarak değiştirildi
    RelationshipType directTypeIfApplicable
) {
    // İsteğe bağlı olarak ek kurucular veya yardımcı metotlar eklenebilir.
    public static RelationshipDescriptionResult success(String localized, String key, PersonSummaryDTO p1, PersonSummaryDTO p2, RelationshipType type) {
        return new RelationshipDescriptionResult(localized, key, RelationshipStatus.FOUND, p1, p2, type);
    }
    public static RelationshipDescriptionResult self(String localized, String key, PersonSummaryDTO p1) {
        return new RelationshipDescriptionResult(localized, key, RelationshipStatus.SELF_REFERENCE, p1, p1, null);
    }
    // etc. for other statuses
}
*/ 