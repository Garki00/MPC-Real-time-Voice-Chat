package com.mpc.repository;

import com.mpc.model.GroupJoinRequest;
import com.mpc.model.GroupJoinRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {
    Optional<GroupJoinRequest> findByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupJoinRequest> findByGroupIdAndStatus(Long groupId, RequestStatus status);
    List<GroupJoinRequest> findByUserIdAndStatus(Long userId, RequestStatus status);
}
