import React from 'react';
import { FormControl, InputLabel, Select, MenuItem, TextField, InputAdornment, Paper, List, ListItemButton, ListItemText, Box, SelectChangeEvent } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { Person } from '../../types/Person'; // Projenizdeki Person tipinin yolu
import { ThemeContextType } from '../../context/ThemeContext'; // Projenizdeki ThemeContextType tipinin yolu

interface FamilyTreeControlsProps {
  allPersons: Person[];
  selectedPersonId?: number | string; // Seçili kişinin ID'si, string olabilir çünkü Select value string dönebilir
  searchValue: string;
  loading: boolean;
  showSearchResults: boolean;
  searchResults: Person[];
  mode: ThemeContextType['mode'];
  t: (key: string) => string; // i18n çeviri fonksiyonu
  onPersonChange: (event: SelectChangeEvent<string | number>) => void; // Değişiklik burada: number | string
  onSearchChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onSearchSelect: (personId: number) => void;
  onSearchKeyDown: (event: React.KeyboardEvent<HTMLDivElement>) => void; // TextField onKeyDown HTMLDivElement bekler
}

const FamilyTreeControls: React.FC<FamilyTreeControlsProps> = ({
  allPersons,
  selectedPersonId,
  searchValue,
  loading,
  showSearchResults,
  searchResults,
  mode,
  t,
  onPersonChange,
  onSearchChange,
  onSearchSelect,
  onSearchKeyDown,
}) => {
  return (
    <Box sx={{ 
      display: 'flex', 
      flexDirection: { xs: 'column', sm: 'row' }, 
      alignItems: 'flex-start', 
      gap: 2, 
      mb: 2, 
      position: 'relative' 
    }}>
      {/* Kişi Seçme Dropdown */} 
      <FormControl 
        fullWidth 
        sx={{
          minWidth: { xs: '100%', sm: 250 },
          flex: { sm: '0 1 300px' }, 
          backgroundColor: mode === 'dark' ? 'background.default' : 'white',
          borderRadius: 2,
          boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
        }}
      >
        <InputLabel id="person-select-label">{t('selectPerson')}</InputLabel>
        <Select
          labelId="person-select-label"
          id="person-select"
          value={selectedPersonId ?? ''} // selectedPersonId string veya number olabilir
          label={t('selectPerson')}
          onChange={onPersonChange}
          disabled={loading}
          MenuProps={{ PaperProps: { sx: { maxHeight: 300 } } }}
        >
          {allPersons.sort((a,b) => `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`)).map((person) => (
            <MenuItem key={person.id} value={person.id}>
              {`${person.firstName} ${person.lastName}`}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      {/* Arama Kutusu */} 
      <Box sx={{ flex: { sm: 1 }, position: 'relative', width: '100%' }}>
        <TextField
          fullWidth
          variant="outlined"
          label={t('searchPerson')}
          value={searchValue}
          onChange={onSearchChange}
          onKeyDown={onSearchKeyDown}
          disabled={loading}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
            sx: {
              backgroundColor: mode === 'dark' ? 'background.default' : 'white',
              borderRadius: 2,
              boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
            }
          }}
        />
        {showSearchResults && searchResults.length > 0 && (
          <Paper elevation={4} sx={{ 
            position: 'absolute', 
            top: '100%', 
            left: 0, 
            right: 0, 
            zIndex: 10,
            mt: 0.5,
            maxHeight: 200,
            overflowY: 'auto' 
          }}>
            <List dense>
              {searchResults.map((person) => (
                <ListItemButton key={person.id} onClick={() => onSearchSelect(person.id)}>
                  <ListItemText primary={`${person.firstName} ${person.lastName}`} />
                </ListItemButton>
              ))}
            </List>
          </Paper>
        )}
      </Box>
    </Box>
  );
};

export default FamilyTreeControls; 