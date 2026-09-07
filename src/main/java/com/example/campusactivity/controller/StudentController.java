package com.example.campusactivity.controller;

import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.ActivityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/activities")
public class StudentController {

    private final ActivityService activityService;

    public StudentController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("activities", activityService.listPublishedActivities());
        return "student/activities";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activity", activityService.getPublishedActivity(id));
            return "student/activity-detail";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/student/activities";
        }
    }
}
