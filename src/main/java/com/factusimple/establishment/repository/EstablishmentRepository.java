package com.factusimple.establishment.repository;

import com.factusimple.establishment.entity.Establishment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
}
