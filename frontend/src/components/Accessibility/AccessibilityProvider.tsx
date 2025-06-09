import React, { createContext, useContext, useState, useEffect } from 'react';

interface AccessibilitySettings {
  // Görsel erişilebilirlik
  highContrast: boolean;
  fontSize: 'small' | 'medium' | 'large' | 'extra-large';
  colorBlindMode: 'none' | 'protanopia' | 'deuteranopia' | 'tritanopia';
  
  // Motor erişilebilirlik  
  reducedMotion: boolean;
  keyboardNavigation: boolean;
  clickDelay: number; // ms
  
  // Bilişsel erişilebilirlik
  simplifiedUI: boolean;
  showHints: boolean;
  extendedTimeouts: boolean;
  
  // Ses erişilebilirliği
  screenReader: boolean;
  audioFeedback: boolean;
  voiceQuestions: boolean;
}

interface AccessibilityContextType {
  settings: AccessibilitySettings;
  updateSetting: <K extends keyof AccessibilitySettings>(
    key: K, 
    value: AccessibilitySettings[K]
  ) => void;
  resetToDefaults: () => void;
  applySystemPreferences: () => void;
}

const defaultSettings: AccessibilitySettings = {
  highContrast: false,
  fontSize: 'medium',
  colorBlindMode: 'none',
  reducedMotion: false,
  keyboardNavigation: true,
  clickDelay: 0,
  simplifiedUI: false,
  showHints: true,
  extendedTimeouts: false,
  screenReader: false,
  audioFeedback: false,
  voiceQuestions: false,
};

const AccessibilityContext = createContext<AccessibilityContextType | undefined>(undefined);

export const useAccessibility = () => {
  const context = useContext(AccessibilityContext);
  if (!context) {
    throw new Error('useAccessibility must be used within AccessibilityProvider');
  }
  return context;
};

interface AccessibilityProviderProps {
  children: React.ReactNode;
}

export const AccessibilityProvider: React.FC<AccessibilityProviderProps> = ({ children }) => {
  const [settings, setSettings] = useState<AccessibilitySettings>(() => {
    const saved = localStorage.getItem('accessibility-settings');
    return saved ? JSON.parse(saved) : defaultSettings;
  });

  // Ayarları localStorage'a kaydet
  useEffect(() => {
    localStorage.setItem('accessibility-settings', JSON.stringify(settings));
    applySettingsToDOM();
  }, [settings]);

  // Sistem tercihlerini algıla
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    const contrastQuery = window.matchMedia('(prefers-contrast: high)');
    
    if (mediaQuery.matches) {
      updateSetting('reducedMotion', true);
    }
    
    if (contrastQuery.matches) {
      updateSetting('highContrast', true);
    }

    // Screen reader algılama
    const hasScreenReader = window.speechSynthesis !== undefined || 
                           'speechSynthesis' in window ||
                           navigator.userAgent.includes('NVDA') ||
                           navigator.userAgent.includes('JAWS');
    
    if (hasScreenReader) {
      updateSetting('screenReader', true);
    }
  }, []);

  const updateSetting = <K extends keyof AccessibilitySettings>(
    key: K, 
    value: AccessibilitySettings[K]
  ) => {
    setSettings(prev => ({ ...prev, [key]: value }));
  };

  const resetToDefaults = () => {
    setSettings(defaultSettings);
  };

  const applySystemPreferences = () => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
    const contrastQuery = window.matchMedia('(prefers-contrast: high)');
    
    setSettings(prev => ({
      ...prev,
      reducedMotion: mediaQuery.matches,
      highContrast: contrastQuery.matches,
    }));
  };

  const applySettingsToDOM = () => {
    const root = document.documentElement;
    
    // Font boyutu
    const fontSizeMap = {
      'small': '14px',
      'medium': '16px', 
      'large': '18px',
      'extra-large': '22px'
    };
    root.style.fontSize = fontSizeMap[settings.fontSize];

    // Yüksek kontrast
    if (settings.highContrast) {
      root.classList.add('high-contrast');
    } else {
      root.classList.remove('high-contrast');
    }

    // Azaltılmış hareket
    if (settings.reducedMotion) {
      root.style.setProperty('--animation-duration', '0s');
      root.style.setProperty('--transition-duration', '0s');
    } else {
      root.style.removeProperty('--animation-duration');
      root.style.removeProperty('--transition-duration');
    }

    // Renk körü modu
    root.setAttribute('data-colorblind-mode', settings.colorBlindMode);

    // Basitleştirilmiş UI
    if (settings.simplifiedUI) {
      root.classList.add('simplified-ui');
    } else {
      root.classList.remove('simplified-ui');
    }
  };

  const value: AccessibilityContextType = {
    settings,
    updateSetting,
    resetToDefaults,
    applySystemPreferences,
  };

  return (
    <AccessibilityContext.Provider value={value}>
      <div className="accessibility-wrapper" data-accessibility-mode={settings.simplifiedUI ? 'simplified' : 'normal'}>
        {children}
        
        {/* Screen Reader Announcements */}
        <div 
          id="accessibility-announcements"
          className="sr-only"
          aria-live="polite"
          aria-atomic="true"
        />
        
        {/* Skip Links */}
        <div className="skip-links">
          <a 
            href="#main-content" 
            className="skip-link"
            onFocus={(e) => e.target.classList.add('focused')}
            onBlur={(e) => e.target.classList.remove('focused')}
          >
            Ana içeriğe geç
          </a>
          <a 
            href="#game-area" 
            className="skip-link"
            onFocus={(e) => e.target.classList.add('focused')}
            onBlur={(e) => e.target.classList.remove('focused')}
          >
            Oyun alanına geç
          </a>
        </div>
      </div>
    </AccessibilityContext.Provider>
  );
};

// Yardımcı hook'lar
export const useAnnounce = () => {
  const announce = (message: string, priority: 'polite' | 'assertive' = 'polite') => {
    const announcer = document.getElementById('accessibility-announcements');
    if (announcer) {
      announcer.setAttribute('aria-live', priority);
      announcer.textContent = message;
      
      // Duyuruyu temizle
      setTimeout(() => {
        announcer.textContent = '';
      }, 1000);
    }
  };

  return announce;
};

export const useKeyboardNavigation = () => {
  const { settings } = useAccessibility();
  
  useEffect(() => {
    if (!settings.keyboardNavigation) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      // Tab döngüsü yönetimi
      if (event.key === 'Tab') {
        const focusableElements = document.querySelectorAll(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        
        const firstElement = focusableElements[0] as HTMLElement;
        const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;
        
        if (event.shiftKey && document.activeElement === firstElement) {
          event.preventDefault();
          lastElement.focus();
        } else if (!event.shiftKey && document.activeElement === lastElement) {
          event.preventDefault();
          firstElement.focus();
        }
      }
      
      // Escape ile modal kapatma
      if (event.key === 'Escape') {
        const modal = document.querySelector('[role="dialog"]') as HTMLElement;
        if (modal) {
          const closeButton = modal.querySelector('[data-close]') as HTMLElement;
          closeButton?.click();
        }
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [settings.keyboardNavigation]);
}; 