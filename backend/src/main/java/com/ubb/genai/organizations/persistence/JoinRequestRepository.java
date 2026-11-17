package com.ubb.genai.organizations.persistence;

import com.ubb.genai.organizations.domain.JoinRequestStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JoinRequestRepository extends JpaRepository<JoinRequestEntity, UUID> {

    Optional<JoinRequestEntity> findFirstByOrganizationIdAndApplicantIdOrderByCreatedAtDescIdDesc(
            UUID organizationId, UUID applicantId);

    Optional<JoinRequestEntity> findByOrganizationIdAndApplicantIdAndStatus(UUID organizationId, UUID applicantId,
            JoinRequestStatus status);

    List<JoinRequestEntity> findAllByOrganizationIdAndStatusOrderByCreatedAtAscIdAsc(UUID organizationId,
            JoinRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from JoinRequestEntity r where r.id = :requestId and r.organizationId = :organizationId")
    Optional<JoinRequestEntity> findForUpdate(@Param("organizationId") UUID organizationId,
            @Param("requestId") UUID requestId);
}
