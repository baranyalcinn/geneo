package by.backend.controller;

import by.backend.exception.GameException;
import by.backend.model.dto.GameAnswerDTO;
import by.backend.model.dto.InitialGameDataDTO;
import by.backend.model.dto.AnswerResponseDTO;
import by.backend.model.dto.RecordScoreRequestDTO;
import by.backend.model.dto.GameResultDTO;
import by.backend.model.dto.StartGameRequestDTO;
import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.dto.GameAnalysisDTO;
import by.backend.model.enums.Difficulty;
import by.backend.service.game.GameService;
import by.backend.service.game.GameAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Locale;
import by.backend.model.dto.GameQuestionFeedbackDTO;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = {GameController.LOCALHOST_3000, GameController.LOCALHOST_5173}, allowCredentials = "true")
public class GameController {

    // Cross-origin URLs
    public static final String LOCALHOST_3000 = "http://localhost:3000";
    public static final String LOCALHOST_5173 = "http://localhost:5173";
    
    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    
    // Response keys
    private static final String MESSAGE_KEY = "message";
    private static final String SUCCESS_KEY = "success";
    private static final String SCORE_RECORDED_KEY = "scoreRecorded";
    private static final String RESULT_KEY = "result";
    
    // Error messages
    private static final String INVALID_REQUEST_MSG = "Geçersiz istek: İstek gövdesi boş";

    private static final String PLAYER_NAME_REQUIRED_MSG = "Oyuncu adı gereklidir";
    private static final String QUESTION_ID_REQUIRED_MSG = "Soru ID gereklidir.";
    private static final String DIFFICULTY_REQUIRED_MSG = "Zorluk seviyesi gereklidir.";
    private static final String REQUEST_BODY_REQUIRED_MSG = "Request body gereklidir.";
    private static final String PLAYER_NAME_DIFFICULTY_REQUIRED_MSG = "Oyuncu adı ve zorluk seviyesi gereklidir.";

    private static final String SCORE_RECORD_ERROR_PREFIX = "Skor kaydedilirken bir hata oluştu: ";
    private static final String ANSWER_PROCESS_ERROR_PREFIX = "Cevap işlenirken bir hata oluştu: ";
    private static final String GAME_START_ERROR_PREFIX = "Oyun başlatılırken bir hata oluştu: ";
    static final String UNEXPECTED_ERROR_MSG = "Beklenmeyen bir hata oluştu";
    
    // Log message prefixes
    private static final String LOG_START_PREFIX = "POST /start: ";
    private static final String LOG_ANSWER_PREFIX = "POST /answer: ";
    private static final String LOG_RECORD_PREFIX = "POST /record-result: ";
    private static final String LOG_HIGHSCORES_PREFIX = "GET /highscores: ";
    private static final String LOG_QUESTION_PREFIX = "GET /question: ";

    private static final String LOG_FEEDBACK_PREFIX = "POST /feedback: ";
    
    // Default values
    private static final String ANONYMOUS_PLAYER = "Anonymous";
    private static final String EMPTY_STRING = "";
    private static final String NOT_AVAILABLE = "N/A";
    
    private final GameService gameService;
    final GameAnalysisService gameAnalysisService;

    public GameController(GameService gameService, GameAnalysisService gameAnalysisService) {
        this.gameService = gameService;
        this.gameAnalysisService = gameAnalysisService;
    }

    @PostMapping("/start")
    public ResponseEntity<Object> startGame(@RequestBody StartGameRequestDTO startGameRequest,
                                              @RequestParam(name = "lang", required = false) String lang) {
        String operationName = "Oyun başlatma";
        try {
            return processStartGameRequest(startGameRequest, lang);
        } catch (GameException e) {
            return handleGameException(e, operationName, LOG_START_PREFIX);
        } catch (Exception e) {
            return handleGenericException(e, operationName, LOG_START_PREFIX, GAME_START_ERROR_PREFIX);
        }
    }

