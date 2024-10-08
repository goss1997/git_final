package com.githrd.figurium.auth.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.githrd.figurium.auth.dto.UserProfile;
import com.githrd.figurium.common.session.SessionConstants;
import com.githrd.figurium.exception.customException.AccountLinkException;
import com.githrd.figurium.exception.customException.RedirectErrorException;
import com.githrd.figurium.exception.customException.SocialLoginException;
import com.githrd.figurium.exception.customException.UserNotFoundException;
import com.githrd.figurium.user.dao.SocialAccountMapper;
import com.githrd.figurium.user.entity.User;
import com.githrd.figurium.user.service.SocialAccountService;
import com.githrd.figurium.user.service.UserService;
import com.githrd.figurium.user.vo.SocialAccountVo;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final HttpSession session;
    private final SocialAccountService socialAccountService;

    @PostMapping("/url")
    @ResponseBody
    public ResponseEntity<?> saveUrlToSession(@RequestParam String url) {
        // 세션에 URL 저장
        session.setAttribute("redirectUrl", url);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/link-account")
    public String linkAccount() {
        try {
            UserProfile userProfile = (UserProfile) session.getAttribute(SessionConstants.USER_PROFILE);
            if (userProfile == null) {
                throw new RedirectErrorException("User profile is missing in session.");
            }

            log.info("연동 승인 했으므로 소셜 정보 db에 추가 및 로그인.");
            User user = userService.findByEmail(userProfile.getEmail());
            if (user == null) {
                log.error("User not found in the database with email: {}", userProfile.getEmail());
                throw new UserNotFoundException("User not found with email: " + userProfile.getEmail());
            }

            SocialAccountVo socialAccountVo = SocialAccountVo.getInstance();
            socialAccountVo.setSocialAccountInfo(user.getId(), userProfile.getProvider(), userProfile.getProviderUserId());

            int result = socialAccountService.insertSocialAccount(socialAccountVo);

            if(result > 0){
                session.removeAttribute(SessionConstants.USER_PROFILE);
                session.setAttribute(SessionConstants.LOGIN_USER, user);
                return redirectToPreviousPage();
            }else {
                throw new AccountLinkException("소셜 정보 DB에 insert 실패");
            }


        } catch (Exception e) {
            log.error("Error occurred while linking account: ", e);
            throw new AccountLinkException("Failed to link account: " + e.getMessage());
        }
    }

    @GetMapping("/oauth/loginInfo")
    public String socialLogin(Authentication authentication) {
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // attributes를 UserProfile 객체로 역직렬화하기.
            UserProfile userProfile = objectMapper.convertValue(attributes, UserProfile.class);

            String email = userProfile.getEmail();

            if (userService.existsByEmail(email)) {
                User user = userService.findByEmail(email);
                if (user == null) {
                    throw new UserNotFoundException("User not found with email: " + email);
                }

                // 해당 이메일로 자체 가입한 회원 탈퇴한 이메일과 같을 경우
                if (user.getDeleted()) {
                    session.setAttribute(SessionConstants.ALERT_MSG,"해당 이메일로 탈퇴한 이력이 있습니다. 다른 방법으로 로그인해주세요!");
                    return redirectToPreviousPage();
                }

                SocialAccountVo socialAccount = userService.selectSocialAccountOne(user.getId(), userProfile.getProvider());
                if (socialAccount == null) {
                    session.setAttribute(SessionConstants.USER_PROFILE, userProfile);
                    return "user/link-account";
                } else {
                    log.info("이미 연동한 사용자");
                    session.setAttribute(SessionConstants.LOGIN_USER, user);

                    return redirectToPreviousPage();
                }
            }

            User loginUser = userService.createSocialAccount(userProfile);
            session.setAttribute(SessionConstants.LOGIN_USER, loginUser);

            return redirectToPreviousPage();

        } catch (Exception e) {
            log.error("Error occurred during social login: ", e);
            throw new SocialLoginException("Failed during social login: " + e.getMessage());
        }
    }


    private String redirectToPreviousPage() {
        try {
            String redirectUrl = (String) session.getAttribute("redirectUrl");
            if (redirectUrl == null || redirectUrl.isEmpty()) {
                redirectUrl = "/";
            }
            session.removeAttribute("redirectUrl");
            return "redirect:" + redirectUrl;
        } catch (Exception e) {
            log.error("Error occurred while redirecting to previous page: ", e);
            throw new RedirectErrorException("Failed to redirect to previous page: " + e.getMessage());
        }
    }
}
