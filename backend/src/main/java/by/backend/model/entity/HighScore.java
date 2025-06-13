package by.backend.model.entity;

import by.backend.model.enums.Difficulty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "high_scores", indexes = {
    @Index(name = "idx_highscore_player", columnList = "player_name"),
    @Index(name = "idx_highscore_difficulty", columnList = "difficulty"),
    @Index(name = "idx_highscore_score", columnList = "score DESC"),
    @Index(name = "idx_highscore_player_difficulty", columnList = "player_name, difficulty"),
    @Index(name = "idx_highscore_difficulty_score", columnList = "difficulty, score DESC"),
    @Index(name = "idx_highscore_played_at", columnList = "played_at DESC"),
    @Index(name = "idx_highscore_leaderboard", columnList = "difficulty, score DESC, played_at DESC") // For leaderboard queries
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "highScoreCache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@NamedQueries({
    @NamedQuery(
        name = "HighScore.findTopByDifficulty",
        query = "SELECT h FROM HighScore h WHERE h.difficulty = :difficulty ORDER BY h.score DESC, h.playedAt DESC"
    ),
    @NamedQuery(
        name = "HighScore.findPlayerBest",
        query = "SELECT h FROM HighScore h WHERE h.playerName = :playerName AND h.difficulty = :difficulty ORDER BY h.score DESC"
    ),
    @NamedQuery(
        name = "HighScore.findPlayerStats",
        query = "SELECT h FROM HighScore h WHERE h.playerName = :playerName ORDER BY h.playedAt DESC"
    ),
    @NamedQuery(
        name = "HighScore.countByDifficulty",
        query = "SELECT COUNT(h) FROM HighScore h WHERE h.difficulty = :difficulty"
    )
})
public class HighScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "player_name", nullable = false, length = 100)
    private String playerName;
    
    @Column(name = "score", nullable = false)
    private int score;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private Difficulty difficulty;
    
    @Column(name = "correct_answers", nullable = false)
    private int correctAnswers;
    
    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;
    
    @Column(name = "max_streak", nullable = false)
    private int maxStreak;
    
    @CreationTimestamp
    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt;

    @Lob
    @Column(name = "progression_data", columnDefinition = "TEXT")
    private String progressionData;

    // Computed fields for performance
    @Column(name = "accuracy_percentage")
    private Double accuracyPercentage;

    @Column(name = "points_per_minute")
    private Double pointsPerMinute;

    @Column(name = "game_duration_seconds")
    private Integer gameDurationSeconds;

    @Column(name = "rank_in_difficulty") // Calculated rank
    private Integer rankInDifficulty;

    // Metadata for analytics
    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "is_personal_best", nullable = false)
    @Builder.Default
    private Boolean isPersonalBest = false;

    // Convenience methods
    public double calculateAccuracy() {
        if (totalQuestions == 0) return 0.0;
        return ((double) correctAnswers / totalQuestions) * 100.0;
    }

    public double calculatePointsPerQuestion() {
        if (totalQuestions == 0) return 0.0;
        return (double) score / totalQuestions;
    }

    public String getFormattedScore() {
        return String.format("%,d", score);
    }

    public String getAccuracyDisplay() {
        return String.format("%.1f%%", calculateAccuracy());
    }

    public boolean isHighPerformance() {
        return calculateAccuracy() >= 80.0 && maxStreak >= 3;
    }

    // Performance rating method
    public String getPerformanceRating() {
        double accuracy = calculateAccuracy();
        if (accuracy >= 95.0 && maxStreak >= 8) return "EXCELLENT";
        if (accuracy >= 85.0 && maxStreak >= 5) return "VERY_GOOD";
        if (accuracy >= 70.0 && maxStreak >= 3) return "GOOD";
        if (accuracy >= 50.0) return "AVERAGE";
        return "NEEDS_IMPROVEMENT";
    }

    // Pre-persist and pre-update hooks
    @PrePersist
    @PreUpdate
    private void updateComputedFields() {
        this.accuracyPercentage = calculateAccuracy();
        
        if (gameDurationSeconds != null && gameDurationSeconds > 0) {
            this.pointsPerMinute = (score * 60.0) / gameDurationSeconds;
        }
    }
} 