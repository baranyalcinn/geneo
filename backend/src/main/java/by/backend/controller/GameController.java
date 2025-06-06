package by.backend.controller;

import by.backend.exception.GameException;
import by.backend.model.dto.GameAnswerDTO;
import by.backend.model.dto.InitialGameDataDTO;
import by.backend.model.dto.AnswerResponseDTO;
import by.backend.model.dto.RecordScoreRequestDTO;
import by.backend.model.dto.GameResultDTO;
import by.backend.model.dto.StartGameRequestDTO;
import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.enums.Difficulty;
import by.backend.service.game.GameService;
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

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/start")
    public ResponseEntity<Object> startGame(@RequestBody StartGameRequestDTO startGameRequest,
                                              @RequestParam(name = "lang", required = false) String lang) {
        try {
            Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.getDefault();
            if (startGameRequest == null || startGameRequest.getPlayerName() == null || startGameRequest.getPlayerName().trim().isEmpty() || startGameRequest.getDifficulty() == null) {
                logger.warn("POST /start: Geçersiz istek: {}", startGameRequest);
                Map<String, Object> errorBody = Map.of("message", "Oyuncu adı ve zorluk seviyesi gereklidir.");
                return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
            }
            logger.info("POST /start: Oyuncu: {}, Zorluk: {}, Dil: {}", startGameRequest.getPlayerName(), startGameRequest.getDifficulty(), locale.toLanguageTag());
            InitialGameDataDTO initialData = gameService.startGame(startGameRequest.getPlayerName(), startGameRequest.getDifficulty(), locale);
            return new ResponseEntity<>(initialData, HttpStatus.OK);
        } catch (GameException e) {
            logger.error("POST /start: Oyun başlatılırken özel hata: {}", e.getMessage());
            Map<String, Object> errorBody = Map.of("message", e.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("POST /start: Oyun başlatılırken beklenmeyen hata: {}", e.getMessage(), e);
            Map<String, Object> errorBody = Map.of("message", "Oyun başlatılırken bir hata oluştu: " + e.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/answer")
    public ResponseEntity<Object> answerQuestion(@RequestBody GameAnswerDTO gameAnswer,
                                                   @RequestParam(name = "lang", required = false) String lang) {
        try {
            Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.getDefault();
            if (gameAnswer == null || gameAnswer.getQuestionId() == null || gameAnswer.getAnswer() == null || gameAnswer.getDifficulty() == null) {
                logger.warn("POST /answer: Eksik bilgi ile çağrıldı: {}", gameAnswer);
                Map<String, Object> errorBody = Map.of("message", "Soru ID, cevap ve zorluk seviyesi gereklidir.");
                return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
            }
            logger.info("POST /answer: Oyuncu: {}, Soru ID: {}, Cevap: {}, Dil: {}", gameAnswer.getPlayerName(), gameAnswer.getQuestionId(), gameAnswer.getAnswer(), locale.toLanguageTag());
            AnswerResponseDTO answerResponse = gameService.answerQuestion(gameAnswer, locale);
            return new ResponseEntity<>(answerResponse, HttpStatus.OK);
        } catch (GameException e) {
            logger.error("POST /answer: Cevap işlenirken özel hata (Oyuncu: {}): {}", gameAnswer != null ? gameAnswer.getPlayerName() : "N/A", e.getMessage());
            Map<String, Object> errorBody = Map.of("message", e.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("POST /answer: Cevap işlenirken beklenmeyen hata (Oyuncu: {}): {}", gameAnswer != null ? gameAnswer.getPlayerName() : "N/A", e.getMessage(), e);
            Map<String, Object> errorBody = Map.of("message", "Cevap işlenirken bir hata oluştu: " + e.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/record-result")
    public ResponseEntity<Object> recordResult(@RequestBody RecordScoreRequestDTO scoreRequest,
                                                 @RequestParam(name = "lang", required = false) String lang) {
        try {
            if (scoreRequest == null) {
                logger.warn("POST /record-result: İstek gövdesi null");
                Map<String, Object> errorBody = Map.of("message", "Geçersiz istek: İstek gövdesi boş");
                return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
            }
            
            if (scoreRequest.getPlayerName() == null || scoreRequest.getPlayerName().trim().isEmpty()) {
                logger.warn("POST /record-result: Oyuncu adı eksik veya boş: {}", scoreRequest);
                Map<String, Object> errorBody = Map.of("message", "Oyuncu adı gereklidir");
                return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
            }
            
            Locale locale = lang != null ? Locale.forLanguageTag(lang) : Locale.getDefault();
            scoreRequest.setLocale(locale);
            
            logger.info("POST /record-result: Oyuncu: {}, Skor: {}, Zorluk: {}, Dil: {}", 
                        scoreRequest.getPlayerName(), 
                        scoreRequest.getScore(), 
                        scoreRequest.getDifficulty(),
                        locale.toLanguageTag());
            
            GameResultDTO savedResult = gameService.recordGameResult(scoreRequest);
            return new ResponseEntity<>(savedResult, HttpStatus.OK);
        } catch (GameException e) {
            logger.error("POST /record-result: Skor kaydedilirken özel hata (Oyuncu: {}): {}", scoreRequest != null ? scoreRequest.getPlayerName() : "N/A", e.getMessage());
            Map<String, Object> errorBody = Map.of("message", e.getMessage());
            return new ResponseEntity<>(errorBody, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("POST /record-result: Skor kaydedilirken beklenmeyen hata (Oyuncu: {}): {}", scoreRequest != null ? scoreRequest.getPlayerName() : "N/A", e.getMessage(), e);
            // Hata durumunda bile, frontend'in skorları gösterebilmesi için boş bir yanıt gönder
            Map<String, Object> errorBody = Map.of(
                "message", "Skor kaydedilirken bir hata oluştu: " + e.getMessage(),
                "success", false,
                "scoreRecorded", false,
                "result", (scoreRequest != null) ? scoreRequest : Map.of()
            );
            return new ResponseEntity<>(errorBody, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/highscores")
    public ResponseEntity<Object> getHighScores() {
        try {
            logger.info("GET /highscores: Yüksek skorlar getiriliyor...");
            // Önbelleğe alma stratejisi ile yüksek skorları al
            Map<Difficulty, List<GameResultDTO>> highScores = gameService.getHighScores();
            
            if (highScores == null || highScores.isEmpty()) {
                logger.warn("GET /highscores: Hiç yüksek skor bulunamadı veya null döndü");
                // Boş highscores objesi döndür
                Map<String, List<GameResultDTO>> emptyScores = new HashMap<>();
                for (Difficulty difficulty : Difficulty.values()) {
                    emptyScores.put(difficulty.name(), new ArrayList<>());
                }
                return new ResponseEntity<>(emptyScores, HttpStatus.OK);
            }
            
            logger.info("GET /highscores: {} farklı zorluk düzeyi için yüksek skorlar başarıyla alındı", highScores.size());
            return new ResponseEntity<>(highScores, HttpStatus.OK);
        } catch (GameException e) {
            logger.error("GET /highscores: Yüksek skorlar getirilirken özel hata: {}", e.getMessage());
            return new ResponseEntity<>(Map.of(), HttpStatus.OK); // Hata durumunda boş obje döndür
        } catch (Exception e) {
            logger.error("GET /highscores: Yüksek skorlar getirilirken beklenmeyen hata: {}", e.getMessage(), e);
            // Ön uç bir şekilde devam edebilsin diye boş bir highscores objesi döndür
            Map<String, List<GameResultDTO>> emptyScores = new HashMap<>();
            for (Difficulty difficulty : Difficulty.values()) {
                emptyScores.put(difficulty.name(), new ArrayList<>());
            }
            return new ResponseEntity<>(emptyScores, HttpStatus.OK);
        }
    }

    @GetMapping("/question")
    public ResponseEntity<GameQuestionDTO> getQuestionByDifficulty(@RequestParam(value = "difficulty", required = false) String difficultyStr) {
        logger.info("GET /question: Zorluk seviyesine göre soru isteniyor: {}", difficultyStr);
        try {
            Difficulty difficulty;
            if (difficultyStr == null || difficultyStr.isEmpty()) {
                // Varsayılan zorluk seviyesi
                difficulty = Difficulty.MEDIUM;
                logger.info("GET /question: Zorluk seviyesi belirtilmedi, varsayılan olarak {} kullanılıyor", difficulty);
            } else {
                difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
            }
            
            GameQuestionDTO question = gameService.generateQuestion(difficulty); 
            if (question != null) {
                logger.debug("GET /question: Soru oluşturuldu: {}", question.getId());
                return new ResponseEntity<>(question, HttpStatus.OK);
            } else {
                logger.warn("GET /question: Zorluk seviyesi {} için soru oluşturulamadı.", difficulty);
                return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
            }
        } catch (IllegalArgumentException e) {
            logger.error("GET /question: Geçersiz zorluk seviyesi parametresi: {}. Hata: {}", difficultyStr, e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        } catch (GameException e) {
            logger.error("GET /question: Soru oluşturulurken oyunla ilgili bir hata oluştu (Zorluk: {}): {}", difficultyStr, e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error("GET /question: Soru oluşturulurken beklenmedik bir hata oluştu (Zorluk: {}): {}", difficultyStr, e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 