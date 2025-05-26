import React from 'react';
import { Box, Typography, alpha, useTheme } from '@mui/material';
import WarningIcon from '@mui/icons-material/Warning';

interface GraphNodeErrorStateProps {
  width?: string;
  height?: string;
  minHeight?: string;
  messageTitle?: string;
  messageBody?: string;
}

const GraphNodeErrorState: React.FC<GraphNodeErrorStateProps> = ({
  width = "100%",
  height = "100%",
  minHeight = "300px",
  messageTitle = "Düğümler Oluşturulamadı",
  messageBody = "Düğümler oluşturulamadığı için ilişki haritası gösterilemiyor.",
}) => {
  const theme = useTheme();

  return (
    <Box
      sx={{
        width: width,
        height: height,
        minHeight: minHeight,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        backgroundColor: alpha(theme.palette.background.paper, 0.7),
        borderRadius: theme.shape.borderRadius * 1.5,
        border: `1px dashed ${alpha(theme.palette.warning.main, 0.4)}`,
        padding: theme.spacing(3),
        textAlign: 'center',
      }}
    >
      <WarningIcon
        color="warning"
        sx={{ fontSize: 48, mb: 2, opacity: 0.7 }}
      />
      <Typography variant="h6" color="warning.main" gutterBottom sx={{fontWeight: 600}}>
        {messageTitle}
      </Typography>
      <Typography variant="body1" color="text.secondary">
        {messageBody}
      </Typography>
    </Box>
  );
};

export default GraphNodeErrorState; 