package com.githrd.figurium.exception;

import com.githrd.figurium.exception.customException.*;
import com.githrd.figurium.exception.type.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.RedirectException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

@Slf4j
public class MyCustomExceptionResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 예외에 대한 로그 기록
        ErrorType errorType = determineErrorType(ex);
        log.error("오류 발생: {} - 요청 URL: {}", errorType.getMessage(), request.getRequestURI(), ex);

        // 상태 코드 가져오기 (기본값으로 500 설정)
        int statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

        // 예외에 따라 상태 코드 설정
        if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) {
            statusCode = HttpServletResponse.SC_NOT_FOUND; // 404 Not Found
        } else if (ex instanceof HttpRequestMethodNotSupportedException) {
            statusCode = HttpServletResponse.SC_METHOD_NOT_ALLOWED; // 405 Method Not Allowed
        }

        System.out.println("statusCode = " + statusCode);
        // 예외에 따라 적절한 상태 코드와 에러 페이지 설정
        // 비동기 요청인지 동기 요청인지 판별
        boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        // 예외에 따라 적절한 상태 코드와 에러 페이지 설정
        if (isAjax) {
            response.setStatus(statusCode);
            response.setContentType("application/json; charset=UTF-8");

            try {
                System.out.println("@@@@ 에러메세지 = " + errorType.getMessage());
                String jsonResponse = String.format("{\"message\": \"%s\"}", errorType.getMessage());
                response.getWriter().write(jsonResponse);
                response.getWriter().flush();
            } catch (IOException e) {
                log.error("JSON 응답 작성 중 오류 발생", e);
            }
            return new ModelAndView(); // 비동기 요청은 ModelAndView를 사용하지 않음
        } else {
            // 동기 요청인 경우 에러 페이지로 이동
            ModelAndView mv = new ModelAndView();
            mv.addObject("errorMessage", errorType.getMessage());
            mv.addObject("statusCode", statusCode);
            mv.setViewName("errorPage/error");
            return mv;
        }
    }




    // 예외 유형에 따라 ErrorType을 결정하는 메서드
    private ErrorType determineErrorType(Exception ex) {
        if (ex instanceof NullPointerException) {
            return ErrorType.NULL_POINTER_EXCEPTION;
        } else if (ex instanceof IllegalArgumentException) {
            return ErrorType.ILLEGAL_ARGUMENT_EXCEPTION;
        } else if (ex instanceof UserNotFoundException) {
            return ErrorType.USER_NOT_FOUND;
        } else if (ex instanceof SocialAccountNotFoundException) {
            return ErrorType.SOCIAL_ACCOUNT_NOT_FOUND;
        } else if (ex instanceof RedirectException) {
            return ErrorType.REDIRECT_EXCEPTION;
        } else if (ex instanceof AccountLinkException) {
            return ErrorType.ACCOUNT_LINK_ERROR;
        } else if (ex instanceof SocialLoginException) {
            return ErrorType.SOCIAL_LOGIN_ERROR;
        } else if (ex instanceof RedirectErrorException) {
            return ErrorType.REDIRECT_ERROR;
        } else if (ex instanceof FailDeleteUserException) {
            return ErrorType.FAIL_DELETE_USER_EXCEPTION;
        } else if (ex instanceof NoResourceFoundException) {
            return ErrorType.NO_RESOURCE_FOUND_EXCEPTION;
        } else if (ex instanceof OutofStockException) {
            return ErrorType.OUT_OF_STOCK_EXCEPTION;
        }

        // 기본적으로 일반 예외로 처리
        return ErrorType.GENERAL_EXCEPTION;
    }
}

