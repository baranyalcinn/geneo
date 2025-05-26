import { createTheme } from '@mui/material/styles';
import { alpha } from '@mui/material/styles';

export const darkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary: {
      main: '#78909c',
      dark: '#546e7a',
      light: '#b0bec5',
    },
    secondary: {
      main: '#ad1457',
      dark: '#880e4f',
      light: '#d81b60',
    },
    background: {
      default: '#121212',
      paper: '#1e1e1e',
    },
    text: {
      primary: '#ffffff',
      secondary: '#b0bec5',
    },
    error: {
      main: '#ef4444',
      light: '#f87171',
      dark: '#b91c1c',
    },
    warning: {
      main: '#f59e0b',
      light: '#fbbf24',
      dark: '#d97706',
    },
    info: {
      main: '#3b82f6',
      light: '#60a5fa',
      dark: '#2563eb',
    },
    success: {
      main: '#10b981',
      light: '#34d399',
      dark: '#059669',
    },
    divider: 'rgba(255, 255, 255, 0.08)',
  },
  typography: {
    fontFamily: [
      'Poppins',
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
    h5: {
      fontWeight: 600,
      fontSize: '1.5rem',
      letterSpacing: '0.01em',
      color: '#f9fafb',
    },
    h6: {
      fontWeight: 600,
      letterSpacing: '0.01em',
    },
    body1: {
      fontSize: '1rem',
      lineHeight: 1.6,
      letterSpacing: '0.00938em',
    },
    body2: {
      lineHeight: 1.6,
    },
    button: {
      textTransform: 'none',
      fontWeight: 500,
      letterSpacing: '0.02em',
    },
    caption: {
      lineHeight: 1.5,
      letterSpacing: '0.03em',
    },
  },
  shape: {
    borderRadius: 12,
  },
  shadows: [
    'none',
    '0px 2px 1px -1px rgba(0,0,0,0.2),0px 1px 1px 0px rgba(0,0,0,0.14),0px 1px 3px 0px rgba(0,0,0,0.12)',
    '0px 3px 3px -2px rgba(0,0,0,0.2),0px 2px 2px 0px rgba(0,0,0,0.14),0px 1px 5px 0px rgba(0,0,0,0.12)',
    '0px 5px 5px -3px rgba(0,0,0,0.2),0px 3px 4px 0px rgba(0,0,0,0.14),0px 1px 8px 0px rgba(0,0,0,0.12)',
    '0px 5px 15px rgba(0,0,0,0.2)',
    '0px 8px 15px -5px rgba(0,0,0,0.2),0px 5px 10px -5px rgba(0,0,0,0.14)',
    '0px 8px 20px -6px rgba(0,0,0,0.2),0px 6px 15px -5px rgba(0,0,0,0.14)',
    '0px 10px 25px -7px rgba(0,0,0,0.2),0px 8px 20px -5px rgba(0,0,0,0.14)',
    '0px 12px 30px -8px rgba(0,0,0,0.2),0px 10px 25px -5px rgba(0,0,0,0.14)',
    '0px 14px 35px -9px rgba(0,0,0,0.2),0px 12px 30px -6px rgba(0,0,0,0.14)',
    '0px 16px 40px -10px rgba(0,0,0,0.2),0px 14px 35px -7px rgba(0,0,0,0.14)',
    '0px 18px 45px -11px rgba(0,0,0,0.2),0px 16px 40px -8px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 20px 50px -12px rgba(0,0,0,0.2),0px 18px 45px -9px rgba(0,0,0,0.14)',
    '0px 11px 14px -7px rgba(0,0,0,0.2), 0px 23px 36px 3px rgba(0,0,0,0.14), 0px 9px 44px 8px rgba(0,0,0,0.12)',
    'none',
    'none'
  ],
  components: {
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundColor: '#1e1e1e',
          backgroundImage: 'none',
          borderRadius: 12,
          boxShadow: '0 6px 16px rgba(0, 0, 0, 0.2)',
        },
        elevation1: {
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.12), 0 1px 2px rgba(0, 0, 0, 0.24)',
        },
        elevation2: {
          boxShadow: '0 3px 6px rgba(0, 0, 0, 0.16), 0 3px 6px rgba(0, 0, 0, 0.23)',
        },
        elevation3: {
          boxShadow: '0 10px 20px rgba(0, 0, 0, 0.19), 0 6px 6px rgba(0, 0, 0, 0.23)',
        },
        elevation4: {
          boxShadow: '0 14px 28px rgba(0, 0, 0, 0.25), 0 10px 10px rgba(0, 0, 0, 0.22)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#1e1e1e',
          border: '1px solid rgba(255, 255, 255, 0.06)',
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 6px 18px 0 rgba(0, 0, 0, 0.15)',
          transition: 'transform 0.3s, box-shadow 0.3s',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: '0 12px 24px 0 rgba(0, 0, 0, 0.25)',
          },
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
          padding: '8px 16px',
          fontWeight: 500,
          boxShadow: '0 2px 6px rgba(0, 0, 0, 0.15)',
          '&:hover': {
            boxShadow: '0 4px 10px rgba(0, 0, 0, 0.25)',
          },
        },
        contained: {
          backgroundColor: '#546e7a',
          boxShadow: 'none',
          '&:hover': {
            backgroundColor: '#455a64',
            boxShadow: '0px 4px 8px rgba(0,0,0,0.2)',
          },
        },
        outlined: {
          borderColor: 'rgba(255, 255, 255, 0.12)',
          '&:hover': {
            backgroundColor: 'rgba(255, 255, 255, 0.05)',
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(255, 255, 255, 0.05)',
          padding: '16px',
        },
        head: {
          fontWeight: 600,
          backgroundColor: 'rgba(255, 255, 255, 0.03)',
          color: '#90a4ae',
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': {
            backgroundColor: 'rgba(176, 190, 197, 0.08)',
          },
        },
      },
    },
    MuiTableSortLabel: {
      styleOverrides: {
        root: {
          '&.Mui-active': {
            color: '#b0bec5',
          },
        },
      },
    },
    MuiInputBase: {
      styleOverrides: {
        root: {
          backgroundColor: 'rgba(255, 255, 255, 0.03)',
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: 'rgba(255, 255, 255, 0.15)',
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: '#78909c',
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderWidth: '2px',
            borderColor: '#78909c',
          },
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          '.MuiOutlinedInput-root': {
            borderRadius: '12px',
            transition: 'all 0.2s',
            backgroundColor: alpha(theme.palette.background.paper, 0.1),
            '&:hover': {
              backgroundColor: alpha(theme.palette.background.paper, 0.2),
            },
            '&.Mui-focused': {
              backgroundColor: alpha(theme.palette.background.paper, 0.2),
              boxShadow: `0 0 0 2px ${alpha(theme.palette.primary.main, 0.4)}`,
            },
            'fieldset': {
              borderColor: 'rgba(255, 255, 255, 0.1)',
            },
            '&:hover fieldset': {
              borderColor: theme.palette.primary.main,
            },
          },
          '.MuiInputLabel-root': {
            fontSize: '0.9rem',
            fontWeight: 500,
            transition: 'all 0.2s',
            color: alpha(theme.palette.text.primary, 0.7),
            '&.Mui-focused': {
              color: theme.palette.primary.main,
            },
          },
          '.MuiInputBase-input': {
            fontSize: '0.95rem',
            padding: '12px 14px',
          },
        }),
      },
    },
    MuiSelect: {
      defaultProps: {
        variant: 'outlined',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          '.MuiOutlinedInput-root': {
            borderRadius: '12px',
            transition: 'all 0.2s',
            backgroundColor: alpha(theme.palette.background.paper, 0.1),
            '&:hover': {
              backgroundColor: alpha(theme.palette.background.paper, 0.2),
            },
            '&.Mui-focused': {
              backgroundColor: alpha(theme.palette.background.paper, 0.2),
              boxShadow: `0 0 0 2px ${alpha(theme.palette.primary.main, 0.4)}`,
            },
            'fieldset': {
              borderColor: 'rgba(255, 255, 255, 0.1)',
            },
            '&:hover fieldset': {
              borderColor: theme.palette.primary.main,
            },
          },
          '.MuiInputLabel-root': {
            fontSize: '0.9rem',
            fontWeight: 500,
            transition: 'all 0.2s',
            color: alpha(theme.palette.text.primary, 0.7),
            '&.Mui-focused': {
              color: theme.palette.primary.main,
            },
          },
          '.MuiSelect-select': {
            fontSize: '0.95rem',
            padding: '12px 14px',
          },
        }),
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          boxShadow: '0 4px 20px 0 rgba(0, 0, 0, 0.15)',
          backgroundColor: '#1e1e1e',
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          transition: 'transform 0.2s, background-color 0.2s',
          '&:hover': {
            backgroundColor: 'rgba(255, 255, 255, 0.08)',
            transform: 'scale(1.05)',
          },
        },
      },
    },
    MuiListItem: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          '&.Mui-selected': {
            backgroundColor: 'rgba(120, 144, 156, 0.15)',
            '&:hover': {
              backgroundColor: 'rgba(120, 144, 156, 0.2)',
            },
          },
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          backgroundColor: 'rgba(176, 190, 197, 0.12)',
        },
        outlined: {
          borderColor: 'rgba(176, 190, 197, 0.3)',
        },
      },
    },
    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 500,
          '&.Mui-selected': {
            color: '#b0bec5',
          },
        },
      },
    },
    MuiTabs: {
      styleOverrides: {
        indicator: {
          backgroundColor: '#78909c',
        },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          '&:hover': {
            backgroundColor: 'rgba(176, 190, 197, 0.08)',
          },
          '&.Mui-selected': {
            backgroundColor: 'rgba(176, 190, 197, 0.16)',
          },
        },
      },
    },
  },
});

