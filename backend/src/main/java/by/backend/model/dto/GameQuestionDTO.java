package by.backend.model.dto;

import by.backend.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GameQuestionDTO {
    private String id;
    private String questionText;
    private String person1;
    private String person2;
    private PersonInfoDTO person1Info;
    private PersonInfoDTO person2Info;
    private List<String> options;
    private String correctAnswer;
    private Difficulty difficulty;
    private int timeLimit;
    
    private List<RelationshipStepDTO> relationshipPath;
} 