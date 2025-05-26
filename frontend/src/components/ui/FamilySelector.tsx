import React from 'react';
import styled from 'styled-components';

const SelectContainer = styled.div`
  margin: 20px 0;
  display: flex;
  justify-content: center;
`;

const StyledSelect = styled.select`
  padding: 10px 20px;
  font-size: 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  background-color: white;
  min-width: 200px;
  cursor: pointer;
  
  &:focus {
    outline: none;
    border-color: #4299e1;
  }
`;

interface FamilySelectorProps {
  families: { id: string; name: string }[];
  selectedFamilyId: string;
  onFamilySelect: (familyId: string) => void;
}

function FamilySelector({ families, selectedFamilyId, onFamilySelect }: FamilySelectorProps) {
  console.log('FamilySelector props:', { families, selectedFamilyId });

  const handleChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    console.log('Dropdown değişti:', e.target.value);
    onFamilySelect(e.target.value);
  };

  return (
    <SelectContainer>
      <StyledSelect
        value={selectedFamilyId}
        onChange={handleChange}
      >
        <option value="">Aile Seçiniz</option>
        {families.map((family) => (
          <option key={family.id} value={family.id}>
            {family.name}
          </option>
        ))}
      </StyledSelect>
    </SelectContainer>
  );
}

export default FamilySelector; 