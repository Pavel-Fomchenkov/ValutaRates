package com.pavelfomchenkov.mapper;

import com.pavelfomchenkov.dto.ValutaMidDTO;
import com.pavelfomchenkov.dto.ValutaRateDTO;
import com.pavelfomchenkov.model.Valuta;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ValutaMapper {
    public ValutaRateDTO mapToValutaRateDTO(LocalDate date, Valuta valuta) {
        return new ValutaRateDTO(valuta.getVUnitRate(), valuta.getName(), date);
    }

}
