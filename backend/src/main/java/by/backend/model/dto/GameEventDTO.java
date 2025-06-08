package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameEventDTO {
    
    public enum EventType {
        GAME_STARTED,
        QUESTION_ANSWERED,
        STREAK_ACHIEVED,
        TIME_WARNING,
        GAME_COMPLETED,
        GAME_PAUSED,
        GAME_RESUMED,
        HIGH_SCORE_ACHIEVED
    }
    
    private String sessionId;
    private String playerName;
    private EventType eventType;
    private LocalDateTime timestamp;
    private Difficulty difficulty;
    
    // Event-specific data
    private Map<String, Object> eventData;
    
    // Score and streak info
    private Integer currentScore;
    private Integer currentStreak;
    private Integer questionsAnswered;
    private Integer totalQuestions;
    
    // Time info
    private Long timeRemainingSeconds;
    private Long totalGameTimeSeconds;
    
    public static GameEventDTO gameStarted(String sessionId, String playerName, Difficulty difficulty) {
        return GameEventDTO.builder()
                .sessionId(sessionId)
                .playerName(playerName)
                .eventType(EventType.GAME_STARTED)
                .timestamp(LocalDateTime.now())
                .difficulty(difficulty)
                .currentScore(0)
                .currentStreak(0)
                .questionsAnswered(0)
                .build();
    }
    
    public static GameEventDTO questionAnswered(String sessionId, String playerName, 
                                              boolean isCorrect, int score, int streak,
                                              int questionsAnswered, int totalQuestions) {
        return GameEventDTO.builder()
                .sessionId(sessionId)
                .playerName(playerName)
                .eventType(EventType.QUESTION_ANSWERED)
                .timestamp(LocalDateTime.now())
                .currentScore(score)
                .currentStreak(streak)
                .questionsAnswered(questionsAnswered)
                .totalQuestions(totalQuestions)
                .eventData(Map.of("isCorrect", isCorrect))
                .build();
    }
    
    public static GameEventDTO streakAchieved(String sessionId, String playerName, 
                                            int streakCount, int score) {
        return GameEventDTO.builder()
                .sessionId(sessionId)
                .playerName(playerName)
                .eventType(EventType.STREAK_ACHIEVED)
                .timestamp(LocalDateTime.now())
                .currentScore(score)
                .currentStreak(streakCount)
                .eventData(Map.of("streakCount", streakCount))
                .build();
    }
    
    public static GameEventDTO timeWarning(String sessionId, String playerName, 
                                         long timeRemainingSeconds) {
        return GameEventDTO.builder()
                .sessionId(sessionId)
                .playerName(playerName)
                .eventType(EventType.TIME_WARNING)
                .timestamp(LocalDateTime.now())
                .timeRemainingSeconds(timeRemainingSeconds)
                .build();
    }
    
    public static GameEventDTO gameCompleted(String sessionId, String playerName, 
                                           int finalScore, int totalQuestions,
                                           boolean isHighScore) {
        return GameEventDTO.builder()
                .sessionId(sessionId)
                .playerName(playerName)
                .eventType(EventType.GAME_COMPLETED)
                .timestamp(LocalDateTime.now())
                .currentScore(finalScore)
                .totalQuestions(totalQuestions)
                .eventData(Map.of("isHighScore", isHighScore))
                .build();
    }
} 