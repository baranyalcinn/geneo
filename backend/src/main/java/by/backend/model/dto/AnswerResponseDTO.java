package by.backend.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;

@Data
@Builder(access = lombok.AccessLevel.PUBLIC)
@AllArgsConstructor
public class AnswerResponseDTO {
    private boolean correctAnswer;
    private String correctAnswerText;
    private int pointsEarned;
    private int updatedScore;
    private int updatedStreak;
    private GameQuestionDTO nextQuestion;
    private boolean gameOver;
    private String gameEndMessage;
    private GameResultDTO finalResult;
    private Long finalScoreId;
    private RelationshipPathDTO relationshipPath;
    private Set<String> askedQuestionSignaturesInThisGame;
    private List<QuestionProgressionPoint> updatedProgression;
    private AnalysisResultDTO analysisResult;
} 