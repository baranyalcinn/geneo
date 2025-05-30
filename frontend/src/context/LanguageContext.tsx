import React, { createContext, useState, useContext, useEffect, ReactNode } from 'react';
import i18n from 'i18next';
import { initReactI18next, useTranslation } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// Dil seçenekleri
type Language = 'tr' | 'en';

// Basit çeviriler
const resources = {
  tr: {
    translation: {
      // Genel
      appName: 'Aile Ağacı',
      darkTheme: 'Koyu Tema',
      lightTheme: 'Açık Tema',
      langSelection: 'Dil Seçimi',
      basicInformation: 'Temel Bilgiler',
      deathInformation: 'Ölüm Bilgileri',
      familyInformation: 'Aile Bilgileri',
      
      // Navbar
      home: 'Ana Sayfa',
      persons: 'Kişiler',
      familyTree: 'Aile Ağacı',
      relationGame: 'İlişkiler Oyunu',
      
      // Kişi sayfası
      addPerson: 'Kişi Ekle',
      editPerson: 'Kişiyi Düzenle',
      firstName: 'Ad',
      lastName: 'Soyad',
      birthDate: 'Doğum Tarihi',
      deathDate: 'Ölüm Tarihi',
      gender: 'Cinsiyet',
      birthPlace: 'Doğum Yeri',
      male: 'Erkek',
      female: 'Kadın',
      save: 'Kaydet',
      cancel: 'İptal',
      
      // Kişi listesi
      searchPerson: 'Kişi Ara',
      noPersonFound: 'Kişi bulunamadı',
      nameLastname: 'Ad Soyad',
      mother: 'Anne',
      father: 'Baba',
      spouse: 'Eş',
      family: 'Aile',
      refresh: 'Yenile',
      search: 'Ara',
      filter: 'Filtrele',
      
      
      // Aile Ağacı sayfası
      treeView: 'Ağaç Görünümü',
      personList: 'Kişi Listesi',
      selectPerson: 'Kişi Seç',
      searchResults: 'Arama Sonuçları',
      noPersonsFound: 'Kişi bulunamadı',
      loadingPersons: 'Kişiler yükleniyor...',
      errorLoadingPersons: 'Kişiler alınırken bir hata oluştu. Lütfen sayfayı yenileyin.',
      totalPersons: 'Toplam kişi sayısı',
      showingPersons: 'Gösterilen kişi sayısı',
      goToHomePage: 'Ana Sayfaya Dön',
      familyMembers: 'Aile Üyeleri',
      relationships: 'İlişkiler',
      
      // İlişki oyunu
      startGame: 'Oyunu Başlat',
      correctAnswer: 'Doğru Cevap!',
      wrongAnswer: 'Yanlış Cevap!',
      nextQuestion: 'Sonraki Soru',
      
      // Cinsiyet
      genderMale: 'Erkek',
      genderFemale: 'Kadın',
      
      // Hata mesajları
      required: 'Bu alan zorunludur',
      invalidDate: 'Geçersiz tarih',
      loadingError: 'Kişi listesi yüklenirken hata oluştu',
      unknownError: 'Bilinmeyen hata',
      
      // Bileşen mesajları
      loading: 'Yükleniyor...',
      noData: 'Veri bulunamadı',
      addNewPerson: 'Yeni Kişi Ekle',
      'gender.erkek': 'Erkek',
      'gender.kadin': 'Kadın',
    }
  },
  en: {
    translation: {
      // General
      appName: 'Family Tree',
      darkTheme: 'Dark Theme',
      lightTheme: 'Light Theme',
      langSelection: 'Language Selection',
      basicInformation: 'Basic Information',
      deathInformation: 'Death Information',
      familyInformation: 'Family Information',
      
      // Navbar
      home: 'Home',
      persons: 'Persons',
      familyTree: 'Family Tree',
      relationGame: 'Relation Game',
      
      // Person page
      addPerson: 'Add Person',
      editPerson: 'Edit Person',
      firstName: 'First Name',
      lastName: 'Last Name',
      birthDate: 'Birth Date',
      deathDate: 'Death Date',
      gender: 'Gender',
      birthPlace: 'Birth Place',
      male: 'Male',
      female: 'Female',
      save: 'Save',
      cancel: 'Cancel',
      
      // Person list
      searchPerson: 'Search Person',
      noPersonFound: 'No person found',
      nameLastname: 'Name Surname',
      mother: 'Mother',
      father: 'Father',
      spouse: 'Spouse',
      family: 'Family',
      refresh: 'Refresh',
      search: 'Search',
      filter: 'Filter',
      
      // Family Tree page
      treeView: 'Tree View',
      personList: 'Person List',
      selectPerson: 'Select Person',
      searchResults: 'Search Results',
      noPersonsFound: 'No persons found',
      loadingPersons: 'Loading persons...',
      errorLoadingPersons: 'An error occurred while loading persons. Please refresh the page.',
      totalPersons: 'Total number of persons',
      showingPersons: 'Showing number of persons',
      goToHomePage: 'Go to Home Page',
      familyMembers: 'Family Members',
      relationships: 'Relationships',
      
      // Relation game
      startGame: 'Start Game',
      correctAnswer: 'Correct Answer!',
      wrongAnswer: 'Wrong Answer!',
      nextQuestion: 'Next Question',
      
      // Gender
      genderMale: 'Male',
      genderFemale: 'Female',
      
      // Error messages
      required: 'This field is required',
      invalidDate: 'Invalid date',
      loadingError: 'Error loading person list',
      unknownError: 'Unknown error',
      
      // Component messages
      loading: 'Loading...',
      noData: 'No data found',
      addNewPerson: 'Add New Person',
      'gender.erkek': 'Male',
      'gender.kadin': 'Female',
    }
  }
};

// i18next başlatma
i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'tr',
    interpolation: {
      escapeValue: false // React zaten XSS'i önler
    },
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'language',
      caches: ['localStorage']
    }
  });

interface LanguageContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: (key: string, options?: any) => string;
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export const useLanguage = () => {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
};

interface LanguageProviderProps {
  children: ReactNode;
}

export const LanguageProvider: React.FC<LanguageProviderProps> = ({ children }) => {
  const { t: i18nT, i18n } = useTranslation();
  const [language, setLanguage] = useState<Language>(() => {
    return (i18n.language as Language) || 'tr';
  });

  // Dil değiştirme fonksiyonu
  const changeLanguage = (lang: Language) => {
    i18n.changeLanguage(lang);
    setLanguage(lang);
  };

  // String dönen bir t fonksiyonu oluştur
  const t = (key: string, options?: any): string => {
    const translated = i18nT(key, options);
    return typeof translated === 'string' ? translated : String(translated);
  };

  return (
    <LanguageContext.Provider value={{ 
      language, 
      setLanguage: changeLanguage, 
      t
    }}>
      {children}
    </LanguageContext.Provider>
  );
}; 