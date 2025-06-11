package by.backend.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class RelationshipPathDTO {
    private List<RelationshipStepDTO> steps;
    private String description;
} 