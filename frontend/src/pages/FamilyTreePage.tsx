import React, { useState, useMemo, useCallback, useEffect } from 'react';
import { Container, Typography, Paper, Box, Divider, FormControl,   InputLabel, Select, MenuItem, SelectChangeEvent, Button,   Alert, TextField, InputAdornment,  ListItem, ListItemButton, List, ListItemText, alpha, Tabs, Tab,  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TableSortLabel,  Pagination } from '@mui/material';import Grid from '@mui/material/Grid';
import { Person } from '../types/Person';
// import { personService } from '../services/personService'; // Context'ten gelecek
import ProvidedFamilyTreeReactFlow from '../components/FamilyTree/FamilyTreeReactFlow';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import RefreshIcon from '@mui/icons-material/Refresh';
import SearchIcon from '@mui/icons-material/Search';
// import PersonIcon from '@mui/icons-material/Person';
// import PeopleIcon from '@mui/icons-material/People';
// import FilterListIcon from '@mui/icons-material/FilterList';
import { useLanguage } from '../context/LanguageContext';
import { useTheme } from '@mui/material/styles';
import { useThemeContext } from '../context/ThemeContext';
// import { useApiRequest } from '../hooks/useApiRequest'; // Context'ten gelecek
import LoadingIndicator from '../components/ui/LoadingIndicator';
import ErrorMessage from '../components/ui/ErrorMessage';
import EmptyState from '../components/ui/EmptyState';
// import LanguageSelector from '../components/settings/LanguageSelector';
import { useFamilyTree } from '../context/FamilyTreeContext'; // Yeni context hook'u
import FamilyTree from '../components/FamilyTree/FamilyTreeReactFlow'; // Düzeltildi: Doğrudan bileşen importu
// import PersonEditModal from '../components/PersonForm/PersonEditModal'; // Yorum satırı: Dosya bulunamadı
import PersonDetailModal from '../components/PersonDetailModal';

import FamilyTreeControls from '../components/FamilyTreePageControls/FamilyTreeControls';
import PersonListTable, { OrderByKey } from '../components/FamilyTreePageControls/PersonListTable';

