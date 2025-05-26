package by.backend.mapper;

import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDTO;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-24T15:08:03+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.1 (Oracle Corporation)"
)
@Component
public class RelationshipMapperImpl implements RelationshipMapper {

    @Autowired
    private PersonMapper personMapper;

    @Override
    public RelationshipDTO toDTO(Relationship relationship) {
        if ( relationship == null ) {
            return null;
        }

        PersonSummaryDTO person1 = null;
        PersonSummaryDTO person2 = null;
        RelationshipType type = null;
        Long id = null;
        LocalDate startDate = null;
        LocalDate endDate = null;

        person1 = personMapper.toSummaryDTO( relationship.getPerson1() );
        person2 = personMapper.toSummaryDTO( relationship.getPerson2() );
        type = relationship.getType();
        id = relationship.getId();
        startDate = relationship.getStartDate();
        endDate = relationship.getEndDate();

        boolean isActive = false;

        RelationshipDTO relationshipDTO = new RelationshipDTO( id, person1, person2, type, startDate, endDate, isActive );

        return relationshipDTO;
    }

    @Override
    public Relationship toEntity(RelationshipDTO relationshipDTO) {
        if ( relationshipDTO == null ) {
            return null;
        }

        Relationship.RelationshipBuilder relationship = Relationship.builder();

        relationship.person1( personMapper.fromSummaryDTO( relationshipDTO.person1() ) );
        relationship.person2( personMapper.fromSummaryDTO( relationshipDTO.person2() ) );
        relationship.type( relationshipDTO.type() );
        relationship.startDate( relationshipDTO.startDate() );
        relationship.endDate( relationshipDTO.endDate() );
        relationship.isActive( relationshipDTO.isActive() );

        return relationship.build();
    }

    @Override
    public List<RelationshipDTO> toDTOList(List<Relationship> relationships) {
        if ( relationships == null ) {
            return null;
        }

        List<RelationshipDTO> list = new ArrayList<RelationshipDTO>( relationships.size() );
        for ( Relationship relationship : relationships ) {
            list.add( toDTO( relationship ) );
        }

        return list;
    }
}
