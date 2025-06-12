import React from 'react';
import { Handle, Position, NodeProps, Node } from '@xyflow/react';
import { Box, Typography, Avatar, Paper, Tooltip, IconButton } from '@mui/material';
import { Person, PersonNodeData } from '../../types/Person';
import { useThemeContext } from '../../context/ThemeContext';
import { useLanguage } from '../../context/LanguageContext';
import { useFamilyTree } from '../../context/FamilyTreeContext';
import EditIcon from '@mui/icons-material/Edit';
import InfoIcon from '@mui/icons-material/Info';
// import DeleteIcon from '@mui/icons-material/Delete'; // Removed for now

// PersonNode component with proper typing
export const PersonNode: React.FC<NodeProps<Node<PersonNodeData>>> = (props) => {
  const { data, id } = props;
  const { mode } = useThemeContext();
  const { t } = useLanguage();
  const { selectPersonById, openEditModal, openDetailModal } = useFamilyTree();

  // Safe access to person data
  const person = data?.person;
  
  if (!person) {
    console.error("PersonNode: Missing person data in node", id);
    return (
      <Paper sx={{ padding: '10px', borderRadius: '8px', width: 130, textAlign: 'center' }}>
        <Typography variant="body2" color="error">
          {t('errorLoadingPerson') || 'Error loading person'}
        </Typography>
      </Paper>
    );
  }

  const handleNodeClick = () => {
    if (selectPersonById && person.id) {
      selectPersonById(person.id);
    }
  };

  const handleEditClick = (event: React.MouseEvent) => {
    event.stopPropagation();
    if (openEditModal) {
      openEditModal(person);
    }
  };

  const handleInfoClick = (event: React.MouseEvent) => {
    event.stopPropagation();
    if (openDetailModal) {
      openDetailModal(person);
    }
  };

  const paperElevation = mode === 'dark' ? 3 : 1;
  const birthYear = person.birthDate ? new Date(person.birthDate).getFullYear() : null;
  const deathYear = person.deathDate ? new Date(person.deathDate).getFullYear() : null;
  const lifeSpan = `${birthYear || 'N/A'}${deathYear ? ` - ${deathYear}` : ''}`;
  const fullName = `${person.firstName || ''} ${person.lastName || ''}`.trim();
  const initials = `${person.firstName?.[0] ?? ''}${person.lastName?.[0] ?? ''}`.toUpperCase();

  return (
    <Paper 
      elevation={paperElevation}
      onClick={handleNodeClick} 
      sx={{ 
        padding: '10px',
        borderRadius: '8px', 
        width: 130, 
        textAlign: 'center',
        position: 'relative',
        cursor: 'pointer',
        border: `1px solid ${mode === 'dark' ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.1)'}`, 
        backgroundColor: mode === 'dark' ? 'grey.800' : 'white',
        '&:hover': {
          borderColor: 'primary.main',
          boxShadow: `0 0 8px ${mode === 'dark' ? 'rgba(100, 180, 255, 0.6)' : 'rgba(0, 120, 255, 0.4)'}`, 
        }
      }}
    >
      {/* Handles must have unique IDs within the node */}
      <Handle 
        type="target" 
        position={Position.Top} 
        id={`${id}-target-top`} // Unique ID
        style={{ background: mode === 'dark' ? '#555' : '#ccc' }}
        isConnectable={false} 
      />
      
      <Avatar 
        alt={fullName}
        sx={{ width: 40, height: 40, margin: 'auto', mb: 1 }}
      >
        {initials}
      </Avatar>
      <Tooltip title={`${fullName} (${lifeSpan})`} placement="top">
        <Typography variant="body2" fontWeight="bold" noWrap sx={{ mb: 0.5 }}>
          {fullName || t('unknownName')}
        </Typography>
      </Tooltip>
      <Typography variant="caption" display="block" color="text.secondary">
        {lifeSpan}
      </Typography>

      <Box sx={{ position: 'absolute', top: 2, right: 2, display: 'flex', flexDirection: 'column', gap: 0.2 }}>
        <Tooltip title={t('editPerson')} placement="left">
          <IconButton 
            size="small" 
            onClick={handleEditClick} 
            sx={{ 
              p: 0.2, 
              backgroundColor: 'rgba(0,0,0,0.05)', 
              "&:hover": { backgroundColor: 'rgba(0,0,0,0.1)' } 
            }}
          >
            <EditIcon fontSize="inherit" />
          </IconButton>
        </Tooltip>
        <Tooltip title={t('personDetails')} placement="left">
          <IconButton 
            size="small" 
            onClick={handleInfoClick} 
            sx={{ 
              p: 0.2, 
              backgroundColor: 'rgba(0,0,0,0.05)', 
              "&:hover": { backgroundColor: 'rgba(0,0,0,0.1)' } 
            }}
          >
            <InfoIcon fontSize="inherit" />
          </IconButton>
        </Tooltip>
      </Box>

      <Handle 
        type="source" 
        position={Position.Bottom} 
        id={`${id}-source-bottom`} // Unique ID
        style={{ background: mode === 'dark' ? '#555' : '#ccc' }}
        isConnectable={false} 
      />
    </Paper>
  );
};

export default PersonNode;