package by.backend.service.social;

import by.backend.model.enums.Difficulty;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public interface SocialGameService {
    
    /**
     * Aile üyeleri arasında multiplayer oyun başlatır
     */
    FamilyChallenge createFamilyChallenge(
        String creatorName,
        List<String> participantEmails,
        Difficulty difficulty,
        int questionCount
    );
    
    /**
     * Günlük aile bilgisi yarışması
     */
    DailyChallenge createDailyChallenge();
    
    /**
     * Leaderboard sistemi
     */
    Leaderboard getFamilyLeaderboard(String familyId);
    
    /**
     * Arkadaş davet sistemi
     */
    FriendInvite sendFriendInvite(String fromPlayer, String toEmail);
    
    /**
     * Sosyal başarım paylaşımı
     */
    SocialPost shareAchievement(String playerName, String achievementId);
    
    @Data
    class FamilyChallenge {
        private String challengeId;
        private String creatorName;
        private List<String> participants;
        private List<ChallengeResult> results;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private boolean completed;
        private String winner;
    }
    
    @Data
    class ChallengeResult {
        private String playerName;
        private int score;
        private double accuracy;
        private long completionTimeSeconds;
        private LocalDateTime completedAt;
    }
    
    @Data
    class DailyChallenge {
        private String challengeId;
        private LocalDateTime date;
        private List<String> questions;
        private List<DailyChallengeEntry> entries;
        private String theme; // "Dayı/Amca Farkı", "Kayın İlişkileri" vb.
    }
    
    @Data
    class DailyChallengeEntry {
        private String playerName;
        private int score;
        private LocalDateTime submittedAt;
        private int rank;
    }
    
    @Data
    class Leaderboard {
        private String familyId;
        private List<LeaderboardEntry> entries;
        private String period; // "weekly", "monthly", "all-time"
        private LocalDateTime lastUpdated;
    }
    
    @Data
    class LeaderboardEntry {
        private int rank;
        private String playerName;
        private int totalScore;
        private double averageAccuracy;
        private int gamesPlayed;
        private List<String> badges;
    }
    
    @Data
    class FriendInvite {
        private String inviteId;
        private String fromPlayer;
        private String toEmail;
        private LocalDateTime sentAt;
        private LocalDateTime expiresAt;
        private boolean accepted;
    }
    
    @Data
    class SocialPost {
        private String postId;
        private String playerName;
        private String content;
        private String achievementId;
        private LocalDateTime createdAt;
        private List<String> likes;
        private List<String> comments;
    }
} 