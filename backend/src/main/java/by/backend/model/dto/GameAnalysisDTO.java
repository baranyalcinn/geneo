package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder(access = lombok.AccessLevel.PUBLIC)
@NoArgsConstructor
@AllArgsConstructor
public class GameAnalysisDTO {
    
    private String sessionId;
    private String playerName;
    private Difficulty difficulty;
    
    // Temel istatistikler
    private int totalQuestions;
    private int questionsAnswered;
    private int correctAnswers;
    private int finalScore;
    private int maxStreak;
    
    // Zaman bilgileri
    private long gameStartTime;
    private long gameDuration; // saniye cinsinden
    
    // Performans metrikleri
    private double accuracyPercentage;
    private double averageResponseTime;
    
    // Öneriler ve analiz
    private List<String> recommendations;
    
    // Gelişmiş analizler (opsiyonel)
    private PerformanceMetrics performanceMetrics;
    private StrengthsAndWeaknesses strengthsAndWeaknesses;
    private StreakAnalysis streakAnalysis;
    private TimeDistribution timeDistribution;
    private DifficultyPerformance difficultyPerformance;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private double pointsPerMinute;
        private double consistency;
        private double speedAccuracyRatio;
        private double improvementPotential;
        private double timeManagement;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrengthsAndWeaknesses {
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> improvementSuggestions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakAnalysis {
        private int longestCorrectStreak;
        private int longestIncorrectStreak;
        private int totalStreakCount;
        private double streakConsistency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeDistribution {
        private long fastResponses;    // 0-10 saniye
        private long mediumResponses;  // 10-20 saniye
        private long slowResponses;    // 20+ saniye
        private double averageTimePerQuestion;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DifficultyPerformance {
        private Difficulty difficulty;
        private double actualAccuracy;
        private double expectedAccuracy;
        private double actualAverageTime;
        private double expectedAverageTime;
        private String performanceRating;
    }
} 