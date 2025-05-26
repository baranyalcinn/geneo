import React, { useRef, useEffect } from 'react';
import { Person, Gender } from '../types/Person';
import LoadingIndicator from './ui/LoadingIndicator';
import ErrorMessage from './ui/ErrorMessage';
import {
  TextField, Button, MenuItem, Select, InputLabel, FormControl,
  Box, Tooltip, Typography, Avatar, Paper,
  InputAdornment, Fade, Alert, Chip, Zoom, Stack
} from '@mui/material';
import { useTheme, alpha } from '@mui/material/styles';

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
import { useLanguage } from '../context/LanguageContext';

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
  const nameInputRef = useRef<HTMLInputElement>(null);
  const theme = useTheme();
  const { t } = useLanguage();
  const isDarkMode = theme.palette.mode === 'dark';
  const isEditing = !!person.id;

  useEffect(() => {
    nameInputRef.current?.focus();
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLFormElement>) => {
    if (e.key === 'Escape') {
      onCancel();
    } else if (e.key === 'Enter' && e.ctrlKey) {
      e.preventDefault();
      onSave();
    }
  };

  // Form alanları için ortak stiller
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

      <Box sx={{ mb: 3 }}>
        <Paper elevation={0} sx={formSectionStyle}>
          <Box sx={sectionHeaderStyle}>
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
          </Box>

          <Stack spacing={2}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                id="firstName"
                name="firstName"
                label={t('firstName')}
                inputRef={nameInputRef}
                value={person.firstName || ''}
                onChange={e => setPerson({ ...person, firstName: e.target.value })}
                fullWidth
                required
                autoFocus
                placeholder={t('enterFirstName')}
                variant="outlined"
                sx={inputBaseStyle}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonIcon 
                        fontSize="small" 
                        sx={{ color: theme.palette.primary.main }}
                      />
                    </InputAdornment>
                  ),
                }}
              />
              <TextField
                id="lastName"
                name="lastName"
                label={t('lastName')}
                value={person.lastName || ''}
                onChange={e => setPerson({ ...person, lastName: e.target.value })}
                fullWidth
                required
                placeholder={t('enterLastName')}
                variant="outlined"
                sx={inputBaseStyle}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <PersonIcon 
                        fontSize="small" 
                        sx={{ color: theme.palette.primary.main, opacity: 0.7 }}
                      />
                    </InputAdornment>
                  ),
                }}
              />
            </Stack>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <FormControl fullWidth sx={inputBaseStyle}>
                <InputLabel id="gender-label" required>
                  {t('gender')}
                </InputLabel>
                <Select
                  labelId="gender-label"
                  id="gender"
                  name="gender"
                  value={person.gender || ''}
                  label={t('gender')}
                  onChange={e => setPerson({ ...person, gender: e.target.value as Gender })}
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
                  <MenuItem value="ERKEK" sx={{ 
                    borderRadius: 1, 
                    m: 0.5,
                    p: 1.5,
                    '&:hover': {
                      backgroundColor: alpha('#2196f3', 0.1)
                    }
                  }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ 
                        width: 30, 
                        height: 30, 
                        backgroundColor: alpha('#2196f3', 0.1) 
                      }}>
                        <MaleIcon sx={{ color: '#2196f3', fontSize: 18 }} />
                      </Avatar>
                      <Typography>{t('male')}</Typography>
                    </Box>
                  </MenuItem>
                  <MenuItem value="KADIN" sx={{ 
                    borderRadius: 1, 
                    m: 0.5,
                    p: 1.5,
                    '&:hover': {
                      backgroundColor: alpha('#e91e63', 0.1)
                    }
                  }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ 
                        width: 30, 
                        height: 30, 
                        backgroundColor: alpha('#e91e63', 0.1) 
                      }}>
                        <FemaleIcon sx={{ color: '#e91e63', fontSize: 18 }} />
                      </Avatar>
                      <Typography>{t('female')}</Typography>
                    </Box>
                  </MenuItem>
                </Select>
              </FormControl>

              <TextField
                id="birthDate"
                name="birthDate"
                label={t('birthDate')}
                type="date"
                value={person.birthDate || ''}
                onChange={e => setPerson({ ...person, birthDate: e.target.value })}
                fullWidth
                InputLabelProps={{ shrink: true }}
                variant="outlined"
                sx={inputBaseStyle}
                placeholder="GG/AA/YYYY"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <CakeIcon 
                        fontSize="small" 
                        sx={{ color: theme.palette.success.main }}
                      />
                    </InputAdornment>
                  ),
                }}
              />

              <TextField
                id="deathDate"
                name="deathDate"
                label={t('deathDate')}
                type="date"
                value={person.deathDate || ''}
                onChange={e => setPerson({ ...person, deathDate: e.target.value })}
                fullWidth
                InputLabelProps={{ shrink: true }}
                variant="outlined"
                sx={inputBaseStyle}
                placeholder="GG/AA/YYYY"
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SentimentVeryDissatisfiedIcon 
                        fontSize="small" 
                        sx={{ color: theme.palette.grey[500] }}
                      />
                    </InputAdornment>
                  ),
                }}
              />
            </Stack>
          </Stack>
        </Paper>
      </Box>

      <Box sx={{ mb: 3 }}>
        <Paper elevation={0} sx={formSectionStyle}>
          <Box sx={sectionHeaderStyle}>
            <GroupsIcon color="primary" />
            <Typography variant="subtitle1" fontWeight={600}>
              {t('familyRelations')}
            </Typography>
          </Box>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <FormControl fullWidth sx={inputBaseStyle}>
              <InputLabel id="mother-label">
                {t('mother')}
              </InputLabel>
              <Select
                labelId="mother-label"
                id="motherId"
                name="motherId"
                value={person.motherId != null ? String(person.motherId) : ''}
                label={t('mother')}
                onChange={e => setPerson({ ...person, motherId: e.target.value === '' ? null : Number(e.target.value) })}
                startAdornment={
                  <InputAdornment position="start">
                    <WomanIcon sx={{ color: '#e91e63' }} />
                  </InputAdornment>
                }
                MenuProps={{
                  PaperProps: {
                    sx: {
                      mt: 1,
                      maxHeight: 300,
                      borderRadius: 2,
                      boxShadow: '0 8px 16px rgba(0,0,0,0.15)'
                    }
                  }
                }}
              >
                <MenuItem value="" sx={{ 
                  borderRadius: 1, 
                  m: 0.5,
                  p: 1
                }}>
                  <Typography color="text.secondary" sx={{ fontStyle: 'italic' }}>
                    {t('notSpecified')}
                  </Typography>
                </MenuItem>
                
                {availableMothers.map(mother => (
                  <MenuItem key={mother.id} value={String(mother.id)} sx={{ 
                    borderRadius: 1, 
                    m: 0.5,
                    p: 1
                  }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ 
                        width: 28, 
                        height: 28, 
                        backgroundColor: alpha('#e91e63', 0.1) 
                      }}>
                        <Typography variant="caption" fontWeight={500} sx={{ color: '#e91e63' }}>
                          {mother.firstName.charAt(0)}{mother.lastName.charAt(0)}
                        </Typography>
                      </Avatar>
                      <Typography>
                        {mother.firstName} {mother.lastName}
                      </Typography>
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            
            <FormControl fullWidth sx={inputBaseStyle}>
              <InputLabel id="father-label">
                {t('father')}
              </InputLabel>
              <Select
                labelId="father-label"
                id="fatherId"
                name="fatherId"
                value={person.fatherId != null ? String(person.fatherId) : ''}
                label={t('father')}
                onChange={e => setPerson({ ...person, fatherId: e.target.value === '' ? null : Number(e.target.value) })}
                startAdornment={
                  <InputAdornment position="start">
                    <ManIcon sx={{ color: '#2196f3' }} />
                  </InputAdornment>
                }
                MenuProps={{
                  PaperProps: {
                    sx: {
                      mt: 1,
                      maxHeight: 300,
                      borderRadius: 2,
                      boxShadow: '0 8px 16px rgba(0,0,0,0.15)'
                    }
                  }
                }}
              >
                <MenuItem value="" sx={{ 
                  borderRadius: 1, 
                  m: 0.5,
                  p: 1
                }}>
                  <Typography color="text.secondary" sx={{ fontStyle: 'italic' }}>
                    {t('notSpecified')}
                  </Typography>
                </MenuItem>
                
                {availableFathers.map(father => (
                  <MenuItem key={father.id} value={String(father.id)} sx={{ 
                    borderRadius: 1, 
                    m: 0.5,
                    p: 1
                  }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ 
                        width: 28, 
                        height: 28, 
                        backgroundColor: alpha('#2196f3', 0.1) 
                      }}>
                        <Typography variant="caption" fontWeight={500} sx={{ color: '#2196f3' }}>
                          {father.firstName.charAt(0)}{father.lastName.charAt(0)}
                        </Typography>
                      </Avatar>
                      <Typography>
                        {father.firstName} {father.lastName}
                      </Typography>
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            
            <FormControl fullWidth sx={inputBaseStyle}>
              <InputLabel id="spouse-label">
                {t('spouse')}
              </InputLabel>
              <Select
                labelId="spouse-label"
                id="spouseId"
                name="spouseId"
                value={person.spouseId != null ? String(person.spouseId) : ''}
                label={t('spouse')}
                onChange={e => setPerson({ ...person, spouseId: e.target.value === '' ? null : Number(e.target.value) })}
                startAdornment={
                  <InputAdornment position="start">
                    <FamilyRestroomIcon sx={{ color: theme.palette.warning.main }} />
                  </InputAdornment>
                }
                MenuProps={{
                  PaperProps: {
                    sx: {
                      mt: 1,
                      maxHeight: 300,
                      borderRadius: 2,
                      boxShadow: '0 8px 16px rgba(0,0,0,0.15)'
                    }
                  }
                }}
              >
                <MenuItem value="" sx={{ 
                  borderRadius: 1, 
                  m: 0.5,
                  p: 1
                }}>
                  <Typography color="text.secondary" sx={{ fontStyle: 'italic' }}>
                    {t('notSpecified')}
                  </Typography>
                </MenuItem>
                
                {availableSpouses.map(spouse => (
                  <MenuItem key={spouse.id} value={String(spouse.id)} sx={{ 
                    borderRadius: 1, 
                    m: 0.5,
                    p: 1
                  }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Avatar sx={{ 
                        width: 28, 
                        height: 28, 
                        backgroundColor: alpha(spouse.gender === 'ERKEK' ? '#2196f3' : '#e91e63', 0.1) 
                      }}>
                        <Typography 
                          variant="caption" 
                          fontWeight={500} 
                          sx={{ color: spouse.gender === 'ERKEK' ? '#2196f3' : '#e91e63' }}
                        >
                          {spouse.firstName.charAt(0)}{spouse.lastName.charAt(0)}
                        </Typography>
                      </Avatar>
                      <Typography>
                        {spouse.firstName} {spouse.lastName}
                      </Typography>
                    </Box>
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </Paper>
      </Box>

      {/* Butonlar */}
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'flex-end', 
        gap: 2, 
        mt: 4,
        position: 'relative',
        borderTop: `1px solid ${alpha(theme.palette.divider, 0.1)}`,
        pt: 3
      }}>
        {isEditing && (
          <Chip
            label={`ID: ${person.id}`}
            variant="outlined"
            size="small"
            sx={{ 
              position: 'absolute',
              left: 0,
              top: '24px',
              borderRadius: '8px',
              fontSize: '0.75rem',
              backgroundColor: isDarkMode 
                ? alpha(theme.palette.background.paper, 0.1) 
                : alpha(theme.palette.background.paper, 0.6),
              borderColor: isDarkMode 
                ? alpha(theme.palette.divider, 0.2) 
                : theme.palette.divider,
            }}
          />
        )}
        
        <Zoom in={true} style={{ transitionDelay: '200ms' }}>
          <Tooltip title={t('cancel')} arrow>
            <Button
              variant="outlined"
              onClick={onCancel}
              startIcon={<CancelIcon />}
              sx={{ 
                borderRadius: '12px',
                px: 3,
                py: 1,
                boxShadow: 'none',
                color: theme.palette.grey[600],
                borderColor: theme.palette.grey[300],
                '&:hover': {
                  borderColor: theme.palette.grey[400],
                  backgroundColor: alpha(theme.palette.grey[500], 0.05)
                }
              }}
            >
              {t('cancel')}
            </Button>
          </Tooltip>
        </Zoom>
        
        <Zoom in={true} style={{ transitionDelay: '300ms' }}>
          <Tooltip title={isEditing ? t('save') : t('add')} arrow>
            <Button
              variant="contained"
              color="primary"
              onClick={onSave}
              startIcon={isEditing ? <SaveIcon /> : <PersonAddIcon />}
              disabled={!person.firstName || !person.lastName || !person.gender}
              sx={{ 
                borderRadius: '12px',
                px: 3,
                py: 1,
                boxShadow: isDarkMode 
                  ? '0 4px 12px rgba(33, 150, 243, 0.3)' 
                  : '0 4px 12px rgba(33, 150, 243, 0.2)',
                background: isDarkMode
                  ? 'linear-gradient(45deg, #1976d2 0%, #2196f3 100%)'
                  : 'linear-gradient(45deg, #2196f3 0%, #64b5f6 100%)',
                '&:hover': {
                  boxShadow: isDarkMode 
                    ? '0 6px 14px rgba(33, 150, 243, 0.4)' 
                    : '0 6px 14px rgba(33, 150, 243, 0.25)',
                  background: isDarkMode
                    ? 'linear-gradient(45deg, #1976d2 30%, #2196f3 100%)'
                    : 'linear-gradient(45deg, #2196f3 30%, #64b5f6 100%)',
                }
              }}
            >
              {isEditing ? t('saveChanges') : t('addPerson')}
            </Button>
          </Tooltip>
        </Zoom>
      </Box>
    </Box>
  );
};

export default PersonForm; 