import React, { useRef, useEffect, FC } from 'react';
import { Person, Gender } from '../../types/Person';
import LoadingIndicator from '../ui/LoadingIndicator';
import ErrorMessage from '../ui/ErrorMessage';
import {
  TextField, Button, MenuItem, Select, InputLabel, FormControl, SelectChangeEvent,
  Box, Tooltip, Typography, Avatar, Paper,
  InputAdornment, Fade, Alert, Chip, Zoom, Stack
} from '@mui/material';
import { useTheme, alpha, Theme, SxProps } from '@mui/material/styles';

// İkonlar
import PersonIcon from '@mui/icons-material/Person';
import FamilyRestroomIcon from '@mui/icons-material/FamilyRestroom';
import ManIcon from '@mui/icons-material/Man';
import WomanIcon from '@mui/icons-material/Woman';
import MaleIcon from '@mui/icons-material/Male';
import FemaleIcon from '@mui/icons-material/Female';
import CakeIcon from '@mui/icons-material/Cake';
import SentimentVeryDissatisfiedIcon from '@mui/icons-material/SentimentVeryDissatisfied';
import SaveIcon from '@mui/icons-material/Save';
import CancelIcon from '@mui/icons-material/Cancel';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import BadgeIcon from '@mui/icons-material/Badge';
import GroupsIcon from '@mui/icons-material/Groups';
import { useLanguage } from '../../context/LanguageContext';

import FamilyInfoSection from './FamilyInfoSection';
import DatesSection from './DatesSection';
import PersonRelationSelect from './PersonRelationSelect';
import { StyledFormSection, StyledSectionHeader } from './StyledComponents';

// t fonksiyonunu string döndürecek şekilde saran yardımcı fonksiyon
const ensureString = (value: any): string => {
  if (value === null || value === undefined) return '';
  return String(value);
};

interface BasicInfoSectionProps {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  nameInputRef: React.RefObject<HTMLInputElement | null>;
  t: (key: string) => React.ReactNode;
}

const BasicInfoSection: FC<BasicInfoSectionProps> = ({
  person, setPerson, nameInputRef, t
}) => {
  const theme = useTheme();
  return (
    <StyledFormSection elevation={0} sx={{ mb:3 }}>
      <StyledSectionHeader>
        <BadgeIcon color="primary" />
        <Typography variant="subtitle1" fontWeight={600}>
          {t('basicInformation')}
        </Typography>
        <Chip
          label={t('required')}
          size="small"
          color="primary"
          variant="outlined"
          sx={{ ml: 'auto', height: 20, fontSize: '0.7rem' }}
        />
      </StyledSectionHeader>

      <Stack spacing={2}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <TextField
            id="firstName"
            name="firstName"
            label={t('firstName')}
            inputRef={nameInputRef}
            value={person.firstName || ''}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPerson({ ...person, firstName: e.target.value })}
            fullWidth
            required
            autoFocus
            placeholder={String(t('enterFirstName'))}
            variant="outlined"
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon
                      fontSize="small"
                      sx={{ color: theme.palette.primary.main }}
                    />
                  </InputAdornment>
                ),
              }
            }}
          />
          <TextField
            id="lastName"
            name="lastName"
            label={t('lastName')}
            value={person.lastName || ''}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPerson({ ...person, lastName: e.target.value })}
            fullWidth
            required
            placeholder={String(t('enterLastName'))}
            variant="outlined"
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon
                      fontSize="small"
                      sx={{ color: theme.palette.primary.main, opacity: 0.7 }}
                    />
                  </InputAdornment>
                ),
              }
            }}
          />
        </Stack>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
          <FormControl fullWidth>
            <InputLabel id="gender-label" required>
              {t('gender')}
            </InputLabel>
            <Select
              labelId="gender-label"
              id="gender"
              name="gender"
              value={person.gender || ''}
              label={t('gender')}
              onChange={(e) => setPerson({ ...person, gender: e.target.value as Gender | undefined })}
              required
              MenuProps={{
                PaperProps: {
                  sx: {
                    mt: 1,
                    borderRadius: 2,
                    boxShadow: '0 8px 16px rgba(0,0,0,0.15)'
                  }
                }
              }}
            >
               <MenuItem value="">
                <em>{t('selectGender') || 'Cinsiyet Seçin'}</em>
              </MenuItem>
              <MenuItem value={Gender.MALE}>
                <ManIcon sx={{ mr: 1, color: theme.palette.info.main }} /> {t('male')}
              </MenuItem>
              <MenuItem value={Gender.FEMALE}>
                <WomanIcon sx={{ mr: 1, color: theme.palette.error.main }} /> {t('female')}
              </MenuItem>
            </Select>
          </FormControl>

          <TextField
            id="birthDate"
            name="birthDate"
            label={t('birthDate')}
            type="date"
            value={person.birthDate || ''}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPerson({ ...person, birthDate: e.target.value || null })}
            fullWidth
            InputLabelProps={{
              shrink: true,
            }}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <CakeIcon
                      fontSize="small"
                      sx={{ color: theme.palette.secondary.main }}
                    />
                  </InputAdornment>
                ),
              }
            }}
          />
        </Stack>

        <TextField
          id="placeOfBirth"
          name="placeOfBirth"
          label={t('placeOfBirth')}
          value={person.placeOfBirth || ''}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPerson({ ...person, placeOfBirth: e.target.value })}
          fullWidth
          placeholder={String(t('enterPlaceOfBirth'))}
          variant="outlined"
        />
      </Stack>
    </StyledFormSection>
  );
};

