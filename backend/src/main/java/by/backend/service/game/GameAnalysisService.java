package by.backend.service.game;

import by.backend.model.dto.GameAnalysisDTO;
import by.backend.service.game.session.GameSession;
import by.backend.service.game.session.PlayerAnswer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class GameAnalysisService {

    public GameAnalysisDTO analyzeGameSession(GameSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Game session cannot be null");
        }

        List<PlayerAnswer> answers = session.getPlayerAnswers();
        
        return GameAnalysisDTO.builder()
                .sessionId(session.getSessionId())
                .playerName(session.getPlayerName())
                .difficulty(session.getDifficulty())
                .totalQuestions(session.getTotalQuestions())
                .questionsAnswered(session.getQuestionsAnswered().get())
                .correctAnswers(session.getCorrectAnswers())
                .finalScore(session.getScore().get())
                .maxStreak(session.getMaxStreak().get())
                .gameStartTime(session.getStartTime())
                .gameDuration(calculateGameDuration(session))
                .accuracyPercentage(calculateAccuracy(answers))
                .averageResponseTime(calculateAverageResponseTime(answers))
                .recommendations(generateRecommendations(session, answers))
                .build();
    }

    private long calculateGameDuration(GameSession session) {
        return Instant.now().getEpochSecond() - session.getStartTime();
    }

    private double calculateAccuracy(List<PlayerAnswer> answers) {
        if (answers.isEmpty()) return 0.0;
        
        long correctCount = answers.stream()
                .mapToLong(answer -> answer.isCorrect() ? 1 : 0)
                .sum();
        
        return (double) correctCount / answers.size() * 100.0;
    }

    private double calculateAverageResponseTime(List<PlayerAnswer> answers) {
        if (answers.isEmpty()) return 0.0;
        
        return answers.stream()
                .filter(answer -> answer.getResponseTimeSeconds() > 0)
                .mapToDouble(PlayerAnswer::getResponseTimeSeconds)
                .average()
                .orElse(0.0);
    }

    private List<String> generateRecommendations(GameSession session, List<PlayerAnswer> answers) {
        List<String> recommendations = new ArrayList<>();
        
        double accuracy = calculateAccuracy(answers);
        double avgTime = calculateAverageResponseTime(answers);
        
        if (accuracy < 60) {
            recommendations.add("Aile iliskileri konusunda daha fazla calisma yapin");
            recommendations.add("Kolay seviyeden baslayarak asamali olarak ilerleyin");
        } else if (accuracy >= 80) {
            recommendations.add("Mukemmel performans! Daha zor seviyeyi deneyin");
            recommendations.add("Hiz odakli antrenman yapabilirsiniz");
        }
        
        if (avgTime > 18) {
            recommendations.add("Daha hizli karar verme konusunda calisin");
            recommendations.add("Iliski kaliplarini ezberlemeye odaklanin");
        }
        
        int maxStreak = session.getMaxStreak().get();
        if (maxStreak < 3) {
            recommendations.add("Konsantrasyon egzersizleri yapin");
        }
        
        return recommendations;
    }
} 