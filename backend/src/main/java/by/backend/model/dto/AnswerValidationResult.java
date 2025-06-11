package by.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Getter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AnswerValidationResult {
    private boolean isCorrect;
    private String correctAnswerText;
    private String category;
    private RelationshipPathDTO relationshipPath;
} 