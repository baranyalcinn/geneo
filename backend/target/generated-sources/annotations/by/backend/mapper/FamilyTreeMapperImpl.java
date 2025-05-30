package by.backend.mapper;

import by.backend.model.dto.FamilyTreeDTO;
import by.backend.model.dto.PersonDTO;
import by.backend.model.dto.RelationshipDTO;
import by.backend.model.entity.FamilyTree;
import by.backend.model.entity.Person;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-05-27T22:05:08+0300",
    comments = "version: 1.6.3, compiler: javac, environment: Java 24.0.1 (Oracle Corporation)"
)
@Component
public class FamilyTreeMapperImpl implements FamilyTreeMapper {

    @Autowired
    private PersonMapper personMapper;

    @Override
    public FamilyTreeDTO createFamilyTreeDTO(List<PersonDTO> persons, List<RelationshipDTO> relationships, PersonDTO rootPerson) {
        if ( persons == null && relationships == null && rootPerson == null ) {
            return null;
        }

        FamilyTreeDTO familyTreeDTO = new FamilyTreeDTO();

        if ( rootPerson != null ) {
            familyTreeDTO.setRootPerson( rootPerson );
            familyTreeDTO.setId( rootPerson.id() );
        }
        List<PersonDTO> list = persons;
        if ( list != null ) {
            familyTreeDTO.setPersons( new ArrayList<PersonDTO>( list ) );
        }
        List<RelationshipDTO> list1 = relationships;
        if ( list1 != null ) {
            familyTreeDTO.setRelationships( new ArrayList<RelationshipDTO>( list1 ) );
        }

        return familyTreeDTO;
    }

    @Override
    public FamilyTreeDTO toDTO(FamilyTree familyTree) {
        if ( familyTree == null ) {
            return null;
        }

        FamilyTreeDTO familyTreeDTO = new FamilyTreeDTO();

        familyTreeDTO.setRootPerson( personMapper.toDTO( familyTree.getRootMember() ) );
        familyTreeDTO.setPersons( personSetToPersonDTOList( familyTree.getMembers() ) );
        familyTreeDTO.setName( familyTree.getName() );
        familyTreeDTO.setId( familyTree.getId() );

        return familyTreeDTO;
    }

    @Override
    public FamilyTree toEntity(FamilyTreeDTO dto) {
        if ( dto == null ) {
            return null;
        }

        FamilyTree familyTree = new FamilyTree();

        familyTree.setRootMember( personMapper.toEntity( dto.getRootPerson() ) );
        familyTree.setMembers( personDTOListToPersonSet( dto.getPersons() ) );
        familyTree.setName( dto.getName() );

        return familyTree;
    }

    protected List<PersonDTO> personSetToPersonDTOList(Set<Person> set) {
        if ( set == null ) {
            return null;
        }

        List<PersonDTO> list = new ArrayList<PersonDTO>( set.size() );
        for ( Person person : set ) {
            list.add( personMapper.toDTO( person ) );
        }

        return list;
    }

    protected Set<Person> personDTOListToPersonSet(List<PersonDTO> list) {
        if ( list == null ) {
            return null;
        }

        Set<Person> set = LinkedHashSet.newLinkedHashSet( list.size() );
        for ( PersonDTO personDTO : list ) {
            set.add( personMapper.toEntity( personDTO ) );
        }

        return set;
    }
}
