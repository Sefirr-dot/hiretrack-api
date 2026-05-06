package com.sefirr.hiretrack.repository;

import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    @Query("""
            SELECT a FROM Application a WHERE a.user.id = :userId
            AND (:status IS NULL OR a.status = :status)
            AND (cast(:company as string) IS NULL OR LOWER(a.company) LIKE LOWER(CONCAT('%', cast(:company as string), '%')))
            AND (:remote IS NULL OR a.remote = :remote)
            AND (:from IS NULL OR a.appliedAt >= :from)
            AND (:to IS NULL OR a.appliedAt <= :to)
            ORDER BY a.appliedAt DESC
            """)
    Page<Application> findAllByUserIdWithFilters(
            @Param("userId") UUID userId,
            @Param("status") ApplicationStatus status,
            @Param("company") String company,
            @Param("remote") Boolean remote,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    long countByUserIdAndStatus(UUID userId, ApplicationStatus status);

    long countByUserId(UUID userId);

    @Query("""
            SELECT a FROM Application a WHERE a.user.id = :userId
            AND a.appliedAt >= :from
            ORDER BY a.appliedAt ASC
            """)
    java.util.List<Application> findByUserIdAndAppliedAtAfter(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from
    );
}
