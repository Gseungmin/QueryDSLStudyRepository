package study.querydsl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Post;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class Init {

    private final InitMemberService initMemberService;
    @PostConstruct
    public void init() {
        initMemberService.init();
    }
    @Component
    static class InitMemberService {
        @PersistenceContext
        EntityManager em;
        @Transactional
        public void init() {
            Member memberA = new Member("memberA", 26);
            Member memberB = new Member("memberB", 32);
            Member memberC = new Member("memberC", 24);
            em.persist(memberA);
            em.persist(memberB);
            em.persist(memberC);
            for (int i = 0; i < 100; i++) {
                Member createMember = i % 2 == 0 ? memberA : memberB;
                em.persist(new Post("post" + i, createMember));
            }
        }
    }
}
