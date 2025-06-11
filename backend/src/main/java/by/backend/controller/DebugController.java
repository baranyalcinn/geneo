package by.backend.controller;

import by.backend.service.debug.DebugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final DebugService debugService;

    @GetMapping("/database-state")
    public ResponseEntity<Map<String, String>> debugDatabaseState() {
        log.info("Debug endpoint çağrıldı");
        
        try {
            debugService.debugDatabaseState();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Debug bilgileri log'larda incelenebilir");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Debug endpoint hatası: {}", e.getMessage(), e);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Debug sırasında hata: " + e.getMessage());
            
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
} 