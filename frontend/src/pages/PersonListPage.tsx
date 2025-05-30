import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { 
  Container, Typography, Paper, Box, 
  Card, CardContent, Divider, CircularProgress,
  alpha, useTheme, Alert, IconButton, Button, Tooltip,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, TableSortLabel,
  TextField, InputAdornment
} from '@mui/material';
import { visuallyHidden } from '@mui/utils';
import RefreshIcon from '@mui/icons-material/Refresh';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import SearchIcon from '@mui/icons-material/Search';
import FilterListIcon from '@mui/icons-material/FilterList';
import { Person } from '../types/Person';
import { useNavigate } from 'react-router-dom';
import { useLanguage } from '../context/LanguageContext';
import { useApiRequest } from '../hooks/useApiRequest';
import { personService } from '../services/personService';
import LoadingIndicator from '../components/ui/LoadingIndicator';
import ErrorMessage from '../components/ui/ErrorMessage';
import EmptyState from '../components/ui/EmptyState';

// API URL'i
const API_URL = 'http://localhost:8080/api';

// Sıralama için tür tanımları
type Order = 'asc' | 'desc';
type OrderByKey = 'name' | 'gender' | 'birthDate' | 'deathDate' | 'mother' | 'father' | 'spouse';

// Ağaç görselleştirmesi için gelişmiş Person tipi
interface TreePerson extends Person {
  children: TreePerson[];
}

// Sıralama fonksiyonu
function getComparator<Key extends OrderByKey>(
  order: Order,
  orderBy: Key,
  persons: Person[]
): (a: Person, b: Person) => number {
  return order === 'desc'
    ? (a, b) => descendingComparator(a, b, orderBy, persons)
    : (a, b) => -descendingComparator(a, b, orderBy, persons);
}

// Azalan sıralama karşılaştırıcısı
function descendingComparator<T>(a: T, b: T, orderBy: OrderByKey, persons: Person[]): number {
  // İsim sıralaması için özel durum
  if (orderBy === 'name') {
    const aName = `${(a as any).firstName} ${(a as any).lastName}`.toLowerCase();
    const bName = `${(b as any).firstName} ${(b as any).lastName}`.toLowerCase();
    return bName.localeCompare(aName);
  }
  
  // Ebeveyn ve eş sıralaması için özel durum (güncellendi)
  if (orderBy === 'mother' || orderBy === 'father' || orderBy === 'spouse') {
    const getRelativeName = (person: any, key: OrderByKey) => {
      const relative = (person as any)[key];
      return relative ? `${relative.firstName} ${relative.lastName}`.toLowerCase() : '';
    };

    const aRelativeName = getRelativeName(a, orderBy);
    const bRelativeName = getRelativeName(b, orderBy);

    if (!aRelativeName && !bRelativeName) return 0;
    if (!aRelativeName) return 1; // null/undefined değerleri sona atsın
    if (!bRelativeName) return -1; // null/undefined değerleri sona atsın
    
    return bRelativeName.localeCompare(aRelativeName);
  }
  
  // Tarih sıralaması için özel durum
  if (orderBy === 'birthDate' || orderBy === 'deathDate') {
    const aDate = (a as any)[orderBy] ? new Date((a as any)[orderBy]).getTime() : 0;
    const bDate = (b as any)[orderBy] ? new Date((b as any)[orderBy]).getTime() : 0;
    
    if (aDate === 0 && bDate === 0) return 0;
    if (aDate === 0) return 1;
    if (bDate === 0) return -1;
    
    return bDate - aDate;
  }
  
  // Diğer tüm değerler için
  if ((b as any)[orderBy] < (a as any)[orderBy]) {
    return -1;
  }
  if ((b as any)[orderBy] > (a as any)[orderBy]) {
    return 1;
  }
  return 0;
}

