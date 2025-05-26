package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAnswerDTO {
    private String questionId;
    private String answer;
    private long timeTakenInSeconds;
    private Difficulty difficulty;
    private Set<String> askedQuestionSignaturesInThisGame;
    private int currentScore;
    private int currentStreak;
    private String playerName;
    private int gameQuestionCount;
}