package com.example.campusactivity.controller;

import com.example.campusactivity.dto.StudentRegistrationView;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.ActivityService;
import com.example.campusactivity.service.RegistrationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;

    public StudentController(ActivityService activityService, RegistrationService registrationService) {
        this.activityService = activityService;
        this.registrationService = registrationService;
    }

    @GetMapping("/student/activities")
    public String list(Model model) {
        model.addAttribute("activities", activityService.listPublishedActivities());
        return "student/activities";
    }

    @GetMapping("/student/activities/{id}")
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

    @PostMapping("/student/activities/{id}/register")
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

    @GetMapping("/student/registrations")
    public String myRegistrations(HttpSession session, Model model) {
        var registrations = registrationService.listStudentRegistrations(studentId(session))
                .stream()
                .map(registration -> new StudentRegistrationView(
                        registration, registrationService.canCancel(registration)
                ))
                .toList();
        model.addAttribute("registrations", registrations);
        return "student/my-registrations";
    }

    @PostMapping("/student/registrations/{activityId}/cancel")
    public String cancel(@PathVariable Long activityId, HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            registrationService.cancel(studentId(session), activityId);
            redirectAttributes.addFlashAttribute("success", "已取消报名");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/student/registrations";
    }

    private Long studentId(HttpSession session) {
        return (Long) session.getAttribute("LOGIN_USER_ID");
    }
}
