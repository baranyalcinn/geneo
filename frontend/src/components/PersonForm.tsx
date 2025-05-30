import React, { useRef, useEffect, FC } from 'react';
import { Person, Gender } from '../types/Person';
import LoadingIndicator from './ui/LoadingIndicator';
import ErrorMessage from './ui/ErrorMessage';
import {
  TextField, Button, MenuItem, Select, InputLabel, FormControl, SelectChangeEvent,
  Box, Typography, Paper, InputAdornment, Fade, Alert, Chip, Stack, Card, CardContent,
  Container, Divider, IconButton, Tooltip, LinearProgress
} from '@mui/material';
import { useTheme, alpha, styled } from '@mui/material/styles';

// Icons
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
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import FavoriteIcon from '@mui/icons-material/Favorite';
import { useLanguage } from '../context/LanguageContext';

// Styled Components
const GradientCard = styled(Card)(({ theme }) => ({
  background: `linear-gradient(135deg, ${alpha(theme.palette.primary.main, 0.05)} 0%, ${alpha(theme.palette.secondary.main, 0.05)} 100%)`,
  borderRadius: 20,
  border: `1px solid ${alpha(theme.palette.primary.main, 0.1)}`,
  backdropFilter: 'blur(20px)',
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  position: 'relative',
  overflow: 'hidden',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: `0 20px 40px ${alpha(theme.palette.primary.main, 0.1)}`,
    border: `1px solid ${alpha(theme.palette.primary.main, 0.2)}`,
  },
  '&::before': {
    content: '""',
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    height: 4,
    background: `linear-gradient(90deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
  }
}));

const ModernTextField = styled(TextField)(({ theme }) => ({
  '& .MuiOutlinedInput-root': {
    borderRadius: 16,
    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
    backgroundColor: alpha(theme.palette.background.paper, 0.8),
    backdropFilter: 'blur(10px)',
    '&:hover': {
      backgroundColor: alpha(theme.palette.background.paper, 0.9),
      '& .MuiOutlinedInput-notchedOutline': {
        borderColor: alpha(theme.palette.primary.main, 0.5),
      },
    },
    '&.Mui-focused': {
      backgroundColor: theme.palette.background.paper,
      transform: 'scale(1.02)',
      '& .MuiOutlinedInput-notchedOutline': {
        borderWidth: 2,
        borderColor: theme.palette.primary.main,
        boxShadow: `0 0 20px ${alpha(theme.palette.primary.main, 0.2)}`,
      },
    },
  },
  '& .MuiInputLabel-root': {
    fontWeight: 500,
    '&.Mui-focused': {
      color: theme.palette.primary.main,
      fontWeight: 600,
    },
  },
}));

const ModernSelect = styled(Select)(({ theme }) => ({
  borderRadius: 16,
  backgroundColor: alpha(theme.palette.background.paper, 0.8),
  backdropFilter: 'blur(10px)',
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  '&:hover': {
    backgroundColor: alpha(theme.palette.background.paper, 0.9),
  },
  '&.Mui-focused': {
    backgroundColor: theme.palette.background.paper,
    transform: 'scale(1.02)',
  },
  '& .MuiOutlinedInput-notchedOutline': {
    borderRadius: 16,
  },
  '&:hover .MuiOutlinedInput-notchedOutline': {
    borderColor: alpha(theme.palette.primary.main, 0.5),
  },
  '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
    borderWidth: 2,
    borderColor: theme.palette.primary.main,
    boxShadow: `0 0 20px ${alpha(theme.palette.primary.main, 0.2)}`,
  },
}));

const AnimatedButton = styled(Button)(({ theme }) => ({
  borderRadius: 16,
  padding: '12px 32px',
  fontWeight: 600,
  textTransform: 'none',
  fontSize: '1rem',
  transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
  position: 'relative',
  overflow: 'hidden',
  '&::before': {
    content: '""',
    position: 'absolute',
    top: 0,
    left: '-100%',
    width: '100%',
    height: '100%',
    background: `linear-gradient(90deg, transparent, ${alpha(theme.palette.common.white, 0.2)}, transparent)`,
    transition: 'left 0.5s',
  },
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: `0 10px 30px ${alpha(theme.palette.primary.main, 0.3)}`,
    '&::before': {
      left: '100%',
    },
  },
  '&:active': {
    transform: 'translateY(0)',
  },
}));

const SectionHeader = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  marginBottom: theme.spacing(3),
  position: 'relative',
  '&::after': {
    content: '""',
    position: 'absolute',
    bottom: -8,
    left: 0,
    width: 60,
    height: 3,
    background: `linear-gradient(90deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
    borderRadius: 2,
  },
}));

