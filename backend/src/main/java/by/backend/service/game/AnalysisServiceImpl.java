package by.backend.service.game;

import by.backend.model.dto.AnalysisResultDTO;
import by.backend.service.game.session.PlayerAnswer;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    private final MessageSource messageSource;

    public AnalysisServiceImpl(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public AnalysisResultDTO analyze(List<PlayerAnswer> answers, Locale locale) {
        if (answers == null || answers.isEmpty()) {
            return new AnalysisResultDTO(Map.of(), getMessage("game.analysis.no_answers", locale));
        }

        // Group answers by relationship category
        Map<String, List<PlayerAnswer>> answersByCategory = answers.stream()
                .collect(Collectors.groupingBy(PlayerAnswer::getRelationshipCategory));

        // Calculate success rate for each category
        Map<String, Double> successRateByCategory = answersByCategory.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<PlayerAnswer> categoryAnswers = entry.getValue();
                            long correctCount = categoryAnswers.stream().filter(PlayerAnswer::isCorrect).count();
                            return (double) correctCount / categoryAnswers.size() * 100.0;
                        }
                ));

        // Build the summary message
        String summaryMessage = buildSummaryMessage(successRateByCategory, locale);

        return new AnalysisResultDTO(successRateByCategory, summaryMessage);
    }

    private String buildSummaryMessage(Map<String, Double> successRateByCategory, Locale locale) {
        if (successRateByCategory.isEmpty()) {
            return getMessage("game.analysis.no_data", locale);
        }
        
        return successRateByCategory.entrySet().stream()
                .map(entry -> {
                    String categoryKey = "relation.category." + entry.getKey();
                    String localizedCategory = getMessage(categoryKey, locale);
                    String pattern = getMessage("game.analysis.summary_pattern", locale);
                    return String.format(pattern, localizedCategory, entry.getValue());
                })
                .collect(Collectors.joining(" "));
    }

    private String getMessage(String code, Locale locale) {
        try {
            return messageSource.getMessage(code, null, locale);
        } catch (Exception _) {
            // Fallback for missing keys
            return code;
        }
    }
} 