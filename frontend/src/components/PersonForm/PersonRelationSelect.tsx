import React, { FC } from 'react';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  SelectChangeEvent,
  Box,
  Avatar,
  Typography,
  InputAdornment,
  useTheme
} from '@mui/material';
import { alpha, SxProps, Theme } from '@mui/material/styles';
import { Person, Gender } from '../../types/Person'; // Adjusted import path
import { useLanguage } from '../../context/LanguageContext'; // Adjusted import path

interface PersonRelationSelectProps {
  id: string;
  label: string;
  value: string | undefined;
  onChange: (event: SelectChangeEvent<string>) => void;
  options: Person[];
  inputBaseStyle?: SxProps<Theme>;
  startIcon?: React.ReactNode;
  disabled?: boolean;
  required?: boolean;
}

const PersonRelationSelect: FC<PersonRelationSelectProps> = ({
  id,
  label,
  value,
  onChange,
  options,
  inputBaseStyle,
  startIcon,
  disabled,
  required,
}) => {
  const { t } = useLanguage();
  const theme = useTheme();

  const menuItemStyle = {
    borderRadius: 1,
    m: 0.5,
    p: 1,
    '&:hover': {
      backgroundColor: alpha(theme.palette.action.hover, 0.08) // Generic hover
    },
  };

  const renderAvatar = (personOption: Person) => {
    // Determine color based on gender - ensure Gender.MALE and Gender.FEMALE match your enum/type
    const avatarColor = personOption.gender === Gender.MALE 
      ? theme.palette.info.main 
      : personOption.gender === Gender.FEMALE 
        ? theme.palette.error.main 
        : theme.palette.grey[500];

    return (
      <Avatar sx={{
        width: 28,
        height: 28,
        backgroundColor: alpha(avatarColor, 0.1),
        mr: 1.5 
      }}>
        <Typography variant="caption" fontWeight={500} sx={{ color: avatarColor }}>
          {personOption.firstName?.charAt(0).toUpperCase()}
          {personOption.lastName?.charAt(0).toUpperCase()}
        </Typography>
      </Avatar>
    );
  };

  return (
    <FormControl fullWidth sx={inputBaseStyle} disabled={disabled} required={required}>
      <InputLabel id={`${id}-label`}>{label}</InputLabel>
      <Select
        labelId={`${id}-label`}
        id={id}
        name={id}
        value={value || ''}
        label={label}
        onChange={onChange}
        startAdornment={startIcon ? <InputAdornment position="start">{startIcon}</InputAdornment> : undefined}
        MenuProps={{
          PaperProps: {
            sx: {
              mt: 1,
              maxHeight: 220,
              borderRadius: 2,
              boxShadow: theme.shadows[3], // Using theme shadows
            },
          },
        }}
      >
        <MenuItem value="" sx={menuItemStyle}>
          <Typography color="text.secondary" sx={{ fontStyle: 'italic' }}>
            {t('select.relation.none')}
          </Typography>
        </MenuItem>
        {options.map(personOption => (
          <MenuItem key={personOption.id} value={String(personOption.id)} sx={menuItemStyle}>
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              {renderAvatar(personOption)}
              <Typography noWrap>
                {personOption.firstName} {personOption.lastName}
              </Typography>
            </Box>
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
};

export default PersonRelationSelect; 