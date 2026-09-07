package com.example.campusactivity.config;

import com.example.campusactivity.entity.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("LOGIN_USER_ID") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        Object roleAttribute = session.getAttribute("LOGIN_USER_ROLE");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/student/") && roleAttribute != UserRole.STUDENT) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        if (path.startsWith("/teacher/") && roleAttribute != UserRole.TEACHER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}
