package by.backend.model.dto;

// Bu record, bir oyun sırasındaki her bir soruya ait ilerleme bilgisini tutar.
// Jackson tarafından JSON'a serileştirilip JSON'dan deserialize edilebilir.
public record QuestionProgressionPoint(
    int questionNumber,      // Sorunun oyun içindeki sırası (1-based)
    int scoreAfter,          // Bu sorudan sonraki toplam skor
    int pointsEarned,        // Bu sorudan kazanılan puan
    boolean correct          // Sorunun doğru cevaplanıp cevaplanmadığı
) {} 