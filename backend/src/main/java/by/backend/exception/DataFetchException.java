package by.backend.exception;

/**
 * Veri getirme işlemleri sırasında oluşan hataları temsil eden exception sınıfı.
 */
public class DataFetchException extends RuntimeException {
    
    public DataFetchException(String message) {
        super(message);
    }
    
    public DataFetchException(String message, Throwable cause) {
        super(message, cause);
    }
} 