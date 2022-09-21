package teamparkinglot.parkinggo.member.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id @GeneratedValue
    private Long id;
    private String email;
    private String password;
    private String nickname;

    @Enumerated(value = EnumType.STRING)
    private MemberRole role;
    private String carNumber;
    private String phone;
    private Long point;
    private Boolean psInfoAgmt;
    private Boolean svcUseAgmt;
    private Boolean eventAgmt;

    private String provider;
    private String providerId;
    private String refreshToken;

    @Builder
    public Member(String email, String password, String name, MemberRole role, String carNumber, String phone, Long point, Boolean psInfoAgmt, Boolean svcUseAgmt, Boolean eventAgmt, String provider, String providerId, String refreshToken) {
        this.email = email;
        this.password = password;
        this.nickname = name;
        this.role = role;
        this.carNumber = carNumber;
        this.phone = phone;
        this.point = point;
        this.psInfoAgmt = psInfoAgmt;
        this.svcUseAgmt = svcUseAgmt;
        this.eventAgmt = eventAgmt;
        this.provider = provider;
        this.providerId = providerId;
        this.refreshToken = refreshToken;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNickname(String name) {
        this.nickname = name;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}