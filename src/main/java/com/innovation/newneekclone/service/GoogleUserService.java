package com.innovation.newneekclone.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.innovation.newneekclone.config.GoogleConfigUtils;
import com.innovation.newneekclone.dto.GoogleLoginDto;
import com.innovation.newneekclone.dto.response.ResponseDto;
import com.innovation.newneekclone.entity.Subscription;
import com.innovation.newneekclone.entity.User;
import com.innovation.newneekclone.repository.SubscriptionRepository;
import com.innovation.newneekclone.repository.UserRepository;
import com.innovation.newneekclone.security.UserDetailsImpl;
import com.innovation.newneekclone.security.jwt.JwtTokenProvider;
import com.innovation.newneekclone.vo.GoogleLoginRequestVo;
import com.innovation.newneekclone.vo.GoogleLoginResponseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final GoogleConfigUtils googleConfigUtils;
    private final JwtTokenProvider jwtTokenProvider;

    public ResponseEntity<?> googleLogin(String authCode, HttpServletResponse response) throws JsonProcessingException {
        GoogleLoginDto userInfo = getGoogleUserInfo(authCode);

        User googleUser = signupGoogleUserIfNeeded(userInfo);

        forceLogin(googleUser);

        String token = jwtTokenProvider.createToken(googleUser);
        response.addHeader("access-token",token);

        return ResponseEntity.ok().body(ResponseDto.success("Google OAuth Success"));
    }


    private GoogleLoginDto getGoogleUserInfo(String authCode) throws JsonProcessingException {
        // HTTP ????????? ?????? RestTemplate ??????
        RestTemplate restTemplate = new RestTemplate();
        GoogleLoginRequestVo requestParams = GoogleLoginRequestVo.builder()
                .clientId(googleConfigUtils.getGoogleClientId())
                .clientSecret(googleConfigUtils.getGoogleSecret())
                .code(authCode)
                .redirectUri(googleConfigUtils.getGoogleRedirectUri())
                .grantType("authorization_code")
                .build();

        // Http Header ??????
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GoogleLoginRequestVo> httpRequestEntity = new HttpEntity<>(requestParams, headers);
        ResponseEntity<String> apiResponseJson = restTemplate.postForEntity(googleConfigUtils.getGoogleAuthUrl() + "/token", httpRequestEntity, String.class);

        // ObjectMapper??? ?????? String to Object??? ??????
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // NULL??? ?????? ?????? ????????????(NULL??? ????????? ??????)
        GoogleLoginResponseVo googleLoginResponse = objectMapper.readValue(apiResponseJson.getBody(), new TypeReference<GoogleLoginResponseVo>() {});

        // ???????????? ????????? JWT Token?????? ???????????? ??????, Id_Token??? ?????? ????????????.
        String jwtToken = googleLoginResponse.getIdToken();

        // JWT Token??? ????????? JWT ????????? ????????? ?????? ??????
        String requestUrl = UriComponentsBuilder.fromHttpUrl(googleConfigUtils.getGoogleAuthUrl() + "/tokeninfo").queryParam("id_token", jwtToken).toUriString();

        String resultJson = restTemplate.getForObject(requestUrl, String.class);
        GoogleLoginDto userInfoDto = objectMapper.readValue(resultJson, new TypeReference<GoogleLoginDto>() {});

        return userInfoDto;
        }

    private User signupGoogleUserIfNeeded(GoogleLoginDto userInfo) {
        String email = userInfo.getEmail();
        User googleUser = userRepository.findByEmail(email).orElse(null);

        if (googleUser == null) { // ????????????
            String nickname = userInfo.getName();

            String password = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

            String role = "ROLE_USER";

            Boolean isSubscribe = true;

            googleUser = new User(email, encodedPassword, nickname, isSubscribe, role);
            userRepository.save(googleUser);

            Subscription subscription = subscriptionRepository.findByEmail(email);
            if (subscription == null) {
                subscription = new Subscription(
                        email,
                        nickname,
                        0L
                );
                subscriptionRepository.save(subscription);
            }
        }
        return googleUser;
    }

    private void forceLogin(User googleUser) {
        UserDetails userDetails = new UserDetailsImpl(googleUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
