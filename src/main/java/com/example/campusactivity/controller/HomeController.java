package com.example.campusactivity.controller;

import com.example.campusactivity.entity.UserRole;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(HttpSession session) {
        if (session.getAttribute("LOGIN_USER_ID") == null) {
            return "redirect:/login";
        }
        Object role = session.getAttribute("LOGIN_USER_ROLE");
        if (role == UserRole.STUDENT) {
            return "redirect:/student/activities";
        }
        if (role == UserRole.TEACHER) {
            return "redirect:/teacher/activities";
        }
        return "redirect:/login";
    }

    @GetMapping("/student/activities")
    public String studentHome() {
        return "student/activities";
    }

}
