package by.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Component
@ConfigurationProperties(prefix = "relationship.settings")
@Data
@Validated
public class RelationshipProperties {

    @Min(1)
    private int defaultPathDisplayMaxDepth = 5;

    @Min(1)
    private int maxBfsPathsToCollect = 5;

    @Min(0) // Minimum ebeveyn yaşı 0 olabilir, ancak mantıksal olarak daha yüksek olmalı
    private int minParentAge = 16;

    // Örnek olarak eklendi, isterseniz kullanabilirsiniz.
    // @Min(100)
    // private int ancestorCacheSize = 1000;
} 