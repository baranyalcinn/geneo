import React from 'react';
import { Person } from '../../types/Person';
import {
  Typography, Stack
} from '@mui/material';
import { useTheme } from '@mui/material/styles';
import CakeIcon from '@mui/icons-material/Cake';
import {
  StyledFormSection, StyledSectionHeader, StyledTextField, 
} from './StyledComponents';

interface DatesSectionProps {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  t: (key: string) => string;
}

const DatesSection: React.FC<DatesSectionProps> = ({ person, setPerson, t }) => {
  const theme = useTheme();
  const isDarkMode = theme.palette.mode === 'dark';

  return (
    <StyledFormSection elevation={0} isDarkMode={isDarkMode}>
      <StyledSectionHeader isDarkMode={isDarkMode}>
        <CakeIcon color="primary" />
        <Typography variant="subtitle1" fontWeight={600}>
          {t('dates')}
        </Typography>
      </StyledSectionHeader>
      <Stack spacing={2}>
        <StyledTextField
          id="birthDate"
          name="birthDate"
          label={t('birthDate')}
          type="date"
          value={person.birthDate || ''}
          onChange={e => setPerson({ ...person, birthDate: e.target.value || undefined })}
          InputLabelProps={{
            shrink: true,
          }}
          fullWidth
          isDarkMode={isDarkMode}
        />
        <StyledTextField
          id="deathDate"
          name="deathDate"
          label={t('deathDate')}
          type="date"
          value={person.deathDate || ''}
          onChange={e => setPerson({ ...person, deathDate: e.target.value || undefined })}
          InputLabelProps={{
            shrink: true,
          }}
          fullWidth
          isDarkMode={isDarkMode}
        />
      </Stack>
    </StyledFormSection>
  );
};

export default DatesSection; 