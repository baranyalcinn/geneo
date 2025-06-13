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
public class RelationshipAnalysisDTO {
    private Long personId;
    private String personName;
    private Integer totalRelationships;
    private Integer directRelationships;
    private Integer extendedRelationships;
    private Map<String, Integer> relationshipTypeCount;
    private List<FamilyMemberDTO> familyMembers;
    private Double averageRelationshipStrength;
    private String familyRole;
    private Integer generationLevel;
    private Boolean hasComplexRelationships;
    private List<String> relationshipPaths;
    private String analysisDate;
} 