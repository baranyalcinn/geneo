package by.backend.model.enums;

public enum RelationshipType {
    // Temel İlişkiler
    SPOUSE,          // Eş
    PARENT_CHILD,    // Ebeveyn-Çocuk (Person1 ebeveyn, Person2 çocuk)
    SIBLING,         // Kardeş

    // Genişletilmiş ve Özel İlişki Tipleri
    GRANDPARENT_GRANDCHILD, // Büyükanne/baba - Torun (Bu genellikle PARENT_CHILD üzerinden hesaplanır, direkt tip gerekmeyebilir)
    UNCLE_AUNT_NEPHEW_NIECE, // Amca/Dayı/Hala/Teyze - Yeğen (Bu da genellikle hesaplanır)
    COUSIN,                 // Kuzen (Hesaplanır)
    SECOND_COUSIN,          // İkinci kuzen
    COUSIN_ONCE_REMOVED,    // Kuzen çocuğu/ebeveyni
    
    // Anne ve baba tarafı ilişkiler (Türkçe'de daha spesifik ilişkiler)
    MATERNAL_UNCLE,         // Dayı (Annenin erkek kardeşi)
    PATERNAL_UNCLE,         // Amca (Babanın erkek kardeşi)
    MATERNAL_AUNT,          // Teyze (Annenin kız kardeşi)
    PATERNAL_AUNT,          // Hala (Babanın kız kardeşi)

    // Evlat Edinme
    ADOPTIVE_PARENT,    // Evlat edinen ebeveyn (Person1 evlat edinen, Person2 çocuk)
    ADOPTED_CHILD,      // Evlat edinilen çocuk (Person1 ebeveyn, Person2 evlat edinilen çocuk)

    // Üvey İlişkiler
    STEP_PARENT,        // Üvey ebeveyn (Person1 üvey ebeveyn, Person2 üvey çocuk)
    STEP_CHILD,         // Üvey çocuk (Person1 ebeveyn, Person2 üvey çocuk)
    STEP_SIBLING,       // Üvey kardeş (Bir ebeveyni ortak olmayan üvey kardeş)
    HALF_SIBLING,       // Yarı kardeş (Tek ebeveyni ortak olan kardeş)

    // Gelin-Damat ilişkileri
    PARENT_IN_LAW,      // Kayınvalide/Kayınpeder (Person1 eşin ebeveyni, Person2 damat/gelin)
    CHILD_IN_LAW,       // Damat/Gelin (Person1 ebeveyn, Person2 çocuğun eşi)
    SIBLING_IN_LAW,     // Kayınbirader/Baldız/Görümce/Elti (Person1 eşin kardeşi, Person2 birey)

    // Diğer Özel Durumlar
    GODPARENT,          // Vaftiz ebeveyni
    GODCHILD,           // Vaftiz çocuğu
    FIANCE,             // Nişanlı
    EX_SPOUSE,          // Eski eş
    GUARDIAN,           // Yasal vasi
    WARD               // Vesayet altındaki
} 