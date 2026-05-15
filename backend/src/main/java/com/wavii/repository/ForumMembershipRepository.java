package com.wavii.repository;

import com.wavii.model.Forum;
import com.wavii.model.ForumMembership;
import com.wavii.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForumMembershipRepository extends JpaRepository<ForumMembership, UUID> {

    boolean existsByForumAndUser(Forum forum, User user);

    Optional<ForumMembership> findByForumAndUser(Forum forum, User user);

    @EntityGraph(attributePaths = "user")
    List<ForumMembership> findByForumOrderByJoinedAtAsc(Forum forum);

    long countByForum(Forum forum);

    void deleteByForum(Forum forum);

    @Query("SELECT fm FROM ForumMembership fm JOIN FETCH fm.forum f JOIN FETCH f.creator WHERE fm.user = :user")
    List<ForumMembership> findByUserWithForum(@Param("user") User user);

    @Query("SELECT fm.forum.id FROM ForumMembership fm WHERE fm.user = :user AND fm.forum.id IN :forumIds")
    List<UUID> findJoinedForumIds(@Param("user") User user, @Param("forumIds") List<UUID> forumIds);
}
