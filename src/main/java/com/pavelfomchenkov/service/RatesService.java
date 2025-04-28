package com.pavelfomchenkov.service;

import com.pavelfomchenkov.dto.ValutaMidDTO;
import com.pavelfomchenkov.dto.ValutaRateDTO;

import java.time.LocalDate;
import java.util.Collection;

public interface RatesService {
    void loadData(LocalDate start, LocalDate end);

    Collection<ValutaRateDTO> getMinRate(LocalDate start, LocalDate end);

    Collection<ValutaRateDTO> getMaxRate(LocalDate start, LocalDate end);

    Collection<ValutaMidDTO> getMidRate(LocalDate start, LocalDate end);
}
