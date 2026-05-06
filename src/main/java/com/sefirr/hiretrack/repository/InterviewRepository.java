package com.sefirr.hiretrack.repository;

import com.sefirr.hiretrack.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    List<Interview> findByApplicationId(UUID applicationId);

    @Query("""
            SELECT i FROM Interview i
            WHERE i.scheduledAt BETWEEN :from AND :to
            AND i.reminderSent = false
            """)
    List<Interview> findUpcomingWithoutReminder(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
