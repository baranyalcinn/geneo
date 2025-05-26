import React from 'react';
import { Modal, Box, Typography, IconButton, Paper } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { useFamilyTree } from '../context/FamilyTreeContext';
import { useLanguage } from '../context/LanguageContext';
import { useThemeContext } from '../context/ThemeContext';

const PersonDetailModal: React.FC = () => {
  const { isDetailModalOpen, closeDetailModal, personForDetails } = useFamilyTree();
  const { t } = useLanguage();
  const { mode } = useThemeContext();

  if (!personForDetails) {
    return null;
  }

  const fullName = `${personForDetails.firstName || ''} ${personForDetails.lastName || ''}`.trim();

  const modalStyle = {
    position: 'absolute' as 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 400,
    bgcolor: mode === 'dark' ? 'grey.900' : 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
    borderRadius: 2,
  };

  return (
    <Modal
      open={isDetailModalOpen}
      onClose={closeDetailModal}
      aria-labelledby="person-detail-modal-title"
      aria-describedby="person-detail-modal-description"
    >
      <Paper sx={modalStyle}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography id="person-detail-modal-title" variant="h6" component="h2">
            {fullName || t('personDetails')}
          </Typography>
          <IconButton onClick={closeDetailModal} size="small">
            <CloseIcon />
          </IconButton>
        </Box>
        <Box id="person-detail-modal-description">
          <Typography sx={{ mt: 2 }}>
            {/* TODO: Kişinin diğer detayları buraya eklenecek */}
            {t('detailsWillAppearHere')}
          </Typography>
          <Typography variant="body2" color="text.secondary">ID: {personForDetails.id}</Typography>
          {/* Daha fazla detay eklenebilir: Doğum/Ölüm tarihi, Cinsiyet, Notlar vb. */}
        </Box>
      </Paper>
    </Modal>
  );
};

export default PersonDetailModal; 