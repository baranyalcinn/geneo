import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { styled, useTheme, alpha } from '@mui/material/styles';
import {
  Container, Typography, Box,
  Card as MuiCard, Avatar,
  IconButton, Fade
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import WestIcon from '@mui/icons-material/West';
import { PersonFormContainer } from '../containers/PersonFormContainer';
import { useLanguage } from '../context/LanguageContext';

const StyledPageBox = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})<{ isDarkMode: boolean }>(({ theme, isDarkMode }) => ({
  minHeight: '100vh',
  background: isDarkMode 
    ? 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)' 
    : 'linear-gradient(135deg, #e4f1ff 0%, #f0f7ff 100%)',
  paddingTop: theme.spacing(4),
  paddingBottom: theme.spacing(4),
  paddingLeft: theme.spacing(2),
  paddingRight: theme.spacing(2),
  transition: 'all 0.3s ease',

  [theme.breakpoints.down('md')]: {
    paddingTop: theme.spacing(2),
    paddingBottom: theme.spacing(2),
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
  },
}));

const StyledCard = styled(MuiCard, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})<{ isDarkMode: boolean }>(({ theme, isDarkMode }) => ({
  display: 'flex',
  flexDirection: 'column',
  minHeight: '85vh',
  borderRadius: '20px',
  overflow: 'hidden',
  background: isDarkMode 
    ? 'rgba(25, 30, 45, 0.85)' 
    : 'rgba(255, 255, 255, 0.85)',
  backdropFilter: 'blur(10px)',
  boxShadow: isDarkMode 
    ? '0 10px 40px 0 rgba(0,0,0,0.3)' 
    : '0 10px 40px 0 rgba(103,152,227,0.15)',
  border: isDarkMode ? '1px solid rgba(255,255,255,0.05)' : 'none',

  [theme.breakpoints.up('md')]: {
    flexDirection: 'row',
  },
}));

const LeftPanel = styled(Box, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})<{ isDarkMode: boolean }>(({ theme, isDarkMode }) => ({
  width: '100%',
  background: isDarkMode
    ? 'linear-gradient(135deg, rgba(32,64,128,0.3) 0%, rgba(18,36,72,0.3) 100%)' 
    : 'linear-gradient(135deg, rgba(103,152,227,0.2) 0%, rgba(92,141,247,0.2) 100%)',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  position: 'relative',
  padding: theme.spacing(4),
  borderRight: isDarkMode 
    ? '1px solid rgba(255,255,255,0.05)' 
    : '1px solid rgba(0,0,0,0.05)',

  [theme.breakpoints.up('md')]: {
    width: '300px',
  },
}));

const StyledIconButton = styled(IconButton, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})<{ isDarkMode: boolean }>(({ theme, isDarkMode }) => ({
  position: 'absolute',
  top: '20px',
  left: '20px',
  background: isDarkMode 
    ? 'rgba(0,0,0,0.2)' 
    : 'rgba(255,255,255,0.5)',
  color: isDarkMode ? 'white' : theme.palette.primary.main,
  backdropFilter: 'blur(5px)',
  '&:hover': {
    background: isDarkMode 
      ? 'rgba(0,0,0,0.3)' 
      : 'rgba(255,255,255,0.7)',
  },
}));

const StyledAvatar = styled(Avatar, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})<{ isDarkMode: boolean }>(({ theme, isDarkMode }) => ({
  width: '80px',
  height: '80px',
  backgroundColor: isDarkMode 
    ? alpha(theme.palette.primary.main, 0.2) 
    : alpha(theme.palette.primary.main, 0.1),
  color: theme.palette.primary.main,
  boxShadow: isDarkMode 
    ? `0 0 30px ${alpha(theme.palette.primary.main, 0.4)}`
    : `0 10px 30px ${alpha(theme.palette.primary.main, 0.25)}`,
  border: isDarkMode 
    ? `4px solid ${alpha(theme.palette.primary.main, 0.2)}`
    : '4px solid rgba(255,255,255,0.8)',
  marginBottom: theme.spacing(3),

  [theme.breakpoints.up('md')]: {
    width: '120px',
    height: '120px',
  },

  '& .MuiSvgIcon-root': {
    fontSize: '40px',
    [theme.breakpoints.up('md')]: {
      fontSize: '60px',
    },
  },
}));

const TitleTypography = styled(Typography, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})<{ isDarkMode: boolean }>(({ theme, isDarkMode }) => ({
  fontWeight: 700,
  color: isDarkMode ? 'white' : theme.palette.primary.main,
  textAlign: 'center',
  marginBottom: theme.spacing(1),
  fontSize: '1.5rem',

  [theme.breakpoints.up('md')]: {
    fontSize: '1.8rem',
  },
}));

const DescriptionTypography = styled(Typography, {
  shouldForwardProp: (prop) => prop !== 'isDarkMode',
})<{ isDarkMode: boolean }>(({ theme, isDarkMode }) => ({
  color: isDarkMode ? 'rgba(255,255,255,0.7)' : 'rgba(0,0,0,0.6)',
  textAlign: 'center',
  maxWidth: '220px',
}));

const RightPanel = styled(Box)(({ theme }) => ({
  flex: 1,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  padding: theme.spacing(2),
  overflowY: 'auto',

  [theme.breakpoints.up('sm')]: {
    padding: theme.spacing(3),
  },
  [theme.breakpoints.up('md')]: {
    padding: theme.spacing(4),
  },
}));

const PersonDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const theme = useTheme();
  const { t } = useLanguage();
  const isDarkMode = theme.palette.mode === 'dark';

  const personId = id ? parseInt(id, 10) : undefined;
  const isEditMode = !!personId;

  const handleSave = () => {
    navigate('/persons');
  };

  const handleCancel = () => {
    navigate(-1);
  };

  return (
    <StyledPageBox isDarkMode={isDarkMode}>
      <Container maxWidth="xl" sx={{ height: '100%' }}>
        <Fade in={true}>
          <StyledCard elevation={0} isDarkMode={isDarkMode}>
            <LeftPanel isDarkMode={isDarkMode}>
              <StyledIconButton 
                onClick={handleCancel}
                isDarkMode={isDarkMode}
              >
                <WestIcon fontSize="small" />
              </StyledIconButton>

              <StyledAvatar isDarkMode={isDarkMode}>
                <PersonIcon />
              </StyledAvatar>

              <TitleTypography variant="h4" isDarkMode={isDarkMode}>
                {isEditMode ? t('editPerson') : t('addNewPerson')}
              </TitleTypography>

              <DescriptionTypography variant="body1" isDarkMode={isDarkMode}>
                {isEditMode 
                  ? t('editPersonDescription') 
                  : t('addPersonDescription')}
              </DescriptionTypography>
            </LeftPanel>

            <RightPanel>
              <PersonFormContainer
                personId={personId}
                onSave={handleSave}
                onCancel={handleCancel}
              />
            </RightPanel>
          </StyledCard>
        </Fade>
      </Container>
    </StyledPageBox>
  );
};

export default PersonDetailPage; 