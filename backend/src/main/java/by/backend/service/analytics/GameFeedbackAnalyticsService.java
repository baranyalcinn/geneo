package by.backend.service.analytics;

import by.backend.model.entity.GameQuestionFeedback;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.GameQuestionFeedbackRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class GameFeedbackAnalyticsService {

    private final GameQuestionFeedbackRepository feedbackRepository;

    /**
     * Analyzes the feedback for game questions to determine the difficulty of relationship types.
     *
     * @return A map where the key is the RelationshipType and the value is the success rate (0.0 to 1.0).
     */
    public Map<RelationshipType, Double> analyzeRelationshipDifficulty() {
        List<GameQuestionFeedback> allFeedback = feedbackRepository.findAll();

        // Group feedback by relationship type
        Map<RelationshipType, List<GameQuestionFeedback>> feedbackByRelationship = allFeedback.stream()
                .collect(Collectors.groupingBy(GameQuestionFeedback::getRelationshipType));

        // Calculate success rate for each relationship type
        return feedbackByRelationship.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateSuccessRate(entry.getValue())
                ));
    }

    private double calculateSuccessRate(List<GameQuestionFeedback> feedbacks) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return 0.0; // No feedback, no success
        }

        long correctAnswers = feedbacks.stream().filter(GameQuestionFeedback::isCorrect).count();
        long totalAnswers = feedbacks.size();

        return (double) correctAnswers / totalAnswers;
    }
} 