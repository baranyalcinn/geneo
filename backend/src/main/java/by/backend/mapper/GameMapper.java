package by.backend.mapper;

// İlgili entity sınıfları varsa ve aktif mappinglerde kullanılacaksa buraya import edilecek
// import by.backend.model.entity.GameQuestion;
// import by.backend.model.entity.GameSession;
// import by.backend.model.entity.GameScore;

import org.mapstruct.Mapper;

/**
 * Oyun modülü için DTO - Entity dönüşümlerini sağlayan mapper.
 * GameDTO ve entity sınıfları arasındaki dönüşümleri tanımlar.
 */
@Mapper(componentModel = "spring", uses = PersonMapper.class)
public interface GameMapper {


} 