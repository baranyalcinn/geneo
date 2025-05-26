package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitialGameDataDTO {
    private GameQuestionDTO firstQuestion;
    private String playerName;
    private Difficulty difficulty;
} 