package com.example.campusactivity.controller;

import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TeacherController.class, HomeController.class})
class TeacherViewRenderingTest {

    private static final Long TEACHER_ID = 10L;
    private static final Long ACTIVITY_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @Test
    void teacherListRendersRealActivityManagementPage() throws Exception {
        when(activityService.listTeacherActivities(TEACHER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/teacher/activities")
                        .sessionAttr("LOGIN_USER_ID", TEACHER_ID)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.TEACHER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("我的活动")))
                .andExpect(content().string(containsString("新建活动")));
    }

    @Test
    void draftDetailShowsOnlyDraftActions() throws Exception {
        when(activityService.getTeacherActivity(TEACHER_ID, ACTIVITY_ID))
                .thenReturn(activity(ActivityStatus.DRAFT));

        mockMvc.perform(get("/teacher/activities/{id}", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", TEACHER_ID)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.TEACHER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("编辑草稿")))
                .andExpect(content().string(containsString("删除草稿")))
                .andExpect(content().string(containsString("发布活动")))
                .andExpect(content().string(not(containsString("关闭活动"))));
    }

    @Test
    void publishedDetailShowsCloseButNotDraftActions() throws Exception {
        when(activityService.getTeacherActivity(TEACHER_ID, ACTIVITY_ID))
                .thenReturn(activity(ActivityStatus.PUBLISHED));

        mockMvc.perform(get("/teacher/activities/{id}", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", TEACHER_ID)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.TEACHER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("关闭活动")))
                .andExpect(content().string(not(containsString("编辑草稿"))))
                .andExpect(content().string(not(containsString("删除草稿"))));
    }

    @Test
    void closedDetailIsReadOnly() throws Exception {
        when(activityService.getTeacherActivity(TEACHER_ID, ACTIVITY_ID))
                .thenReturn(activity(ActivityStatus.CLOSED));

        mockMvc.perform(get("/teacher/activities/{id}", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", TEACHER_ID)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.TEACHER))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("只读")))
                .andExpect(content().string(not(containsString("编辑草稿"))))
                .andExpect(content().string(not(containsString("删除草稿"))))
                .andExpect(content().string(not(containsString("关闭活动"))));
    }

    private Activity activity(ActivityStatus status) {
        User teacher = new User("teacher", "hash", "测试教师", UserRole.TEACHER);
        ReflectionTestUtils.setField(teacher, "id", TEACHER_ID);
        Activity activity = new Activity(
                "测试活动", "活动说明", "教学楼 A101",
                LocalDateTime.of(2026, 10, 1, 12, 0),
                LocalDateTime.of(2026, 10, 2, 9, 0),
                LocalDateTime.of(2026, 10, 2, 11, 0),
                30, teacher
        );
        activity.setStatus(status);
        ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
        return activity;
    }
}
