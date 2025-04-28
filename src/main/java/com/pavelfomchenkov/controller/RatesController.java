package com.pavelfomchenkov.controller;

import com.pavelfomchenkov.dto.ValutaMidDTO;
import com.pavelfomchenkov.dto.ValutaRateDTO;
import com.pavelfomchenkov.service.RatesService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collection;

@RestController
@RequiredArgsConstructor
public class RatesController {
    private final RatesService service;

    @GetMapping("period")
    @Operation(summary = "Загрузка данных с сайта cbr.ru для работы")
    public ResponseEntity<String> loadData(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        try {
            service.loadData(start, end);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
        return ResponseEntity.ok("Данные за период доступны для работы");
    }

    @GetMapping("/min")
    @Operation(summary = "Получение минимального курса за период")
    public ResponseEntity<Collection<ValutaRateDTO>> minRate(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        return ResponseEntity.ok(service.getMinRate(start, end));
    }

    @GetMapping("/max")
    @Operation(summary = "Получение максимального курса за период")
    public ResponseEntity<Collection<ValutaRateDTO>> maxRate(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        return ResponseEntity.ok(service.getMaxRate(start, end));
    }

    @GetMapping("/mid")
    @Operation(summary = "Получение среднего курса за период")
    public ResponseEntity<Collection<ValutaMidDTO>> midRate(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        return ResponseEntity.ok(service.getMidRate(start, end));
    }
}
