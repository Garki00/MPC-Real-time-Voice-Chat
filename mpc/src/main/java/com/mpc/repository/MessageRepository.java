package com.mpc.repository;

import com.mpc.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE (m.senderId = :uid AND m.receiverId = :fid) OR (m.senderId = :fid AND m.receiverId = :uid) ORDER BY m.createdAt ASC")
    List<Message> findPrivateHistory(@Param("uid") Long userId, @Param("fid") Long friendId);

    List<Message> findByGroupIdOrderByCreatedAtAsc(Long groupId);
}
