package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberPostDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.QMemberPostDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.member;

@Repository
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
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

    /**페이징 구현
     * Count 쿼리를 같이 날린다.
     * */
    @Override
    public Page<MemberPostDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberPostDto> result = queryFactory.select(new QMemberPostDto(member.id.as("memberId"), member.name, member.age,
                        member.posts.size().as("postCount"))).from(member)
                .where(
                        combineMethod(condition.name, condition.count)
                ).offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();
        List<MemberPostDto> content = result.getResults();
        long totalCount = result.getTotal();
        return new PageImpl<>(content, pageable, totalCount);
    }

    /**페이징 구현
     * Count 쿼리를 따로 날린다. 따라서 Count 쿼리를 최적화하려면 이렇게 사용해야 한다.
     * 데이터가 없는 경우 굳이 따로 나눠서 날리지 않아도 된다.
     * */
    @Override
    public Page<MemberPostDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberPostDto> content = queryFactory.select(new QMemberPostDto(member.id.as("memberId"), member.name, member.age,
                        member.posts.size().as("postCount"))).from(member)
                .where(
                        combineMethod(condition.name, condition.count)
                ).offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Member> countQuery = queryFactory.selectFrom(member)
                .where(
                        combineMethod(condition.name, condition.count)
                );

        //조건을 만족하지 않으면 countQuery를 날린다.
        //조건: 첫페이지에 모든 데이터를 조회한경우
        return PageableExecutionUtils.getPage(content, pageable, ()->countQuery.fetchCount());
    }
}
