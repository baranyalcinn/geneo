import React from 'react';
import { Person } from '../../types/Person';
import {
  InputLabel, MenuItem, Typography, Tooltip,
  Stack
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import MaleIcon from '@mui/icons-material/Male';
import FemaleIcon from '@mui/icons-material/Female';
import {
  StyledFormSection, StyledSectionHeader, 
  StyledFormControl, StyledSelect, selectMenuProps
} from './StyledComponents';

interface FamilyInfoSectionProps {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  availableFathers: Person[];
  availableMothers: Person[];
  availableSpouses: Person[];
  isEditing: boolean;
  t: (key: string) => string;
}

const FamilyInfoSection: React.FC<FamilyInfoSectionProps> = ({ 
  person, setPerson, availableFathers, availableMothers, 
  availableSpouses, isEditing, t
}) => {
  const theme = useTheme();
  const isDarkMode = theme.palette.mode === 'dark';

  return (
    <StyledFormSection elevation={0} isDarkMode={isDarkMode}>
      <StyledSectionHeader isDarkMode={isDarkMode}>
        <FamilyRestroomIcon color="primary" />
        <Typography variant="subtitle1" fontWeight={600}>
          {t('familyInformation')}
        </Typography>
      </StyledSectionHeader>
      <Stack spacing={2}>
        <StyledFormControl fullWidth isDarkMode={isDarkMode}>
          <InputLabel id="father-label">{t('father')}</InputLabel>
          <StyledSelect
            labelId="father-label"
            id="fatherId"
            name="fatherId"
            value={person.father?.id || ''}
            label={t('father')}
            onChange={e => setPerson({ ...person, father: e.target.value ? availableFathers.find(p => p.id === Number(e.target.value)) : undefined })}
            MenuProps={selectMenuProps(isDarkMode)}
          >
            <MenuItem value=""><em>{t('unknown')}</em></MenuItem>
            {availableFathers.map(p => (
              <MenuItem key={p.id} value={p.id} sx={{ gap: 1 }}>
                <MaleIcon fontSize="small" sx={{ color: isDarkMode ? '#66b2ff' : '#007bff'}} /> 
                {p.firstName} {p.lastName}
              </MenuItem>
            ))}
          </StyledSelect>
        </StyledFormControl>

        <StyledFormControl fullWidth isDarkMode={isDarkMode}>
          <InputLabel id="mother-label">{t('mother')}</InputLabel>
          <StyledSelect
            labelId="mother-label"
            id="motherId"
            name="motherId"
            value={person.mother?.id || ''}
            label={t('mother')}
            onChange={e => setPerson({ ...person, mother: e.target.value ? availableMothers.find(p => p.id === Number(e.target.value)) : undefined })}
            MenuProps={selectMenuProps(isDarkMode)}
          >
            <MenuItem value=""><em>{t('unknown')}</em></MenuItem>
            {availableMothers.map(p => (
              <MenuItem key={p.id} value={p.id} sx={{ gap: 1 }}>
                <FemaleIcon fontSize="small" sx={{ color: isDarkMode ? '#ffb2d9' : '#ff66b2'}} />
                {p.firstName} {p.lastName}
              </MenuItem>
            ))}
          </StyledSelect>
        </StyledFormControl>

        <StyledFormControl fullWidth isDarkMode={isDarkMode}>
          <InputLabel id="spouse-label">{t('spouse')}</InputLabel>
          <StyledSelect
            labelId="spouse-label"
            id="spouseId"
            name="spouseId"
            value={person.spouse?.id || ''}
            label={t('spouse')}
            onChange={e => setPerson({ ...person, spouse: e.target.value ? availableSpouses.find(p => p.id === Number(e.target.value)) : undefined })}
            disabled={!isEditing} // Sadece düzenleme modunda aktif
            MenuProps={selectMenuProps(isDarkMode)}
          >
            <MenuItem value=""><em>{t('none')}</em></MenuItem>
            {availableSpouses.map(p => (
              <MenuItem key={p.id} value={p.id} sx={{ gap: 1 }}>
                {p.gender === 'ERKEK' 
                  ? <MaleIcon fontSize="small" sx={{ color: isDarkMode ? '#66b2ff' : '#007bff'}} /> 
                  : <FemaleIcon fontSize="small" sx={{ color: isDarkMode ? '#ffb2d9' : '#ff66b2'}} />}
                {p.firstName} {p.lastName}
              </MenuItem>
            ))}
          </StyledSelect>
          {!isEditing && (
            <Tooltip title={t('saveFirstToSelectSpouse')} arrow placement="top">
              <Typography variant="caption" sx={{ mt: 1, color: 'text.secondary' }}>
                ({t('saveFirstToSelectSpouseShort')})
              </Typography>
            </Tooltip>
          )}
        </StyledFormControl>
      </Stack>
    </StyledFormSection>
  );
};

export default FamilyInfoSection; 