const FloatingActionArea = styled(Paper)(({ theme }) => ({
  position: 'sticky',
  bottom: 24,
  padding: theme.spacing(2.5),
  marginTop: theme.spacing(4),
  borderRadius: 20,
  background: `linear-gradient(135deg, ${alpha(theme.palette.background.paper, 0.95)} 0%, ${alpha(theme.palette.background.paper, 0.98)} 100%)`,
  backdropFilter: 'blur(20px)',
  border: `1px solid ${alpha(theme.palette.divider, 0.1)}`,
  boxShadow: `0 20px 60px ${alpha(theme.palette.common.black, 0.1)}`,
  zIndex: 100,
}));

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
    <GradientCard sx={{ mb: 4 }}>
      <CardContent sx={{ p: 4 }}>
        <SectionHeader>
          <BadgeIcon sx={{ 
            color: theme.palette.primary.main, 
            mr: 2, 
            fontSize: 28,
            filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
          }} />
          <Typography variant="h5" fontWeight={700} sx={{ 
            background: `linear-gradient(45deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            {t('basicInformation')}
          </Typography>
          <Chip
            label={t('required')}
            size="small"
            sx={{ 
              ml: 'auto',
              background: `linear-gradient(45deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
              color: 'white',
              fontWeight: 600,
              '&:hover': {
                transform: 'scale(1.05)',
              }
            }}
          />
        </SectionHeader>

        <Stack spacing={3}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
            <ModernTextField
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
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonIcon sx={{ 
                        color: theme.palette.primary.main,
                        filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
                      }} />
                    </InputAdornment>
                  ),
                },
              }}
            />
            
            <ModernTextField
              id="lastName"
              name="lastName"
              label={t('lastName')}
              value={person.lastName || ''}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPerson({ ...person, lastName: e.target.value })}
              fullWidth
              required
              placeholder={t('enterLastName')}
              variant="outlined"
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonIcon sx={{ 
                        color: theme.palette.secondary.main,
                        filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
                      }} />
                    </InputAdornment>
                  ),
                },
              }}
            />
          </Stack>

          <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
            <FormControl fullWidth>
              <InputLabel id="gender-label" required sx={{ fontWeight: 500 }}>
                {t('gender')}
              </InputLabel>
              <ModernSelect
                labelId="gender-label"
                id="gender"
                name="gender"
                value={person.gender || ''}
                label={t('gender')}
                onChange={(e) => setPerson({ ...person, gender: e.target.value as Gender | undefined })}
                required
                startAdornment={
                  <InputAdornment position="start">
                    <AutoAwesomeIcon sx={{ 
                      color: theme.palette.info.main,
                      filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
                    }} />
                  </InputAdornment>
                }
              >
                <MenuItem value="">
                  <em>{t('selectGender') || 'Cinsiyet Seçin'}</em>
                </MenuItem>
                <MenuItem value={Gender.MALE} sx={{ borderRadius: 2, m: 0.5 }}>
                  <ManIcon sx={{ mr: 2, color: theme.palette.info.main }} /> 
                  {t('male')}
                </MenuItem>
                <MenuItem value={Gender.FEMALE} sx={{ borderRadius: 2, m: 0.5 }}>
                  <WomanIcon sx={{ mr: 2, color: theme.palette.error.main }} /> 
                  {t('female')}
                </MenuItem>
              </ModernSelect>
            </FormControl>

            <ModernTextField
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
                      <CakeIcon sx={{ 
                        color: theme.palette.warning.main,
                        filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
                      }} />
                    </InputAdornment>
                  ),
                },
              }}
            />
          </Stack>

          <ModernTextField
            id="placeOfBirth"
            name="placeOfBirth"
            label={t('placeOfBirth')}
            value={person.placeOfBirth || ''}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => setPerson({ ...person, placeOfBirth: e.target.value })}
            fullWidth
            placeholder={t('enterPlaceOfBirth')}
            variant="outlined"
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <LocationOnIcon sx={{ 
                      color: theme.palette.success.main,
                      filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
                    }} />
                  </InputAdornment>
                ),
              },
            }}
          />
        </Stack>
      </CardContent>
    </GradientCard>
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
  const theme = useTheme();
  
  return (
    <GradientCard sx={{ mb: 4 }}>
      <CardContent sx={{ p: 4 }}>
        <SectionHeader>
          <SentimentVeryDissatisfiedIcon sx={{ 
            color: theme.palette.text.secondary, 
            mr: 2, 
            fontSize: 28,
            filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
          }} />
          <Typography variant="h5" fontWeight={700} color="text.secondary">
            {t('deathInformation') || 'Ölüm Bilgileri'}
          </Typography>
        </SectionHeader>
        
        <Stack spacing={3}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
            <ModernTextField
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
                      <SentimentVeryDissatisfiedIcon sx={{ 
                        color: theme.palette.text.secondary,
                        filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
                      }} />
                    </InputAdornment>
                  ),
                },
              }}
            />
            
            <ModernTextField
              id="placeOfDeath"
              name="placeOfDeath"
              label={t('placeOfDeath')}
              value={person.placeOfDeath || ''}
              onChange={e => setPerson({ ...person, placeOfDeath: e.target.value })}
              fullWidth
              placeholder={t('enterPlaceOfDeath')}
              variant="outlined"
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <LocationOnIcon sx={{ 
                        color: theme.palette.text.secondary,
                        filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))'
                      }} />
                    </InputAdornment>
                  ),
                },
              }}
            />
          </Stack>
        </Stack>
      </CardContent>
    </GradientCard>
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
    <GradientCard sx={{ mb: 4 }}>
      <CardContent sx={{ p: 4 }}>
        <SectionHeader>
          <GroupsIcon sx={{ 
            color: theme.palette.primary.main, 
            mr: 2, 
            fontSize: 28,
            filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
          }} />
          <Typography variant="h5" fontWeight={700} sx={{ 
            background: `linear-gradient(45deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            {t('familyInformation') || 'Aile Bilgileri'}
          </Typography>
        </SectionHeader>

        <Box sx={{ 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          minHeight: 120,
          background: `linear-gradient(135deg, ${alpha(theme.palette.info.main, 0.1)} 0%, ${alpha(theme.palette.warning.main, 0.1)} 100%)`,
          borderRadius: 4,
          border: `2px dashed ${alpha(theme.palette.primary.main, 0.3)}`,
        }}>
          <Stack alignItems="center" spacing={2}>
            <FavoriteIcon sx={{ 
              fontSize: 48, 
              color: alpha(theme.palette.primary.main, 0.6),
              filter: 'drop-shadow(0 2px 4px rgba(0,0,0,0.1))'
            }} />
            <Typography variant="h6" color="text.secondary" textAlign="center">
              PersonRelationSelect bileşeni geçici olarak kaldırıldı.
            </Typography>
            <Typography variant="body2" color="text.secondary" textAlign="center">
              Aile ilişkileri bölümü yakında eklenecek
            </Typography>
          </Stack>
        </Box>
      </CardContent>
    </GradientCard>
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
    return (
      <Container maxWidth="md" sx={{ py: 4 }}>
        <LinearProgress sx={{ borderRadius: 2, height: 6 }} />
        <LoadingIndicator />
      </Container>
    );
  }

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Box 
        component="form" 
        onKeyDown={handleKeyDown} 
        aria-busy={loading} 
        aria-live="polite"
        sx={{ 
          position: 'relative',
          '&::before': {
            content: '""',
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            background: `radial-gradient(circle at 20% 80%, ${alpha(theme.palette.primary.main, 0.03)} 0%, transparent 50%),
                        radial-gradient(circle at 80% 20%, ${alpha(theme.palette.secondary.main, 0.03)} 0%, transparent 50%)`,
            zIndex: -1,
            pointerEvents: 'none',
          }
        }}
      >
        {error && (
          <Fade in={!!error}>
            <Alert 
              severity="error" 
              variant="filled"
              sx={{ 
                mb: 4, 
                borderRadius: 3,
                boxShadow: `0 8px 32px ${alpha(theme.palette.error.main, 0.2)}`,
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
        
        <FloatingActionArea elevation={8}>
          <Stack 
            direction={{ xs: 'column', sm: 'row' }} 
            spacing={2} 
            justifyContent="flex-end"
          >
            <AnimatedButton 
              variant="outlined" 
              onClick={onCancel} 
              startIcon={<CancelIcon />} 
              color="inherit"
              sx={{ 
                borderWidth: 2,
                '&:hover': {
                  borderWidth: 2,
                  backgroundColor: alpha(theme.palette.text.primary, 0.04),
                }
              }}
            >
              {t('cancel')}
            </AnimatedButton>
            
            <AnimatedButton 
              variant="contained" 
              onClick={onSave} 
              startIcon={isEditing ? <SaveIcon /> : <PersonAddIcon />} 
              disabled={loading || !person.firstName || !person.lastName || !person.gender}
              sx={{ 
                background: `linear-gradient(45deg, ${theme.palette.primary.main}, ${theme.palette.secondary.main})`,
                '&:hover': {
                  background: `linear-gradient(45deg, ${theme.palette.primary.dark}, ${theme.palette.secondary.dark})`,
                },
                '&:disabled': {
                  background: alpha(theme.palette.action.disabled, 0.12),
                  color: theme.palette.action.disabled,
                }
              }}
            >
              {loading ? t('saving') : (isEditing ? t('saveChanges') : t('addPerson'))}
            </AnimatedButton>
          </Stack>
        </FloatingActionArea>
      </Box>
    </Container>
  );
};

export default PersonForm;