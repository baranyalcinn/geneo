import React from 'react';
import { Box, CircularProgress, alpha, useTheme } from '@mui/material';

interface GraphLoadingIndicatorProps {
  width?: string;
  height?: string;
  minHeight?: string;
}

const GraphLoadingIndicator: React.FC<GraphLoadingIndicatorProps> = ({
  width = "100%",
  height = "100%", // Default to 100% of parent
  minHeight = "300px",
}) => {
  const theme = useTheme();

  return (
    <Box
      sx={{
        width: width,
        height: height,
        minHeight: minHeight,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: alpha(theme.palette.background.paper, 0.6),
        borderRadius: theme.shape.borderRadius,
      }}
    >
      <CircularProgress size={40} color="primary" />
    </Box>
  );
};

export default GraphLoadingIndicator; 