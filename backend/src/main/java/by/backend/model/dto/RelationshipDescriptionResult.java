package by.backend.model.dto;

// import by.backend.model.entity.Person; // Person importu kaldırıldı
import by.backend.model.enums.RelationshipStatus;
import by.backend.model.enums.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipDescriptionResult {
    private RelationshipStatus status;
    private String messageKey;
    private List<String> acceptableMessageKeys;
    private String localizedDescription;
    private List<RelationshipStepDTO> path;
    private PersonSummaryDTO person1;
    private PersonSummaryDTO person2;
    private RelationshipType directTypeIfApplicable;
    private RelationshipPathDTO relationshipPath;
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