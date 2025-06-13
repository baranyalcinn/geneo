import React, { ReactNode } from 'react';
import { ThemeProvider } from './ThemeContext';
import { CssBaseline } from '@mui/material';
import { LanguageProvider } from './LanguageContext';
import { FamilyTreeProvider } from './FamilyTreeContext';
import { GameProvider } from '../contexts/GameContext';

interface AppProvidersProps {
  children: ReactNode;
}

/**
 * AppProviders - Tüm context provider'ları bir araya getirir
 * Daha iyi organizasyon için Composition Pattern kullanılıyor
 */
const AppProviders: React.FC<AppProvidersProps> = ({ children }) => {
  return (
    <ThemeProvider>
      {/* CssBaseline, MUI'nin normalize.css eşdeğeridir */}
      <CssBaseline />
      {/* Diğer provider'lar da burada iç içe geçebilir */}
      <LanguageProvider>
        <FamilyTreeProvider>
          <GameProvider>
            {children}
          </GameProvider>
        </FamilyTreeProvider>
      </LanguageProvider>
    </ThemeProvider>
  );
};

export default AppProviders; 