package by.backend.service.game;

import by.backend.exception.GameException;
import by.backend.model.dto.AnswerValidationResult;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipPathDTO;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.enums.RelationshipStatus;
import by.backend.repository.PersonRepository;
import by.backend.service.relationship.RelationshipService;
import by.backend.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnswerValidatorServiceImpl implements AnswerValidatorService {

    private final PersonRepository personRepository;
    private final RelationshipService relationshipService;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;

    private static final String RELATIONSHIP_PREFIX = "relationship.";
    private static final String GAME_ANSWER_PREFIX = "game.answer.";

    @Override
    public AnswerValidationResult validateAnswer(String questionId, String userAnswer, Locale locale) {
        try {
            String[] ids = questionId.split("_");
            if (ids.length != 2) {
                throw new GameException("Invalid Question ID format: " + questionId);
            }

            long person1Id = Long.parseLong(ids[0]);
            long person2Id = Long.parseLong(ids[1]);
            
            Person p1 = personRepository.findById(person1Id)
                    .orElseThrow(() -> new GameException("Person 1 not found with id: " + person1Id));
            Person p2 = personRepository.findById(person2Id)
                    .orElseThrow(() -> new GameException("Person 2 not found with id: " + person2Id));
            
            PersonSummaryDTO p1Summary = personMapper.toSummaryDTO(p1);
            PersonSummaryDTO p2Summary = personMapper.toSummaryDTO(p2);

            RelationshipDescriptionResult result = relationshipService.findRelationshipDescription(p1Summary, p2Summary);

            if (result.getStatus() != RelationshipStatus.FOUND) {
                log.warn("Could not determine relationship for question ID: {}. Status: {}", questionId, result.getStatus());
                throw new GameException("Could not determine relationship for validation.");
            }

            Set<String> correctAnswers = new HashSet<>();
            addAnswerVariations(result.getMessageKey(), p1, correctAnswers, locale);

            if (result.getAcceptableMessageKeys() != null) {
                for (String acceptableKey : result.getAcceptableMessageKeys()) {
                    addAnswerVariations(acceptableKey, p1, correctAnswers, locale);
                }
            }
            
            boolean isCorrect = correctAnswers.stream()
                    .anyMatch(answer -> normalizeAnswer(answer).equalsIgnoreCase(normalizeAnswer(userAnswer)));
            
            String specificAnswerKey = generateSpecificAnswerKey(result.getMessageKey(), p1);
            String correctAnswerText = getMessage(specificAnswerKey, locale);

            if (correctAnswerText.equals(specificAnswerKey) || correctAnswerText.startsWith(GAME_ANSWER_PREFIX)) {
                String genericGameKey = result.getMessageKey().replace(RELATIONSHIP_PREFIX, GAME_ANSWER_PREFIX);
                correctAnswerText = getMessage(genericGameKey, locale);
                if (correctAnswerText.equals(genericGameKey) || correctAnswerText.startsWith(GAME_ANSWER_PREFIX)) {
                    correctAnswerText = getMessage(result.getMessageKey(), locale);
                }
            }
            
            RelationshipPathDTO relationshipPath = null;
            if (isCorrect) {
                List<RelationshipStepDTO> pathSteps = result.getPath();
                relationshipPath = new RelationshipPathDTO();
                relationshipPath.setSteps(pathSteps);
                relationshipPath.setDescription(correctAnswerText);
            }
            
            String category = getRelationshipCategory(result.getMessageKey());

            return new AnswerValidationResult(isCorrect, correctAnswerText, category, relationshipPath);

        } catch (Exception e) {
            log.error("Error validating answer for questionId '{}': {}", questionId, e.getMessage(), e);
            throw new GameException(getMessage("game.error.could_not_determine_correct_answer", locale));
        }
    }

    private void addAnswerVariations(String relationshipKey, Person targetPerson, Set<String> correctAnswers, Locale locale) {
        if (relationshipKey == null || relationshipKey.isEmpty()) {
            return;
        }

        String genericGameKey = relationshipKey.replace(RELATIONSHIP_PREFIX, GAME_ANSWER_PREFIX);
        String genericAnswer = getMessage(genericGameKey, locale);
        if (!genericAnswer.equals(genericGameKey) && !genericAnswer.startsWith(GAME_ANSWER_PREFIX)) {
            correctAnswers.add(genericAnswer);
        }

        String specificGameKey = generateSpecificAnswerKey(relationshipKey, targetPerson);
        String specificAnswer = getMessage(specificGameKey, locale);
        if (!specificAnswer.equals(specificGameKey) && !specificAnswer.startsWith(GAME_ANSWER_PREFIX)) {
            correctAnswers.add(specificAnswer);
        }
        
        String directAnswer = getMessage(relationshipKey, locale);
        if(!directAnswer.equals(relationshipKey) && !directAnswer.startsWith(RELATIONSHIP_PREFIX)) {
            correctAnswers.add(directAnswer);
        }
    }

    private String generateSpecificAnswerKey(String relationshipKey, Person targetPerson) {
        if (relationshipKey == null || targetPerson == null) {
            return "";
        }
        String baseKey = relationshipKey.replace(RELATIONSHIP_PREFIX, GAME_ANSWER_PREFIX);
        String gender = targetPerson.getGender().name();
        
        if (relationshipKey.contains("father")) return GAME_ANSWER_PREFIX + "father";
        if (relationshipKey.contains("mother")) return GAME_ANSWER_PREFIX + "mother";
        if (relationshipKey.contains("son")) return GAME_ANSWER_PREFIX + "son";
        if (relationshipKey.contains("daughter")) return GAME_ANSWER_PREFIX + "daughter";
        if (relationshipKey.contains("brother")) return GAME_ANSWER_PREFIX + "brother";
        if (relationshipKey.contains("sister")) return GAME_ANSWER_PREFIX + "sister";
        if (relationshipKey.contains("grandfather")) return GAME_ANSWER_PREFIX + "grandfather";
        if (relationshipKey.contains("grandmother")) return GAME_ANSWER_PREFIX + "grandmother";
        if (relationshipKey.contains("grandson")) return GAME_ANSWER_PREFIX + "grandson";
        if (relationshipKey.contains("granddaughter")) return GAME_ANSWER_PREFIX + "granddaughter";
        if (relationshipKey.contains("nephew")) return GAME_ANSWER_PREFIX + "nephew";
        if (relationshipKey.contains("niece")) return GAME_ANSWER_PREFIX + "niece";
        if (relationshipKey.contains("spouse")) return GAME_ANSWER_PREFIX + "spouse";
        
        if (relationshipKey.contains("parent")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "father" : GAME_ANSWER_PREFIX + "mother";
        if (relationshipKey.contains("child")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "son" : GAME_ANSWER_PREFIX + "daughter";
        if (relationshipKey.contains("sibling")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "brother" : GAME_ANSWER_PREFIX + "sister";
        if (relationshipKey.contains("grandparent")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "grandfather" : GAME_ANSWER_PREFIX + "grandmother";
        if (relationshipKey.contains("grandchild")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "grandson" : GAME_ANSWER_PREFIX + "granddaughter";
        if (relationshipKey.contains("cousin")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "cousin_male" : GAME_ANSWER_PREFIX + "cousin_female";
        if (relationshipKey.contains("uncle") || relationshipKey.contains("aunt")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "uncle" : GAME_ANSWER_PREFIX + "aunt";
        if (relationshipKey.contains("nephew") || relationshipKey.contains("niece")) return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "nephew" : GAME_ANSWER_PREFIX + "niece";
        
        return baseKey;
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().toLowerCase(Locale.ROOT);
    }

    private String getMessage(String code, Locale locale) {
        try {
            return messageSource.getMessage(code, null, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Translation not found for key: '{}', locale: {}. Using key as fallback.", code, locale);
            return code;
        }
    }
    
    private String getRelationshipCategory(String messageKey) {
        if (messageKey == null) return "undefined";
        String key = messageKey.toLowerCase(Locale.ROOT);
        if (key.startsWith("relationship.direct") || key.startsWith("relationship.reverse") || key.contains(".spouse")) return "direct";
        if (key.contains(".sibling")) return "siblings";
        if (key.contains("grandparent")) return "grandparent";
        if (key.contains("grandchild")) return "grandchild";
        if (key.contains("aunt") || key.contains("uncle")) return "aunt_uncle";
        if (key.contains("nephew") || key.contains("niece")) return "nephew_niece";
        if (key.contains("cousin")) return "cousin";
        if (key.contains("inlaw")) return "inlaw";
        if (key.contains("step")) return "step";
        if (key.contains("itself")) return "self";
        if (key.contains("not_found")) return "none";
        return "other";
    }
} 