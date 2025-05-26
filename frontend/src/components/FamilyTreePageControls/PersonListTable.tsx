import React from 'react';
import {
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TableSortLabel, 
  Paper, Box, Typography, alpha, Pagination
} from '@mui/material';
import { Person } from '../../types/Person'; // Projenizdeki Person tipinin yolu
import { ThemeContextType } from '../../context/ThemeContext'; // Projenizdeki ThemeContextType tipinin yolu
import { useTheme } from '@mui/material/styles'; // useTheme hook'unu import ediyoruz

type Order = 'asc' | 'desc';
export type OrderByKey = 'name' | 'gender' | 'birthDate' | 'deathDate' | 'motherId' | 'fatherId' | 'spouseId';

interface HeadCell {
  id: OrderByKey;
  label: string;
  disablePadding?: boolean;
  numeric?: boolean;
}

interface PersonListTableProps {
  currentPersons: Person[];
  allPersons: Person[]; // For looking up related names
  selectedPersonId?: number;
  order: Order;
  orderBy: OrderByKey;
  headCells: HeadCell[];
  loading: boolean;
  searchValue: string;
  mode: ThemeContextType['mode'];
  t: (key: string, params?: object) => string;
  onRowClick: (personId: number) => void;
  onRequestSort: (property: OrderByKey) => void;
  // Pagination props
  page: number;
  rowsPerPage: number;
  totalPersons: number;
  onPageChange: (event: React.ChangeEvent<unknown>, newPage: number) => void;
}

const PersonListTable: React.FC<PersonListTableProps> = ({
  currentPersons,
  allPersons,
  selectedPersonId,
  order,
  orderBy,
  headCells,
  loading,
  searchValue,
  mode,
  t,
  onRowClick,
  onRequestSort,
  page,
  rowsPerPage,
  totalPersons,
  onPageChange,
}) => {
  const theme = useTheme(); // Temayı burada alıyoruz

  const renderPagination = () => {
    if (searchValue.trim() || totalPersons <= rowsPerPage) return null;
    
    const pageCount = Math.ceil(totalPersons / rowsPerPage);
    if (pageCount <= 1) return null; // Tek sayfa varsa gösterme

    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2, mb: 2 }}>
        <Pagination
          count={pageCount}
          page={page + 1} // MUI Pagination 1-indexed
          onChange={onPageChange}
          color="primary"
          showFirstButton
          showLastButton
          siblingCount={1}
        />
      </Box>
    );
  };

  return (
    <>
      <TableContainer 
        component={Paper} 
        elevation={0} 
        sx={{ 
          border: '1px solid', 
          borderColor: theme.palette.divider, 
          borderRadius: 1,
          backgroundColor: mode === 'dark' ? alpha(theme.palette.background.paper, 0.6) : alpha(theme.palette.background.paper, 0.8),
          maxHeight: 'calc(100vh - 400px)', // Yüksekliği parent'a göre ayarlamak gerekebilir
          overflowY: 'auto' 
        }}
      >
        <Table stickyHeader sx={{ minWidth: 650 }} aria-label={t('personList')}>
          <TableHead>
            <TableRow sx={{ th: { fontWeight: 'bold', backgroundColor: mode === 'dark' ? alpha(theme.palette.background.default, 0.9) : alpha(theme.palette.grey[200], 0.9) } }}>
              {headCells.map((headCell) => (
                <TableCell
                  key={headCell.id}
                  align="left"
                  sortDirection={orderBy === headCell.id ? order : false}
                >
                  <TableSortLabel
                    active={orderBy === headCell.id}
                    direction={orderBy === headCell.id ? order : 'asc'}
                    onClick={() => onRequestSort(headCell.id)}
                  >
                    {headCell.label}
                  </TableSortLabel>
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {currentPersons.map((person) => (
              <TableRow 
                key={person.id}
                onClick={() => onRowClick(person.id)}
                hover
                selected={selectedPersonId === person.id}
                sx={{
                  cursor: 'pointer',
                  bgcolor: selectedPersonId === person.id 
                    ? alpha(theme.palette.primary.main, mode === 'dark' ? 0.25 : 0.15)
                    : 'inherit',
                  '&:last-child td, &:last-child th': { border: 0 },
                  '&.Mui-selected': {
                    bgcolor: alpha(theme.palette.primary.main, mode === 'dark' ? 0.35 : 0.25),
                    '&:hover': {
                      bgcolor: alpha(theme.palette.primary.main, mode === 'dark' ? 0.45 : 0.35),
                    }
                  }
                }}
              >
                <TableCell>{`${person.firstName} ${person.lastName}`}</TableCell>
                <TableCell>{t(person.gender.toLowerCase() as 'male' | 'female' | 'other') || person.gender}</TableCell>
                <TableCell>{person.birthDate ? new Date(person.birthDate).toLocaleDateString() : '-'}</TableCell>
                <TableCell>{person.deathDate ? new Date(person.deathDate).toLocaleDateString() : '-'}</TableCell>
                <TableCell>
                  {person.motherId ? (allPersons.find(p => p.id === person.motherId)?.firstName + ' ' + allPersons.find(p => p.id === person.motherId)?.lastName) : '-'}
                </TableCell>
                <TableCell>
                  {person.fatherId ? (allPersons.find(p => p.id === person.fatherId)?.firstName + ' ' + allPersons.find(p => p.id === person.fatherId)?.lastName) : '-'}
                </TableCell>
                <TableCell>
                  {person.spouseId ? (allPersons.find(p => p.id === person.spouseId)?.firstName + ' ' + allPersons.find(p => p.id === person.spouseId)?.lastName) : '-'}
                </TableCell>
              </TableRow>
            ))}
            {currentPersons.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={headCells.length} align="center">
                  <Typography sx={{p: 2}}>
                    {searchValue.trim() ? t('noSearchResults') : t('noData')}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
      {renderPagination()}
    </>
  );
};

export default PersonListTable; 