// Sıralama için tür tanımları
type Order = 'asc' | 'desc';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`family-tabpanel-${index}`}
      aria-labelledby={`family-tab-${index}`}
      {...other}
      style={{ width: '100%' }}
    >
      {value === index && (
        <Box sx={{ py: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
}

function a11yProps(index: number) {
  return {
    id: `family-tab-${index}`,
    'aria-controls': `family-tabpanel-${index}`,
  };
}

// Güncellenmiş Sıralama Fonksiyonları
function descendingComparator<T>(
  a: T, 
  b: T, 
  orderBy: OrderByKey, 
  persons: Person[], 
  orderDirection: Order // orderDirection eklendi
): number {
  const getFullName = (personId?: number) => {
    if (!personId) return '';
    const person = persons.find(p => p.id === personId);
    return person ? `${person.firstName} ${person.lastName}`.toLowerCase() : '';
  };

  const getDateValue = (dateString?: string) => dateString ? new Date(dateString).getTime() : 0;

  let valA: any;
  let valB: any;

  if (orderBy === 'name') {
    valA = `${(a as any).firstName} ${(a as any).lastName}`.toLowerCase();
    valB = `${(b as any).firstName} ${(b as any).lastName}`.toLowerCase();
  } else if (orderBy === 'mother' || orderBy === 'father' || orderBy === 'spouse') {
    // OrderByKey tipi ile Person'daki alan adları uyumlu
    const personA = (a as any)[orderBy];
    const personB = (b as any)[orderBy];
    valA = personA ? `${personA.firstName} ${personA.lastName}`.toLowerCase() : '';
    valB = personB ? `${personB.firstName} ${personB.lastName}`.toLowerCase() : '';
  } else if (orderBy === 'birthDate' || orderBy === 'deathDate') {
    valA = getDateValue((a as any)[orderBy]);
    valB = getDateValue((b as any)[orderBy]);
  } else {
    valA = (a as any)[orderBy];
    valB = (b as any)[orderBy];
  }

  // Null/undefined/boş string kontrolü
  const isNullOrEmpty = (value: any) => value === null || value === undefined || value === '' || value === 0;

  const aIsEmpty = isNullOrEmpty(valA);
  const bIsEmpty = isNullOrEmpty(valB);

  if (aIsEmpty && bIsEmpty) return 0;
  if (aIsEmpty) return orderDirection === 'desc' ? -1 : 1; // Boş/null değerler sıralama yönüne göre sonda
  if (bIsEmpty) return orderDirection === 'desc' ? 1 : -1; // Boş/null değerler sıralama yönüne göre sonda

  // Karşılaştırma
  if (typeof valA === 'string' && typeof valB === 'string') {
    return orderDirection === 'desc' ? valB.localeCompare(valA) : valA.localeCompare(valB);
  }
  if (typeof valA === 'number' && typeof valB === 'number') {
    return orderDirection === 'desc' ? valB - valA : valA - valB;
  }
  
  // Fallback (genellikle olmamalı, tipler tutarlıysa)
  if (valB < valA) return orderDirection === 'desc' ? 1 : -1; 
  if (valB > valA) return orderDirection === 'desc' ? -1 : 1;
  return 0;
}

const FamilyTreePage: React.FC = () => {
  const theme = useTheme();
  const { t } = useLanguage();
  const { mode } = useThemeContext();
  const { 
    allPersons, 
    selectedPerson, // Artık seçili Person nesnesini alıyoruz
    treeData, 
    loading, 
    error, 
    fetchPersons: refetch, // Context'teki fetchPersons fonksiyonunu refetch olarak kullan
    selectPersonById, // Context'teki seçme fonksiyonu
    isEditModalOpen, // Assuming PersonEditModal is already handled
  } = useFamilyTree();
  
  // Yerel state'ler (Context tarafından yönetilmeyenler)
  // const [selectedPerson, setSelectedPerson] = useState<number | undefined>(undefined); // Context'e taşındı
  const [searchValue, setSearchValue] = useState<string>('');
  const [showSearchResults, setShowSearchResults] = useState<boolean>(false);
  const [tabValue, setTabValue] = useState(0);
  
  // Sıralama durumu (Bu hala yerel kalabilir, çünkü sadece tabloyu etkiliyor)
  const [order, setOrder] = useState<Order>('asc');
  const [orderBy, setOrderBy] = useState<OrderByKey>('name');

  // Sayfalama için state'ler (Bu da yerel kalabilir)
  const [page, setPage] = useState<number>(0);
  const [rowsPerPage, setRowsPerPage] = useState<number>(10);
  // const [totalPersons, setTotalPersons] = useState<number>(0); // allPersons.length kullanılabilir

  // API isteği hook'u kaldırıldı, context kullanılacak
  // const getAllPersonsRequest = useCallback(
  //   () => personService.getAllPersons().then(res => res.data),
  //   []
  // );
  // const { data: persons = [], loading, error, refetch } = useApiRequest(getAllPersonsRequest);

  // totalPersons artık doğrudan allPersons.length'den türetilebilir
  const totalPersons = useMemo(() => allPersons.length, [allPersons]);

  // useEffect(() => {
  //   if (Array.isArray(persons)) {
  //     setTotalPersons(persons.length);
  //   } else {
  //     setTotalPersons(0); // Eğer persons bir dizi değilse veya tanımsızsa sıfırla
  //   }
  // }, [persons]);

  // Sıralama değişikliğinde ilk sayfaya dön
  // Bu useEffect hala geçerli, ancak bağımlılık allPersons olacak
  React.useEffect(() => {
    setPage(0);
  }, [orderBy, order, allPersons]); // allPersons eklendi, veri değiştiğinde de sıfırlanmalı


  // Filtrelenmiş ve Sayfalanmış Kişiler
  const filteredPersons = useMemo(() => {
    if (!Array.isArray(allPersons)) return [];
    return allPersons.filter(person => 
      `${person.firstName} ${person.lastName}`.toLowerCase().includes(searchValue.toLowerCase())
    );
  }, [allPersons, searchValue]);

  const sortedPersons = useMemo(() => {
    return [...filteredPersons].sort((a, b) => descendingComparator(a, b, orderBy, allPersons, order));
  }, [filteredPersons, order, orderBy, allPersons]);

  const currentTablePersons = useMemo(() => {
    // Sayfala
    return sortedPersons.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage);
  }, [sortedPersons, page, rowsPerPage]);

  // Arama sonuçları için ayrı filtreleme (sadece arama kutusu için)
  const searchResults = useMemo(() => {
    if (!searchValue.trim()) return [];
    return allPersons.filter(person => 
      `${person.firstName} ${person.lastName}`.toLowerCase().includes(searchValue.toLowerCase())
    ).slice(0, 5); // İlk 5 sonucu göster
  }, [allPersons, searchValue]);


  // Sayfalama bileşeni
  const renderPagination = useMemo(() => {
    // Arama aktifken sayfalama gösterme (ya da filtrelenmiş kişilere göre ayarla)
    // Şimdilik, arama yapılıyorsa (searchValue doluysa) tüm sonuçlar tek sayfada gösterilecek gibi varsayalım
    // veya arama sonuçlarına göre sayfalama yapalım?
    // Şimdilik basit tutalım: Arama aktifse gösterme
    if (searchValue.trim() || totalPersons <= rowsPerPage) return null;
    
    const pageCount = Math.ceil(totalPersons / rowsPerPage);
    
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2, mb: 2 }}>
        <Pagination
          count={pageCount}
          page={page + 1}
          onChange={(_event, newPage) => setPage(newPage - 1)}
          color="primary"
          showFirstButton
          showLastButton
          siblingCount={1}
        />
      </Box>
    );
  }, [totalPersons, rowsPerPage, page, searchValue]); // searchValue eklendi

  const handlePersonChange = (event: SelectChangeEvent<number>) => {
    selectPersonById(event.target.value as number); // Context fonksiyonunu çağır
    setShowSearchResults(false);
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setSearchValue(value);
    setShowSearchResults(value.trim().length > 0);
  };

  const handleSearchSelect = (personId: number) => {
    selectPersonById(personId); // Context fonksiyonunu çağır
    setSearchValue(''); // Arama kutusunu temizle
    setShowSearchResults(false);
    // Kişi seçildiğinde ilk sekmeye geç
    setTabValue(0);
  };
  
  const handleKeyDown = (event: React.KeyboardEvent) => {
    // Enter tuşuna basıldığında ve arama sonuçları varsa ilk kişiyi seç
    if (event.key === 'Enter' && searchResults.length > 0) {
      handleSearchSelect(searchResults[0].id);
    }
    // Escape tuşuna basıldığında arama sonuçlarını kapat
    else if (event.key === 'Escape') {
      setShowSearchResults(false);
      setSearchValue('');
    }
  };

  // Sekme değişikliği
  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  // Sıralama değişikliği
  const handleRequestSort = (property: OrderByKey) => {
    const isAsc = orderBy === property && order === 'asc';
    setOrder(isAsc ? 'desc' : 'asc');
    setOrderBy(property);
  };

  // Sütun başlıkları için yapılandırma
  const headCells = [
    { id: 'name' as OrderByKey, label: t('nameLastName') }, // t() kullanıldı
    { id: 'gender' as OrderByKey, label: t('gender') },     // t() kullanıldı
    { id: 'birthDate' as OrderByKey, label: t('birthDate') }, // t() kullanıldı
    { id: 'deathDate' as OrderByKey, label: t('deathDate') }, // t() kullanıldı
    { id: 'motherId' as OrderByKey, label: t('mother') },    // t() kullanıldı
    { id: 'fatherId' as OrderByKey, label: t('father') },    // t() kullanıldı
    { id: 'spouseId' as OrderByKey, label: t('spouse') },    // t() kullanıldı
  ];

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
      <Paper 
        elevation={3} 
        sx={{ 
          p: 3, 
          borderRadius: 2,
          backgroundColor: mode === 'dark' ? 'background.paper' : 'rgba(240, 245, 255, 0.6)',
          border: mode === 'dark' ? '1px solid rgba(80, 80, 80, 0.3)' : '1px solid rgba(200, 220, 240, 0.3)',
          boxShadow: mode === 'dark' ? '0 8px 20px rgba(0,0,0,0.15)' : '0 8px 20px rgba(0,0,0,0.08)'
        }}
      >
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h5" fontWeight={600} color="primary" gutterBottom sx={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: 1,
            '& svg': { fontSize: 28 }
          }}>
            <AccountTreeIcon />
            {t('familyTree')}
          </Typography>
          <Button
            variant="contained"
            color="primary"
            startIcon={<RefreshIcon />}
            onClick={refetch} // Context'ten gelen refetch
            disabled={loading} // Context'ten gelen loading
            sx={{ borderRadius: 8, px: 3 }}
          >
            {t('refresh')}
          </Button>
        </Box>

        {/* Hata durumunu göster */}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        {/* Yükleme durumu artık tüm sayfa için geçerli değil, belirli alanlar için context'ten alınacak */}
        {/* Sadece ilk yükleme veya refetch sırasında genel bir yükleme göstergesi olabilir */}
        {loading && allPersons.length === 0 && <LoadingIndicator />} 

        {/* Veri yoksa veya yüklenememişse */}
        {!loading && allPersons.length === 0 && !error && <EmptyState message={t('noPersonsFound')} />}

        {/* Kişiler yüklendiyse içeriği göster */}
        {allPersons.length > 0 && (
          <>
            {/* Kontrol Paneli: Kişi Seçme, Arama ve Sekmeler ÜSTTE */}
            <Box sx={{ 
              display: 'flex', 
              flexDirection: { xs: 'column', sm: 'row' }, // Küçük ekranlarda alt alta, genişte yan yana
              alignItems: 'flex-start', 
              gap: 2, 
              mb: 2, // Kontroller ve sekmeler arası boşluk
              position: 'relative' // Arama sonuçları için
            }}>
              {/* Kişi Seçme Dropdown */}
              <FormControl 
                fullWidth 
                sx={{ 
                  minWidth: { xs: '100%', sm: 250 }, // Küçük ekranda tam genişlik
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
                  value={selectedPerson?.id ?? ''}
                  label={t('selectPerson')}
                  onChange={handlePersonChange}
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
              <Box sx={{ flex: { sm: 1 }, position: 'relative', width: '100%' }}> {/* Küçük ekranda tam genişlik */}
                <TextField
                  fullWidth
                  variant="outlined"
                  label={t('searchPerson')}
                  value={searchValue}
                  onChange={handleSearchChange}
                  onKeyDown={handleKeyDown}
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
                        <ListItemButton key={person.id} onClick={() => handleSearchSelect(person.id)}>
                          <ListItemText primary={`${person.firstName} ${person.lastName}`} />
                        </ListItemButton>
                      ))}
                    </List>
                  </Paper>
                )}
              </Box>
            </Box>

            <Divider sx={{ my: 2 }} />

            {/* Sekmeler */}      
            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
              <Tabs value={tabValue} onChange={handleTabChange} aria-label="family view tabs" centered>
                <Tab label={t('familyTree')} {...a11yProps(0)} />
                <Tab label={t('personList')} {...a11yProps(1)} />
              </Tabs>
            </Box>

            {/* Sekme İçerikleri */}
            <Box sx={{ flexGrow: 1, width: '100%'}}> {/* Kapsayıcı Box eklendi */}
              <TabPanel value={tabValue} index={0}>
                {treeData ? (
                  <FamilyTree /> /* Doğrudan FamilyTree bileşeni */
                ) : (
                  <Box sx={{display: 'flex', justifyContent: 'center', alignItems: 'center', height: 'calc(100vh - 330px)', /* Yüksekliği koruyabiliriz */ p: 3 }}>
                     <Typography variant="subtitle1" color="textSecondary">
                      {selectedPerson ? t('loadingTree') : t('selectPersonToViewTree')}
                    </Typography>
                  </Box>
                )}
              </TabPanel>
              <TabPanel value={tabValue} index={1}>
                <PersonListTable
                  currentPersons={currentTablePersons}
                  allPersons={allPersons} 
                  selectedPersonId={selectedPerson?.id}
                  order={order}
                  orderBy={orderBy}
                  headCells={headCells}
                  loading={loading} 
                  searchValue={searchValue} 
                  mode={mode}
                  t={t}
                  onRowClick={(personId) => { selectPersonById(personId); setTabValue(0); }}
                  onRequestSort={handleRequestSort}
                  page={page}
                  rowsPerPage={rowsPerPage}
                  totalPersons={totalPersons} 
                  onPageChange={(_event, newPage) => setPage(newPage - 1)}
                />
              </TabPanel>
            </Box> {/* Kapsayıcı Box kapatıldı */}
          </>
        )}
      </Paper>

      {/* Modals */}
      {/* <PersonEditModal /> */}
      <PersonDetailModal />
    </Container>
  );
};

export default FamilyTreePage; 