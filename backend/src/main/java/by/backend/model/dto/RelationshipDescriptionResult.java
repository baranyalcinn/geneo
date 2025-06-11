package by.backend.model.dto;

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
    
    @Builder.Default
    private Double confidenceScore = 1.0;
    @Builder.Default
    private Integer complexityLevel = 1;
    @Builder.Default
    private Integer pathLength = 0;
    private String relationshipCategory;
    @Builder.Default
    private Boolean isBloodRelated = false;
    @Builder.Default
    private Boolean isInLawRelated = false;
    @Builder.Default
    private Boolean isStepRelated = false;
    private String maternalPaternal;
    @Builder.Default
    private Integer generationDifference = 0;
    private String specialNotes;
    @Builder.Default
    private Long computationTimeMs = 0L;
    
    // Helper metodları aynı
    public boolean isSuitableForHardQuestions() {
        return complexityLevel >= 3 || 
               (confidenceScore != null && confidenceScore < 0.9) ||
               isInLawRelated ||
               isStepRelated ||
               (pathLength != null && pathLength >= 3) ||
               (messageKey != null && (messageKey.contains("spouse_sibling_spouse") ||
                messageKey.contains("distant") ||
                messageKey.contains("complex")));
    }
    
    public String getDifficultyRecommendation() {
        if (complexityLevel <= 2 && (confidenceScore == null || confidenceScore >= 0.95)) {
            return "EASY";
        } else if (complexityLevel <= 3 && (confidenceScore == null || confidenceScore >= 0.8)) {
            return "MEDIUM";
        } else {
            return "HARD";
        }
    }
}