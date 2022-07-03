package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "name", "age"})
public class Member {

    @Id @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;
    private String name;
    private int age;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
    private List<Post> posts = new ArrayList<>();

    public Member(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
