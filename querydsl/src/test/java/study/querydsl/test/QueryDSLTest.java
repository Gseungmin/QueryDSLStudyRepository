package study.querydsl.test;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Post;
import study.querydsl.entity.QMember;
import study.querydsl.repository.MemberRepository;
import study.querydsl.repository.PostRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QPost.*;


@SpringBootTest
@Transactional
public class QueryDSLTest {

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    PostRepository postRepository;
    @PersistenceContext
    EntityManager em;

    //em자체가 멀티쓰레드에서 문제가 없도록 설계 따라서 멀티쓰레드 신경쓰지 않아도 된다.
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
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
     * MemberA를 찾을때 Jpql VS QueryDsl 비교
     * Sql 문법을 자바 코드로 작성할 수 있다. 이는 오류를 런타임 시점이 아닌 컴파일 시점에 해결해준다.
     * */
    @Test
    @DisplayName("Jpql VS QueryDsl")
    void test1() {
        //JPQL을 통해 memberA 찾기
        Member memberByJpql = em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", "memberA")
                .getSingleResult();
        assertThat(memberByJpql.getName()).isEqualTo("memberA");

        //QueryDsl을 통해 memberB찾기
        //member = QMember를 staticMethod로 줄여준다.
        Member memberByQueryDsl = queryFactory.selectFrom(member).where(member.name.eq("memberB")).fetchOne();
        assertThat(memberByQueryDsl.getName()).isEqualTo("memberB");
    }

    /**
     * 다양한 검색 조건 TEST + Fetch Test
     * 각 Fetch에 쿼리가 어떻게 나가는지 확인해야한다.
     * */
    @Test
    @DisplayName("다양한 검색 조건 TEST + Fetch Test")
    void test2() {
        //member 라는 이름을 포함하고 그 중 나이가 30이하인 모든 member 조회
        List<Member> members = queryFactory.selectFrom(member)
                .where(
                        member.name.contains("member"),
                        member.age.loe(30)
                )
                .fetch();
        assertThat(members.size()).isEqualTo(2);

        //select 쿼리 및 count 쿼리도 함께 나간다.
        QueryResults<Member> results = queryFactory.selectFrom(member)
                .where(
                        member.name.contains("member"),
                        member.age.loe(30)
                )
                .fetchResults();
        long total = results.getTotal();
        assertThat(total).isEqualTo(2);

        //count 쿼리만 나간다.
        long count = queryFactory.selectFrom(member)
                .where(
                        member.name.contains("member"),
                        member.age.loe(30)
                )
                .fetchCount();
        assertThat(count).isEqualTo(2);
    }


