package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAnswerDTO {
    @NonNull
    private String questionId;

    private String answer;
    private long timeTakenInSeconds;
    private Difficulty difficulty;
    private int currentScore;
    private int currentStreak;
    private String playerName;
    private int questionsAnswered;
    private int correctAnswersCount;
}