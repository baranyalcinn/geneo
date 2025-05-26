import React, { createContext, useState, useContext, useEffect, ReactNode, useMemo } from 'react';
import { Theme, ThemeProvider as MUIThemeProvider } from '@mui/material/styles';
import { lightTheme, darkTheme } from '../theme';

// Tema modlarını açıkça tanımla
export type ThemeMode = 'light' | 'dark';

// ThemeContext'in tipi - SOLID ve DRY'a uygun
export interface ThemeContextType {
  mode: ThemeMode;
  theme: Theme;
  toggleTheme: () => void;
  setMode: (mode: ThemeMode) => void;
}

// Context oluştur
const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

/**
 * ThemeContext'i kullanmak için custom hook
 * Bileşenlerin doğrudan context'e bağımlılığını azaltır
 */
export const useThemeContext = () => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useThemeContext must be used within a ThemeProvider');
  }
  return context;
};

// Context Provider'ın props tipi
interface ThemeProviderProps {
  children: ReactNode;
}

/**
 * ThemeProvider - Tema yönetiminden sorumlu
 * Single Responsibility prensibine uygun
 */
export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children }) => {
  // Kullanıcı tercihini localStorage'dan al veya varsayılan tema kullan
  const [mode, setMode] = useState<ThemeMode>(() => {
    const savedMode = localStorage.getItem('themeMode');
    return (savedMode as ThemeMode) || 'light';
  });

  // Active temayı belirle - useMemo ile performans optimizasyonu
  const theme = useMemo(() => 
    mode === 'dark' ? darkTheme : lightTheme,
    [mode]
  );

  // Tema değiştirme fonksiyonunu useMemo ile optimize et
  const toggleTheme = useMemo(() => 
    () => setMode((prevMode) => (prevMode === 'dark' ? 'light' : 'dark')),
    []
  );

  // Mode değiştiğinde localStorage'a kaydet - Side Effect'i yönet
  useEffect(() => {
    localStorage.setItem('themeMode', mode);
  }, [mode]);

  // Tema context değerini useMemo ile optimize et - gereksiz render'ları önle
  const contextValue = useMemo(
    () => ({ mode, theme, toggleTheme, setMode }),
    [mode, theme, toggleTheme]
  );

  return (
    <ThemeContext.Provider value={contextValue}>
      <MUIThemeProvider theme={theme}>
        {children}
      </MUIThemeProvider>
    </ThemeContext.Provider>
  );
}; 