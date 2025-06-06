package by.backend.service.game;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerValidationResult {
    private boolean isCorrect;
    private String correctAnswerText;
    private String category;
} 