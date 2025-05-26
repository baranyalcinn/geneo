import { styled, alpha } from '@mui/material/styles';
import { Box, Paper, Select, TextField, FormControl } from '@mui/material';

export const StyledFormSection = styled(Paper)(({ theme }) => ({
  padding: theme.spacing(2),
  borderRadius: theme.shape.borderRadius * 3,
  marginBottom: theme.spacing(2),
  backgroundColor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.05 : 0.4),
  backdropFilter: 'blur(10px)',
  transition: 'all 0.2s ease',
  border: `1px solid ${alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.1 : 0.08)}`,
  boxShadow: theme.palette.mode === 'dark' ? 'none' : '0 2px 20px rgba(0,0,0,0.03)',

  '&:hover': {
    backgroundColor: alpha(theme.palette.background.paper, theme.palette.mode === 'dark' ? 0.08 : 0.6),
    boxShadow: theme.palette.mode === 'dark' ? 'none' : '0 4px 20px rgba(0,0,0,0.06)',
  },
}));

export const StyledSectionHeader = styled(Box)(({ theme }) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(1),
  marginBottom: theme.spacing(2),
  paddingBottom: theme.spacing(1),
  borderBottom: `1px solid ${alpha(theme.palette.divider, theme.palette.mode === 'dark' ? 0.1 : 0.08)}`,
}));

export const StyledTextField = styled(TextField)({});

export const StyledFormControl = styled(FormControl)({});

export const StyledSelect = styled(Select)({});

export const selectMenuProps = (isDarkMode: boolean) => ({
  PaperProps: {
    sx: {
      mt: 1,
      borderRadius: 2,
      boxShadow: '0 8px 16px rgba(0,0,0,0.15)',
      backgroundColor: isDarkMode ? 'background.default' : 'background.paper',
    }
  }
}); 