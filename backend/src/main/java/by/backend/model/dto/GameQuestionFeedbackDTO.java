package by.backend.model.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import by.backend.model.enums.RelationshipType;

@Data
public class GameQuestionFeedbackDTO {
    @NotBlank(message = "Question ID cannot be blank")
    private String questionId;

    @NotNull(message = "RelationshipType must be provided")
    private RelationshipType relationshipType;

    @NotNull(message = "isCorrect flag must be provided")
    private boolean isCorrect;

    @NotBlank(message = "Feedback cannot be blank")
    private String feedback; // 'good' or 'bad'
} 