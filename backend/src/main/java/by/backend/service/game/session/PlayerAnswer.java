package by.backend.service.game.session;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class PlayerAnswer {
    @NonNull
    private String questionId;
    @NonNull
    private String relationshipCategory; // e.g., "family", "work", "default"
    @NonNull
    private String userAnswer;
    private boolean correct;
} 