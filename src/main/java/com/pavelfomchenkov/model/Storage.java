package com.pavelfomchenkov.model;

import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Getter
public class Storage {
    private final Map<LocalDate, Collection<Valuta>> storage = new HashMap<>();
}
