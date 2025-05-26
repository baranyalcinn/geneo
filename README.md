# Aile Ağacı Uygulaması

Aile üyelerinizi ve ilişkilerini yönetebileceğiniz, görselleştirebileceğiniz modern bir platform.

## Özellikler

- Kişi ekleme, düzenleme, silme
- Ebeveyn-çocuk, eş, kardeş gibi ilişkiler tanımlama
- Hiyerarşik aile ağacı görselleştirmesi ([React Flow](https://reactflow.dev/))
- Kişi arama ve filtreleme
- Doğum ve ölüm tarihi desteği
- İlişki tabanlı oyun modülü
- Çoklu dil desteği (Türkçe/İngilizce)
- Koyu/açık tema desteği ([MUI](https://mui.com/))
- Modern, responsive arayüz

## Kullanılan Teknolojiler

### Frontend

- [React](https://react.dev/) (TypeScript)
- [Vite](https://vitejs.dev/)
- [Material UI (MUI)](https://mui.com/)
- [React Flow](https://reactflow.dev/)
- [Axios](https://axios-http.com/)
- Zustand, i18next, d3, styled-components
- Test: [Vitest](https://vitest.dev/), [Testing Library](https://testing-library.com/)

### Backend

- [Spring Boot](https://spring.io/projects/spring-boot)
- Spring Data JPA, Validation, Actuator, Cache
- [PostgreSQL](https://www.postgresql.org/)
- MapStruct, Lombok, Caffeine
- Test: Spring Boot Test

## Kurulum

### Gereksinimler

- Node.js >= 18
- Java >= 21
- PostgreSQL

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Uygulama: [http://localhost:3000](http://localhost:3000)

### Backend

```bash
# PostgreSQL'de bir veritabanı oluşturun (ör: familytree)
# application.properties dosyasını yapılandırın
mvn spring-boot:run
```

API: [http://localhost:8080/api](http://localhost:8080/api)

## Mimarî

- **Monorepo**: frontend (React) ve backend (Spring Boot) ayrı klasörlerde
- **Katmanlı yapı**: Controller, Service, Repository, DTO, Entity
- **Context ve Provider**: React context ile tema, dil, veri yönetimi
- **Veri akışı**: Axios ile REST API üzerinden
- **Aile ağacı**: React Flow ile dinamik node/edge yapısı

## Test

### Frontend

```bash
cd frontend
npm run test
```

### Backend

```bash
mvn test
```

## Katkı

Katkıda bulunmak için fork'layın, yeni bir branch açın ve PR gönderin. Kod kalitesi için ESLint ve Prettier kurallarına uyun.

## Lisans

MIT

## İletişim

Her türlü soru ve öneri için: [github issues](https://github.com/USERNAME/REPO/issues)

---

## Context7 ile Kullanılan Temel Teknolojiler ve API'ler

### React

Bileşen tabanlı, fonksiyonel ve hook odaklı modern arayüz geliştirme kütüphanesi.

**Temel Hook'lar:**
- **useState**: Bileşenlerde durum yönetimi sağlar.
  ```jsx
  const [count, setCount] = useState(0);
  ```
- **useEffect**: Yan etkileri (API çağrıları, DOM manipülasyonu) yönetir.
  ```jsx
  useEffect(() => {
    // Bileşen mount olduğunda çalışır
    return () => {
      // Bileşen unmount olduğunda temizlik işlemleri
    };
  }, [bağımlılıklar]);
  ```
- **useContext**: Context API ile global state erişimi sağlar.
  ```jsx
  const theme = useContext(ThemeContext);
  ```

### Spring Boot

Hızlı, üretim hazır Java backend geliştirme framework'ü.

**Temel Anotasyonlar:**
- **@RestController**: HTTP endpoint'leri tanımlar
- **@Service**: İş mantığı katmanını tanımlar
- **@Repository**: Veritabanı işlemlerini yapan arayüzler
- **@Entity**: Veritabanı tablolarını temsil eden sınıflar

### React Flow

Node-edge tabanlı görselleştirme kütüphanesi, aile ağacı gibi hiyerarşik yapıları görselleştirmek için idealdir.

**Temel Kullanım:**
```jsx
import { ReactFlow, useNodesState, useEdgesState, addEdge } from '@xyflow/react';

function Flow() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback((params) => 
    setEdges((eds) => addEdge(params, eds)), [setEdges]);

  return (
    <ReactFlow
      nodes={nodes}
      edges={edges}
      onNodesChange={onNodesChange}
      onEdgesChange={onEdgesChange}
      onConnect={onConnect}
    />
  );
}
```

### MUI (Material UI)

Material Design temelli, özelleştirilebilir React UI bileşenleri kütüphanesi.

**Tema Oluşturma:**
```jsx
import { createTheme, ThemeProvider } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#556cd6',
    },
    secondary: {
      main: '#19857b',
    },
  },
  colorSchemes: {
    light: true,
    dark: true,
  },
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      {/* Uygulama içeriği */}
    </ThemeProvider>
  );
}
```

### Axios

Promise tabanlı HTTP istemcisi, REST API ile haberleşmeyi kolaylaştırır.

**Temel Kullanım:**
```js
// GET isteği
axios.get('/api/persons')
  .then(response => console.log(response.data))
  .catch(error => console.error(error));

// POST isteği
axios.post('/api/persons', {
  name: 'John Doe',
  birthDate: '1990-01-01'
})
  .then(response => console.log(response.data))
  .catch(error => console.error(error));

// İnterceptor kullanımı
axios.interceptors.request.use(config => {
  // İstek gönderilmeden önce yapılacak işlemler
  return config;
});
```

## Yeni Eklenen Özellik: Ölüm Tarihi

Uygulamada artık kişiler için ölüm tarihi bilgisi ekleyebilirsiniz. Bu özellik, vefat eden aile üyeleri için önemli bir bilgi olarak sisteme eklendi.

### Yapılan Değişiklikler:

1. Veritabanı şemasına `death_date` sütunu eklendi
2. Model sınıflarında ölüm tarihi desteği eklendi
3. Kullanıcı arayüzünde ölüm tarihi giriş alanı eklendi
4. Aile ağacı görselleştirmesinde ölüm tarihleri gösteriliyor

### Nasıl Kullanılır:

1. "Kişi Ekle" veya "Kişi Düzenle" sayfalarında ölüm tarihi alanını doldurabilirsiniz
2. Kişi hayatta ise bu alanı boş bırakın
3. Aile ağacı görünümünde kişilerin doğum ve ölüm tarihleri otomatik olarak gösterilecektir 