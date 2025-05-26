import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Container, Typography, Paper } from '@mui/material';
import { PersonFormContainer } from '../containers/PersonFormContainer';

function AddPerson() {
  const navigate = useNavigate();
  
  const handleSave = () => {
    // Kaydetme işlemi tamamlandıktan sonra ana sayfaya yönlendir
    navigate('/');
  };
  
  const handleCancel = () => {
    // İptal edildiğinde önceki sayfaya dön
    navigate(-1);
  };

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 3 }}>
        <Typography variant="h5" component="h1" gutterBottom>
          Aile Ağacına Yeni Kişi Ekle
        </Typography>
        
        <PersonFormContainer
          onSave={handleSave}
          onCancel={handleCancel}
        />
      </Paper>
    </Container>
  );
}

export default AddPerson; 