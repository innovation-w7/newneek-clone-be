package com.innovation.newneekclone.service;

import com.innovation.newneekclone.dto.response.NewsResponseDto;
import com.innovation.newneekclone.dto.request.ProfileRequestDto;
import com.innovation.newneekclone.dto.response.ProfileResponseDto;
import com.innovation.newneekclone.dto.response.ResponseDto;
import com.innovation.newneekclone.entity.Like;
import com.innovation.newneekclone.entity.News;
import com.innovation.newneekclone.entity.User;
import com.innovation.newneekclone.repository.LikeRepository;
import com.innovation.newneekclone.repository.UserRepository;
import com.innovation.newneekclone.security.UserDetailsImpl;
import com.innovation.newneekclone.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public ResponseEntity<?> getMyLike(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.getAuthentication(jwtTokenProvider.resolveToken(request));
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Like> likeList = likeRepository.findAllByUser(userDetails.getUser());
        List<NewsResponseDto> newsList = new ArrayList<>();
        for (Like like : likeList) {
            News news = like.getNews();
            newsList.add(
                    NewsResponseDto.builder()
                            .id(news.getId())
                            .date(news.getDate())
                            .title(news.getTitle())
                            .category(news.getCategory())
                            .contentSum(news.getContentSum())
                            .build());
        }
        //news list ????????????
        return ResponseEntity.ok().body(ResponseDto.success(newsList));
    }

    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.getAuthentication(jwtTokenProvider.resolveToken(request));
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        //User user = userDetails.getUser(); //?????? ?????? ????????????
        return ResponseEntity.ok().body(
                ResponseDto.success(
                        ProfileResponseDto.builder()
                        .nickname(userDetails.getUser().getNickname()) // ????????? ?????????,
                        .isSubscribe(userDetails.getUser().getIsSubscribe()) // ???????????? ????????????
                        .build()
                )
        );

    }

    @Transactional
    public ResponseEntity<?> changeMyProfile(HttpServletRequest request, ProfileRequestDto requestDto) {
        Authentication authentication = jwtTokenProvider.getAuthentication(jwtTokenProvider.resolveToken(request));
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser(); //?????? ?????? ????????????

        if (requestDto.getNickname() != null) {
            userDetails.getUser().updateNickname(requestDto.getNickname());
            userRepository.save(user);
            return ResponseEntity.ok().body(ResponseDto.success("Nickname Changed"));
        } //????????? ????????? ??????
        if (requestDto.getPassword() != null) {
            userDetails.getUser().updatePassword(requestDto.getPassword());
            userRepository.save(user);
            return ResponseEntity.ok().body(ResponseDto.success("Password Changed"));
        } //???????????? ????????? ?????? -> ???????????? ????????? ????????????
        if (requestDto.getIsSubscribe() != userDetails.getUser().getIsSubscribe()) {
            userDetails.getUser().updateIsSubcribe(requestDto.getIsSubscribe());
            userRepository.save(user);
            return ResponseEntity.ok().body(ResponseDto.success("IsSubscribe Changed"));
        }//?????? ?????? ????????? ??????

        return ResponseEntity.ok().body(ResponseDto.fail("NOT_CHANGED","Nothing Changed"));
    }

    public ResponseEntity<?> deleteMyAccount(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.getAuthentication(jwtTokenProvider.resolveToken(request));
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        userRepository.deleteById(userDetails.getUser().getId()); //????????????????????? ?????? ????????????
        return ResponseEntity.ok().body(ResponseDto.success("Delete Success"));
    }
}