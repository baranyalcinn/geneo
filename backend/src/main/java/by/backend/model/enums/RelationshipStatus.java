package by.backend.model.enums;

public enum RelationshipStatus {
    FOUND,                      // İlişki başarıyla bulundu ve tanımlandı
    FOUND_DIRECT,               // Doğrudan ilişki bulundu (örn. ebeveyn-çocuk, eş, kardeş)
    FOUND_INDIRECT,             // Dolaylı ilişki bulundu (örn. kuzen, kayın, büyükanne/büyükbaba)
    FOUND_BLOOD_RELATED,        // Kan bağı ilişkisi bulundu ancak tam tanımlanamadı
    NOT_FOUND,                  // Kişiler arasında belirli bir dolaylı veya doğrudan ilişki bulunamadı
    SELF_REFERENCE,             // Kişi kendisiyle sorgulandı
    INVALID_INPUT,              // Geçersiz giriş parametreleri (örn: null kişiler)
    ERROR_DETERMINING_PATH,     // Dolaylı ilişki yolu bulunurken bir hata oluştu
    ERROR,                      // Genel bir hata oluştu
    AMBIGUOUS                   // Birden fazla olası ilişki bulundu, kesin bir ilişki belirlenemedi
} 