const PersonListPage: React.FC = () => {
  const navigate = useNavigate();
  const theme = useTheme();
  const { t } = useLanguage();
  const [selectedPerson, setSelectedPerson] = useState<Person | null>(null);
  const [searchValue, setSearchValue] = useState<string>('');
  const [order, setOrder] = useState<Order>('asc');
  const [orderBy, setOrderBy] = useState<OrderByKey>('name');

  const getAllPersonsRequest = useCallback(
    () => personService.getAllPersons().then(response => {
      // API yanıtı doğrudan dizi veya sayfalanmış içerik olabilir
      if (Array.isArray(response)) {
        return response;
      } else if (response && response.content) {
        return response.content;
      } else {
        console.warn('Beklenmeyen API yanıt formatı:', response);
        return [];
      }
    }),
    []
  );

  const { data: persons = [], loading, error, refetch } = useApiRequest(['persons'], getAllPersonsRequest);

  // Sıralama değişikliği
  const handleRequestSort = (property: OrderByKey) => {
    const isAsc = orderBy === property && order === 'asc';
    setOrder(isAsc ? 'desc' : 'asc');
    setOrderBy(property);
  };

  const handleAddPerson = () => {
    navigate('/persons/new');
  };

  const handleRefreshData = () => {
    refetch();
  };

  const handlePersonSelect = (person: Person) => {
    setSelectedPerson(person);
    console.log('Seçilen kişi:', person);
  };

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(event.target.value);
  };

  // Filtrelenmiş kişiler - arama sonuçlarına göre
  const filteredPersons = useMemo(() => {
    if (!Array.isArray(persons)) return [];
    if (!searchValue.trim()) {
      return persons;
    }
    const searchTerms = searchValue.toLowerCase().split(' ').filter(term => term.length > 0);
    return persons.filter(person => {
      const fullName = `${person.firstName} ${person.lastName}`.toLowerCase();
      // Her bir arama terimi için kontrol et
      return searchTerms.every(term => fullName.includes(term));
    });
  }, [searchValue, persons]);

  // Sıralanmış ve filtrelenmiş kişiler
  const sortedAndFilteredPersons = useMemo(() => {
    if (!Array.isArray(filteredPersons)) return [];
    return [...filteredPersons].sort(getComparator(order, orderBy, Array.isArray(persons) ? persons : []));
  }, [filteredPersons, order, orderBy, persons]);

  // Kişiler arasındaki ilişkileri kurarak ağaç yapısı oluştur
  const prepareTreeData = useCallback(() => {
    if (!persons || persons.length === 0) {
      console.log('Kişi verisi bulunamadı');
      return [];
    }

    console.log(`🌳 Ağaç verileri hazırlanıyor - ${persons.length} kişi`);

    try {
      // Önce her kişiyi ID'sine göre bir haritada sakla
      const personMap = new Map<number, TreePerson>();

      // Tüm kişileri kopyala ve TreePerson tipinde bir harita oluştur
      persons.forEach((person: Person) => {
        if (person && person.id) {
          personMap.set(person.id, { 
            ...person, 
            children: [] 
          } as TreePerson);
        }
      });

      console.log(`🗺️ Harita oluşturuldu - ${personMap.size} kişi`);

      // Eş ilişkilerini kur
      personMap.forEach((personNode) => {
        if (personNode.motherId) {
          const mother = personMap.get(personNode.motherId);
          if (mother) {
            mother.children.push(personNode);
          }
        }
        if (personNode.fatherId) {
          const father = personMap.get(personNode.fatherId);
          if (father) {
            father.children.push(personNode);
          }
        }
        // Eş bilgisini de TreePerson'a ekleyelim, eğer varsa
        if (personNode.spouseId) {
          const spouse = personMap.get(personNode.spouseId);
          if (spouse) {
            // Dairesel referansı önlemek için spouse nesnesinin tamamını değil,
            // temel bilgilerini veya sadece ID'sini atayabiliriz.
            // Şimdilik, spouse nesnesini atıyoruz ama JSON.stringify'da ele alacağız.
            personNode.spouse = spouse;
          }
        }
      });

      // Kök düğümleri bul (anne-babası olmayan kişiler)
      const rootNodes: TreePerson[] = [];
      personMap.forEach((person: TreePerson) => {
        // Anne-babası olmayan kişiler kök olabilir
        if (!person.motherId && !person.fatherId) {
          rootNodes.push(person);
        }
      });

      console.log(`🌱 Kök düğüm sayısı: ${rootNodes.length}`);

      // Eğer kök bulunamadıysa, herkesi düz liste yap
      if (rootNodes.length === 0) {
        console.log('⚠️ Kök düğüm bulunamadı, tüm kişiler liste olarak gösteriliyor');
        return Array.from(personMap.values());
      }

      // Döngüsel referans sorunu olmaması için, verileri JSON serileştirme ile temizle
      // const processedData = JSON.parse(JSON.stringify(rootNodes));

      // JSON.stringify için özel bir replacer fonksiyonu
      const replacer = (key: string, value: any) => {
        if (key === "spouse" && value && typeof value === 'object' && value.id) {
          // Eş nesnesi yerine sadece ID'sini veya temel bir temsilini döndür
          // Bu, dairesel bağımlılığı kırar.
          // Örneğin, sadece ID'yi döndürebiliriz: return value.id;
          // Veya daha basit bir nesne:
          return { id: value.id, firstName: value.firstName, lastName: value.lastName };
        }
        if (key === "children" && Array.isArray(value)) {
          // Çocuklar dizisindeki her bir çocuk için de aynı mantığı uygulayabiliriz
          // Ancak bu, ağacın derinliğini sınırlar.
          // Genellikle çocuklar zaten işlenmiş ve dairesel olmamalıdır.
          // Eğer çocuklarda da dairesel referans varsa, bu kısmı da ele almak gerekir.
        }
        // Diğer tüm değerler için varsayılan davranışı kullan
        return value;
      };
      
      // JSON.stringify'ı replacer ile kullan
      const processedData = JSON.parse(JSON.stringify(rootNodes, replacer));
      return processedData;
    } catch (err) {
      console.error('❌ Ağaç verisi hazırlama hatası:', err);
      return [];
    }
  }, [persons]);

  // Sütun başlıkları için yapılandırma
  const headCells = [
    { id: 'name', label: t('nameLastname') },
    { id: 'gender', label: t('gender') },
    { id: 'birthDate', label: t('birthDate') },
    { id: 'deathDate', label: t('deathDate') },
    { id: 'mother', label: t('mother') },
    { id: 'father', label: t('father') },
    { id: 'spouse', label: t('spouse') },
  ];

  if (loading) {
    return <LoadingIndicator />;
  }

  if (error) {
    return <ErrorMessage message={error} />;
  }

  if (!loading && Array.isArray(persons) && persons.length === 0) {
    return <EmptyState message={t('noPersonsFound')} />;
  }

  // Kişi ve aile sayısı bilgilerini hazırla
  const totalPersons = Array.isArray(persons) ? persons.length : 0;
  const filteredCount = Array.isArray(filteredPersons) ? filteredPersons.length : 0;
  const familyCount = Array.isArray(persons) ? new Set((persons as Person[]).filter((p: Person) => p.familyTreeId).map((p: Person) => p.familyTreeId)).size : 0;

  // Ağaç verilerini hazırla
  const treeData = prepareTreeData();

  return (
    <Container maxWidth="xl" sx={{ mt: 3, mb: 4 }}>
      <Paper elevation={3} sx={{ p: 3, borderRadius: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Typography variant="h5" gutterBottom>
            {t('persons')}
          </Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Tooltip title={t('refresh')}>
              <IconButton 
                onClick={handleRefreshData}
                sx={{ 
                  bgcolor: alpha(theme.palette.primary.main, 0.1),
                  '&:hover': { bgcolor: alpha(theme.palette.primary.main, 0.2) }
                }}
              >
                <RefreshIcon />
              </IconButton>
            </Tooltip>
            <Button
              variant="contained"
              color="primary"
              startIcon={<PersonAddIcon />}
              onClick={handleAddPerson}
            >
              {t('addNewPerson')}
            </Button>
          </Box>
        </Box>

        {/* Arama alanı */}
        <Box sx={{ mb: 3 }}>
          <TextField
            fullWidth
            variant="outlined"
            size="small"
            placeholder={t('searchPerson')}
            value={searchValue}
            onChange={handleSearchChange}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
              endAdornment: (
                <InputAdornment position="end">
                  <Tooltip title={t('filter')}>
                    <IconButton edge="end">
                      <FilterListIcon />
                    </IconButton>
                  </Tooltip>
                </InputAdornment>
              )
            }}
            sx={{ bgcolor: alpha(theme.palette.background.paper, 0.6) }}
          />
        </Box>

        {error && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {error}
          </Alert>
        )}

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}>
            <CircularProgress />
            <Typography variant="body1" sx={{ ml: 2 }}>
              {t('loading')}
            </Typography>
          </Box>
        ) : (
          <TableContainer component={Paper} sx={{ boxShadow: 'none', overflow: 'auto' }}>
            <Table sx={{ minWidth: 750 }} size="medium">
              <TableHead>
                <TableRow>
                  {headCells.map((headCell) => (
                    <TableCell
                      key={headCell.id}
                      align={headCell.numeric ? 'right' : 'left'}
                      padding={headCell.disablePadding ? 'none' : 'normal'}
                      sortDirection={orderBy === headCell.id ? order : false}
                    >
                      <TableSortLabel
                        active={orderBy === headCell.id}
                        direction={orderBy === headCell.id ? order : 'asc'}
                        onClick={() => handleRequestSort(headCell.id as OrderByKey)}
                      >
                        {headCell.label}
                        {orderBy === headCell.id ? (
                          <Box component="span" sx={visuallyHidden}>
                            {order === 'desc' ? 'sorted descending' : 'sorted ascending'}
                          </Box>
                        ) : null}
                      </TableSortLabel>
                    </TableCell>
                  ))}
                  <TableCell>{t('actions')}</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {sortedAndFilteredPersons.map((person) => (
                  <TableRow
                    hover
                    onClick={() => handlePersonSelect(person)}
                    key={person.id}
                    sx={{ cursor: 'pointer', '&:hover': { backgroundColor: alpha(theme.palette.action.hover, 0.1) } }}
                  >
                    <TableCell component="th" scope="row">
                      {`${person.firstName} ${person.lastName}`}
                    </TableCell>
                    <TableCell>{person.gender ? t(`gender.${person.gender.toLowerCase()}`) : '-'}</TableCell>
                    <TableCell>{person.birthDate ? new Date(person.birthDate).toLocaleDateString() : '-'}</TableCell>
                    <TableCell>{person.deathDate ? new Date(person.deathDate).toLocaleDateString() : '-'}</TableCell>
                    <TableCell>{person.mother ? `${person.mother.firstName} ${person.mother.lastName}` : '-'}</TableCell>
                    <TableCell>{person.father ? `${person.father.firstName} ${person.father.lastName}` : '-'}</TableCell>
                    <TableCell>{person.spouse ? `${person.spouse.firstName} ${person.spouse.lastName}` : '-'}</TableCell>
                    <TableCell>
                      <Button 
                        variant="outlined" 
                        size="small" 
                        onClick={(e) => { e.stopPropagation(); navigate(`/persons/${person.id}`); }}
                        sx={{ mr: 1 }}
                      >
                        {t('viewDetails')}
                      </Button>
                      {/* Silme ve düzenleme butonları buraya eklenebilir */}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Paper>
    </Container>
  );
};

export default PersonListPage; 