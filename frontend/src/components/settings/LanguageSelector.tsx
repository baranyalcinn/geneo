import React from 'react';
import { IconButton, Tooltip, Menu, MenuItem } from '@mui/material';
import TranslateIcon from '@mui/icons-material/Translate';

interface Language {
  code: string;
  label: string;
}

interface LanguageSelectorProps {
  language: string;
  languages: Language[];
  anchorEl: null | HTMLElement;
  onOpen: (event: React.MouseEvent<HTMLElement>) => void;
  onClose: () => void;
  onChange: (lang: string) => void;
}

const LanguageSelector: React.FC<LanguageSelectorProps> = ({
  language, languages, anchorEl, onOpen, onClose, onChange
}) => (
  <>
    <Tooltip title="Dil Seçimi">
      <IconButton onClick={onOpen} color="inherit">
        <TranslateIcon />
      </IconButton>
    </Tooltip>
    <Menu
      anchorEl={anchorEl}
      open={Boolean(anchorEl)}
      onClose={onClose}
    >
      {languages.map((lang) => (
        <MenuItem
          key={lang.code}
          onClick={() => onChange(lang.code)}
          selected={language === lang.code}
        >
          {lang.label}
        </MenuItem>
      ))}
    </Menu>
  </>
);

export default LanguageSelector; 