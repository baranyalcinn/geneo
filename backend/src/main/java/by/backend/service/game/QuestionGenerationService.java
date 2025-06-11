package by.backend.service.game;

import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.enums.Difficulty;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public interface QuestionGenerationService {
    /**
     * Generates a single game question based on difficulty and already asked questions.
     * @param difficulty The desired difficulty.
     * @param askedSignatures A set of signatures for questions already asked in the session.
     * @param locale The locale for localization.
     * @return A generated GameQuestionDTO, or null if one could not be generated.
     */
    GameQuestionDTO generateQuestion(Difficulty difficulty, Set<String> askedSignatures, Locale locale);

    /**
     * Generates a list of candidate questions for selection.
     * @param difficulty The desired difficulty.
     * @param count The number of questions to generate.
     * @param askedSignatures A set of signatures for questions already asked in the session.
     * @param locale The locale for localization.
     * @return A list of generated GameQuestionDTOs.
     */
    List<GameQuestionDTO> generateCandidateQuestions(Difficulty difficulty, int count, Set<String> askedSignatures, Locale locale);
} 