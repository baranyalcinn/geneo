import React, { useRef, useEffect, FC } from 'react';
import { Person, Gender } from '../types/Person';
import LoadingIndicator from './ui/LoadingIndicator';
import ErrorMessage from './ui/ErrorMessage';
import {
  TextField, Button, MenuItem, Select, InputLabel, FormControl, SelectChangeEvent,
  Box, Typography, Paper, InputAdornment, Fade, Alert, Chip, Stack, Card, CardContent
} from '@mui/material';
import { useTheme, alpha } from '@mui/material/styles';

// İkonlar
import PersonIcon from '@mui/icons-material/Person';
import ManIcon from '@mui/icons-material/Man';
import WomanIcon from '@mui/icons-material/Woman';
import CakeIcon from '@mui/icons-material/Cake';
import SentimentVeryDissatisfiedIcon from '@mui/icons-material/SentimentVeryDissatisfied';
import SaveIcon from '@mui/icons-material/Save';
import CancelIcon from '@mui/icons-material/Cancel';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import BadgeIcon from '@mui/icons-material/Badge';
import GroupsIcon from '@mui/icons-material/Groups';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import { useLanguage } from '../context/LanguageContext';

// import PersonRelationSelect from './PersonRelationSelect'; // Geçici olarak yorum satırı yapıldı

interface BasicInfoSectionProps {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  nameInputRef: React.RefObject<HTMLInputElement | null>;
  t: (key: string) => string;
}

const BasicInfoSection: FC<BasicInfoSectionProps> = ({
  person, setPerson, nameInputRef, t
}) => {
  const theme = useTheme();
  
  return (
    <Card sx={{ mb: 2, borderRadius: 2, boxShadow: 2 }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <BadgeIcon color="primary" sx={{ mr: 1 }} />
          <Typography variant="h6" fontWeight={600}>
            {t('basicInformation')}
          </Typography>
          <Chip
            label={t('required')}
            size="small"
            color="primary"
            variant="outlined"
            sx={{ ml: 'auto', height: 24 }}
          />
        </Box>

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
              placeholder={t('enterFirstName')}
              variant="outlined"
              size="small"
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon fontSize="small" color="primary" />
                  </InputAdornment>
                ),
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
              placeholder={t('enterLastName')}
              variant="outlined"
              size="small"
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon fontSize="small" color="action" />
                  </InputAdornment>
                ),
              }}
            />
          </Stack>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <FormControl fullWidth size="small">
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
              >
                <MenuItem value="">
                  <em>{t('selectGender') || 'Cinsiyet Seçin'}</em>
                </MenuItem>
                <MenuItem value={Gender.MALE}>
                  <ManIcon sx={{ mr: 1, color: theme.palette.info.main }} fontSize="small" /> 
                  {t('male')}
                </MenuItem>
                <MenuItem value={Gender.FEMALE}>
                  <WomanIcon sx={{ mr: 1, color: theme.palette.error.main }} fontSize="small" /> 
                  {t('female')}
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
              size="small"
              InputLabelProps={{
                shrink: true,
              }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <CakeIcon fontSize="small" color="secondary" />
                  </InputAdornment>
                ),
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
            size="small"
            placeholder={t('enterPlaceOfBirth')}
            variant="outlined"
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <LocationOnIcon fontSize="small" color="action" />
                </InputAdornment>
              ),
            }}
          />
        </Stack>
      </CardContent>
    </Card>
  );
};

interface DeathInfoSectionProps {
  person: Partial<Person>;
  setPerson: (person: Partial<Person>) => void;
  t: (key: string) => string;
}

const DeathInfoSection: FC<DeathInfoSectionProps> = ({
  person, setPerson, t
}) => {
  return (
    <Card sx={{ mb: 2, borderRadius: 2, boxShadow: 1 }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <SentimentVeryDissatisfiedIcon sx={{ color: 'text.secondary', mr: 1 }} />
          <Typography variant="h6" fontWeight={600}>
            {t('deathInformation') || 'Ölüm Bilgileri'} 
          </Typography>
        </Box>
        
        <Stack spacing={2}>
          <TextField
            id="deathDate"
            name="deathDate"
            label={t('deathDate')}
            type="date"
            value={person.deathDate || ''}
            onChange={e => setPerson({ ...person, deathDate: e.target.value || null })}
            fullWidth
            size="small"
            InputLabelProps={{
              shrink: true,
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SentimentVeryDissatisfiedIcon fontSize="small" color="action" />
                </InputAdornment>
              ),
            }}
          />
          
          <TextField
            id="placeOfDeath"
            name="placeOfDeath"
            label={t('placeOfDeath')}
            value={person.placeOfDeath || ''}
            onChange={e => setPerson({ ...person, placeOfDeath: e.target.value })}
            fullWidth
            size="small"
            placeholder={t('enterPlaceOfDeath')}
            variant="outlined"
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <LocationOnIcon fontSize="small" color="action" />
                </InputAdornment>
              ),
            }}
          />
        </Stack>
      </CardContent>
    </Card>
  );
};

