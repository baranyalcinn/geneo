package by.backend.service.game.session;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;
import by.backend.config.GameProperties;
import by.backend.model.enums.Difficulty;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;

@Service
@Slf4j
public class GameSessionManager {
    
    private final ConcurrentMap<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final GameProperties gameProperties;
    
    public GameSessionManager(GameProperties gameProperties) {
        this.gameProperties = gameProperties;
    }
    
    /**
     * Yeni oyun session'ı oluştur
     */
    public GameSession createSession(String sessionId, String playerName, Difficulty difficulty) {
        GameSession session = new GameSession(
            sessionId, 
            playerName, 
            difficulty,
            gameProperties.getDurationInSeconds().get(difficulty),
            gameProperties.getQuestionsPerGame(),
            new ArrayList<>()
        );
        
        activeSessions.put(sessionId, session);
        log.info("Created new game session: {} for player: {}", sessionId, playerName);
        return session;
    }
    
    /**
     * Session getir
     */
    public GameSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Session'ı kaldır
     */
    public void removeSession(String sessionId) {
        GameSession removed = activeSessions.remove(sessionId);
        if (removed != null) {
            log.info("Removed game session: {} for player: {}", sessionId, removed.getPlayerName());
        }
    }
    
    /**
     * Aktif session sayısı
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Tüm aktif sessionları getir (admin/monitoring için)
     */
    public List<GameSession> getAllActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Süresi dolmuş sessionları temizle - her 5 dakikada bir çalışır
     */
    @Scheduled(fixedRate = 300000) // 5 dakika
    public void cleanupExpiredSessions() {
        long currentTime = Instant.now().getEpochSecond();
        List<String> expiredSessions = new ArrayList<>();
        
        activeSessions.forEach((sessionId, session) -> {
            if (session.isTimeUp()) {
                expiredSessions.add(sessionId);
            }
        });
        
        expiredSessions.forEach(sessionId -> {
            GameSession removed = activeSessions.remove(sessionId);
            if (removed != null) {
                log.info("Cleaned up expired session: {} for player: {}", 
                        sessionId, removed.getPlayerName());
            }
        });
        
        if (!expiredSessions.isEmpty()) {
            log.info("Cleaned up {} expired sessions", expiredSessions.size());
        }
    }
    
    /**
     * Session durumunu kontrol et
     */
    public boolean isSessionActive(String sessionId) {
        GameSession session = activeSessions.get(sessionId);
        return session != null && !session.isGameOver();
    }
    
    /**
     * Oyuncu adına göre aktif sessionları bul
     */
    public List<GameSession> getSessionsByPlayer(String playerName) {
        return activeSessions.values().stream()
                .filter(session -> session.getPlayerName().equals(playerName))
                .toList();
    }
} 