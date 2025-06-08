package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResultDTO {
    private String playerName;
    private int score;
    private Difficulty difficulty;
    private java.time.LocalDate date;
    private int correctAnswers;
    private int totalQuestions;
    private int maxStreak;
    private List<String> badges;
    private boolean isHighScore;
    private boolean correct;
    private boolean gameOver;
} 