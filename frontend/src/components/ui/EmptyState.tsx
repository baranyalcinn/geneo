import { Alert } from "@mui/material";
import React from "react";

interface EmptyStateProps {
  message: string;
}

const EmptyState: React.FC<EmptyStateProps> = ({ message }) => (
  <Alert severity="info" sx={{ my: 2 }}>
    {message}
  </Alert>
);

export default EmptyState; 