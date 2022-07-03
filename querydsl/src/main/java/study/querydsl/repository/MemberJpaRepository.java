package study.querydsl.repository;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberPostDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.QMemberPostDto;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.member;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    public List<MemberPostDto> searchByWhere(MemberSearchCondition condition) {
        return queryFactory.select(new QMemberPostDto(member.id.as("memberId"), member.name, member.age,
                member.posts.size().as("postCount"))).from(member)
                .where(
                        combineMethod(condition.name, condition.count)
                ).fetch();
    }

    /** Where 다중 파라미터 사용
     * 조건: 이름이 이름 param을 포함하고 올린 게시물이 param보다 크거나 같은 모든 회원 조회 동적 쿼리
     * 조립이 가능하고 재사용성이 있다는 장점이 있다.
     * */
    private BooleanExpression nameContain(String nameCond) {
        return nameCond != null ? member.name.contains(nameCond) : null;
    }

    private BooleanExpression countGoe(Integer countCond) {
        return countCond != null ? member.posts.size().goe(countCond) : null;
    }

    private Predicate combineMethod(String nameCond, Integer countCond) {
        if (nameCond == null && countCond == null) {
            return null;
        } else if(nameCond == null && countCond != null) {
            return countGoe(countCond);
        } else if(nameCond != null && countCond == null) {
            return nameContain(nameCond);
        } else {
            return nameContain(nameCond).and(countGoe(countCond));
        }
    }
}
