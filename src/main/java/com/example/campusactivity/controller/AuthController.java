package com.example.campusactivity.controller;

import com.example.campusactivity.dto.LoginForm;
import com.example.campusactivity.dto.RegisterForm;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginForm", new LoginForm());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm form, BindingResult bindingResult,
                        HttpServletRequest request, Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        try {
            User user = userService.authenticate(form.getUsername(), form.getPassword());
            HttpSession session = request.getSession();
            session.setAttribute("LOGIN_USER_ID", user.getId());
            session.setAttribute("LOGIN_USER_NAME", user.getName());
            session.setAttribute("LOGIN_USER_ROLE", user.getRole());
            if (user.getRole() == UserRole.STUDENT) {
                return "redirect:/student/activities";
            }
            return "redirect:/teacher/activities";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterForm form, BindingResult bindingResult,
                           Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("success", "注册成功，请登录");
            return "redirect:/login";
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}
