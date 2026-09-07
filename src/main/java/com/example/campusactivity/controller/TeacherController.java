package com.example.campusactivity.controller;

import com.example.campusactivity.dto.ActivityForm;
import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.ActivityService;
import com.example.campusactivity.service.RegistrationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/teacher/activities")
public class TeacherController {

    private final ActivityService activityService;
    private final RegistrationService registrationService;

    public TeacherController(ActivityService activityService, RegistrationService registrationService) {
        this.activityService = activityService;
        this.registrationService = registrationService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("activities", activityService.listTeacherActivities(teacherId(session)));
        return "teacher/activities";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("activityForm", new ActivityForm());
        prepareFormModel(model, false, null);
        return "teacher/activity-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ActivityForm activityForm, BindingResult bindingResult,
                         HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepareFormModel(model, false, null);
            return "teacher/activity-form";
        }
        try {
            Activity activity = activityService.createDraft(teacherId(session), activityForm);
            redirectAttributes.addFlashAttribute("success", "草稿创建成功");
            return detailRedirect(activity.getId());
        } catch (BusinessException ex) {
            model.addAttribute("error", ex.getMessage());
            prepareFormModel(model, false, null);
            return "teacher/activity-form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("activity", activityService.getTeacherActivity(teacherId(session), id));
            return "teacher/activity-detail";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/activities";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, HttpSession session, Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            Activity activity = activityService.getTeacherActivity(teacherId(session), id);
            if (activity.getStatus() != ActivityStatus.DRAFT) {
                redirectAttributes.addFlashAttribute("error", "只有草稿活动可以编辑");
                return detailRedirect(id);
            }
            model.addAttribute("activityForm", formFrom(activity));
            prepareFormModel(model, true, id);
            return "teacher/activity-form";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/activities";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute ActivityForm activityForm, BindingResult bindingResult,
                         HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            try {
                Activity activity = activityService.getTeacherActivity(teacherId(session), id);
                if (activity.getStatus() != ActivityStatus.DRAFT) {
                    redirectAttributes.addFlashAttribute("error", "只有草稿活动可以编辑");
                    return detailRedirect(id);
                }
                prepareFormModel(model, true, id);
                return "teacher/activity-form";
            } catch (BusinessException ex) {
                redirectAttributes.addFlashAttribute("error", ex.getMessage());
                return "redirect:/teacher/activities";
            }
        }
        try {
            activityService.updateDraft(teacherId(session), id, activityForm);
            redirectAttributes.addFlashAttribute("success", "草稿更新成功");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return detailRedirect(id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            activityService.deleteDraft(teacherId(session), id);
            redirectAttributes.addFlashAttribute("success", "草稿已删除");
            return "redirect:/teacher/activities";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return detailRedirect(id);
        }
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, HttpSession session,
                          RedirectAttributes redirectAttributes) {
        try {
            activityService.publish(teacherId(session), id);
            redirectAttributes.addFlashAttribute("success", "活动发布成功");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return detailRedirect(id);
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, HttpSession session,
                        RedirectAttributes redirectAttributes) {
        try {
            activityService.close(teacherId(session), id);
            redirectAttributes.addFlashAttribute("success", "活动已关闭");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return detailRedirect(id);
    }

    @GetMapping("/{id}/registrations")
    public String registrations(@PathVariable Long id, HttpSession session, Model model,
                                RedirectAttributes redirectAttributes) {
        Long teacherId = teacherId(session);
        try {
            model.addAttribute("activity", activityService.getTeacherActivity(teacherId, id));
            model.addAttribute("registrations",
                    registrationService.listActivityRegistrations(teacherId, id));
            model.addAttribute("registrationCount",
                    registrationService.countActivityRegistrations(teacherId, id));
            return "teacher/registrations";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/teacher/activities";
        }
    }

    private Long teacherId(HttpSession session) {
        return (Long) session.getAttribute("LOGIN_USER_ID");
    }

    private String detailRedirect(Long activityId) {
        return "redirect:/teacher/activities/" + activityId;
    }

    private void prepareFormModel(Model model, boolean editing, Long activityId) {
        model.addAttribute("editing", editing);
        model.addAttribute("activityId", activityId);
    }

    private ActivityForm formFrom(Activity activity) {
        ActivityForm form = new ActivityForm();
        form.setTitle(activity.getTitle());
        form.setDescription(activity.getDescription());
        form.setLocation(activity.getLocation());
        form.setRegistrationDeadline(activity.getRegistrationDeadline());
        form.setStartTime(activity.getStartTime());
        form.setEndTime(activity.getEndTime());
        form.setCapacity(activity.getCapacity());
        return form;
    }
}
