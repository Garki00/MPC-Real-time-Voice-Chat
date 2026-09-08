package com.mpc.repository;

import com.mpc.model.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, Long> {

    Optional<MessageReadStatus> findByUserIdAndConversationTypeAndConversationId(
            Long userId,
            MessageReadStatus.ConversationType conversationType,
            Long conversationId
    );

    List<MessageReadStatus> findByUserId(Long userId);

    @Query("SELECT mrs FROM MessageReadStatus mrs WHERE mrs.userId = :userId " +
           "AND mrs.conversationType = :conversationType")
    List<MessageReadStatus> findByUserIdAndConversationType(
            Long userId,
            MessageReadStatus.ConversationType conversationType
    );
}
