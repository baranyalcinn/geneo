package by.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(access = lombok.AccessLevel.PUBLIC)
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipPathDTO {
    private List<RelationshipStepDTO> steps;
    private String description;
} 