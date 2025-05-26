import React, { memo } from 'react';
import { Card, CardContent, CardMedia, Typography, Box } from '@mui/material';
import { Person } from '../../types/Person';
import OptimizedImage from './OptimizedImage';

interface PersonCardProps {
    person: Person;
    onClick?: (id: number) => void;
}

const PersonCard: React.FC<PersonCardProps> = ({ person, onClick }) => {
    const handleClick = () => {
        if (onClick) {
            onClick(person.id);
        }
    };

    return (
        <Card 
            onClick={handleClick} 
            sx={{ 
                cursor: onClick ? 'pointer' : 'default',
                transition: 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
                '&:hover': onClick ? {
                    transform: 'translateY(-5px)',
                    boxShadow: 3
                } : {}
            }}
        >
            {person.avatarUrl ? (
                <Box sx={{ position: 'relative', paddingTop: '75%' }}>
                    <OptimizedImage
                        src={person.avatarUrl}
                        alt={`${person.firstName} ${person.lastName}`}
                        width={200}
                        height={150}
                        className="person-image"
                    />
                </Box>
            ) : (
                <Box 
                    sx={{ 
                        height: 150, 
                        backgroundColor: '#f5f5f5', 
                        display: 'flex', 
                        justifyContent: 'center', 
                        alignItems: 'center' 
                    }}
                >
                    <Typography variant="body2" color="text.secondary">
                        Fotoğraf yok
                    </Typography>
                </Box>
            )}
            <CardContent>
                <Typography gutterBottom variant="h6" component="div">
                    {person.firstName} {person.lastName}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                    {person.birthDate && new Date(person.birthDate).toLocaleDateString()}
                    {person.deathDate && ` - ${new Date(person.deathDate).toLocaleDateString()}`}
                </Typography>
            </CardContent>
        </Card>
    );
};

// React.memo ile sarmalayarak gereksiz yeniden render'ları önle
export default memo(PersonCard); 