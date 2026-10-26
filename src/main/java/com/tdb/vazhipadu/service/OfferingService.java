package com.tdb.vazhipadu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tdb.vazhipadu.contract.request.OfferingRequest;
import com.tdb.vazhipadu.contract.response.OfferingResponse;
import com.tdb.vazhipadu.exception.ConflictException;
import com.tdb.vazhipadu.exception.NotFoundException;
import com.tdb.vazhipadu.model.Offering;
import com.tdb.vazhipadu.repository.OfferingRepository;

@Service
public class OfferingService {

    private static final String DUPLICATE_NAME = "Offering '%s' already exists for this temple";

    private final OfferingRepository offeringRepository;
    private final TempleGuard templeGuard;

    public OfferingService(OfferingRepository offeringRepository, TempleGuard templeGuard) {
        this.offeringRepository = offeringRepository;
        this.templeGuard = templeGuard;
    }

    @Transactional
    public OfferingResponse create(Long templeId, OfferingRequest request) {
        templeGuard.requireExists(templeId);
        String name = request.name().trim();
        if (offeringRepository.existsByTempleIdAndNameIgnoreCase(templeId, name)) {
            throw new ConflictException(DUPLICATE_NAME.formatted(name));
        }
        Offering offering = new Offering(templeId, name, request.price(), request.active());
        return OfferingResponse.from(offeringRepository.save(offering));
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> list(Long templeId) {
        templeGuard.requireExists(templeId);
        return offeringRepository.findByTempleIdOrderById(templeId).stream().map(OfferingResponse::from).toList();
    }

    @Transactional
    public OfferingResponse update(Long templeId, Long offeringId, OfferingRequest request) {
        Offering offering = offeringRepository.findByIdAndTempleId(offeringId, templeId)
                .orElseThrow(() -> NotFoundException.of("Offering", offeringId));
        String name = request.name().trim();
        if (offeringRepository.existsByTempleIdAndNameIgnoreCaseAndIdNot(templeId, name, offeringId)) {
            throw new ConflictException(DUPLICATE_NAME.formatted(name));
        }
        offering.update(name, request.price(), request.active());
        return OfferingResponse.from(offering);
    }
}
