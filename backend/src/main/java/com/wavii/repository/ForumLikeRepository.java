package com.wavii.repository;

import com.wavii.model.Forum;
import com.wavii.model.ForumLike;
import com.wavii.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForumLikeRepository extends JpaRepository<ForumLike, UUID> {

    boolean existsByForumAndUser(Forum forum, User user);

    Optional<ForumLike> findByForumAndUser(Forum forum, User user);

    void deleteByForum(Forum forum);

    @Query("SELECT fl.forum.id FROM ForumLike fl WHERE fl.user = :user AND fl.forum.id IN :forumIds")
    List<UUID> findLikedForumIds(@Param("user") User user, @Param("forumIds") List<UUID> forumIds);
}
