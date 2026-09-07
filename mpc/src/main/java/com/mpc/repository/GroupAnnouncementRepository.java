package com.mpc.repository;

import com.mpc.model.GroupAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupAnnouncementRepository extends JpaRepository<GroupAnnouncement, Long> {
    List<GroupAnnouncement> findByGroupIdOrderByCreatedAtDesc(Long groupId);
}
