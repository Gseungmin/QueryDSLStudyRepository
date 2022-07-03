package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString(of = {"memberId", "name", "age", "postCount"})
public class MemberPostDto {

    private Long memberId;
    private String name;
    private int age;
    private int postCount;

    @QueryProjection
    public MemberPostDto(Long memberId, String name, int age, int postCount) {
        this.memberId = memberId;
        this.name = name;
        this.age = age;
        this.postCount = postCount;
    }
}
