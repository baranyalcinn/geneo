package by.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Collections;
import java.util.List;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> handleDataAccessException(DataAccessException e) {
        log.error("Veritabanı erişim hatası: {}", e.getMessage(), e);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("Kaynak bulunamadı: {}", e.getMessage());
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unchecked")
    public <T> ResponseEntity<T> handleGenericException(Exception e) {
        log.error("Beklenmeyen hata: {}", e.getMessage(), e);
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Boş liste döndürmesi gereken hata durumları için özel metot
    @ExceptionHandler(DataFetchException.class)
    public <T> ResponseEntity<List<T>> handleDataFetchException(DataFetchException e) {
        log.error("Veri getirme hatası: {}", e.getMessage(), e);
        List<T> emptyList = Collections.emptyList();
        return new ResponseEntity<>(emptyList, HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 