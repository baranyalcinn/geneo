package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class InitialGameDataDTO {
    @NonNull
    private String sessionId;

    @NonNull
    private GameQuestionDTO firstQuestion;

    @NonNull
    private String playerName;

    @NonNull
    private Difficulty difficulty;
    
    private long gameDurationInSeconds;

    private int totalQuestions;
} 