package by.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResultDTO {
    /**
     * A map where the key is the relationship category (e.g., "family", "work")
     * and the value is the success percentage (0-100) for that category.
     */
    private Map<String, Double> successRateByCategory;

    /**
     * A summary message of the analysis, localized.
     * e.g., "Family relations success: 90%. Work relations success: 50%."
     */
    private String summaryMessage;
} 