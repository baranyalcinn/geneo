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
    public ResponseEntity<?> startGame(@RequestBody StartGameRequestDTO startGameRequest) {
        try {
            if (startGameRequest == null || startGameRequest.getPlayerName() == null || startGameRequest.getPlayerName().trim().isEmpty() || startGameRequest.getDifficulty() == null) {
                logger.warn("POST /start: Geçersiz istek: {}", startGameRequest);
                return ResponseEntity.badRequest().body(Map.of("message", "Oyuncu adı ve zorluk seviyesi gereklidir."));
            }
            logger.info("POST /start: Oyuncu: {}, Zorluk: {}", startGameRequest.getPlayerName(), startGameRequest.getDifficulty());
            InitialGameDataDTO initialData = gameService.startGame(startGameRequest.getPlayerName(), startGameRequest.getDifficulty());
            return ResponseEntity.ok(initialData);
        } catch (GameException e) {
            logger.error("POST /start: Oyun başlatılırken özel hata: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("POST /start: Oyun başlatılırken beklenmeyen hata: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Oyun başlatılırken bir hata oluştu: " + e.getMessage()));
        }
    }

    @PostMapping("/answer")
    public ResponseEntity<?> answerQuestion(@RequestBody GameAnswerDTO gameAnswer) {
        try {
            if (gameAnswer == null || gameAnswer.getQuestionId() == null || gameAnswer.getAnswer() == null || gameAnswer.getDifficulty() == null) {
                logger.warn("POST /answer: Eksik bilgi ile çağrıldı: {}", gameAnswer);
                return ResponseEntity.badRequest().body(Map.of("message", "Soru ID, cevap ve zorluk seviyesi gereklidir."));
            }
            logger.info("POST /answer: Oyuncu: {}, Soru ID: {}, Cevap: {}", gameAnswer.getPlayerName(), gameAnswer.getQuestionId(), gameAnswer.getAnswer());
            AnswerResponseDTO answerResponse = gameService.answerQuestion(gameAnswer);
            return ResponseEntity.ok(answerResponse);
        } catch (GameException e) {
            logger.error("POST /answer: Cevap işlenirken özel hata (Oyuncu: {}): {}", gameAnswer != null ? gameAnswer.getPlayerName() : "N/A", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("POST /answer: Cevap işlenirken beklenmeyen hata (Oyuncu: {}): {}", gameAnswer != null ? gameAnswer.getPlayerName() : "N/A", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Cevap işlenirken bir hata oluştu: " + e.getMessage()));
        }
    }

    @PostMapping("/record-result")
    public ResponseEntity<?> recordResult(@RequestBody RecordScoreRequestDTO scoreRequest) {
        try {
            if (scoreRequest == null) {
                logger.warn("POST /record-result: İstek gövdesi null");
                return ResponseEntity.badRequest().body(Map.of("message", "Geçersiz istek: İstek gövdesi boş"));
            }
            
            if (scoreRequest.getPlayerName() == null || scoreRequest.getPlayerName().trim().isEmpty()) {
                logger.warn("POST /record-result: Oyuncu adı eksik veya boş: {}", scoreRequest);
                return ResponseEntity.badRequest().body(Map.of("message", "Oyuncu adı gereklidir"));
            }
            
            logger.info("POST /record-result: Oyuncu: {}, Skor: {}, Zorluk: {}, Doğru Cevaplar: {}/{}", 
                        scoreRequest.getPlayerName(), 
                        scoreRequest.getScore(), 
                        scoreRequest.getDifficulty(),
                        scoreRequest.getCorrectAnswers(),
                        scoreRequest.getTotalQuestions());
            
            GameResultDTO savedResult = gameService.recordGameResult(scoreRequest);
            return ResponseEntity.ok(savedResult);
        } catch (GameException e) {
            logger.error("POST /record-result: Skor kaydedilirken özel hata (Oyuncu: {}): {}", scoreRequest != null ? scoreRequest.getPlayerName() : "N/A", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("POST /record-result: Skor kaydedilirken beklenmeyen hata (Oyuncu: {}): {}", scoreRequest != null ? scoreRequest.getPlayerName() : "N/A", e.getMessage(), e);
            // Hata durumunda bile, frontend'in skorları gösterebilmesi için boş bir yanıt gönder
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "message", "Skor kaydedilirken bir hata oluştu: " + e.getMessage(),
                    "success", false,
                    "scoreRecorded", false,
                    "result", (scoreRequest != null) ? scoreRequest : Map.of()
                ));
        }
    }

    @GetMapping("/highscores")
    public ResponseEntity<?> getHighScores() {
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
                return ResponseEntity.ok(emptyScores);
            }
            
            logger.info("GET /highscores: {} farklı zorluk düzeyi için yüksek skorlar başarıyla alındı", highScores.size());
            return ResponseEntity.ok(highScores);
        } catch (GameException e) {
            logger.error("GET /highscores: Yüksek skorlar getirilirken özel hata: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.OK).body(Map.of()); // Hata durumunda boş obje döndür
        } catch (Exception e) {
            logger.error("GET /highscores: Yüksek skorlar getirilirken beklenmeyen hata: {}", e.getMessage(), e);
            // Ön uç bir şekilde devam edebilsin diye boş bir highscores objesi döndür
            Map<String, List<GameResultDTO>> emptyScores = new HashMap<>();
            for (Difficulty difficulty : Difficulty.values()) {
                emptyScores.put(difficulty.name(), new ArrayList<>());
            }
            return ResponseEntity.status(HttpStatus.OK).body(emptyScores);
        }
    }

    @GetMapping("/question")
    public ResponseEntity<GameQuestionDTO> getQuestionByDifficulty(@RequestParam("difficulty") String difficultyStr) {
        logger.info("GET /question: Zorluk seviyesine göre soru isteniyor: {}", difficultyStr);
        try {
            Difficulty difficulty = Difficulty.valueOf(difficultyStr.toUpperCase());
            GameQuestionDTO question = gameService.generateQuestion(difficulty); 
            if (question != null) {
                logger.debug("GET /question: Soru oluşturuldu: {}", question.getId());
                return ResponseEntity.ok(question);
            } else {
                logger.warn("GET /question: Zorluk seviyesi {} için soru oluşturulamadı.", difficulty);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); 
            }
        } catch (IllegalArgumentException e) {
            logger.error("GET /question: Geçersiz zorluk seviyesi parametresi: {}. Hata: {}", difficultyStr, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (GameException e) {
            logger.error("GET /question: Soru oluşturulurken oyunla ilgili bir hata oluştu (Zorluk: {}): {}", difficultyStr, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("GET /question: Soru oluşturulurken beklenmedik bir hata oluştu (Zorluk: {}): {}", difficultyStr, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 