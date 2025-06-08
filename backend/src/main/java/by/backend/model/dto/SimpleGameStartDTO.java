package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleGameStartDTO {
    private String playerName;
    private Difficulty difficulty;
} 