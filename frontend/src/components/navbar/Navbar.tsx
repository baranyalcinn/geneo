import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { AppBar, Toolbar, Container, useTheme, alpha, useMediaQuery, Box } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import PeopleIcon from '@mui/icons-material/People';
import SportsEsportsIcon from '@mui/icons-material/SportsEsports';
import { useThemeContext } from '../../context/ThemeContext';
import { useLanguage } from '../../context/LanguageContext';
import NavbarLogo from './NavbarLogo';
import NavbarMenu from './NavbarMenu';
import ThemeToggle from '../settings/ThemeToggle';
import LanguageSelector from '../settings/LanguageSelector';

function Navbar() {
  const location = useLocation();
  const { mode, toggleTheme } = useThemeContext();
  const theme = useTheme();
  const { language, setLanguage, t } = useLanguage();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [scrolled, setScrolled] = useState(false);
  const [langMenuAnchorEl, setLangMenuAnchorEl] = useState<null | HTMLElement>(null);

  useEffect(() => {
    const handleScroll = () => {
      const isScrolled = window.scrollY > 20;
      if (isScrolled !== scrolled) {
        setScrolled(isScrolled);
      }
    };
    window.addEventListener('scroll', handleScroll);
    return () => {
      window.removeEventListener('scroll', handleScroll);
    };
  }, [scrolled]);

  const isActive = (path: string) => {
    return location.pathname === path || location.pathname.startsWith(path + '/');
  };

  
  const handleLangMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setLangMenuAnchorEl(event.currentTarget);
  };
  const handleLangMenuClose = () => {
    setLangMenuAnchorEl(null);
  };
  const handleLanguageChange = (lang: string) => {
    if (lang === 'tr' || lang === 'en') {
      setLanguage(lang);
    }
    handleLangMenuClose();
  };

  const navItems = [
    { path: '/', label: t('home'), icon: <HomeIcon fontSize="small" /> },
    { path: '/persons', label: t('persons'), icon: <PeopleIcon fontSize="small" /> },
    { path: '/family-tree', label: t('familyTree'), icon: <AccountTreeIcon fontSize="small" /> },
    { path: '/game', label: t('relationGame'), icon: <SportsEsportsIcon fontSize="small" /> },
  ];
  const languages = [
    { code: 'tr', label: 'Türkçe' },
    { code: 'en', label: 'English' },
  ];

  return (
    <AppBar
      position="sticky"
      elevation={scrolled ? 4 : 0}
      sx={{
        transition: 'all 0.3s ease',
        background: scrolled
          ? `linear-gradient(145deg, ${alpha(theme.palette.primary.dark, 0.98)}, ${alpha(theme.palette.primary.main, 0.95)})`
          : `linear-gradient(145deg, ${alpha(theme.palette.primary.dark, 0.85)}, ${alpha(theme.palette.primary.main, 0.8)})`,
        backdropFilter: 'blur(8px)',
        borderBottom: `1px solid ${alpha(theme.palette.divider, scrolled ? 0.1 : 0.05)}`,
      }}
    >
      <Container maxWidth="lg">
        <Toolbar sx={{ px: { xs: 0 }, py: { xs: 1, md: 0.75 } }}>
          <NavbarLogo />
          {!isMobile && (
            <NavbarMenu navItems={navItems} isActive={isActive} />
          )}
          {!isMobile && (
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <ThemeToggle mode={mode} toggleTheme={toggleTheme} />
              <LanguageSelector
                language={language}
                languages={languages}
                anchorEl={langMenuAnchorEl}
                onOpen={handleLangMenuOpen}
                onClose={handleLangMenuClose}
                onChange={handleLanguageChange}
              />
            </Box>
          )}
          {/* Mobil menü ve diğer alt bileşenler için benzer şekilde bölünebilir */}
        </Toolbar>
      </Container>
    </AppBar>
  );
}

export default Navbar; 