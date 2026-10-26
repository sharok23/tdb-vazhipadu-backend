package com.tdb.vazhipadu.service;

import org.springframework.stereotype.Component;

import com.tdb.vazhipadu.exception.NotFoundException;
import com.tdb.vazhipadu.repository.TempleRepository;

@Component
public class TempleGuard {

    private final TempleRepository templeRepository;

    public TempleGuard(TempleRepository templeRepository) {
        this.templeRepository = templeRepository;
    }

    public void requireExists(Long templeId) {
        if (!templeRepository.existsById(templeId)) {
            throw NotFoundException.of("Temple", templeId);
        }
    }
}
