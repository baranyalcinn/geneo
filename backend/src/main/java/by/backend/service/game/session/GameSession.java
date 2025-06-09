package by.backend.service.game.session;

import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.enums.Difficulty;
import lombok.Data;
import lombok.NonNull;


import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class GameSession {
    @NonNull
    private String sessionId;

    @NonNull
    private String playerName;

    @NonNull
    private Difficulty difficulty;

    private final long startTime;
    private final long gameDurationInSeconds;
    private final int totalQuestions;
    private final AtomicInteger score = new AtomicInteger(0);
    private final AtomicInteger currentStreak = new AtomicInteger(0);
    private final AtomicInteger maxStreak = new AtomicInteger(0);
    private final AtomicInteger questionsAnswered = new AtomicInteger(0);

    private final Queue<GameQuestionDTO> questions;
    private final Set<String> askedQuestionSignatures = new CopyOnWriteArraySet<>();
    private final List<PlayerAnswer> playerAnswers = new CopyOnWriteArrayList<>();
    private boolean active = true;

    public GameSession(@NonNull String sessionId, @NonNull String playerName, @NonNull Difficulty difficulty,
                       long gameDurationInSeconds, int totalQuestions, List<GameQuestionDTO> gameQuestions) {
        this.sessionId = sessionId;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.startTime = System.currentTimeMillis();
        this.gameDurationInSeconds = gameDurationInSeconds;
        this.totalQuestions = totalQuestions;
        this.questions = new ConcurrentLinkedQueue<>(gameQuestions != null ? gameQuestions : new ArrayList<>());
        // Track signatures to avoid asking the same question again if the queue is refilled
        if (gameQuestions != null) {
            gameQuestions.forEach(q -> this.askedQuestionSignatures.add(q.getId()));
        }
    }

    public boolean isTimeUp() {
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - startTime) / 1000;
        return elapsedSeconds > gameDurationInSeconds;
    }

    public boolean areAllQuestionsAnswered() {
        return questionsAnswered.get() >= totalQuestions;
    }

    public boolean isGameOver() {
        return isTimeUp() || areAllQuestionsAnswered();
    }

    public GameQuestionDTO getNextQuestion() {
        return questions.poll();
    }

    public void recordAnswer(PlayerAnswer answer, int points) {
        playerAnswers.add(answer);
        questionsAnswered.incrementAndGet();
        if (answer.isCorrect()) {
            score.addAndGet(points);
            int newStreak = currentStreak.incrementAndGet();
            if (newStreak > maxStreak.get()) {
                maxStreak.set(newStreak);
            }
        } else {
            currentStreak.set(0);
        }
    }

    public int getCorrectAnswers() {
        return (int) playerAnswers.stream().filter(PlayerAnswer::isCorrect).count();
    }
} 