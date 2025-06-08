package by.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import java.util.EnumMap;
import java.util.Map;
import by.backend.model.enums.Difficulty;

@Configuration
@ConfigurationProperties(prefix = "game")
@Data
@Validated
public class GameProperties {

    /**
     * Her bir zorluk seviyesi için oyun süresi (saniye cinsinden) - 3 dakika toplam oyun
     */
    private Map<Difficulty, @Min(30) Integer> durationInSeconds = new EnumMap<>(Map.of(
            Difficulty.EASY, 180,    // 3 dakika
            Difficulty.MEDIUM, 180,  // 3 dakika
            Difficulty.HARD, 180     // 3 dakika
    ));

    /**
     * Bir oyun oturumunda sorulacak toplam soru sayısı - Sabit 10 soru
     */
    @Min(1)
    private int questionsPerGame = 10;
    
    /**
     * Her soru için maksimum süre (saniye cinsinden) - 18 saniye ortalama
     */
    private Map<Difficulty, @Min(5) Integer> questionTimeLimit = new EnumMap<>(Map.of(
            Difficulty.EASY, 20,     // Kolay sorular için 20 saniye
            Difficulty.MEDIUM, 18,   // Orta sorular için 18 saniye
            Difficulty.HARD, 15      // Zor sorular için 15 saniye
    ));

    /**
     * Her bir zorluk seviyesi için seçenek sayısı.
     */
    private Map<Difficulty, @Min(2) Integer> optionsCount = new EnumMap<>(Map.of(
            Difficulty.EASY, 3,
            Difficulty.MEDIUM, 4,
            Difficulty.HARD, 6
    ));

    public int getTimeLimit(Difficulty difficulty) {
        return durationInSeconds.getOrDefault(difficulty, 90);
    }

    public int getOptionsCount(Difficulty difficulty) {
        return optionsCount.getOrDefault(difficulty, 4);
    }
    
    public int getQuestionTimeLimit(Difficulty difficulty) {
        return questionTimeLimit.getOrDefault(difficulty, 18);
    }
} 