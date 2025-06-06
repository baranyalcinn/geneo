import { styled, alpha } from '@mui/material/styles';
import { Box, Paper, Select, TextField, FormControl } from '@mui/material';

export const StyledFormSection = styled(Paper, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})< { isDarkMode?: boolean }>(({ theme, isDarkMode }) => ({
  padding: theme.spacing(2),
  borderRadius: `${theme.shape.borderRadius}px`,
  marginBottom: theme.spacing(2),
  backgroundColor: alpha(theme.palette.background.paper, isDarkMode ? 0.05 : 0.4),
  backdropFilter: 'blur(10px)',
  transition: 'all 0.2s ease',
  border: `1px solid ${alpha(theme.palette.divider, isDarkMode ? 0.1 : 0.08)}`,
  boxShadow: isDarkMode ? 'none' : '0 2px 20px rgba(0,0,0,0.03)',

  '&:hover': {
    backgroundColor: alpha(theme.palette.background.paper, isDarkMode ? 0.08 : 0.6),
    boxShadow: isDarkMode ? 'none' : '0 4px 20px rgba(0,0,0,0.06)',
  },
}));

export const StyledSectionHeader = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})< { isDarkMode?: boolean }>(({ theme, isDarkMode }) => ({
  display: 'flex',
  alignItems: 'center',
  gap: theme.spacing(1),
  marginBottom: theme.spacing(2),
  paddingBottom: theme.spacing(1),
  borderBottom: `1px solid ${alpha(theme.palette.divider, isDarkMode ? 0.1 : 0.08)}`,
}));

export const StyledTextField = styled(TextField, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})< { isDarkMode?: boolean }>(({ theme, isDarkMode }) => ({
  // isDarkMode true ise uygulanacak özel stiller buraya eklenebilir.
  // Örneğin:
  // ...(isDarkMode && {
  //   backgroundColor: alpha(theme.palette.background.default, 0.1),
  //   '.MuiOutlinedInput-root': {
  //     color: theme.palette.text.primary,
  //   }
  // })
}));

export const StyledFormControl = styled(FormControl, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})< { isDarkMode?: boolean }>(({ theme, isDarkMode }) => ({
  // isDarkMode'a bağlı özel stiller buraya eklenebilir, eğer gerekliyse
}));

export const StyledSelect = styled(Select, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})< { isDarkMode?: boolean }>(({ theme, isDarkMode }) => ({
  // isDarkMode'a bağlı özel stiller buraya eklenebilir, eğer gerekliyse
  // Örneğin: backgroundColor: isDarkMode ? theme.palette.grey[700] : theme.palette.background.paper,
}));

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