package by.backend.repository;

import by.backend.model.entity.GameQuestionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameQuestionFeedbackRepository extends JpaRepository<GameQuestionFeedback, Long> {
} 