export const lightTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#546e7a',
      dark: '#37474f',
      light: '#78909c',
    },
    secondary: {
      main: '#ad1457',
      dark: '#880e4f',
      light: '#d81b60',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
    text: {
      primary: '#212121',
      secondary: '#546e7a',
    },
    error: {
      main: '#ef4444',
      light: '#f87171',
      dark: '#b91c1c',
    },
    warning: {
      main: '#f59e0b',
      light: '#fbbf24',
      dark: '#d97706',
    },
    info: {
      main: '#3b82f6',
      light: '#60a5fa',
      dark: '#2563eb',
    },
    success: {
      main: '#10b981',
      light: '#34d399',
      dark: '#059669',
    },
    divider: 'rgba(0, 0, 0, 0.12)',
  },
  typography: {
    fontFamily: [
      'Poppins',
      '-apple-system',
      'BlinkMacSystemFont',
      '"Segoe UI"',
      'Roboto',
      '"Helvetica Neue"',
      'Arial',
      'sans-serif',
    ].join(','),
    h5: {
      fontWeight: 600,
      fontSize: '1.5rem',
      letterSpacing: '0.01em',
      color: '#212121',
    },
    h6: {
      fontWeight: 600,
      letterSpacing: '0.01em',
    },
    body1: {
      fontSize: '1rem',
      lineHeight: 1.6,
      letterSpacing: '0.00938em',
    },
    body2: {
      lineHeight: 1.6,
    },
    button: {
      textTransform: 'none',
      fontWeight: 500,
      letterSpacing: '0.02em',
    },
    caption: {
      lineHeight: 1.5,
      letterSpacing: '0.03em',
    },
  },
  shape: {
    borderRadius: 12,
  },
  shadows: [
    'none',
    '0px 1px 2px rgba(0, 0, 0, 0.05)',
    '0px 2px 4px rgba(0, 0, 0, 0.08)',
    '0px 4px 8px rgba(0, 0, 0, 0.08)',
    '0px 6px 12px rgba(0, 0, 0, 0.08)',
    '0px 8px 16px rgba(0, 0, 0, 0.08)',
    '0px 10px 20px rgba(0, 0, 0, 0.08)',
    '0px 12px 24px rgba(0, 0, 0, 0.1)',
    '0px 14px 28px rgba(0, 0, 0, 0.1)',
    '0px 16px 32px rgba(0, 0, 0, 0.1)',
    '0px 18px 36px rgba(0, 0, 0, 0.1)',
    '0px 20px 40px rgba(0, 0, 0, 0.1)',
    '0px 22px 44px rgba(0, 0, 0, 0.12)',
    '0px 24px 48px rgba(0, 0, 0, 0.12)',
    '0px 26px 52px rgba(0, 0, 0, 0.12)',
    '0px 28px 56px rgba(0, 0, 0, 0.12)',
    '0px 30px 60px rgba(0, 0, 0, 0.12)',
    '0px 32px 64px rgba(0, 0, 0, 0.15)',
    '0px 34px 68px rgba(0, 0, 0, 0.15)',
    '0px 36px 72px rgba(0, 0, 0, 0.15)',
    '0px 38px 76px rgba(0, 0, 0, 0.15)',
    '0px 40px 80px rgba(0, 0, 0, 0.15)',
    '0px 40px 80px rgba(0, 0, 0, 0.15)',
    '0px 40px 80px rgba(0, 0, 0, 0.15)',
    'none'
  ],
  components: {
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          backgroundImage: 'none',
          borderRadius: 12,
          boxShadow: '0 6px 16px rgba(0, 0, 0, 0.08)',
        },
        elevation1: {
          boxShadow: '0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.12)',
        },
        elevation2: {
          boxShadow: '0 3px 6px rgba(0, 0, 0, 0.08), 0 3px 6px rgba(0, 0, 0, 0.12)',
        },
        elevation3: {
          boxShadow: '0 10px 20px rgba(0, 0, 0, 0.10), 0 6px 6px rgba(0, 0, 0, 0.12)',
        },
        elevation4: {
          boxShadow: '0 14px 28px rgba(0, 0, 0, 0.15), 0 10px 10px rgba(0, 0, 0, 0.12)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          border: '1px solid rgba(0, 0, 0, 0.06)',
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 6px 18px 0 rgba(0, 0, 0, 0.08)',
          transition: 'transform 0.3s, box-shadow 0.3s',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: '0 12px 24px 0 rgba(0, 0, 0, 0.15)',
          },
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
          padding: '8px 16px',
          fontWeight: 500,
          boxShadow: '0 2px 6px rgba(0, 0, 0, 0.08)',
          '&:hover': {
            boxShadow: '0 4px 10px rgba(0, 0, 0, 0.15)',
          },
        },
        contained: {
          backgroundColor: '#546e7a',
          boxShadow: 'none',
          '&:hover': {
            backgroundColor: '#455a64',
            boxShadow: '0px 4px 8px rgba(0,0,0,0.1)',
          },
        },
        outlined: {
          borderColor: 'rgba(0, 0, 0, 0.12)',
          '&:hover': {
            backgroundColor: 'rgba(0, 0, 0, 0.05)',
          },
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid rgba(0, 0, 0, 0.08)',
          padding: '16px',
        },
        head: {
          fontWeight: 600,
          backgroundColor: 'rgba(0, 0, 0, 0.02)',
          color: '#546e7a',
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': {
            backgroundColor: 'rgba(84, 110, 122, 0.08)',
          },
        },
      },
    },
    MuiTableSortLabel: {
      styleOverrides: {
        root: {
          '&.Mui-active': {
            color: '#546e7a',
          },
        },
      },
    },
    MuiInputBase: {
      styleOverrides: {
        root: {
          backgroundColor: 'rgba(0, 0, 0, 0.02)',
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: 'rgba(0, 0, 0, 0.15)',
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: '#546e7a',
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderWidth: '2px',
            borderColor: '#546e7a',
          },
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          '.MuiOutlinedInput-root': {
            borderRadius: '12px',
            transition: 'all 0.2s',
            backgroundColor: alpha(theme.palette.background.paper, 0.8),
            '&:hover': {
              backgroundColor: alpha(theme.palette.background.paper, 1),
            },
            '&.Mui-focused': {
              backgroundColor: alpha(theme.palette.background.paper, 1),
              boxShadow: `0 0 0 2px ${alpha(theme.palette.primary.main, 0.2)}`,
            },
            'fieldset': {
              borderColor: 'rgba(0, 0, 0, 0.1)',
            },
            '&:hover fieldset': {
              borderColor: theme.palette.primary.main,
            },
          },
          '.MuiInputLabel-root': {
            fontSize: '0.9rem',
            fontWeight: 500,
            transition: 'all 0.2s',
            color: theme.palette.text.secondary,
            '&.Mui-focused': {
              color: theme.palette.primary.main,
            },
          },
          '.MuiInputBase-input': {
            fontSize: '0.95rem',
            padding: '12px 14px',
          },
        }),
      },
    },
    MuiSelect: {
      defaultProps: {
        variant: 'outlined',
      },
      styleOverrides: {
        root: ({ theme }) => ({
          '.MuiOutlinedInput-root': {
            borderRadius: '12px',
            transition: 'all 0.2s',
            backgroundColor: alpha(theme.palette.background.paper, 0.8),
            '&:hover': {
              backgroundColor: alpha(theme.palette.background.paper, 1),
            },
            '&.Mui-focused': {
              backgroundColor: alpha(theme.palette.background.paper, 1),
              boxShadow: `0 0 0 2px ${alpha(theme.palette.primary.main, 0.2)}`,
            },
            'fieldset': {
              borderColor: 'rgba(0, 0, 0, 0.1)',
            },
            '&:hover fieldset': {
              borderColor: theme.palette.primary.main,
            },
          },
          '.MuiInputLabel-root': {
            fontSize: '0.9rem',
            fontWeight: 500,
            transition: 'all 0.2s',
            color: theme.palette.text.secondary,
            '&.Mui-focused': {
              color: theme.palette.primary.main,
            },
          },
          '.MuiSelect-select': {
            fontSize: '0.95rem',
            padding: '12px 14px',
          },
        }),
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          boxShadow: '0 4px 20px 0 rgba(0, 0, 0, 0.08)',
          backgroundColor: '#ffffff',
          color: '#212121',
        },
      },
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          transition: 'transform 0.2s, background-color 0.2s',
          '&:hover': {
            backgroundColor: 'rgba(0, 0, 0, 0.04)',
            transform: 'scale(1.05)',
          },
        },
      },
    },
  },
});

export default darkTheme; 