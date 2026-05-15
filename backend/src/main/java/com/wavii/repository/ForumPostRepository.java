package com.wavii.repository;

import com.wavii.model.Forum;
import com.wavii.model.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {

    @EntityGraph(attributePaths = "author")
    Page<ForumPost> findByForumOrderByCreatedAtDesc(Forum forum, Pageable pageable);

    void deleteByForum(Forum forum);
}
