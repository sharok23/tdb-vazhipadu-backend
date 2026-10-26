package com.tdb.vazhipadu.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tdb.vazhipadu.model.Offering;

public interface OfferingRepository extends JpaRepository<Offering, Long> {

    List<Offering> findByTempleIdOrderById(Long templeId);

    Optional<Offering> findByIdAndTempleId(Long id, Long templeId);

    boolean existsByTempleIdAndNameIgnoreCase(Long templeId, String name);

    boolean existsByTempleIdAndNameIgnoreCaseAndIdNot(Long templeId, String name, Long id);
}
