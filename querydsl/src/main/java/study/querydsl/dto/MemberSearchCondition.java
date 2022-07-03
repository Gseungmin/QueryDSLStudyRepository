package study.querydsl.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class MemberSearchCondition {

    public String name;
    public Integer count;

}
