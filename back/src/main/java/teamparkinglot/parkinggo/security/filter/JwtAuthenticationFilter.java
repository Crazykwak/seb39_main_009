package teamparkinglot.parkinggo.security.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import teamparkinglot.parkinggo.exception.BusinessException;
import teamparkinglot.parkinggo.exception.ExceptionCode;
import teamparkinglot.parkinggo.member.dto.MemberLoginDto;
import teamparkinglot.parkinggo.member.entity.Member;
import teamparkinglot.parkinggo.member.repository.MemberRepository;
import teamparkinglot.parkinggo.member.service.MemberService;
import teamparkinglot.parkinggo.secret.SecretCode;
import teamparkinglot.parkinggo.security.principal.PrincipalDetails;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final SecretCode secretCode;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        log.info("===== 로그인 시도 =====");

        try {
            ObjectMapper om = new ObjectMapper();
            MemberLoginDto memberLoginDto = om.readValue(request.getInputStream(), MemberLoginDto.class);
            Member member = memberRepository.findByEmail(memberLoginDto.getEmail()).orElseThrow(
                    () -> new BusinessException(ExceptionCode.MEMBER_NOT_EXISTS)
            );

            if (!bCryptPasswordEncoder.matches(memberLoginDto.getPassword(), member.getPassword())) {
                log.error("==== 비번이 안맞아요 ====");
                throw new BusinessException(ExceptionCode.PASSWORD_NOT_MATCH);
            }

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(memberLoginDto.getEmail(), memberLoginDto.getPassword());
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            return authentication;
        } catch (IOException e) {
            log.error("에러 발생!! = {}", e);
        }
        return null;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        log.info("===== 인증 성공 =====");

        Date now = new Date();

        PrincipalDetails principalDetails = (PrincipalDetails) authResult.getPrincipal();

        // 액세스 토큰 발급
        String email = principalDetails.getUsername();

        log.info("잘 되나? = {}", secretCode.getAccessTokenExpireTime());
        String accessToken = getToken("AccessToken", secretCode.getAccessTokenExpireTime(), email);

        response.addHeader("Authorization", "Bearer " + accessToken);

        // 리프레시 토큰 발급
        String refreshToken = getToken("RefreshToken", secretCode.getRefreshTokenExpireTime(), email);
        Cookie cookie = new Cookie("Refresh", refreshToken);
        log.info("쿠키값 = {}", refreshToken);

        response.addCookie(cookie);
        memberService.setRefreshToken(refreshToken, email);

    }

    private String getToken(String tokenKind, Long accessTokenExpireTime, String email) {
        String accessToken = JWT.create()
                .withSubject(tokenKind)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpireTime))
                .withClaim("email", email)
                .sign(Algorithm.HMAC512(secretCode.getTokenSecurityKey()));
        return accessToken;
    }
}
