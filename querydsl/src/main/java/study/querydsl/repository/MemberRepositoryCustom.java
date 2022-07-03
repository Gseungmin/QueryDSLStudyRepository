package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberPostDto;
import study.querydsl.dto.MemberSearchCondition;

import java.util.List;

public interface MemberRepositoryCustom {

    List<MemberPostDto> searchByWhere(MemberSearchCondition condition);

    /**QueryDsl과 Spring Data Jpa 페이징 연동*/
    Page<MemberPostDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    Page<MemberPostDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
}
