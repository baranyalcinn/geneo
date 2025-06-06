import React from 'react';
import { Box, Typography, alpha, useTheme } from '@mui/material';
import InfoIcon from '@mui/icons-material/Info';

interface GraphEmptyStateProps {
  width?: string;
  height?: string;
  minHeight?: string;
  messageTitle?: string;
  messageBody?: string;
}

const GraphEmptyState: React.FC<GraphEmptyStateProps> = ({
  width = "100%",
  height = "100%",
  minHeight = "300px",
  messageTitle = "İlişki Haritası Yok",
  messageBody = "Görüntülenecek ilişki haritası bilgisi bulunamadı.",
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
        borderRadius: `${theme.shape.borderRadius}px`,
        border: `1px dashed ${alpha(theme.palette.primary.main, 0.3)}`,
        padding: theme.spacing(3),
        textAlign: 'center',
      }}
    >
      <InfoIcon
        color="primary"
        sx={{ fontSize: 48, mb: 2, opacity: 0.7 }}
      />
      <Typography variant="h6" color="primary" gutterBottom sx={{fontWeight: 600}}>
        {messageTitle}
      </Typography>
      <Typography variant="body1" color="text.secondary">
        {messageBody}
      </Typography>
    </Box>
  );
};

export default GraphEmptyState; 