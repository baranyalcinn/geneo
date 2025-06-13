package by.backend.service.game;

import by.backend.config.GameProperties;
import by.backend.exception.GameException;
import by.backend.model.dto.*;
import by.backend.model.entity.GameQuestionFeedback;
import by.backend.model.entity.HighScore;
import by.backend.model.enums.Difficulty;
import by.backend.repository.GameQuestionFeedbackRepository;
import by.backend.repository.HighScoreRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);
    
    // Error message keys
    private static final String ERROR_GENERATE_QUESTION_FAILED = "game.error.generate_question_failed";
    private static final String FEEDBACK_SAVE_ERROR_MSG = "Feedback could not be saved due to an internal error.";
    
    private final HighScoreRepository highScoreRepository;
    private final MessageSource messageSource;
    private final GameProperties gameProperties;
    private final QuestionGenerationService questionGenerationService;
    private final AnswerValidatorService answerValidatorService;
    private final GameQuestionFeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public InitialGameDataDTO startGame(String playerName, Difficulty difficulty, Locale locale) {
        log.info("Starting game for player '{}' with difficulty '{}'", playerName, difficulty);

        GameQuestionDTO firstQuestion = questionGenerationService.generateQuestion(difficulty, Collections.emptySet(), locale);

        if (firstQuestion == null) {
            log.warn("startGame: {} zorluğunda soru üretilemedi, bir alt/üst zorluk deneniyor.", difficulty);
            firstQuestion = findQuestionInOtherDifficulties(difficulty, locale);
        }
        
        if (firstQuestion == null) {
             log.error("startGame: İlk soru üretilemedi. Oyuncu: {}, İstenen Zorluk: {}", playerName, difficulty);
             throw new GameException(getMessage(ERROR_GENERATE_QUESTION_FAILED, locale));
        }

        Difficulty actualDifficulty = firstQuestion.getDifficulty();
        log.info("Oyun {} zorluğunda başlatıldı (istenen: {})", actualDifficulty, difficulty);

        long gameDurationInSeconds = gameProperties.getGameDuration(actualDifficulty);
        int totalQuestions = gameProperties.getQuestionsPerGame();

        log.info("New game started for player '{}', first question: {}", playerName, firstQuestion.getId());
        
        firstQuestion.setCorrectAnswer(null);

        return InitialGameDataDTO.builder()
                .firstQuestion(firstQuestion)
                .gameDurationInSeconds((int) gameDurationInSeconds)
                .totalQuestions(totalQuestions)
                .playerName(playerName)
                .difficulty(actualDifficulty)
                .build();
    }

    private GameQuestionDTO findQuestionInOtherDifficulties(Difficulty originalDifficulty, Locale locale) {
        GameQuestionDTO question = null;
        List<Difficulty> searchOrder = new ArrayList<>(Arrays.asList(Difficulty.values()));
        searchOrder.remove(originalDifficulty);
        
        for (Difficulty diff : searchOrder) {
            question = questionGenerationService.generateQuestion(diff, Collections.emptySet(), locale);
            if (question != null) {
                log.info("Soru orijinal zorluk {} yerine {} zorluğunda bulundu.", originalDifficulty, diff);
                return question;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public AnswerResponseDTO answerQuestion(GameAnswerDTO answerDetails, Locale locale) {
        log.info("Processing answer for question '{}'", answerDetails.getQuestionId());

        AnswerValidationResult validationResult = answerValidatorService.validateAnswer(
            answerDetails.getQuestionId(), 
            answerDetails.getAnswer(), 
            locale
        );

        // Basit puan hesaplama
        int pointsEarned = 0;
        if (validationResult.isCorrect()) {
            pointsEarned = calculateSimplePoints(answerDetails.getDifficulty(), answerDetails.getTimeTakenInSeconds());
        }

        // Yeni soru üret (filtreleme olmadan, random)
        GameQuestionDTO nextQuestion = questionGenerationService.generateQuestion(
            answerDetails.getDifficulty(), 
            Collections.emptySet(), 
            locale
        );
        
        // 10 soru sonunda oyunu bitir
        boolean gameOver = (nextQuestion == null) || (answerDetails.getQuestionsAnswered() >= gameProperties.getQuestionsPerGame());
        
        if (nextQuestion != null) {
            nextQuestion.setCorrectAnswer(null);
        }

        log.info("Answer processed for question '{}'. Correct: {}, Points: {}, Questions answered: {}/{}, Game Over: {}", 
                answerDetails.getQuestionId(), validationResult.isCorrect(), pointsEarned, 
                answerDetails.getQuestionsAnswered(), gameProperties.getQuestionsPerGame(), gameOver);

        return AnswerResponseDTO.builder()
                .correctAnswer(validationResult.isCorrect())
                .correctAnswerText(validationResult.getCorrectAnswerText())
                .pointsEarned(pointsEarned)
                .updatedScore(answerDetails.getCurrentScore() + pointsEarned)
                .updatedStreak(validationResult.isCorrect() ? answerDetails.getCurrentStreak() + 1 : 0)
                .nextQuestion(nextQuestion)
                .gameOver(gameOver)
                .relationshipPath(validationResult.isCorrect() ? validationResult.getRelationshipPath() : null)
                .build();
    }

    private int calculateSimplePoints(Difficulty difficulty, double timeTakenInSeconds) {
        int basePoints = switch (difficulty) {
            case EASY -> 10;
            case MEDIUM -> 15;
            case HARD -> 25;
        };
        
        // Süre bonusu
        double timeMultiplier = Math.max(0.5, 1.0 - (timeTakenInSeconds / 30.0));
        return (int) (basePoints * timeMultiplier);
    }

    @Override
    @Transactional
    public GameResultDTO recordGameResult(RecordScoreRequestDTO scoreDetails) {
        log.info("Oyun sonucu kaydediliyor: Oyuncu '{}', Skor '{}', Zorluk '{}'",
                scoreDetails.getPlayerName(), scoreDetails.getScore(), scoreDetails.getDifficulty());

        HighScore existingHighScore = highScoreRepository.findTopByPlayerNameAndDifficultyOrderByScoreDesc(
            scoreDetails.getPlayerName(), 
            scoreDetails.getDifficulty()
        );
        
        if (existingHighScore == null || scoreDetails.getScore() > existingHighScore.getScore()) {
            HighScore scoreToSave = (existingHighScore != null) ? existingHighScore : new HighScore();
            scoreToSave.setPlayerName(scoreDetails.getPlayerName());
            scoreToSave.setScore(scoreDetails.getScore());
            scoreToSave.setDifficulty(scoreDetails.getDifficulty());
            scoreToSave.setCorrectAnswers(scoreDetails.getCorrectAnswers());
            scoreToSave.setTotalQuestions(scoreDetails.getTotalQuestions());
            scoreToSave.setMaxStreak(scoreDetails.getMaxStreak());
            scoreToSave.setPlayedAt(LocalDateTime.now());
            
            HighScore savedScore = highScoreRepository.save(scoreToSave);
            log.info("Yeni rekor kaydedildi: {} - {}", scoreDetails.getPlayerName(), scoreDetails.getScore());
            return convertToGameResultDTO(savedScore);
        }
        
        log.info("Skor yeterince yüksek değil, kayıt yapılmadı: {} - {}", scoreDetails.getPlayerName(), scoreDetails.getScore());
        return GameResultDTO.builder()
                .playerName(scoreDetails.getPlayerName())
                .score(scoreDetails.getScore())
                .difficulty(scoreDetails.getDifficulty())
                .date(LocalDateTime.now().toLocalDate())
                .correctAnswers(scoreDetails.getCorrectAnswers())
                .totalQuestions(scoreDetails.getTotalQuestions())
                .maxStreak(scoreDetails.getMaxStreak())
                .build();
    }

    @Override
    public Map<Difficulty, List<GameResultDTO>> getHighScores() {
        List<HighScore> allHighScores = highScoreRepository.findTop50ByOrderByScoreDesc();
        return allHighScores.stream()
                .map(this::convertToGameResultDTO)
                .collect(Collectors.groupingBy(
                        GameResultDTO::getDifficulty,
                        () -> new EnumMap<>(Difficulty.class),
                        Collectors.toList()
                ));
    }

    @Override
    public GameQuestionDTO generateQuestion(Difficulty difficulty, Locale locale) {
        GameQuestionDTO question = questionGenerationService.generateQuestion(difficulty, Collections.emptySet(), locale);
        if (question == null) {
            throw new GameException(getMessage(ERROR_GENERATE_QUESTION_FAILED, locale));
        }
        question.setCorrectAnswer(null);
        return question;
    }
    


    @Override
    @Transactional
    public void saveFeedback(GameQuestionFeedbackDTO feedbackDTO) {
        log.info("Saving feedback for question ID: {}", feedbackDTO.getQuestionId());
        try {
            GameQuestionFeedback feedback = new GameQuestionFeedback();
            feedback.setQuestionId(feedbackDTO.getQuestionId());
            feedback.setRelationshipType(feedbackDTO.getRelationshipType());
            feedback.setCorrect(feedbackDTO.isCorrect());
            feedback.setFeedback(feedbackDTO.getFeedback());
            
            feedbackRepository.save(feedback);
            log.info("Feedback saved successfully for question ID: {}", feedbackDTO.getQuestionId());
        } catch (Exception e) {
            log.error("Error saving feedback for question ID: {}. Error: {}", feedbackDTO.getQuestionId(), e.getMessage(), e);
            throw new GameException(FEEDBACK_SAVE_ERROR_MSG);
        }
    }

    private GameResultDTO convertToGameResultDTO(HighScore score) {
        if (score == null) return null;
        return GameResultDTO.builder()
                .playerName(score.getPlayerName())
                .score(score.getScore())
                .difficulty(score.getDifficulty())
                .date(score.getPlayedAt().toLocalDate())
                .correctAnswers(score.getCorrectAnswers())
                .totalQuestions(score.getTotalQuestions())
                .maxStreak(score.getMaxStreak())
                .build();
    }
    
    private String getMessage(String code, Locale locale) {
        try {
            return messageSource.getMessage(code, null, locale);
        } catch (NoSuchMessageException _) {
            log.warn("Çeviri bulunamadı: '{}'", code);
            return code;
        }
    }

    @Override
    public Map<String, Object> getPersonsDebugInfo() {
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            debugInfo.put("message", "QuestionGenerationService cache durumu kontrol ediliyor");
            
            if (questionGenerationService instanceof QuestionGenerationServiceImpl) {
                QuestionGenerationServiceImpl impl = (QuestionGenerationServiceImpl) questionGenerationService;
                impl.refreshActivePersonsCache();
                debugInfo.put("cacheRefreshStatus", "Cache yenilendi");
            }
            
            try {
                GameQuestionDTO testQuestion = questionGenerationService.generateQuestion(Difficulty.EASY, Collections.emptySet(), Locale.of("tr"));
                if (testQuestion != null) {
                    debugInfo.put("testQuestionGeneration", "BAŞARILI - Soru üretilebiliyor");
                    debugInfo.put("testQuestionId", testQuestion.getId());
                } else {
                    debugInfo.put("testQuestionGeneration", "BAŞARISIZ - Soru üretilemiyor");
                }
            } catch (Exception e) {
                debugInfo.put("testQuestionGeneration", "HATA: " + e.getMessage());
            }
            
        } catch (Exception e) {
            debugInfo.put("error", e.getMessage());
            log.error("Debug bilgisi alınırken hata: {}", e.getMessage(), e);
        }
        
        return debugInfo;
    }
}