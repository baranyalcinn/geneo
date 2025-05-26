import React from 'react';
import { IconButton, Tooltip, useTheme } from '@mui/material';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import { useThemeContext } from '../../context/ThemeContext';

interface ThemeToggleProps {
  tooltipPlacement?: 'top' | 'bottom' | 'left' | 'right';
  size?: 'small' | 'medium' | 'large';
  iconColor?: string;
}

/**
 * ThemeToggle - Temayı değiştirmek için kullanılan düğme bileşeni
 * Hafif ve tek sorumluluk ilkesine uygun
 */
const ThemeToggle: React.FC<ThemeToggleProps> = ({
  tooltipPlacement = 'bottom',
  size = 'medium',
  iconColor
}) => {
  // Tema bilgilerini ve tema değiştirme fonksiyonunu al
  const { mode, toggleTheme } = useThemeContext();
  // MUI tema nesnesine erişim
  const theme = useTheme();
  
  // İkon rengini belirle
  const color = iconColor || (mode === 'dark' ? theme.palette.primary.light : theme.palette.primary.main);
  
  return (
    <Tooltip title={mode === 'dark' ? 'Açık tema' : 'Koyu tema'} placement={tooltipPlacement}>
      <IconButton 
        onClick={toggleTheme} 
        size={size}
        aria-label="tema değiştir"
        color="inherit"
        sx={{ 
          color,
          transition: 'all 0.3s ease',
          '&:hover': {
            transform: 'scale(1.1)',
            color: mode === 'dark' ? theme.palette.primary.main : theme.palette.primary.dark
          }
        }}
      >
        {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
      </IconButton>
    </Tooltip>
  );
};

export default ThemeToggle; 