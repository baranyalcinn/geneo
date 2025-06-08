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
    private double responseTimeSeconds = 0.0; // Default value

    public PlayerAnswer(@NonNull String questionId, @NonNull String relationshipCategory, 
                       @NonNull String userAnswer, boolean correct) {
        this.questionId = questionId;
        this.relationshipCategory = relationshipCategory;
        this.userAnswer = userAnswer;
        this.correct = correct;
        this.responseTimeSeconds = 0.0;
    }
} 