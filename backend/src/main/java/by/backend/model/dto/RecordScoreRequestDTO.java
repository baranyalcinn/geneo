package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordScoreRequestDTO {
    private String playerName;
    private int score;
    private Difficulty difficulty;
    private int correctAnswers;
    private int totalQuestions;
    private int maxStreak;
    private Locale locale;
} 