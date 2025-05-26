import React from 'react';
import { Typography, Avatar } from '@mui/material';
import { Link } from 'react-router-dom';
import AccountTreeIcon from '@mui/icons-material/AccountTree';

const NavbarLogo: React.FC = () => (
  <Typography 
    variant="h6" 
    component={Link} 
    to="/" 
    sx={{ 
      color: 'white', 
      textDecoration: 'none',
      fontWeight: 700,
      letterSpacing: 0.5,
      display: 'flex',
      alignItems: 'center',
      transition: 'opacity 0.2s ease',
      '&:hover': {
        opacity: 0.9
      },
      mr: 2
    }}
  >
    <Avatar 
      sx={{ 
        mr: 1.5, 
        bgcolor: 'rgba(144,202,249,0.25)',
        color: '#90caf9',
        width: 36,
        height: 36
      }}
    >
      <AccountTreeIcon fontSize="small" />
    </Avatar>
    Aile Ağacı
  </Typography>
);

export default NavbarLogo; 