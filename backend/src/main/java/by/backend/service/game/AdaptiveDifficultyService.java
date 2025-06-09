package by.backend.service.game;

import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.entity.Person;
import by.backend.model.enums.Difficulty;
import by.backend.service.game.session.GameSession;
import by.backend.service.game.session.PlayerAnswer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Adaptif zorluk sistemi ve ML tabanlı soru seçimi
 * TODO: RelationshipCacheService entegrasyonu sonrası aktif edilecek
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveDifficultyService {
    
    // Oyuncu performans geçmişi
    private final Map<String, PlayerProfile> playerProfiles = new ConcurrentHashMap<>();
    
    // Soru performans istatistikleri
    private final Map<String, QuestionStats> questionStats = new ConcurrentHashMap<>();
    
    // Adaptif zorluk parametreleri
    private static final int MIN_QUESTIONS_FOR_ADAPTATION = 3;
    
    /**
     * Oyuncu profiline göre optimal zorluk seviyesi öner
     */
    public Difficulty suggestOptimalDifficulty(String playerName) {
        PlayerProfile profile = playerProfiles.get(playerName);
        
        if (profile == null || profile.getTotalGamesPlayed() < 3) {
            log.info("Yeni oyuncu için varsayılan zorluk öneriliyor: {}", playerName);
            return Difficulty.EASY; // Yeni oyuncular için
        }
        
        double skillScore = calculateSkillScore(profile);
        log.info("Oyuncu {} için beceri skoru: {:.2f}", playerName, skillScore);
        
        if (skillScore >= 0.75) return Difficulty.HARD;
        if (skillScore >= 0.45) return Difficulty.MEDIUM;
        return Difficulty.EASY;
    }
    
    /**
     * Oyun sırasında dinamik zorluk ayarlama
     */
    public Difficulty adjustDifficultyDuringGame(GameSession session) {
        if (session.getQuestionsAnswered().get() < MIN_QUESTIONS_FOR_ADAPTATION) {
            return session.getDifficulty(); // Henüz yeterli veri yok
        }
        
        List<PlayerAnswer> recentAnswers = session.getPlayerAnswers()
                .stream()
                .skip(Math.max(0, session.getPlayerAnswers().size() - 5)) // Son 5 soru
                .toList();
        
        double recentAccuracy = calculateAccuracy(recentAnswers);
        double averageResponseTime = calculateAverageResponseTime(recentAnswers);
        int currentStreak = session.getCurrentStreak().get();
        
        double performanceScore = calculatePerformanceScore(recentAccuracy, averageResponseTime, currentStreak);
        
        Difficulty currentDifficulty = session.getDifficulty();
        Difficulty suggestedDifficulty = adjustDifficultyBasedOnPerformance(currentDifficulty, performanceScore);
        
        if (!suggestedDifficulty.equals(currentDifficulty)) {
            log.info("Session {} için zorluk ayarlandı: {} -> {} (Performans: {:.2f})",
                    session.getSessionId(), currentDifficulty, suggestedDifficulty, performanceScore);
        }
        
        return suggestedDifficulty;
    }
    
    /**
     * ML tabanlı soru seçimi - oyuncu profiline göre en uygun soruyu seç
     */
    public GameQuestionDTO selectOptimalQuestion(List<GameQuestionDTO> availableQuestions, 
                                                String playerName, 
                                                GameSession session) {
        if (availableQuestions.isEmpty()) {
            return null;
        }
        
        PlayerProfile profile = playerProfiles.get(playerName);
        
        // Skorlama sistemi ile en uygun soruyu bul
        Map<GameQuestionDTO, Double> questionScores = new HashMap<>();
        
        for (GameQuestionDTO question : availableQuestions) {
            double score = calculateQuestionSuitabilityScore(question, profile, session);
            questionScores.put(question, score);
        }
        
        // En yüksek skorlu soruyu seç (biraz randomness ekle)
        List<Map.Entry<GameQuestionDTO, Double>> sortedQuestions = questionScores.entrySet()
                .stream()
                .sorted(Map.Entry.<GameQuestionDTO, Double>comparingByValue().reversed())
                .toList();
        
        // Top 3 içinden seç (exploration vs exploitation)
        int topN = Math.min(3, sortedQuestions.size());
        Random random = new Random();
        
        // %70 en iyi, %20 ikinci, %10 üçüncü
        double rand = random.nextDouble();
        int selectedIndex;
        if (rand < 0.7) selectedIndex = 0;
        else if (rand < 0.9 && topN > 1) selectedIndex = 1;
        else if (topN > 2) selectedIndex = 2;
        else selectedIndex = 0;
        
        GameQuestionDTO selectedQuestion = sortedQuestions.get(selectedIndex).getKey();
        
        log.debug("Soru seçimi: {} sorudan {} seçildi (skor: {:.3f})",
                availableQuestions.size(), selectedQuestion.getId(), 
                sortedQuestions.get(selectedIndex).getValue());
        
        return selectedQuestion;
    }
    
    /**
     * Oyuncu cevabını kaydet ve profilini güncelle
     */
    public void recordPlayerAnswer(String playerName, PlayerAnswer answer, long responseTimeMs) {
        // Oyuncu profilini güncelle
        PlayerProfile profile = playerProfiles.computeIfAbsent(playerName, k -> new PlayerProfile());
        profile.recordAnswer(answer, responseTimeMs);
        
        // Soru istatistiklerini güncelle
        QuestionStats stats = questionStats.computeIfAbsent(answer.getQuestionId(), k -> new QuestionStats());
        stats.recordAttempt(answer.isCorrect(), responseTimeMs);
        
        log.debug("Oyuncu {} yanıtı kaydedildi: soru {}, doğru: {}, süre: {}ms",
                playerName, answer.getQuestionId(), answer.isCorrect(), responseTimeMs);
    }
    
    /**
     * Soru tipine göre oyuncu güçlü/zayıf alanlarını analiz et
     */
    public PlayerStrengthAnalysis analyzePlayerStrengths(String playerName) {
        PlayerProfile profile = playerProfiles.get(playerName);
        if (profile == null) {
            return new PlayerStrengthAnalysis();
        }
        
        PlayerStrengthAnalysis analysis = new PlayerStrengthAnalysis();
        
        // İlişki tipine göre performans analizi - getRelationshipCategory() kullan
        Map<String, List<PlayerAnswer>> answersByRelationType = profile.getAllAnswers()
                .stream()
                .collect(Collectors.groupingBy(PlayerAnswer::getRelationshipCategory));
        
        for (Map.Entry<String, List<PlayerAnswer>> entry : answersByRelationType.entrySet()) {
            String relationType = entry.getKey();
            List<PlayerAnswer> answers = entry.getValue();
            
            double accuracy = calculateAccuracy(answers);
            double avgTime = answers.stream()
                    .mapToLong(a -> profile.getResponseTimes().getOrDefault(a.getQuestionId(), 0L))
                    .average()
                    .orElse(0.0);
            
            RelationshipTypePerformance performance = new RelationshipTypePerformance();
            performance.setRelationType(relationType);
            performance.setAccuracy(accuracy);
            performance.setAverageResponseTime(avgTime);
            performance.setQuestionCount(answers.size());
            
            if (accuracy >= 0.8 && avgTime <= 8000) { // 8 saniye altı + yüksek doğruluk
                analysis.getStrengths().add(performance);
            } else if (accuracy < 0.5 || avgTime > 15000) { // Düşük doğruluk veya yavaş
                analysis.getWeaknesses().add(performance);
            } else {
                analysis.getNeutralAreas().add(performance);
            }
        }
        
        // Genel öneriler oluştur
        analysis.setRecommendations(generateRecommendations(analysis, profile));
        
        return analysis;
    }
    
    /**
     * Soru zorluk kalibrasyon sistemi
     */
    public void calibrateQuestionDifficulty() {
        log.info("Soru zorluk kalibrasyonu başlatılıyor...");
        
        int recalibrated = 0;
        for (Map.Entry<String, QuestionStats> entry : questionStats.entrySet()) {
            String questionId = entry.getKey();
            QuestionStats stats = entry.getValue();
            
            if (stats.getTotalAttempts() >= 10) { // Minimum deneme sayısı
                double currentAccuracy = stats.getAccuracyRate();
                double avgResponseTime = stats.getAverageResponseTime();
                
                // Zorluk seviyesi önerisi
                Difficulty suggestedDifficulty = calibrateDifficultyFromStats(currentAccuracy, avgResponseTime);
                
                if (suggestedDifficulty != stats.getCurrentDifficulty()) {
                    stats.setCurrentDifficulty(suggestedDifficulty);
                    recalibrated++;
                    
                    log.debug("Soru {} zorluğu yeniden kalibre edildi: {}",
                            questionId, suggestedDifficulty);
                }
            }
        }
        
        log.info("Soru zorluk kalibrasyonu tamamlandı: {} soru yeniden kalibre edildi", recalibrated);
    }
    
    // Yardımcı metodlar
    private double calculateSkillScore(PlayerProfile profile) {
        double baseAccuracy = profile.getOverallAccuracy();
        double timeBonus = calculateTimeBonus(profile.getAverageResponseTime());
        double experienceBonus = Math.min(0.1, profile.getTotalGamesPlayed() * 0.01);
        double streakBonus = Math.min(0.15, profile.getBestStreak() * 0.01);
        
        return Math.min(1.0, baseAccuracy + timeBonus + experienceBonus + streakBonus);
    }
    
    private double calculateTimeBonus(double avgResponseTime) {
        // Hızlı cevaplar için bonus (5-10 saniye optimal)
        if (avgResponseTime <= 5000) return 0.1;
        if (avgResponseTime <= 10000) return 0.05;
        if (avgResponseTime <= 15000) return 0.0;
        return -0.05; // Yavaş cevaplar için penalty
    }
    
    private double calculateAccuracy(List<PlayerAnswer> answers) {
        if (answers.isEmpty()) return 0.0;
        return (double) answers.stream()
                .mapToInt(a -> a.isCorrect() ? 1 : 0)
                .sum() / answers.size();
    }
    
    private double calculateAverageResponseTime(List<PlayerAnswer> answers) {
        return answers.stream()
                .mapToLong(a -> 8000L) // Placeholder - gerçek response time'ları session'dan alınmalı
                .average()
                .orElse(10000.0);
    }
    
    private double calculatePerformanceScore(double accuracy, double avgResponseTime, int streak) {
        double base = accuracy;
        double timeBonus = Math.max(0, (15000 - avgResponseTime) / 15000) * 0.2; // 15 saniye üstü penalty
        double streakBonus = Math.min(0.2, streak * 0.02);
        
        return Math.min(1.0, base + timeBonus + streakBonus);
    }
    
    private Difficulty adjustDifficultyBasedOnPerformance(Difficulty current, double performanceScore) {
        if (performanceScore >= 0.8 && current != Difficulty.HARD) {
            return Difficulty.values()[Math.min(current.ordinal() + 1, Difficulty.values().length - 1)];
        } else if (performanceScore <= 0.3 && current != Difficulty.EASY) {
            return Difficulty.values()[Math.max(current.ordinal() - 1, 0)];
        }
        return current;
    }
    
    private double calculateQuestionSuitabilityScore(GameQuestionDTO question, 
                                                   PlayerProfile profile, 
                                                   GameSession session) {
        double baseScore = 0.5;
        
        // Zorluk uyumluluğu
        if (question.getDifficulty() == session.getDifficulty()) {
            baseScore += 0.3;
        } else {
            baseScore += Math.max(0, 0.3 - Math.abs(question.getDifficulty().ordinal() - session.getDifficulty().ordinal()) * 0.15);
        }
        
        // Soru istatistikleri
        QuestionStats stats = questionStats.get(question.getId());
        if (stats != null) {
            // Orta zorlukta sorular tercih edilir (çok kolay veya çok zor değil)
            double idealAccuracy = 0.6; // %60 ideal doğruluk oranı
            double accuracyDiff = Math.abs(stats.getAccuracyRate() - idealAccuracy);
            baseScore += Math.max(0, 0.2 - accuracyDiff);
        }
        
        // Oyuncu profili uyumluluğu
        if (profile != null) {
            // Oyuncunun zayıf olduğu alanları pratik ettir
            // (learning-focused approach)
            baseScore += calculatePersonalizedScore(question, profile);
        }
        
        // Çeşitlilik bonusu (aynı tip sorular art arda gelmemesi için)
        if (session.getPlayerAnswers().size() >= 2) {
            String lastQuestionType = session.getPlayerAnswers()
                    .get(session.getPlayerAnswers().size() - 1)
                    .getRelationshipCategory(); // getQuestionType() yerine getRelationshipCategory() kullan
            if (!lastQuestionType.equals(question.getPerson1())) { // Basit çeşitlilik kontrolü
                baseScore += 0.1;
            }
        }
        
        return Math.min(1.0, baseScore);
    }
    
    private double calculatePersonalizedScore(GameQuestionDTO question, PlayerProfile profile) {
        // Oyuncunun bu tip sorulardaki performansına göre skor hesapla
        String questionType = extractQuestionType(question);
        
        List<PlayerAnswer> relatedAnswers = profile.getAllAnswers().stream()
                .filter(a -> a.getRelationshipCategory().equals(questionType)) // getQuestionType() yerine getRelationshipCategory() kullan
                .toList();
        
        if (relatedAnswers.isEmpty()) {
            return 0.1; // Yeni tip soru - keşfetme bonusu
        }
        
        double accuracy = calculateAccuracy(relatedAnswers);
        
        // Orta performans gösteren alanları pratik ettir
        if (accuracy >= 0.3 && accuracy <= 0.7) {
            return 0.15; // İyileştirme fırsatı
        } else if (accuracy < 0.3) {
            return 0.1; // Zayıf alan - dikkatli yaklaş
        } else {
            return 0.05; // Güçlü alan - daha az öncelik
        }
    }
    
    private String extractQuestionType(GameQuestionDTO question) {
        // Soru tipini çıkar (basit implementation)
        if (question.getId().contains("parent") || question.getId().contains("child")) {
            return "parent_child";
        } else if (question.getId().contains("sibling")) {
            return "sibling";
        } else if (question.getId().contains("spouse")) {
            return "spouse";
        }
        return "complex";
    }
    
    private Difficulty calibrateDifficultyFromStats(double accuracy, double avgResponseTime) {
        // İstatistiklere göre zorluk seviyesi öner
        if (accuracy >= 0.85 && avgResponseTime <= 8000) return Difficulty.EASY;
        if (accuracy >= 0.65 && avgResponseTime <= 12000) return Difficulty.MEDIUM;
        return Difficulty.HARD;
    }
    
    private List<String> generateRecommendations(PlayerStrengthAnalysis analysis, PlayerProfile profile) {
        List<String> recommendations = new ArrayList<>();
        
        if (!analysis.getWeaknesses().isEmpty()) {
            RelationshipTypePerformance weakest = analysis.getWeaknesses().get(0);
            recommendations.add("'" + weakest.getRelationType() + "' ilişki tipinde pratik yapmanız önerilir");
        }
        
        if (profile.getAverageResponseTime() > 15000) {
            recommendations.add("Cevap hızınızı artırmak için daha basit sorularla pratik yapabilirsiniz");
        }
        
        if (profile.getBestStreak() < 3) {
            recommendations.add("Ardışık doğru cevaplar için konsantrasyonunuzu artırın");
        }
        
        if (analysis.getStrengths().size() >= 2) {
            recommendations.add("Güçlü olduğunuz alanları kullanarak daha zor seviyelere geçebilirsiniz");
        }
        
        return recommendations;
    }
    
    // İç sınıflar
    public static class PlayerProfile {
        private int totalGamesPlayed = 0;
        private double overallAccuracy = 0.0;
        private double averageResponseTime = 0.0;
        private int bestStreak = 0;
        private int totalQuestions = 0;
        private int totalCorrect = 0;
        private long totalResponseTime = 0;
        private List<PlayerAnswer> allAnswers = new ArrayList<>();
        private Map<String, Long> responseTimes = new HashMap<>();
        
        public void recordAnswer(PlayerAnswer answer, long responseTimeMs) {
            allAnswers.add(answer);
            responseTimes.put(answer.getQuestionId(), responseTimeMs);
            
            totalQuestions++;
            totalResponseTime += responseTimeMs;
            
            if (answer.isCorrect()) {
                totalCorrect++;
            }
            
            // Güncel istatistikleri hesapla
            overallAccuracy = (double) totalCorrect / totalQuestions;
            averageResponseTime = (double) totalResponseTime / totalQuestions;
        }
        
        // Getters
        public int getTotalGamesPlayed() { return totalGamesPlayed; }
        public double getOverallAccuracy() { return overallAccuracy; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public int getBestStreak() { return bestStreak; }
        public List<PlayerAnswer> getAllAnswers() { return allAnswers; }
        public Map<String, Long> getResponseTimes() { return responseTimes; }
        
        public void incrementGamesPlayed() { totalGamesPlayed++; }
        public void updateBestStreak(int streak) { bestStreak = Math.max(bestStreak, streak); }
    }
    
    public static class QuestionStats {
        private int totalAttempts = 0;
        private int correctAttempts = 0;
        private long totalResponseTime = 0;
        private Difficulty currentDifficulty = Difficulty.MEDIUM;
        private LocalDateTime lastUpdated = LocalDateTime.now();
        
        public void recordAttempt(boolean correct, long responseTimeMs) {
            totalAttempts++;
            totalResponseTime += responseTimeMs;
            
            if (correct) {
                correctAttempts++;
            }
            
            lastUpdated = LocalDateTime.now();
        }
        
        public double getAccuracyRate() {
            return totalAttempts > 0 ? (double) correctAttempts / totalAttempts : 0.0;
        }
        
        public double getAverageResponseTime() {
            return totalAttempts > 0 ? (double) totalResponseTime / totalAttempts : 0.0;
        }
        
        // Getters and setters
        public int getTotalAttempts() { return totalAttempts; }
        public Difficulty getCurrentDifficulty() { return currentDifficulty; }
        public void setCurrentDifficulty(Difficulty currentDifficulty) { this.currentDifficulty = currentDifficulty; }
    }
    
    public static class PlayerStrengthAnalysis {
        private List<RelationshipTypePerformance> strengths = new ArrayList<>();
        private List<RelationshipTypePerformance> weaknesses = new ArrayList<>();
        private List<RelationshipTypePerformance> neutralAreas = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();
        
        // Getters and setters
        public List<RelationshipTypePerformance> getStrengths() { return strengths; }
        public List<RelationshipTypePerformance> getWeaknesses() { return weaknesses; }
        public List<RelationshipTypePerformance> getNeutralAreas() { return neutralAreas; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }
    
    public static class RelationshipTypePerformance {
        private String relationType;
        private double accuracy;
        private double averageResponseTime;
        private int questionCount;
        
        // Getters and setters
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public int getQuestionCount() { return questionCount; }
        public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    }
} 