    @PostMapping("/answer")
    public ResponseEntity<Object> answerQuestion(@RequestBody GameAnswerDTO gameAnswer,
                                                   @RequestParam(name = "lang", required = false) String lang) {
        String operationName = "Cevap işleme";
        try {
            return processAnswerRequest(gameAnswer, lang);
        } catch (GameException e) {
            String playerName = getPlayerNameSafely(gameAnswer);
            logger.error("{}{} özel hata (Oyuncu: {}): {}", LOG_ANSWER_PREFIX, operationName, playerName, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            String playerName = getPlayerNameSafely(gameAnswer);
            logger.error("{}{} beklenmeyen hata (Oyuncu: {}): {}", LOG_ANSWER_PREFIX, operationName, playerName, e.getMessage(), e);
            return createErrorResponse(ANSWER_PROCESS_ERROR_PREFIX + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/record-score")
    public ResponseEntity<Object> recordScore(@RequestBody RecordScoreRequestDTO scoreRequest,
                                                 @RequestParam(name = "lang", required = false) String lang) {
        String operationName = "Skor kaydetme";
        try {
            return processScoreRequest(scoreRequest, lang);
        } catch (GameException e) {
            String playerName = getPlayerNameSafely(scoreRequest);
            logger.error("{}{} özel hata (Oyuncu: {}): {}", LOG_RECORD_PREFIX, operationName, playerName, e.getMessage());
            return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            String playerName = getPlayerNameSafely(scoreRequest);
            logger.error("{}{} beklenmeyen hata (Oyuncu: {}): {}", LOG_RECORD_PREFIX, operationName, playerName, e.getMessage(), e);
            return createScoreErrorResponse(e.getMessage(), scoreRequest);
        }
    }

    @GetMapping("/highscores")
    public ResponseEntity<Object> getHighScores() {
        try {
            return processHighScoresRequest();
        } catch (GameException e) {
            logger.error("{}Yüksek skorlar getirilirken özel hata: {}", LOG_HIGHSCORES_PREFIX, e.getMessage());
            return ResponseEntity.ok(Map.of());
        } catch (Exception e) {
            logger.error("{}Yüksek skorlar getirilirken beklenmeyen hata: {}", LOG_HIGHSCORES_PREFIX, e.getMessage(), e);
            return ResponseEntity.ok(createEmptyHighScores());
        }
    }

    @GetMapping("/question")
    public ResponseEntity<GameQuestionDTO> getQuestionByDifficulty(
            @RequestParam(value = "difficulty", required = false) String difficultyStr,
            @RequestParam(name = "lang", required = false) String lang) {
        
        logger.info("{}Zorluk seviyesine göre soru isteniyor: {}, Dil: {}", LOG_QUESTION_PREFIX, difficultyStr, lang);
        
        try {
            return processQuestionRequest(difficultyStr, lang);
        } catch (GameException e) {
            logger.error("{}Soru oluşturulurken oyunla ilgili bir hata oluştu (Zorluk: {}): {}", 
                        LOG_QUESTION_PREFIX, difficultyStr, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("{}Soru oluşturulurken beklenmedik bir hata oluştu (Zorluk: {}): {}", 
                        LOG_QUESTION_PREFIX, difficultyStr, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    

    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@Valid @RequestBody GameQuestionFeedbackDTO feedbackDTO) {
        logger.info("{}Geri bildirim alındı. Soru ID: {}, Feedback: {}", 
                    LOG_FEEDBACK_PREFIX, feedbackDTO.getQuestionId(), feedbackDTO.getFeedback());
        try {
            gameService.saveFeedback(feedbackDTO);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            logger.warn("{}Geçersiz geri bildirim: {}", LOG_FEEDBACK_PREFIX, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("{}Geri bildirim kaydedilirken hata oluştu", LOG_FEEDBACK_PREFIX, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/debug/persons")
    public ResponseEntity<Object> getPersonsDebugInfo() {
        try {
            Map<String, Object> debugInfo = gameService.getPersonsDebugInfo();
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            logger.error("Debug bilgileri alınırken hata oluştu: {}", e.getMessage(), e);
            return createErrorResponse("Debug bilgileri alınamadı: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Request processing methods
    private ResponseEntity<Object> processStartGameRequest(StartGameRequestDTO startGameRequest, String lang) {
        Locale locale = parseLocale(lang);
        
        if (!isValidStartGameRequest(startGameRequest)) {
            logger.warn("{}Geçersiz istek: {}", LOG_START_PREFIX, startGameRequest);
            return createErrorResponse(PLAYER_NAME_DIFFICULTY_REQUIRED_MSG, HttpStatus.BAD_REQUEST);
        }
        
        if (logger.isInfoEnabled()) {
            logger.info("{}Oyuncu: {}, Zorluk: {}, Dil: {}", LOG_START_PREFIX,
                       startGameRequest.getPlayerName(), startGameRequest.getDifficulty(), locale.toLanguageTag());
        }
        
        InitialGameDataDTO initialData = gameService.startGame(
            startGameRequest.getPlayerName(), 
            startGameRequest.getDifficulty(), 
            locale
        );
        return ResponseEntity.ok(initialData);
    }

    private ResponseEntity<Object> processAnswerRequest(GameAnswerDTO gameAnswer, String lang) {
        Locale locale = parseLocale(lang);
        
        ResponseEntity<Object> validationError = validateGameAnswer(gameAnswer);
        if (validationError != null) {
            return validationError;
        }
        
        sanitizeGameAnswer(gameAnswer);
        
        if (logger.isInfoEnabled()) {
            logger.info("{}Oyuncu: {}, Soru ID: {}, Cevap: {}, Dil: {}", LOG_ANSWER_PREFIX,
                       gameAnswer.getPlayerName(), gameAnswer.getQuestionId(), gameAnswer.getAnswer(), 
                       locale.toLanguageTag());
        }
        
        AnswerResponseDTO answerResponse = gameService.answerQuestion(gameAnswer, locale);
        return ResponseEntity.ok(answerResponse);
    }

    private ResponseEntity<Object> processScoreRequest(RecordScoreRequestDTO scoreRequest, String lang) {
        ResponseEntity<Object> validationError = validateScoreRequest(scoreRequest);
        if (validationError != null) {
            return validationError;
        }
        
        Locale locale = parseLocale(lang);
        scoreRequest.setLocale(locale);
        
        if (logger.isInfoEnabled()) {
            logger.info("{}Oyuncu: {}, Skor: {}, Zorluk: {}, Dil: {}", LOG_RECORD_PREFIX,
                        scoreRequest.getPlayerName(), 
                        scoreRequest.getScore(), 
                        scoreRequest.getDifficulty(),
                        locale.toLanguageTag());
        }
        
        GameResultDTO savedResult = gameService.recordGameResult(scoreRequest);
        return ResponseEntity.ok(savedResult);
    }

    private ResponseEntity<Object> processHighScoresRequest() {
        logger.info("{}Yüksek skorlar getiriliyor...", LOG_HIGHSCORES_PREFIX);
        Map<Difficulty, List<GameResultDTO>> highScores = gameService.getHighScores();
        
        if (isHighScoresEmpty(highScores)) {
            logger.warn("{}Hiç yüksek skor bulunamadı veya null döndü", LOG_HIGHSCORES_PREFIX);
            return ResponseEntity.ok(createEmptyHighScores());
        }
        
        logger.info("{}{} farklı zorluk düzeyi için yüksek skorlar başarıyla alındı", LOG_HIGHSCORES_PREFIX, highScores.size());
        return ResponseEntity.ok(highScores);
    }

    private ResponseEntity<GameQuestionDTO> processQuestionRequest(String difficultyStr, String lang) {
        Locale locale = parseLocale(lang);
        Difficulty difficulty = parseDifficulty(difficultyStr);

        GameQuestionDTO question = gameService.generateQuestion(difficulty, locale);
        if (question != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}Soru oluşturuldu: {}", LOG_QUESTION_PREFIX, question.getId());
            }
            return ResponseEntity.ok(question);
        } else {
            logger.warn("{}Zorluk seviyesi {} için soru oluşturulamadı.", LOG_QUESTION_PREFIX, difficulty);
            return ResponseEntity.notFound().build();
        }
    }



    // Exception handling methods
    private ResponseEntity<Object> handleGameException(GameException e, String operationName, String logPrefix) {
        logger.error("{}{} oyunla ilgili hata: {}", logPrefix, operationName, e.getMessage());
        return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> handleGenericException(Exception e, String operationName, String logPrefix, String errorMessage) {
        logger.error("{}{} beklenmeyen hata: {}", logPrefix, operationName, e.getMessage(), e);
        return createErrorResponse(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Utility methods
    private Locale parseLocale(String lang) {
        return lang != null ? Locale.forLanguageTag(lang) : Locale.getDefault();
    }
    
    private boolean isValidStartGameRequest(StartGameRequestDTO request) {
        return request != null 
                && request.getPlayerName() != null 
                && !request.getPlayerName().trim().isEmpty() 
                && request.getDifficulty() != null;
    }
    
    private ResponseEntity<Object> validateGameAnswer(GameAnswerDTO gameAnswer) {
        if (gameAnswer == null) {
            return createErrorResponse(INVALID_REQUEST_MSG, HttpStatus.BAD_REQUEST);
        }
        
        if (isInvalidString(gameAnswer.getQuestionId())) {
            return createErrorResponse(QUESTION_ID_REQUIRED_MSG, HttpStatus.BAD_REQUEST);
        }
        
        if (gameAnswer.getDifficulty() == null) {
            return createErrorResponse(DIFFICULTY_REQUIRED_MSG, HttpStatus.BAD_REQUEST);
        }
        
        if (isInvalidString(gameAnswer.getPlayerName())) {
            return createErrorResponse(PLAYER_NAME_REQUIRED_MSG, HttpStatus.BAD_REQUEST);
        }
        
        return null; // No validation errors
    }
    
    private void sanitizeGameAnswer(GameAnswerDTO gameAnswer) {
        if (gameAnswer.getAnswer() == null) {
            logger.warn("{}Answer is null, setting empty string: {}", LOG_ANSWER_PREFIX, gameAnswer);
            gameAnswer.setAnswer(EMPTY_STRING);
        }
        
        if (isInvalidString(gameAnswer.getPlayerName())) {
            logger.warn("{}Player name eksik, setting default: {}", LOG_ANSWER_PREFIX, gameAnswer);
            gameAnswer.setPlayerName(ANONYMOUS_PLAYER);
        }
    }
    
    private ResponseEntity<Object> validateScoreRequest(RecordScoreRequestDTO scoreRequest) {
        if (scoreRequest == null) {
            logger.warn("{}İstek gövdesi null", LOG_RECORD_PREFIX);
            return createErrorResponse(INVALID_REQUEST_MSG, HttpStatus.BAD_REQUEST);
        }
        
        if (isInvalidString(scoreRequest.getPlayerName())) {
            logger.warn("{}Oyuncu adı eksik veya boş: {}", LOG_RECORD_PREFIX, scoreRequest);
            return createErrorResponse(PLAYER_NAME_REQUIRED_MSG, HttpStatus.BAD_REQUEST);
        }
        
        return null;
    }
    
    private boolean isHighScoresEmpty(Map<Difficulty, List<GameResultDTO>> highScores) {
        return highScores == null || highScores.isEmpty();
    }
    
    private Map<String, List<GameResultDTO>> createEmptyHighScores() {
        Map<String, List<GameResultDTO>> emptyScores = new HashMap<>();
        for (Difficulty difficulty : Difficulty.values()) {
            emptyScores.put(difficulty.name(), new ArrayList<>());
        }
        return emptyScores;
    }
    
    private Difficulty parseDifficulty(String difficultyStr) {
        if (isInvalidString(difficultyStr)) {
            logger.info("{}Zorluk seviyesi belirtilmedi, varsayılan olarak {} kullanılıyor", LOG_QUESTION_PREFIX, Difficulty.MEDIUM);
            return Difficulty.MEDIUM;
        }
        
        try {
            return Difficulty.valueOf(difficultyStr.toUpperCase());
        } catch (IllegalArgumentException _) {
            logger.warn("{}Geçersiz zorluk seviyesi: {}, varsayılan {} kullanılıyor", LOG_QUESTION_PREFIX, difficultyStr, Difficulty.MEDIUM);
            return Difficulty.MEDIUM;
        }
    }
    

    
    private boolean isInvalidString(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    private String getPlayerNameSafely(Object request) {
        if (request instanceof GameAnswerDTO gameAnswerDto) {
            return gameAnswerDto.getPlayerName();
        } else if (request instanceof RecordScoreRequestDTO recordScoreRequestDto) {
            return recordScoreRequestDto.getPlayerName();
        }
        return NOT_AVAILABLE;
    }
    
    private ResponseEntity<Object> createErrorResponse(String message, HttpStatus status) {
        Map<String, Object> errorBody = Map.of(MESSAGE_KEY, message);
        return ResponseEntity.status(status).body(errorBody);
    }
    
    private ResponseEntity<Object> createScoreErrorResponse(String errorMessage, RecordScoreRequestDTO scoreRequest) {
        Map<String, Object> errorBody = Map.of(
            MESSAGE_KEY, SCORE_RECORD_ERROR_PREFIX + errorMessage,
            SUCCESS_KEY, false,
            SCORE_RECORDED_KEY, false,
            RESULT_KEY, scoreRequest != null ? scoreRequest : Map.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody);
    }
} 