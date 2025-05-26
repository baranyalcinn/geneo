import React from 'react';
import { IconButton, Tooltip } from '@mui/material';
import LightModeIcon from '@mui/icons-material/LightMode';
import DarkModeIcon from '@mui/icons-material/DarkMode';

interface ThemeToggleProps {
  mode: 'light' | 'dark';
  toggleTheme: () => void;
}

const ThemeToggle: React.FC<ThemeToggleProps> = ({ mode, toggleTheme }) => (
  <Tooltip title={mode === 'dark' ? 'Açık Tema' : 'Koyu Tema'}>
    <IconButton onClick={toggleTheme} color="inherit">
      {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
    </IconButton>
  </Tooltip>
);

export default ThemeToggle; 