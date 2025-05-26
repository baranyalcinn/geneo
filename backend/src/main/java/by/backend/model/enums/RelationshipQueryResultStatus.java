package by.backend.model.enums;

public enum RelationshipQueryResultStatus {
    VALID,      // Geçerli bir ilişki bulundu
    SELF,       // Kişi kendisiyle karşılaştırılıyor
    NOT_FOUND,  // İlişki bulunamadı
    ERROR       // Genel bir hata oluştu
} 