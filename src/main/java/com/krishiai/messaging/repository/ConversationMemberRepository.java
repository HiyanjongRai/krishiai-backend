package com.krishiai.messaging.repository;

import com.krishiai.messaging.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    @Query("SELECT cm FROM ConversationMember cm JOIN FETCH cm.user WHERE cm.conversationId = :conversationId")
    List<ConversationMember> findByConversationIdWithUser(@Param("conversationId") Long conversationId);

    List<ConversationMember> findByConversationId(Long conversationId);

    @Query("SELECT cm FROM ConversationMember cm WHERE cm.conversationId = :conversationId AND cm.user.id = :userId")
    Optional<ConversationMember> findByConversationIdAndUserId(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END FROM ConversationMember cm WHERE cm.conversationId = :conversationId AND cm.user.id = :userId")
    boolean existsByConversationIdAndUserId(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId);

    long countByConversationId(Long conversationId);
}
