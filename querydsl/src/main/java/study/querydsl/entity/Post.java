package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id", "title"})
public class Post {

    @Id @GeneratedValue
    @Column(name = "POST_ID")
    private Long id;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    public Post(String title, Member member) {
        this.title = title;
        addMember(member);
    }

    public void addMember(Member member) {
        this.member = member;
        member.getPosts().add(this);
    }
}
