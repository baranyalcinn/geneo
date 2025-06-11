package by.backend.service.game;

import by.backend.model.dto.AnswerValidationResult;

import java.util.Locale;

public interface AnswerValidatorService {
    AnswerValidationResult validateAnswer(String questionId, String userAnswer, Locale locale);
} 