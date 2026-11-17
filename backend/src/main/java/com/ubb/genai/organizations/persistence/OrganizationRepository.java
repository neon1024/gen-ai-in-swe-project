package com.ubb.genai.organizations.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {

    List<OrganizationEntity> findAllByActiveTrueOrderByNameAsc();

    Optional<OrganizationEntity> findByIdAndActiveTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrganizationEntity o where o.id = :id and o.active = true")
    Optional<OrganizationEntity> findActiveForUpdate(@Param("id") UUID id);
}
