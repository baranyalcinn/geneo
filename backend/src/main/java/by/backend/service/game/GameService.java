package by.backend.service.game;

import by.backend.model.dto.GameAnswerDTO;
import by.backend.model.dto.InitialGameDataDTO;
import by.backend.model.dto.AnswerResponseDTO;
import by.backend.model.dto.RecordScoreRequestDTO;
import by.backend.model.dto.GameResultDTO;
import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.dto.GameAnalysisDTO;
import by.backend.model.enums.Difficulty;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public interface GameService {
    InitialGameDataDTO startGame(String playerName, Difficulty difficulty, Locale locale);
    AnswerResponseDTO answerQuestion(GameAnswerDTO answerDetails, Locale locale);
    GameResultDTO recordGameResult(RecordScoreRequestDTO scoreDetails);
    Map<Difficulty, List<GameResultDTO>> getHighScores();
    GameQuestionDTO generateQuestion(Difficulty difficulty, Locale locale);
    
    // Yeni analiz metotları
    GameAnalysisDTO getGameAnalysis(String sessionId);
    GameAnalysisDTO endGame(String sessionId);
}