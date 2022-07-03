package study.querydsl.test;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberPostDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.Post;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;
import study.querydsl.repository.PostRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class JpaRepositoryTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    PostRepository postRepository;
    @Autowired
    MemberJpaRepository memberJpaRepository;
    @PersistenceContext
    EntityManager em;

    @BeforeEach
    public void before() {
        Member memberA = memberRepository.save(new Member("memberA", 26));
        Member memberB = memberRepository.save(new Member("memberB", 23));
        Member memberC = memberRepository.save(new Member("memberC", 38));
        Member memberD = memberRepository.save(new Member("memberD", 40));

        Post post1 = postRepository.save(new Post("Post1", memberA));
        Post post2 = postRepository.save(new Post("Post2", memberA));
        Post post3 = postRepository.save(new Post("Post3", memberB));
        Post post4 = postRepository.save(new Post("Post4", memberB));
        Post post5 = postRepository.save(new Post("Post5", memberC));
    }

    /**
     * name을 포함하는 회원과 그 회원중 올린 게시물이 count 이상이 회원 모두 조회
     */
    @Test
    @DisplayName("JPA리포지토리를 직접 사용한 동적 쿼리")
    void test1() {
        MemberSearchCondition cond1 = new MemberSearchCondition("memberA", null);
        List<MemberPostDto> result1 = memberJpaRepository.searchByWhere(cond1);
        for (MemberPostDto memberPostDto : result1) {
            System.out.println("memberPostDto = " + memberPostDto);
        }

        MemberSearchCondition cond2 = new MemberSearchCondition(null, 2);
        List<MemberPostDto> result2 = memberJpaRepository.searchByWhere(cond2);
        for (MemberPostDto memberPostDto : result2) {
            System.out.println("memberPostDto = " + memberPostDto);
        }

        MemberSearchCondition cond3 = new MemberSearchCondition(null, null);
        List<MemberPostDto> result3 = memberJpaRepository.searchByWhere(cond3);
        for (MemberPostDto memberPostDto : result3) {
            System.out.println("memberPostDto = " + memberPostDto);
        }
    }

    @Test
    @DisplayName("스프링 데이터 JPA를 통해 사용자 정의 리포지토리로 사용한 동적 쿼리")
    void test2() {
        MemberSearchCondition cond1 = new MemberSearchCondition("memberA", null);
        List<MemberPostDto> result1 = memberRepository.searchByWhere(cond1);
        for (MemberPostDto memberPostDto : result1) {
            System.out.println("memberPostDto = " + memberPostDto);
        }

        MemberSearchCondition cond2 = new MemberSearchCondition(null, 2);
        List<MemberPostDto> result2 = memberRepository.searchByWhere(cond2);
        for (MemberPostDto memberPostDto : result2) {
            System.out.println("memberPostDto = " + memberPostDto);
        }

        MemberSearchCondition cond3 = new MemberSearchCondition(null, null);
        List<MemberPostDto> result3 = memberRepository.searchByWhere(cond3);
        for (MemberPostDto memberPostDto : result3) {
            System.out.println("memberPostDto = " + memberPostDto);
        }
    }

    @Test
    @DisplayName("페이징 연동 Count쿼리 같이 날리는 경우")
    void test3() {
        MemberSearchCondition cond = new MemberSearchCondition(null, 1);
        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberPostDto> result = memberRepository.searchPageSimple(cond, pageRequest);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("name").containsExactly("memberA","memberB","memberC");
    }

    @Test
    @DisplayName("페이징 연동 Count 쿼리 최적화후 따로 날리는 경우")
    void test4() {
        MemberSearchCondition cond = new MemberSearchCondition(null, 1);
        PageRequest pageRequest = PageRequest.of(0, 3);

        Page<MemberPostDto> result = memberRepository.searchPageComplex(cond, pageRequest);
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result.getContent()).extracting("name").containsExactly("memberA","memberB","memberC");
    }



}
