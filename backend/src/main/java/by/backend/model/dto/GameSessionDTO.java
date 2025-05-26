package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class GameSessionDTO {
    private String sessionId;
    private GameQuestionDTO currentQuestion;
    private int questionsAnswered; // Cevaplanan soru sayısı
    private int totalQuestionsInSession; // Oturumdaki toplam soru sayısı
    private int currentScore;
    private int currentStreak;
    private Difficulty difficulty;
    private boolean gameOver;
    private GameResultDTO finalResult; // Oyun bittiğinde dolu olacak
} 