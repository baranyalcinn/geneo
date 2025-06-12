package by.backend.service.game;

import by.backend.config.GameProperties;
import by.backend.exception.GameException;
import by.backend.model.dto.*;
import by.backend.model.entity.GameQuestionFeedback;
import by.backend.model.entity.HighScore;
import by.backend.model.enums.Difficulty;
import by.backend.repository.GameQuestionFeedbackRepository;
import by.backend.repository.HighScoreRepository;
import by.backend.service.game.session.GameSession;
import by.backend.service.game.session.GameSessionManager;
import by.backend.service.game.session.PlayerAnswer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameServiceImpl implements GameService {

    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);
    
    // Magic numbers converted to constants
    private static final int CANDIDATE_QUESTIONS_COUNT = 5;
    private static final int EASY_BASE_POINTS = 10;
    private static final int MEDIUM_BASE_POINTS = 15;
    private static final int HARD_BASE_POINTS = 25;
    private static final double MIN_TIME_MULTIPLIER = 0.5;
    private static final double BASE_TIME_MULTIPLIER = 1.0;
    private static final double TIME_LIMIT_FACTOR = 2.0;
    private static final int STREAK_DIVISOR = 2;
    private static final int EASY_STREAK_BONUS = 2;
    private static final int MEDIUM_STREAK_BONUS = 3;
    private static final int HARD_STREAK_BONUS = 5;
    private static final double MIN_TIME_TAKEN = 1.0;
    
    // Error message keys
    private static final String ERROR_SESSION_NOT_FOUND = "game.error.session_not_found";
    private static final String ERROR_START_FAILED_NO_QUESTION = "game.error.start_failed_no_question";
    private static final String ERROR_GENERATE_QUESTION_FAILED = "game.error.generate_question_failed";
    private static final String SESSION_NOT_FOUND_MSG = "Oyun oturumu bulunamadı: ";
    private static final String FEEDBACK_SAVE_ERROR_MSG = "Feedback could not be saved due to an internal error.";
    
    private static final Random random = new Random();

    private final HighScoreRepository highScoreRepository;
    private final MessageSource messageSource;
    private final GameProperties gameProperties;
    private final GameAnalysisService gameAnalysisService;
    private final GameSessionManager gameSessionManager;
    private final QuestionGenerationService questionGenerationService;
    private final AdaptiveDifficultyService adaptiveDifficultyService;
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
             throw new GameException(getMessage(ERROR_START_FAILED_NO_QUESTION, locale));
        }

        Difficulty actualDifficulty = firstQuestion.getDifficulty();
        log.info("Oyun {} zorluğunda başlatıldı (istenen: {})", actualDifficulty, difficulty);

        String sessionId = UUID.randomUUID().toString();
        long gameDurationInSeconds = gameProperties.getGameDuration(actualDifficulty);
        int totalQuestions = gameProperties.getQuestionsPerGame();

        GameSession newSession = new GameSession(sessionId, playerName, actualDifficulty, gameDurationInSeconds, totalQuestions, new ArrayList<>());
        newSession.getAskedQuestionSignatures().add(firstQuestion.getId());
        gameSessionManager.addSession(newSession);

        log.info("New game session created with ID '{}' for player '{}', first question: {}", sessionId, playerName, firstQuestion.getId());
        
        firstQuestion.setCorrectAnswer(null);
        // İlişki yolunu oyun başlangıcında da koruyalım
        // firstQuestion.setRelationshipPath(null); // Bu satırı kaldırdık

        return InitialGameDataDTO.builder()
                .sessionId(sessionId)
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
        log.info("Processing answer for session '{}', question '{}'", answerDetails.getSessionId(), answerDetails.getQuestionId());

        GameSession session = gameSessionManager.getSession(answerDetails.getSessionId());
        if (session == null || !session.isActive()) {
            throw new GameException(getMessage(ERROR_SESSION_NOT_FOUND, locale));
        }

        AnswerValidationResult validationResult = answerValidatorService.validateAnswer(answerDetails.getQuestionId(), answerDetails.getAnswer(), locale);

        PlayerAnswer playerAnswer = new PlayerAnswer(
                answerDetails.getQuestionId(),
                validationResult.getCategory(),
                answerDetails.getAnswer(),
                validationResult.isCorrect(),
                answerDetails.getTimeTakenInSeconds()
        );
        adaptiveDifficultyService.recordPlayerAnswer(session.getPlayerName(), playerAnswer, (long) (answerDetails.getTimeTakenInSeconds() * 1000));

        int pointsEarned = 0;
        if (validationResult.isCorrect()) {
            pointsEarned = calculatePoints(session.getDifficulty(), answerDetails.getTimeTakenInSeconds(), session.getCurrentStreak().get() + 1);
        }
        session.recordAnswer(playerAnswer, pointsEarned);

        boolean gameOver = session.isGameOver();
        GameQuestionDTO nextQuestion = gameOver ? null : generateNextQuestion(session, locale);
        
        if (nextQuestion == null && !gameOver) {
            gameOver = true;
        }
        
        if (gameOver) {
            session.setActive(false);
            gameSessionManager.removeSession(session.getSessionId());
            log.info("Game over for session '{}'. Final score: {}", session.getSessionId(), session.getScore().get());
        }

        return AnswerResponseDTO.builder()
                .correctAnswer(validationResult.isCorrect())
                .correctAnswerText(validationResult.getCorrectAnswerText())
                .pointsEarned(pointsEarned)
                .updatedScore(session.getScore().get())
                .updatedStreak(session.getCurrentStreak().get())
                .nextQuestion(nextQuestion)
                .gameOver(gameOver)
                .relationshipPath(validationResult.isCorrect() ? validationResult.getRelationshipPath() : null)
                .build();
    }

    @Override
    @Transactional
    public GameResultDTO recordGameResult(RecordScoreRequestDTO scoreDetails) {
        log.info("Oyun sonucu kaydediliyor: Oyuncu '{}', Skor '{}', Zorluk '{}'",
                scoreDetails.getPlayerName(), scoreDetails.getScore(), scoreDetails.getDifficulty());

        HighScore existingHighScore = highScoreRepository.findTopByPlayerNameAndDifficultyOrderByScoreDesc(scoreDetails.getPlayerName(), scoreDetails.getDifficulty());
        
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
            return convertToGameResultDTO(savedScore);
        }
        
        return GameResultDTO.builder()
            .playerName(scoreDetails.getPlayerName())
            .score(scoreDetails.getScore())
            .difficulty(scoreDetails.getDifficulty())
            .date(LocalDate.now())
            .correctAnswers(scoreDetails.getCorrectAnswers())
            .totalQuestions(scoreDetails.getTotalQuestions())
            .maxStreak(scoreDetails.getMaxStreak())
            .build();
    }
    
    @Override
    public Map<Difficulty, List<GameResultDTO>> getHighScores() {
        log.info("Tüm zorluk seviyeleri için yüksek skorlar isteniyor");
        return Arrays.stream(Difficulty.values())
                .collect(Collectors.toMap(
                        difficulty -> difficulty,
                        difficulty -> highScoreRepository.findTop10ByDifficultyOrderByScoreDesc(difficulty)
                                .stream()
                                .map(this::convertToGameResultDTO)
                                .toList(),
                        (a, b) -> b,
                        () -> new EnumMap<>(Difficulty.class)
                ));
    }

    @Override
    public GameQuestionDTO generateQuestion(Difficulty difficulty, Locale locale) {
        GameQuestionDTO question = questionGenerationService.generateQuestion(difficulty, Collections.emptySet(), locale);
        if (question == null) {
            throw new GameException(getMessage(ERROR_GENERATE_QUESTION_FAILED, locale));
        }
        question.setCorrectAnswer(null);
        // İlişki yolunu manuel soru üretiminde de koruyalım
        // question.setRelationshipPath(null); // Bu satırı kaldırdık
        return question;
    }
    
    @Override
    public GameAnalysisDTO getGameAnalysis(String sessionId) {
        GameSession session = gameSessionManager.getSession(sessionId);
        if (session == null) {
            throw new GameException(SESSION_NOT_FOUND_MSG + sessionId);
        }
        return gameAnalysisService.analyzeGameSession(session);
    }
    
    @Override
    public GameAnalysisDTO endGame(String sessionId) {
        GameSession session = gameSessionManager.getSession(sessionId);
        if (session == null) {
            throw new GameException(SESSION_NOT_FOUND_MSG + sessionId);
        }
        session.setActive(false);
        GameAnalysisDTO finalAnalysis = gameAnalysisService.analyzeGameSession(session);
        gameSessionManager.removeSession(sessionId);
        log.info("Oyun bitirildi ve analiz oluşturuldu, Session: {}", sessionId);
        return finalAnalysis;
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
    
    private int calculatePoints(Difficulty difficulty, double timeTakenInSeconds, int streak) {
        if (timeTakenInSeconds <= 0) timeTakenInSeconds = MIN_TIME_TAKEN;
        int basePoints = switch (difficulty) {
            case EASY -> EASY_BASE_POINTS;
            case MEDIUM -> MEDIUM_BASE_POINTS;
            case HARD -> HARD_BASE_POINTS;
        };
        double timeMultiplier = Math.max(MIN_TIME_MULTIPLIER, BASE_TIME_MULTIPLIER - (timeTakenInSeconds / (gameProperties.getTimeLimit(difficulty) * TIME_LIMIT_FACTOR)));
        int streakBonusMultiplier = switch (difficulty) {
            case HARD -> HARD_STREAK_BONUS;
            case MEDIUM -> MEDIUM_STREAK_BONUS;
            case EASY -> EASY_STREAK_BONUS;
        };
        int streakBonus = (streak / STREAK_DIVISOR) * streakBonusMultiplier;
        return (int) (basePoints * timeMultiplier) + streakBonus;
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
    
    private GameQuestionDTO generateNextQuestion(GameSession session, Locale locale) {
        adjustDifficultyIfNeeded(session);
        
        List<GameQuestionDTO> candidates = questionGenerationService.generateCandidateQuestions(
                session.getDifficulty(), CANDIDATE_QUESTIONS_COUNT, session.getAskedQuestionSignatures(), locale
        );

        if (!candidates.isEmpty()) {
            return selectFromCandidates(candidates, session);
        }
        
        return handleNoCandidatesScenario(session, locale);
    }
    
    private void adjustDifficultyIfNeeded(GameSession session) {
        Difficulty newDifficulty = adaptiveDifficultyService.adjustDifficultyDuringGame(session);
        if (newDifficulty != session.getDifficulty()) {
            log.info("Difficulty for session {} adjusted from {} to {}", session.getSessionId(), session.getDifficulty(), newDifficulty);
            session.setDifficulty(newDifficulty);
        }
    }
    
    private GameQuestionDTO selectFromCandidates(List<GameQuestionDTO> candidates, GameSession session) {
        GameQuestionDTO nextQuestion = adaptiveDifficultyService.selectOptimalQuestion(candidates, session.getPlayerName(), session);
        if (nextQuestion == null) {
            nextQuestion = candidates.get(random.nextInt(candidates.size()));
        }
        session.getAskedQuestionSignatures().add(nextQuestion.getId());
        nextQuestion.setCorrectAnswer(null);
        return nextQuestion;
    }
    
    private GameQuestionDTO handleNoCandidatesScenario(GameSession session, Locale locale) {
        log.warn("Mevcut zorlukta soru üretilemedi, alternatif yöntemler deneniyor. Session: {}", session.getSessionId());
        
        GameQuestionDTO nextQuestion = tryDifferentDifficulties(session, locale);
        if (nextQuestion != null) {
            return nextQuestion;
        }
        
        return tryResetQuestionHistory(session, locale);
    }
    
    private GameQuestionDTO tryDifferentDifficulties(GameSession session, Locale locale) {
        GameQuestionDTO nextQuestion = findQuestionInOtherDifficulties(session.getDifficulty(), locale);
        if (nextQuestion != null) {
            log.info("Farklı zorlukta soru bulundu: {} -> {}", session.getDifficulty(), nextQuestion.getDifficulty());
            session.getAskedQuestionSignatures().add(nextQuestion.getId());
            nextQuestion.setCorrectAnswer(null);
        }
        return nextQuestion;
    }
    
    private GameQuestionDTO tryResetQuestionHistory(GameSession session, Locale locale) {
        log.warn("Hiçbir zorlukta soru bulunamadı, soru geçmişi sıfırlanıyor. Session: {}", session.getSessionId());
        session.getAskedQuestionSignatures().clear();
        
        GameQuestionDTO nextQuestion = questionGenerationService.generateQuestion(session.getDifficulty(), Collections.emptySet(), locale);
        if (nextQuestion != null) {
            log.info("Soru geçmişi temizlendikten sonra yeni soru üretildi: {}", nextQuestion.getId());
            session.getAskedQuestionSignatures().add(nextQuestion.getId());
            nextQuestion.setCorrectAnswer(null);
        } else {
            log.error("Hiçbir fallback yöntemi işe yaramadı, oyun bitiriliyor. Session: {}", session.getSessionId());
        }
        return nextQuestion;
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
            // QuestionGenerationService'den cache bilgilerini al
            debugInfo.put("message", "QuestionGenerationService cache durumu kontrol ediliyor");
            
            // Cache'i manuel olarak yenilemek için service'den yardım al
            if (questionGenerationService instanceof QuestionGenerationServiceImpl) {
                QuestionGenerationServiceImpl impl = (QuestionGenerationServiceImpl) questionGenerationService;
                impl.refreshActivePersonsCache();
                debugInfo.put("cacheRefreshStatus", "Cache yenilendi");
            }
            
            // Test soru üretmeyi dene
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