interface AdditionalInfoSectionProps {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  t: (key: string) => React.ReactNode;
}

const AdditionalInfoSection: FC<AdditionalInfoSectionProps> = ({
  person, setPerson, t
}) => {
  const theme = useTheme();
  return (
    <>
      <StyledFormSection elevation={0} sx={{ mb:3 }}>
        <StyledSectionHeader>
           <SentimentVeryDissatisfiedIcon 
              sx={{ color: theme.palette.grey[600], mr: 1 }}
            />
          <Typography variant="subtitle1" fontWeight={600}>
            {t('deathInformation')}
          </Typography>
        </StyledSectionHeader>
        <Stack spacing={2}>
          <Stack direction="row" spacing={2} alignItems="center">
            <TextField
              id="deathDate"
              name="deathDate"
              label={t('deathDate')}
              type="date"
              value={person.deathDate || ''}
              onChange={e => setPerson({ ...person, deathDate: e.target.value || null })}
              fullWidth
              InputLabelProps={{
                shrink: true,
              }}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SentimentVeryDissatisfiedIcon 
                        fontSize="small" 
                        sx={{ color: theme.palette.grey[600] }}
                      />
                    </InputAdornment>
                  ),
                }
              }}
            />
            {person.deathDate && (
               <Zoom in={!!person.deathDate}>
                  <Tooltip title={t('clearDeathDate') || 'Ölüm Tarihini Temizle'}>
                      <Button 
                          variant='outlined' 
                          color='warning' 
                          size='small'
                          onClick={() => setPerson({ ...person, deathDate: undefined })}
                          sx={{ borderRadius: '8px', minWidth: 'auto', p: '6px'}}
                      >
                          <CancelIcon fontSize='small' />
                      </Button>
                  </Tooltip>
              </Zoom>
            )}
          </Stack>
          
          <TextField
            id="placeOfDeath"
            name="placeOfDeath"
            label={t('placeOfDeath')}
            value={person.placeOfDeath || ''}
            onChange={e => setPerson({ ...person, placeOfDeath: e.target.value })}
            fullWidth
            placeholder="Ölüm yeri"
            variant="outlined"
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon
                      fontSize="small"
                      sx={{ color: theme.palette.primary.main }}
                    />
                  </InputAdornment>
                ),
              }
            }}
          />
        </Stack>
      </StyledFormSection>
    </>
  );
};

interface FamilyRelationsSectionProps {
  person: Partial<Person>;
  onRelationChange: (field: 'father' | 'mother' | 'spouse') => (event: SelectChangeEvent<string>) => void;
  availableFathers: Person[];
  availableMothers: Person[];
  availableSpouses: Person[];
  t: (key: string) => React.ReactNode;
}

