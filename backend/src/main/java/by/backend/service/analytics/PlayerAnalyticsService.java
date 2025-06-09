package by.backend.service.analytics;

import by.backend.model.enums.RelationshipType;
import by.backend.model.enums.Difficulty;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Service
public interface PlayerAnalyticsService {
    
    /**
     * Oyuncunun güçlü ve zayıf olduğu akrabalık türlerini analiz eder
     */
    PlayerInsights getPlayerInsights(String playerName);
    
    /**
     * Oyuncunun öğrenme eğrisini takip eder
     */
    LearningCurve getLearningCurve(String playerName);
    
    /**
     * Kişiselleştirilmiş soru önerileri
     */
    List<QuestionRecommendation> getPersonalizedQuestions(String playerName, Difficulty difficulty);
    
    @Data
    class PlayerInsights {
        private Map<RelationshipType, Double> accuracyByRelationType;
        private Map<Difficulty, Double> accuracyByDifficulty;
        private List<String> strongAreas;
        private List<String> weakAreas;
        private Double overallAccuracy;
        private Integer totalGamesPlayed;
        private LocalDateTime lastPlayed;
    }
    
    @Data
    class LearningCurve {
        private List<DataPoint> accuracyOverTime;
        private List<DataPoint> speedOverTime;
        private Double improvementRate;
        private String trend; // "improving", "stable", "declining"
    }
    
    @Data
    class DataPoint {
        private LocalDateTime date;
        private Double value;
    }
    
    @Data
    class QuestionRecommendation {
        private RelationshipType focusArea;
        private String reason;
        private Double priority;
    }
} 