interface FamilyRelationsSectionProps {
  person: Partial<Person>;
  onRelationChange: (field: 'father' | 'mother' | 'spouse') => (event: SelectChangeEvent<string>) => void;
  availableFathers: Person[];
  availableMothers: Person[];
  availableSpouses: Person[];
  t: (key: string) => string;
}

const FamilyRelationsSection: FC<FamilyRelationsSectionProps> = ({
  person,
  onRelationChange,
  availableFathers, availableMothers, availableSpouses,
  t
}) => {
  const theme = useTheme();
  
  return (
    <Card sx={{ mb: 2, borderRadius: 2, boxShadow: 1 }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <GroupsIcon color="primary" sx={{ mr: 1 }} />
          <Typography variant="h6" fontWeight={600}>
            {t('familyInformation') || 'Aile Bilgileri'}
          </Typography>
        </Box>

        <Stack spacing={2}>
          { /* PersonRelationSelect kullanımı geçici olarak kaldırıldı, import çözülünce eklenecek */}
          {/* <PersonRelationSelect
            id="fatherId"
            label={t('father')}
            value={person.father?.id !== undefined ? String(person.father.id) : ''} 
            onChange={onRelationChange('father')}
            options={availableFathers}
            startIcon={<ManIcon sx={{ color: theme.palette.info.main }} fontSize="small" />}
          /> */}
          
          {/* <PersonRelationSelect
            id="motherId"
            label={t('mother')}
            value={person.mother?.id !== undefined ? String(person.mother.id) : ''} 
            onChange={onRelationChange('mother')}
            options={availableMothers}
            startIcon={<WomanIcon sx={{ color: theme.palette.error.main }} fontSize="small" />}
          /> */}
          
          {/* <PersonRelationSelect
            id="spouseId"
            label={t('spouse')}
            value={person.spouse?.id !== undefined ? String(person.spouse.id) : ''} 
            onChange={onRelationChange('spouse')}
            options={availableSpouses}
            startIcon={<GroupsIcon sx={{ color: theme.palette.warning.main }} fontSize="small" />}
          /> */}
          <Typography>PersonRelationSelect bileşeni geçici olarak kaldırıldı.</Typography> {/* Yer tutucu */}
        </Stack>
      </CardContent>
    </Card>
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
  const isEditing = !!person.id;

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

  const handleRelationChange = (field: 'father' | 'mother' | 'spouse') => (event: SelectChangeEvent<string>) => {
    const value = event.target.value;
    const idValue = value ? parseInt(value, 10) : undefined;
    
    let selectedPersonSummary: Person | undefined = undefined;
    if (idValue !== undefined) {
      const sourceArray = field === 'father' ? availableFathers :
                        field === 'mother' ? availableMothers :
                        availableSpouses;
      selectedPersonSummary = sourceArray.find(p => p.id === idValue);
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
      sx={{ width: '100%', maxWidth: 800, mx: 'auto' }}
    >
      {error && (
        <Fade in={!!error}>
          <Alert 
            severity="error" 
            variant="filled"
            sx={{ mb: 2, borderRadius: 2 }}
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

      <DeathInfoSection
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
      
      <Paper 
        elevation={2}
        sx={{
          p: 2,
          mt: 2,
          borderRadius: 2,
          position: 'sticky',
          bottom: 16,
          bgcolor: alpha(theme.palette.background.paper, 0.95),
          backdropFilter: 'blur(10px)',
        }}
      >
        <Stack 
          direction="row" 
          spacing={2} 
          justifyContent="flex-end"
        >
          <Button 
            variant="outlined" 
            onClick={onCancel} 
            startIcon={<CancelIcon />} 
            color="inherit"
            sx={{ borderRadius: 2 }}
          >
            {t('cancel')}
          </Button>
          
          <Button 
            variant="contained" 
            onClick={onSave} 
            startIcon={isEditing ? <SaveIcon /> : <PersonAddIcon />} 
            disabled={loading || !person.firstName || !person.lastName || !person.gender}
            color="primary"
            sx={{ borderRadius: 2 }}
          >
            {loading ? t('saving') : (isEditing ? t('saveChanges') : t('addPerson'))}
          </Button>
        </Stack>
      </Paper>
    </Box>
  );
};

export default PersonForm;