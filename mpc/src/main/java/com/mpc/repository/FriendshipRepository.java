package com.mpc.repository;

import com.mpc.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :uid AND f.friendId = :fid) OR (f.userId = :fid AND f.friendId = :uid)")
    Optional<Friendship> findByPair(@Param("uid") Long userId, @Param("fid") Long friendId);

    @Query("SELECT f FROM Friendship f WHERE (f.userId = :uid OR f.friendId = :uid) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("uid") Long userId);

    @Query("SELECT f FROM Friendship f WHERE f.friendId = :uid AND f.status = 'PENDING'")
    List<Friendship> findPendingRequests(@Param("uid") Long userId);
}
