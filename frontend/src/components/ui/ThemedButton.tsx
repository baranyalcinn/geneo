import React from 'react';
import { Button, ButtonProps, styled } from '@mui/material';
import { useThemeContext } from '../../context/ThemeContext';

// Button tipinin genişletilmesi
interface ThemedButtonProps extends Omit<ButtonProps, 'color'> { // Omit Mui 'color' to avoid confusion
  themeColor?: 'primary' | 'secondary' | 'error' | 'warning' | 'info' | 'success';
}

// Styled Button bileşeni - tema özelliklerini kullanarak
const StyledButton = styled(Button, {
  shouldForwardProp: (prop) => prop !== 'themeColor'
})<ThemedButtonProps>(({ theme, themeColor = 'primary', disableElevation: externalDisableElevation }) => {
  const { mode } = useThemeContext(); // Access theme mode here
  const internalDisableElevation = mode === 'dark'; // Determine elevation based on theme mode

  return {
    borderRadius: theme.shape.borderRadius,
    textTransform: 'none',
    fontWeight: theme.typography.fontWeightMedium,
    // Dinamik gölge efekti
    boxShadow: theme.palette.mode === 'dark'
      ? '0 3px 5px 2px rgba(0, 0, 0, .3)'
      : '0 3px 5px 2px rgba(0, 0, 0, .1)',
    transition: 'all 0.3s ease',
    // Pass the determined disableElevation and the original themeColor as Mui's color prop
    color: theme.palette[themeColor]?.contrastText, // Set text color based on themeColor for better contrast
    backgroundColor: theme.palette[themeColor]?.main, // Set background color based on themeColor
    '&:hover': {
      boxShadow: theme.palette.mode === 'dark'
        ? '0 5px 8px 2px rgba(0, 0, 0, .5)'
        : '0 5px 8px 2px rgba(0, 0, 0, .2)',
      backgroundColor: theme.palette[themeColor]?.dark, // Adjust hover background color
    },
    // Apply elevation based on theme mode, allow override
    disableElevation: externalDisableElevation !== undefined ? externalDisableElevation : internalDisableElevation,
  };
});

/**
 * ThemedButton - Tema özelliklerini kullanan buton bileşeni
 * Tema değişikliklerine otomatik tepki verir
 */
const ThemedButton: React.FC<ThemedButtonProps> = ({
  children,
  themeColor = 'primary',
  variant = 'contained',
  // disableElevation prop is now handled by StyledButton, but can be passed to override
  ...props
}) => {
  // useThemeContext hook ile tema özelliklerine erişim
  // const { mode } = useThemeContext(); // No longer needed here

  return (
    <StyledButton
      variant={variant}
      themeColor={themeColor} // Pass themeColor to StyledButton
      // Mui's 'color' prop is now set within StyledButton based on themeColor
      // disableElevation is also handled by StyledButton
      {...props}
    >
      {children}
    </StyledButton>
  );
};

export default ThemedButton; 