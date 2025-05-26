# Aile Ağacı Projesi

![Family Tree](https://img.shields.io/badge/Aile_Ağacı-v0.1.0-blue) ![React](https://img.shields.io/badge/React-v19.1.0-61DAFB) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-v3.5.0-6DB33F)

Aile Ağacı, aile üyelerinizi ve aralarındaki ilişkileri görselleştirip yönetebileceğiniz modern ve kullanıcı dostu bir web uygulamasıdır.

## 📋 İçindekiler

- [Özellikler](#-özellikler)
- [Ekran Görüntüleri](#-ekran-görüntüleri)
- [Teknoloji Yığını](#-teknoloji-yığını)
- [Mimari](#-mimari)
- [Kurulum](#-kurulum)
- [Kullanım](#-kullanım)
- [Geliştirme](#-geliştirme)
- [Test](#-test)
- [Dağıtım](#-dağıtım)
- [Katkıda Bulunma](#-katkıda-bulunma)
- [Lisans](#-lisans)
- [İletişim](#-iletişim)

## ✨ Özellikler

### Temel Özellikler
- ✅ Kişi yönetimi (ekleme, düzenleme, silme)
- ✅ Çoklu ilişki türleri (ebeveyn-çocuk, eş, kardeş)
- ✅ Etkileşimli aile ağacı görselleştirmesi
- ✅ Kişi arama ve filtreleme
- ✅ Doğum ve ölüm tarihi desteği
- ✅ Çoklu dil desteği (Türkçe/İngilizce)
- ✅ Koyu/açık tema desteği

### Gelişmiş Özellikler
- ✅ İlişki tabanlı oyun modülü
- ✅ Soy ağacı analizi
- ✅ Kişilere özel notlar ve anılar
- ✅ Modern, responsive arayüz
- ✅ Veri yedekleme ve içe/dışa aktarma

## 🖼️ Ekran Görüntüleri

*Ekran görüntüleri eklenecek*

## 🔧 Teknoloji Yığını

### Frontend
- **Framework**: [React](https://react.dev/) v19.1.0 (TypeScript)
- **Build Tool**: [Vite](https://vitejs.dev/) v6.3.5
- **UI Kütüphanesi**: [Material UI (MUI)](https://mui.com/) v7.1.0
- **Görselleştirme**: [React Flow](https://reactflow.dev/) v12.6.0
- **Durum Yönetimi**: [Zustand](https://zustand-demo.pmnd.rs/) v5.0.4
- **API İstemcisi**: [Axios](https://axios-http.com/) v1.9.0
- **Uluslararasılaştırma**: [i18next](https://www.i18next.com/) v25.1.2
- **Animasyon**: [Framer Motion](https://www.framer.com/motion/) v12.10.5
- **Stil**: [Styled Components](https://styled-components.com/) v6.1.18
- **Test**: [Vitest](https://vitest.dev/), [Testing Library](https://testing-library.com/)

### Backend
- **Framework**: [Spring Boot](https://spring.io/projects/spring-boot) v3.5.0
- **Java Sürümü**: Java 24
- **Veritabanı**: [PostgreSQL](https://www.postgresql.org/) v42.7.5
- **ORM**: Spring Data JPA
- **Nesne Haritalama**: [MapStruct](https://mapstruct.org/) v1.6.3
- **Kod Azaltma**: [Lombok](https://projectlombok.org/) v1.18.38
- **Önbellek**: [Caffeine](https://github.com/ben-manes/caffeine) v3.1.8
- **Doğrulama**: Spring Validation
- **İzleme**: Spring Actuator

## 🏗️ Mimari

### Genel Yapı
- **Monorepo**: Frontend ve backend kodları aynı repo içinde ayrı klasörlerde
- **REST API**: Backend ve frontend arasında JSON tabanlı iletişim
- **Katmanlı Mimari**: Backend'de Controller-Service-Repository deseni

### Backend
```
backend/
  ├── config/         # Uygulama yapılandırmaları
  ├── controller/     # REST API endpoint'leri
  ├── exception/      # Özel hata işleme
  ├── mapper/         # DTO-Entity dönüşümleri
  ├── model/          # Veri modelleri
  │   ├── dto/        # Veri transfer nesneleri
  │   ├── entity/     # Veritabanı varlıkları
  │   └── enums/      # Sabit değerler
  ├── repository/     # Veritabanı işlemleri
  └── service/        # İş mantığı katmanı
      ├── family/     # Aile servisleri
      ├── familytree/ # Aile ağacı servisleri
      ├── game/       # Oyun modülü servisleri
      ├── person/     # Kişi servisleri
      └── relationship/ # İlişki servisleri
```

### Frontend
```
frontend/
  ├── components/     # Yeniden kullanılabilir UI bileşenleri
  │   ├── FamilyTree/ # Aile ağacı görselleştirme
  │   ├── PersonForm/ # Kişi ekleme/düzenleme formları
  │   ├── ui/         # Temel UI bileşenleri
  │   └── ...
  ├── context/        # React context sağlayıcılar
  ├── hooks/          # Özel React hook'ları
  ├── pages/          # Sayfa bileşenleri
  ├── services/       # API iletişim servisleri
  ├── store/          # Zustand global durum yönetimi
  ├── theme/          # MUI tema yapılandırması
  ├── types/          # TypeScript tür tanımlamaları
  └── utils/          # Yardımcı fonksiyonlar
```

## 📥 Kurulum

### Gereksinimler

- Node.js >= 23.11.0
- Java >= 24
- PostgreSQL >= 15
- Maven veya Gradle

### Frontend Kurulumu

```bash
# Repo'yu klonla
git clone https://github.com/USERNAME/familytree.git
cd familytree/frontend

# Bağımlılıkları yükle
npm install

# Geliştirme sunucusunu başlat
npm run dev
```

Uygulama şu adreste çalışacak: [http://localhost:3000](http://localhost:3000)

### Backend Kurulumu

```bash
# PostgreSQL'de veritabanı oluştur
createdb familytree

# Backend dizinine git
cd familytree/backend

# application.properties dosyasını yapılandır
# src/main/resources/application.properties

# Spring Boot uygulamasını başlat
mvn spring-boot:run
```

API şu adreste çalışacak: [http://localhost:8080/api](http://localhost:8080/api)

### Yapılandırma

`backend/src/main/resources/application.properties` dosyasını düzenleyin:

```properties
# Veritabanı
spring.datasource.url=jdbc:postgresql://localhost:5432/familytree
spring.datasource.username=postgres
spring.datasource.password=your_password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Sunucu
server.port=8080
server.servlet.context-path=/api
```

## 🎮 Kullanım

### Kişi Yönetimi

1. Ana sayfada "Kişi Ekle" butonuna tıklayın
2. Kişi bilgilerini girin (ad, soyadı, doğum tarihi, vb.)
3. Kişiyi kaydedin
4. Kişi detaylarını görüntülemek için listedeki kişiye tıklayın
5. Düzenlemek için "Düzenle" butonunu kullanın

### Aile Ağacı Görselleştirmesi

1. "Aile Ağacı" sekmesine gidin
2. Görüntülemek istediğiniz kişiyi merkez olarak seçin
3. Görünümü yakınlaştırmak/uzaklaştırmak için kaydırma tekerleğini kullanın
4. Düğümleri sürükleyerek düzeni özelleştirin
5. İlişki türlerini görüntülemek için bağlantılara tıklayın

### İlişki Ekleme

1. "İlişkiler" sekmesine gidin
2. "Yeni İlişki" butonuna tıklayın
3. İlişki türünü seçin (ebeveyn, eş, kardeş)
4. İlişkilendirilecek kişileri seçin
5. İlişkiyi kaydedin

## 🧩 Geliştirme

### Yeni Bir Bileşen Ekleme

```jsx
// src/components/MyNewComponent.tsx
import React from 'react';
import { Typography, Box } from '@mui/material';

interface MyNewComponentProps {
  title: string;
  // diğer prop'lar
}

const MyNewComponent: React.FC<MyNewComponentProps> = ({ title }) => {
  return (
    <Box sx={{ padding: 2 }}>
      <Typography variant="h4">{title}</Typography>
      {/* bileşen içeriği */}
    </Box>
  );
};

export default MyNewComponent;
```

### Yeni Bir Service Oluşturma

```java
// src/main/java/by/backend/service/mynewfeature/MyNewService.java
package by.backend.service.mynewfeature;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyNewService {
    private final SomeRepository someRepository;
    
    public SomeDTO doSomething(Long id) {
        // iş mantığı
        return new SomeDTO();
    }
}
```

## 🧪 Test

### Frontend Testleri

```bash
# Birim testleri çalıştır
cd frontend
npm run test

# Kod kapsamı raporu
npm run test:coverage
```

### Backend Testleri

```bash
# Maven ile testleri çalıştır
cd backend
mvn test

# Tek bir test sınıfını çalıştır
mvn test -Dtest=PersonServiceTest
```

## 🚀 Dağıtım

### Frontend Build

```bash
cd frontend
npm run build
```

Build çıktısı `frontend/build` dizininde oluşturulacaktır.

### Backend Build

```bash
cd backend
mvn package
```

Çalıştırılabilir JAR dosyası `backend/target` dizininde oluşturulacaktır.

### Docker ile Dağıtım

```bash
# Docker imajları oluştur
docker-compose build

# Servisleri başlat
docker-compose up -d
```

## 👥 Katkıda Bulunma

1. Bu repo'yu fork'layın
2. Yeni bir branch oluşturun (`git checkout -b feature/amazing-feature`)
3. Değişikliklerinizi commit'leyin (`git commit -m 'Add some amazing feature'`)
4. Branch'inizi push'layın (`git push origin feature/amazing-feature`)
5. Pull Request gönderin

## 📝 Lisans

Bu proje MIT Lisansı altında lisanslanmıştır - ayrıntılar için [LICENSE](LICENSE) dosyasına bakın.

## 📞 İletişim

Proje Yöneticisi - [@twitter_handle](https://twitter.com/twitter_handle)

Proje Linki: [https://github.com/USERNAME/familytree](https://github.com/USERNAME/familytree)

---

## 🔄 Son Güncellemeler

### v0.1.0 (Mayıs 2024)
- Ölüm tarihi desteği eklendi
- Performans iyileştirmeleri yapıldı
- Hata düzeltmeleri

### v0.0.9 (Nisan 2024)
- İlişki tabanlı oyun modülü eklendi
- Kullanıcı arayüzü iyileştirmeleri
- Çoklu dil desteği genişletildi

---

## 💡 Teknoloji Detayları

### React Flow Kullanımı

Aile ağacı görselleştirmesi için React Flow kütüphanesini kullanıyoruz. Temel yapı şu şekildedir:

```jsx
import { ReactFlow, useNodesState, useEdgesState, addEdge } from '@xyflow/react';

function FamilyTreeView() {
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
    >
      <Controls />
      <Background />
    </ReactFlow>
  );
}
```

### Spring Boot REST API

Backend'de Spring Boot REST Controller'lar kullanarak veri sunarız:

```java
@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping
    public List<PersonDTO> getAllPersons() {
        return personService.getAllPersons();
    }

    @GetMapping("/{id}")
    public PersonDTO getPerson(@PathVariable Long id) {
        return personService.getPersonById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonDTO createPerson(@Valid @RequestBody PersonDTO personDTO) {
        return personService.createPerson(personDTO);
    }
}
```

### Zustand Durum Yönetimi

Frontend'de global durum yönetimi için Zustand kullanıyoruz:

```typescript
import { create } from 'zustand';

interface PersonState {
  persons: Person[];
  selectedPerson: Person | null;
  loading: boolean;
  fetchPersons: () => Promise<void>;
  selectPerson: (id: number) => void;
}

const usePersonStore = create<PersonState>((set) => ({
  persons: [],
  selectedPerson: null,
  loading: false,
  fetchPersons: async () => {
    set({ loading: true });
    try {
      const response = await axios.get('/api/persons');
      set({ persons: response.data, loading: false });
    } catch (error) {
      console.error('Kişiler yüklenirken hata oluştu:', error);
      set({ loading: false });
    }
  },
  selectPerson: (id) => {
    set((state) => ({
      selectedPerson: state.persons.find(p => p.id === id) || null
    }));
  }
}));
``` 