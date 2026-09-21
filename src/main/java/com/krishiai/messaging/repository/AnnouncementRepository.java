package com.krishiai.messaging.repository;

import com.krishiai.messaging.entity.Announcement;
import com.krishiai.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a JOIN FETCH a.createdBy WHERE (a.targetRole IS NULL OR a.targetRole = :role) AND (a.expiresAt IS NULL OR a.expiresAt > :now) ORDER BY a.createdAt DESC")
    List<Announcement> findActiveAnnouncementsForRole(@Param("role") UserRole role, @Param("now") LocalDateTime now);

    @Query(value = "SELECT a FROM Announcement a JOIN FETCH a.createdBy ORDER BY a.createdAt DESC",
           countQuery = "SELECT count(a) FROM Announcement a")
    Page<Announcement> findAllWithCreator(Pageable pageable);
}
