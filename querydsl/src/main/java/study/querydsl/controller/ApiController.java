package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberPostDto;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ApiController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/members/postCount")
    public List<MemberPostDto> searchMember1(MemberSearchCondition condition) {
        return memberJpaRepository.searchByWhere(condition);
    }

    @GetMapping("/members/postCount/simple")
    public Page<MemberPostDto> searchMember2(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageSimple(condition, pageable);
    }

    @GetMapping("/members/postCount/complex")
    public Page<MemberPostDto> searchMember3(MemberSearchCondition condition, Pageable pageable) {
        return memberRepository.searchPageComplex(condition, pageable);
    }



}
