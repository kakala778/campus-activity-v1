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

@WebMvcTest({StudentController.class, HomeController.class})
class StudentViewRenderingTest {

    private static final Long ACTIVITY_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @Test
    void studentListRendersPublishedActivityFieldsAndDetailLink() throws Exception {
        Activity published = activity();
        when(activityService.listPublishedActivities()).thenReturn(List.of(published));

        mockMvc.perform(get("/student/activities")
                        .sessionAttr("LOGIN_USER_ID", 30L)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.STUDENT))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("可浏览活动")))
                .andExpect(content().string(containsString("测试活动")))
                .andExpect(content().string(containsString("教学楼 A101")))
                .andExpect(content().string(containsString("查看详情")));
    }

    @Test
    void studentDetailRendersAllCoreFieldsWithoutRegistrationButton() throws Exception {
        Activity published = activity();
        when(activityService.getPublishedActivity(ACTIVITY_ID)).thenReturn(published);

        mockMvc.perform(get("/student/activities/{id}", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", 30L)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.STUDENT))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("测试活动")))
                .andExpect(content().string(containsString("活动说明")))
                .andExpect(content().string(containsString("教学楼 A101")))
                .andExpect(content().string(containsString("30")))
                .andExpect(content().string(containsString("报名功能将在下一阶段提供")))
                .andExpect(content().string(not(containsString("<button type=\"submit\">报名</button>"))));
    }

    private Activity activity() {
        User teacher = new User("teacher", "hash", "测试教师", UserRole.TEACHER);
        ReflectionTestUtils.setField(teacher, "id", 10L);
        Activity activity = new Activity(
                "测试活动", "活动说明", "教学楼 A101",
                LocalDateTime.of(2026, 10, 1, 12, 0),
                LocalDateTime.of(2026, 10, 2, 9, 0),
                LocalDateTime.of(2026, 10, 2, 11, 0),
                30, teacher
        );
        activity.setStatus(ActivityStatus.PUBLISHED);
        ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
        return activity;
    }
}