const FamilyRelationsSection: FC<FamilyRelationsSectionProps> = ({
  person,
  onRelationChange,
  availableFathers, availableMothers, availableSpouses,
  t
}) => {
  const theme = useTheme();
  return (
    <StyledFormSection elevation={0} sx={{ mb: 3 }}>
      <StyledSectionHeader>
        <GroupsIcon color="primary" />
        <Typography variant="subtitle1" fontWeight={600}>
          {t('familyInformation')}
        </Typography>
      </StyledSectionHeader>

      <Stack spacing={2}>
        <PersonRelationSelect
          id="fatherId"
          label={String(t('father'))}
          value={person.father?.id !== undefined ? String(person.father.id) : undefined}
          onChange={onRelationChange('father')}
          options={availableFathers}
          startIcon={<ManIcon sx={{ color: theme.palette.info.main }} />}
        />
        <PersonRelationSelect
          id="motherId"
          label={String(t('mother'))}
          value={person.mother?.id !== undefined ? String(person.mother.id) : undefined}
          onChange={onRelationChange('mother')}
          options={availableMothers}
          startIcon={<WomanIcon sx={{ color: theme.palette.error.main }} />}
        />
        <PersonRelationSelect
          id="spouseId"
          label={String(t('spouse'))}
          value={person.spouse?.id !== undefined ? String(person.spouse.id) : undefined}
          onChange={onRelationChange('spouse')}
          options={availableSpouses}
          startIcon={<FamilyRestroomIcon sx={{ color: theme.palette.warning.main }} />}
        />
      </Stack>
    </StyledFormSection>
  );
};

export interface PersonFormUIProps {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  availableFathers: Person[];
  availableMothers: Person[];
  availableSpouses: Person[];
  loading: boolean;
  error: string | null;
  onSave: () => void;
  onCancel: () => void;
}

