package com.example.campusactivity.controller;

import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.ActivityService;
import com.example.campusactivity.service.RegistrationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/activities")
public class StudentController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;

    public StudentController(ActivityService activityService, RegistrationService registrationService) {
        this.activityService = activityService;
        this.registrationService = registrationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activities", activityService.listPublishedActivities());
        return "student/activities";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            var activity = activityService.getPublishedActivity(id);
            model.addAttribute("activity", activity);
            model.addAttribute("registrationStatus", registrationService.getStatus(studentId(session), activity));
            return "student/activity-detail";
        } catch (BusinessException ex) {
            Object existingError = model.getAttribute("error");
            redirectAttributes.addFlashAttribute(
                    "error", existingError != null ? existingError : ex.getMessage()
            );
            return "redirect:/student/activities";
        }
    }

    @PostMapping("/{id}/register")
    public String register(@PathVariable Long id, HttpSession session,
                           RedirectAttributes redirectAttributes) {
        try {
            registrationService.register(studentId(session), id);
            redirectAttributes.addFlashAttribute("success", "报名成功");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/activities/" + id;
    }

    private Long studentId(HttpSession session) {
        return (Long) session.getAttribute("LOGIN_USER_ID");
    }
}