    /**
     * member 정렬
     * 1. 나이 내림차순(DESC) 2. 이름 올림차순(ASC) 3. 단 회원 이름이 없으면 마지막에 출력
     * */
    @Test
    @DisplayName("정렬")
    void test3() {
        /**예제 추가*/
        memberRepository.save(new Member(null, 100));
        memberRepository.save(new Member(null, 26));
        memberRepository.save(new Member("member5", 100));
        memberRepository.save(new Member("member6", 100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100)).orderBy(member.age.desc(), member.name.asc().nullsLast()).fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getName()).isEqualTo("member5");
        assertThat(member6.getName()).isEqualTo("member6");
        assertThat(memberNull.getName()).isNull();
    }

    /**
     * 페이지 + 전체 페이지 조회
     * 전체 조회는 fetchResult로 가지고 오면 된다.
     * */
    @Test
    @DisplayName("페이지 + 전체 페이지 조회")
    void test4() {
        //페이지 조회
        List<Member> page = queryFactory.selectFrom(member).orderBy(member.name.desc()).offset(1).limit(2).fetch();
        assertThat(page.size()).isEqualTo(2);

        //전체 페이지 조회
        QueryResults<Member> totalPage = queryFactory
                .selectFrom(member).orderBy(member.name.desc()).offset(1).limit(2).fetchResults();
        assertThat(totalPage.getTotal()).isEqualTo(4);
        assertThat(totalPage.getLimit()).isEqualTo(2);
        assertThat(totalPage.getOffset()).isEqualTo(1);
        assertThat(totalPage.getResults().size()).isEqualTo(2);
    }

    /**
     * 집합 조회는 QueryDsl 이 제공하는 Tuple 로 조회
     * 실무에서는 tuple 보다 Dto 활용을 더 많이 한다.
     */
    @Test
    @DisplayName("집합 조회")
    void test5() {
        List<Tuple> results = queryFactory.select(
                member.count(), member.age.sum(), member.age.avg(), member.age.min(), member.age.max()
        ).from(member).fetch();
        Tuple tuple = results.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
    }

    /**
     * GroupBy를 사용해 member의 이름과 해당 member가 작성한 post의 개수
     */
    @Test
    @DisplayName("Group By")
    void test6() {
        List<Tuple> result = queryFactory.select(
                member.name, post.count()
        ).from(post).join(post.member, member).groupBy(member.name).fetch();

        assertThat(result.size()).isEqualTo(3);
        result.stream().forEach(tuple -> System.out.println("[member.name, post.count] = " + tuple));
    }

    /**
     * join을 통해 memberA가 쓴 모든 post 조회
     */
    @Test
    @DisplayName("Join")
    void test7() {
        List<Post> postByA = queryFactory.selectFrom(post)
                .join(post.member, member).where(member.name.eq("memberA")).fetch();
        assertThat(postByA).extracting("title").containsExactly("Post1", "Post2");
    }

    /**
     * on 절읗 사용해 join 대상 필터링하여 member를 이름으로 포함하는 회원만 조회하고 해당 회원이 작성한 Post 모두 조회
     * on 절은 inner 조인에서 where와 동일하게 쿼리가 나가므로 외부 조인에서만 on절을 활용하면 된다.
     */
    @Test
    @DisplayName("Join on Filtering")
    void test8() {
        List<Tuple> result = queryFactory.select(member, post).from(post).join(post.member, member)
                .on(member.name.contains("member")).fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        //memberA가 작성한 post의 개수
        List<Tuple> resultByMemberA = queryFactory.select(member.name, post.count()).from(post).join(post.member, member)
                .groupBy(member.name)
                .on(member.name.contains("memberA"))
                .fetch();
        for (Tuple tuple : resultByMemberA) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * fetch join으로 post와 작성 member가지고 오기
     * */
    @Test
    @DisplayName("Fetch join")
    void test9() {
        em.flush();
        em.clear();
        List<Post> fetchJoin = queryFactory.selectFrom(post).join(post.member, member).fetchJoin().fetch();
    }

    /***
     * 나이에 관해 서브쿼리로 조회
     */
    @Test
    @DisplayName("SubQuery 사용")
    void 테스트10() {
        //나이가 가장 많은 회원 조회
        QMember memberSub = new QMember("memberSub");
        List<Member> first = queryFactory.selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max()).from(memberSub)
                )).fetch();
        assertThat(first).extracting("age").containsExactly(40);

        //나이가 평균 이상인 회원 조회
        List<Member> second = queryFactory.selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg()).from(memberSub)
                )).fetch();
        assertThat(second.size()).isEqualTo(2);
    }

    /***
     * DTO조회 JPQL과 QueryDSL 비교
     * Member의 name과 age속성만 가져고 오는 MemberDto 조회
     * 1. property 2. Fileds 3. 생성자 접근 4.QueryProjection 조회
     */
    @Test
    @DisplayName("JPQL과 QueryDsl에서의 DTO조회 비교")
    void 테스트11() {
        //JQQL DTO조회
        List<MemberDto> byJPQL = em.createQuery("select new study.querydsl.dto.MemberDto(m.name, m.age) from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : byJPQL) {
            System.out.println("memberDto = " + memberDto);
        }

        //QueryDsl DTO조회, 1.property 접근방법 단 MemberDto의 기본 생성자 필요
        List<MemberDto> usingProperty = queryFactory.select(Projections.bean(MemberDto.class, member.name, member.age)).from(member).fetch();
        for (MemberDto memberDto : usingProperty) {
            System.out.println("memberDto = " + memberDto);
        }

        //QueryDsl DTO조회, 2.Fields 접근방법 @Getter @Setter 없어도 바로 필드에 값을 넣는다. 단 필드의 명이 정확하게 일치해야 한다.
        //따라서 필드의 명이 정확하지 않다면 as를 사용해준다.
        List<MemberDto> usingFields = queryFactory.select(Projections.fields(MemberDto.class, member.name, member.age)).from(member).fetch();
        for (MemberDto memberDto : usingFields) {
            System.out.println("memberDto = " + memberDto);
        }

        //QueryDsl DTO조회, 3.생성자 접근방법 타입에 맞춰 값이 할당된다.
        List<MemberDto> usingConstructor = queryFactory.select(Projections.constructor(MemberDto.class, member.name, member.age)).from(member).fetch();
        for (MemberDto memberDto : usingConstructor) {
            System.out.println("memberDto = " + memberDto);
        }

        /***
         * QueryProjection DTO조회
         * DTO에 생성자에 @QueryProjection 애노테이션 사용하는데 Dto 파일도 Q파일로 생성이 된다.
         * 컴파일 시점에 타입 검증이 되므로 안전한 방식이지만 Q파일을 생성해야한다는 단점과 Dto가 QueryDsl에 의존성을 가지게 된다.
         * 상황에 따라 유연하게 결정해야한다.
         */
    }

    /**동적쿼리 문제 해결
     * 1. BooleanBuilder
     * 2. Where 다중 파라미터 사용(분해 및 조립에서 장점)
     * */
    @Test
    @DisplayName("Dynamic Query")
    void 테스트12() {
        //이름이 memberA이고 나이가26인 회원 조회
        String nameParam = null;
        Integer ageParam = null;
        List<Member> resultByBooleanBuilder = searchMemberByBB(nameParam, ageParam);
        assertThat(resultByBooleanBuilder.size()).isEqualTo(4);

        List<Member> resultByWhere = searchMemberByW(nameParam, ageParam);
        assertThat(resultByWhere.size()).isEqualTo(4);
    }

    /**BooleanBuilder
     * 조건: 이름이 이름 param을 포함하고 나이가 나이 param보다 작거나 같은 모든 회원 조회 동적 쿼리
     * */
    private List<Member> searchMemberByBB(String nameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (nameCond != null) {
            builder.and(member.name.contains(nameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.loe(ageCond));
        }
        return queryFactory.selectFrom(member).where(builder).fetch();
    }

    /** Where 다중 파라미터 사용
     * 조건: 이름이 이름 param을 포함하고 나이가 나이 param보다 작거나 같은 모든 회원 조회 동적 쿼리
     * 조립이 가능하고 재사용성이 있다는 장점이 있다.
     * */
    private List<Member> searchMemberByW(String nameParam, Integer ageParam) {
        return queryFactory.selectFrom(member)
                .where(
                        combineMethod(nameParam, ageParam)
                ).fetch();
    }

    private BooleanExpression nameContain(String nameCond) {
        return nameCond != null ? member.name.contains(nameCond) : null;
    }

    private BooleanExpression ageLoe(Integer ageCond) {
        return ageCond != null ? member.age.loe(ageCond) : null;
    }

    private Predicate combineMethod(String nameCond, Integer ageCond) {
        if (nameCond == null && ageCond == null) {
            return null;
        }
        return nameContain(nameCond).and(ageLoe(ageCond));
    }

    /**
     * Bulk 연산 단 영속성 컨텍스트에서 발생할 수 있는 문제를 위해 em.flush와 em.clear가 필요
     * */
    @Test
    @DisplayName("Bulk 연산")
    void 테스트13() {
        //memberA의 나이만 26 따라서 memberA의 나이가 27으로 변경되어야 한다.
        long bulkCount = queryFactory.update(member).set(member.age, member.age.add(1))
                .where(member.age.eq(26)).execute();
        assertThat(bulkCount).isEqualTo(1);

        em.flush();
        em.clear();

        List<Member> members = queryFactory.selectFrom(member).where(member.name.eq("memberA")).fetch();
        for (Member member1 : members) {
            System.out.println("member = " + member1);
        }
    }

    /**
     * SQL Function 사용
     * */
    @Test
    @DisplayName("Sql function")
    void 테스트14() {
        //member라는 단어를 M으로 바꿔서 조회
        List<String> results = queryFactory.select(Expressions.
                        stringTemplate(
                                "function('replace',{0},{1},{2})"
                                , member.name, "member", "M"))
                .from(member).fetch();
        for (String result : results) {
            System.out.println("result = " + result);
        }
    }
}
