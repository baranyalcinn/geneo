package by.backend.service.ai;

import by.backend.model.enums.Difficulty;
import by.backend.model.dto.GameQuestionDTO;
import by.backend.service.analytics.PlayerAnalyticsService.PlayerInsights;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public interface SmartQuestionGenerator {
    
    /**
     * Oyuncunun geçmiş performansına göre adaptif sorular üretir
     */
    List<GameQuestionDTO> generateAdaptiveQuestions(
        String playerName, 
        Difficulty baseDifficulty, 
        int questionCount,
        Locale locale
    );
    
    /**
     * Oyuncunun zayıf alanlarına odaklanan sorular üretir
     */
    List<GameQuestionDTO> generateFocusedQuestions(
        PlayerInsights insights,
        int questionCount,
        Locale locale
    );
    
    /**
     * Öğrenme teorisine dayalı spaced repetition sistemi
     */
    List<GameQuestionDTO> generateSpacedRepetitionQuestions(
        String playerName,
        Locale locale
    );
    
    /**
     * Oyuncunun öğrenme stiline göre soru tiplerini özelleştirir
     */
    List<GameQuestionDTO> generatePersonalizedQuestions(
        String playerName,
        LearningStyle learningStyle,
        Difficulty difficulty,
        Locale locale
    );
    
    enum LearningStyle {
        VISUAL,      // Görsel öğrenme
        ANALYTICAL,  // Analitik öğrenme  
        INTUITIVE,   // Sezgisel öğrenme
        SEQUENTIAL   // Adım adım öğrenme
    }
    
    /**
     * Oyuncunun öğrenme stilini belirler
     */
    LearningStyle detectLearningStyle(String playerName);
    
    /**
     * Gerçek zamanlı zorluk ayarlama
     */
    Difficulty adjustDifficultyRealTime(
        String playerName,
        double currentAccuracy,
        long averageResponseTime
    );
} 