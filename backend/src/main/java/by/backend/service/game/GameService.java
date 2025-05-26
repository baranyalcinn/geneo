package by.backend.service.game;

import by.backend.model.dto.GameAnswerDTO;
import by.backend.model.dto.InitialGameDataDTO;
import by.backend.model.dto.AnswerResponseDTO;
import by.backend.model.dto.RecordScoreRequestDTO;
import by.backend.model.dto.GameResultDTO;
import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.enums.Difficulty;
import java.util.List;
import java.util.Map;

public interface GameService {
    InitialGameDataDTO startGame(String playerName, Difficulty difficulty);
    AnswerResponseDTO answerQuestion(GameAnswerDTO answerDetails);
    GameResultDTO recordGameResult(RecordScoreRequestDTO scoreDetails);
    Map<Difficulty, List<GameResultDTO>> getHighScores();
    GameQuestionDTO generateQuestion(Difficulty difficulty);
}