package by.backend.exception;

/**
 * Kaynak bulunamadığında fırlatılan exception sınıfı.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceType, Long id) {
        super(String.format("%s (ID: %d) bulunamadı", resourceType, id));
    }
    
    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s (%s) bulunamadı", resourceType, identifier));
    }
} 