const PersonForm: React.FC<PersonFormUIProps> = ({
  person, setPerson,
  availableFathers, availableMothers, availableSpouses,
  loading, error, onSave, onCancel
}) => {
  const nameInputRef = useRef<HTMLInputElement | null>(null);
  const theme = useTheme();
  const { t } = useLanguage();
  const isDarkMode = theme.palette.mode === 'dark';
  const isEditing = !!person.id;

  // t fonksiyonunu string döndürecek şekilde saran
  const tString = (key: string): string => ensureString(t(key));

  useEffect(() => {
    if (nameInputRef.current) {
        nameInputRef.current.focus();
    }
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLFormElement>) => {
    if (e.key === 'Escape') {
      onCancel();
    } else if (e.key === 'Enter' && e.ctrlKey) {
      e.preventDefault();
      onSave();
    }
  };

  const inputBaseStyle = {
    '& .MuiOutlinedInput-root': {
      borderRadius: '12px',
      transition: 'all 0.2s',
      backgroundColor: isDarkMode 
        ? alpha(theme.palette.background.paper, 0.1) 
        : alpha(theme.palette.background.paper, 0.8),
      '&:hover': {
        backgroundColor: isDarkMode
          ? alpha(theme.palette.background.paper, 0.2)
          : alpha(theme.palette.background.paper, 1),
      },
      '&.Mui-focused': {
        backgroundColor: isDarkMode
          ? alpha(theme.palette.background.paper, 0.2)
          : alpha(theme.palette.background.paper, 1),
        boxShadow: isDarkMode
          ? `0 0 0 2px ${alpha(theme.palette.primary.main, 0.4)}`
          : `0 0 0 2px ${alpha(theme.palette.primary.main, 0.2)}`
      }
    },
    '& .MuiInputLabel-root': {
      fontSize: '0.9rem',
      fontWeight: 500,
      transition: 'all 0.2s',
      color: isDarkMode 
        ? alpha(theme.palette.text.primary, 0.7) 
        : theme.palette.text.secondary
    },
    '& .MuiInputBase-input': {
      fontSize: '0.95rem',
      padding: '12px 14px'
    }
  };

  const formSectionStyle = {
    p: 2,
    borderRadius: 3,
    mb: 2,
    backgroundColor: isDarkMode 
      ? alpha(theme.palette.background.paper, 0.05) 
      : alpha(theme.palette.background.paper, 0.4),
    backdropFilter: 'blur(10px)',
    transition: 'all 0.2s ease',
    border: isDarkMode 
      ? `1px solid ${alpha(theme.palette.divider, 0.1)}` 
      : `1px solid ${alpha(theme.palette.divider, 0.08)}`,
    boxShadow: isDarkMode 
      ? 'none' 
      : '0 2px 20px rgba(0,0,0,0.03)',
    '&:hover': {
      backgroundColor: isDarkMode 
        ? alpha(theme.palette.background.paper, 0.08) 
        : alpha(theme.palette.background.paper, 0.6),
      boxShadow: isDarkMode 
        ? 'none' 
        : '0 4px 20px rgba(0,0,0,0.06)'
    }
  };

  const sectionHeaderStyle = {
    display: 'flex',
    alignItems: 'center',
    gap: 1,
    mb: 2,
    pb: 1,
    borderBottom: `1px solid ${alpha(theme.palette.divider, 0.1)}`
  };

  const handleRelationChange = (field: 'father' | 'mother' | 'spouse') => (event: SelectChangeEvent<string>) => {
    const value = event.target.value;
    const idValue = value ? parseInt(value, 10) : undefined;

    let selectedPersonSummary: Person | undefined = undefined;
    if (idValue !== undefined) {
      switch (field) {
        case 'father':
          selectedPersonSummary = availableFathers.find(p => p.id === idValue);
          break;
        case 'mother':
          selectedPersonSummary = availableMothers.find(p => p.id === idValue);
          break;
        case 'spouse':
          selectedPersonSummary = availableSpouses.find(p => p.id === idValue);
          break;
      }
    }
    setPerson({ ...person, [field]: selectedPersonSummary });
  };

  if (loading) {
    return <LoadingIndicator />;
  }

  return (
    <Box 
      component="form" 
      onKeyDown={handleKeyDown} 
      aria-busy={loading} 
      aria-live="polite"
      sx={{ width: '100%' }}
    >
      {error && (
        <Fade in={!!error}>
          <Alert 
            severity="error" 
            variant="filled"
            sx={{ 
              mb: 3, 
              borderRadius: 2,
              boxShadow: '0 4px 12px rgba(211,47,47,0.2)'
            }}
          >
            {error}
          </Alert>
        </Fade>
      )}

      <BasicInfoSection
        person={person}
        setPerson={setPerson}
        nameInputRef={nameInputRef}
        t={t}
      />

      <AdditionalInfoSection
        person={person}
        setPerson={setPerson}
        t={t}
      />

      <FamilyRelationsSection
        person={person}
        onRelationChange={handleRelationChange}
        availableFathers={availableFathers}
        availableMothers={availableMothers}
        availableSpouses={availableSpouses}
        t={t}
      />
      
      <Stack 
        direction="row" 
        spacing={2} 
        justifyContent="flex-end" 
        sx={{
          p: 2,
          position: 'sticky',
          bottom: 0,
          bgcolor: isDarkMode 
            ? alpha(theme.palette.background.default, 0.8) 
            : alpha(theme.palette.background.paper, 0.8),
          backdropFilter: 'blur(10px)',
          borderTop: `1px solid ${alpha(theme.palette.divider, 0.1)}`,
          zIndex: 1,
          mt: 2,
          borderRadius: '12px 12px 0 0',
          boxShadow: '0 -4px 12px rgba(0,0,0,0.05)'
        }}
      >
        <Button 
          variant="outlined" 
          onClick={onCancel} 
          startIcon={<CancelIcon />} 
          color="inherit"
          sx={{ borderRadius: '8px', textTransform: 'none', fontWeight: 500 }}
        >
          {t('cancel')}
        </Button>
        <Button 
          variant="contained" 
          onClick={onSave} 
          startIcon={isEditing ? <SaveIcon /> : <PersonAddIcon />} 
          disabled={loading || !person.firstName || !person.lastName || !person.gender}
          color="primary"
          sx={{ borderRadius: '8px', textTransform: 'none', fontWeight: 500 }}
        >
          {loading ? t('saving') : (isEditing ? t('saveChanges') : t('addPerson'))}
        </Button>
      </Stack>
    </Box>
  );
};

export default PersonForm; 