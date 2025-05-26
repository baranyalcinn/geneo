import React from 'react';
import { Button, Tooltip, Fade, Box } from '@mui/material';
import { Link } from 'react-router-dom';

interface NavItem {
  path: string;
  label: string;
  icon: React.ReactNode;
}

interface NavbarMenuProps {
  navItems: NavItem[];
  isActive: (path: string) => boolean;
}

const NavbarMenu: React.FC<NavbarMenuProps> = ({ navItems, isActive }) => (
  <Box sx={{ display: 'flex', flexGrow: 1 }}>
    {navItems.map((item) => (
      <Tooltip
        key={item.path}
        title={item.label}
        placement="bottom"
        arrow
        TransitionComponent={Fade}
        TransitionProps={{ timeout: 600 }}
      >
        <Button
          component={Link}
          to={item.path}
          sx={{
            mx: 0.5,
            px: 2,
            py: 1,
            borderRadius: '8px',
            color: 'white',
            position: 'relative',
            fontWeight: isActive(item.path) ? 500 : 400,
            opacity: isActive(item.path) ? 1 : 0.85,
            transition: 'all 0.2s ease',
            overflow: 'hidden',
            '&:hover': {
              backgroundColor: 'rgba(255,255,255,0.15)',
              opacity: 1,
              transform: 'translateY(-2px)'
            },
            '&::before': isActive(item.path) ? {
              content: '""',
              position: 'absolute',
              bottom: 0,
              left: 0,
              width: '100%',
              height: '3px',
              background: 'linear-gradient(90deg, #90caf9, #f48fb1)',
              borderTopLeftRadius: '2px',
              borderTopRightRadius: '2px',
            } : {},
            '&::after': {
              content: '""',
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              background: 'rgba(255,255,255,0.1)',
              opacity: 0,
              transition: 'opacity 0.3s ease',
            },
            '&:hover::after': {
              opacity: 1,
            }
          }}
          startIcon={item.icon}
        >
          {item.label}
        </Button>
      </Tooltip>
    ))}
  </Box>
);

export default NavbarMenu; 