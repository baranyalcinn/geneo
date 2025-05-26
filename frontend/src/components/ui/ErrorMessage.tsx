import { Alert } from "@mui/material";
import React from "react";

interface ErrorMessageProps {
  message: string;
}

const ErrorMessage: React.FC<ErrorMessageProps> = ({ message }) => (
  <Alert severity="error" sx={{ my: 2 }}>
    {message}
  </Alert>
);

export default ErrorMessage; 