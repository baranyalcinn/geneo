package by.backend.service.game;

import by.backend.model.dto.AnalysisResultDTO;
import by.backend.service.game.session.PlayerAnswer;

import java.util.List;
import java.util.Locale;

public interface AnalysisService {
    /**
     * Analyzes a list of player answers and returns a summary of performance.
     * @param answers The list of answers from a game session.
     * @param locale The locale to generate the summary message in.
     * @return An {@link AnalysisResultDTO} containing the analysis.
     */
    AnalysisResultDTO analyze(List<PlayerAnswer> answers, Locale locale);
} 