package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;
import study.querydsl.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
}
