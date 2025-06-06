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
     * Her bir zorluk seviyesi için oyun süresi (saniye cinsinden).
     */
    private Map<Difficulty, @Min(30) Integer> durationInSeconds = new EnumMap<>(Map.of(
            Difficulty.EASY, 180,
            Difficulty.MEDIUM, 120,
            Difficulty.HARD, 90
    ));

    /**
     * Bir oyun oturumunda sorulacak toplam soru sayısı.
     */
    @Min(1)
    private int questionsPerGame = 10;
} 