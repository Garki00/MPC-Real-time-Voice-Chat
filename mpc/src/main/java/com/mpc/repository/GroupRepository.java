package com.mpc.repository;

import com.mpc.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByOwnerId(Long ownerId);
}
