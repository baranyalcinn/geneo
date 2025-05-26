package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StartGameRequestDTO {
    private String playerName;
    private Difficulty difficulty;
} 