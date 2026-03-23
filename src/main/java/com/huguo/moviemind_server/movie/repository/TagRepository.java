package com.huguo.moviemind_server.movie.repository;

import com.huguo.moviemind_server.movie.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    List<Tag> findByType(Tag.TagType type);
    boolean existsByName(String name);
}