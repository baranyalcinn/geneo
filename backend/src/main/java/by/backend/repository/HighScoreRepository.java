package by.backend.repository;

import by.backend.model.entity.HighScore;
import by.backend.model.enums.Difficulty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HighScoreRepository extends JpaRepository<HighScore, Long> {
    // Zorluk seviyesine göre puanları azalan sırayla getir
    List<HighScore> findByDifficultyOrderByScoreDesc(Difficulty difficulty);

    // Zorluk seviyesine göre en yüksek ilk 10 skoru puanları azalan sırayla getir
    List<HighScore> findTop10ByDifficultyOrderByScoreDesc(Difficulty difficulty);
} 