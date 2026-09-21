package com.krishiai.messaging.repository;

import com.krishiai.messaging.entity.Conversation;
import com.krishiai.messaging.entity.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByConsultationId(Long consultationId);

    boolean existsByConsultationId(Long consultationId);

    Optional<Conversation> findByTypeAndTitle(ConversationType type, String title);

    @Query("SELECT c FROM Conversation c JOIN ConversationMember cm ON cm.conversationId = c.id WHERE cm.user.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsByUserId(@Param("userId") Long userId);
}
