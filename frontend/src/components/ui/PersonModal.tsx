import React from 'react';
import { Modal, Box, Typography, IconButton } from '@mui/material';
import { Person } from '../../types/Person'; // Güncellendi: ../ -> ../../

interface PersonModalProps {
    person: Person | null;
    open: boolean;
    onClose: () => void;
}

function PersonModal({ person, open, onClose }: PersonModalProps) {
    if (!person) return null;

    const formatDate = (date: string) => {
        return new Date(date).toLocaleDateString('tr-TR', {
            year: 'numeric',
            month: 'numeric',
            day: 'numeric'
        });
    };

    return (
        <Modal
            open={open}
            onClose={onClose}
            aria-labelledby="person-modal-title"
        >
            <Box sx={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                width: 300,
                bgcolor: 'background.paper',
                borderRadius: 2,
                boxShadow: 24,
                p: 3,
            }}>
                <IconButton
                    aria-label="close"
                    onClick={onClose}
                    sx={{
                        position: 'absolute',
                        right: 8,
                        top: 8,
                        color: 'text.secondary',
                        fontSize: '1.2rem',
                        fontWeight: 'bold',
                        minWidth: '24px',
                        padding: '4px',
                    }}
                >
                    ✕
                </IconButton>
                
                <Typography variant="h6" component="h2" gutterBottom>
                    {person.firstName} {person.lastName}
                </Typography>
                
                <Typography variant="body1" color="text.secondary" gutterBottom>
                    {person.birthDate ? formatDate(person.birthDate) : 'Doğum tarihi bilinmiyor'} {/* birthDate null/undefined kontrolü eklendi */}
                    {person.deathDate ? ` - ${formatDate(person.deathDate)}` : ' - Yaşıyor'}
                </Typography>

                <Typography variant="body2" color="text.secondary">
                    Cinsiyet: {person.gender === 'ERKEK' ? 'Erkek' : 'Kadın'}
                </Typography>
            </Box>
        </Modal>
    );
}

export default PersonModal; 