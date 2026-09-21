package com.krishiai.messaging.repository;

import com.krishiai.messaging.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.conversationId = :conversationId AND m.deletedAt IS NULL ORDER BY m.id DESC")
    List<Message> findLatestByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.conversationId = :conversationId AND m.id < :beforeId AND m.deletedAt IS NULL ORDER BY m.id DESC")
    List<Message> findByConversationIdAndBeforeId(@Param("conversationId") Long conversationId, @Param("beforeId") Long beforeId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND m.deletedAt IS NULL AND (:lastReadId IS NULL OR m.id > :lastReadId) AND m.sender.id != :userId")
    long countUnreadMessages(@Param("conversationId") Long conversationId, @Param("lastReadId") Long lastReadId, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender WHERE m.conversationId = :conversationId AND m.deletedAt IS NULL ORDER BY m.id DESC LIMIT 1")
    Optional<Message> findLatestMessageByConversationId(@Param("conversationId") Long conversationId);
}
