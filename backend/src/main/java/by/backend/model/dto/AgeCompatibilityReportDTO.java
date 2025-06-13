package by.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgeCompatibilityReportDTO {
    private String relationshipType;
    private List<FamilyMemberDTO> compatibleMembers;
    private List<FamilyMemberDTO> incompatibleMembers;
    private Map<String, Integer> ageDistribution;
    private Double averageAgeDifference;
    private Integer minCompatibleAge;
    private Integer maxCompatibleAge;
    private String compatibilityRules;
    private Integer totalAnalyzedRelationships;
    private Double compatibilityPercentage;
    private List<String> recommendedMatches;
    private String reportGeneratedDate;
} 