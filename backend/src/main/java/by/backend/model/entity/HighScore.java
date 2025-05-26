package by.backend.model.entity;

import by.backend.model.enums.Difficulty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String playerName;
    private int score;
    
    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;
    
    private int correctAnswers;
    private int totalQuestions;
    private int maxStreak;
    private LocalDateTime playedAt;
} 