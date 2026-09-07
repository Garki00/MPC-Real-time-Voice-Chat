package com.mpc.repository;

import com.mpc.model.GroupMember;
import com.mpc.model.GroupMember.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMember> findByGroupId(Long groupId);
    List<GroupMember> findByUserId(Long userId);
    boolean existsByGroupIdAndUserId(Long groupId, Long userId);
    List<GroupMember> findByGroupIdAndRole(Long groupId, MemberRole role);
    void deleteByGroupIdAndUserId(Long groupId, Long userId);
}
