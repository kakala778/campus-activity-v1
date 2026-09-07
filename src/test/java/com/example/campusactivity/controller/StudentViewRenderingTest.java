package com.example.campusactivity.controller;

import com.example.campusactivity.dto.RegistrationStatusView;
import com.example.campusactivity.entity.Activity;
import com.example.campusactivity.entity.ActivityStatus;
import com.example.campusactivity.entity.User;
import com.example.campusactivity.entity.UserRole;
import com.example.campusactivity.exception.BusinessException;
import com.example.campusactivity.service.ActivityService;
import com.example.campusactivity.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({StudentController.class, HomeController.class})
class StudentViewRenderingTest {

    private static final Long ACTIVITY_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private RegistrationService registrationService;

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
    void availableActivityShowsRegistrationCountAndPostButton() throws Exception {
        Activity published = activity();
        when(activityService.getPublishedActivity(ACTIVITY_ID)).thenReturn(published);
        when(registrationService.getStatus(30L, published))
                .thenReturn(new RegistrationStatusView(2L, false, false, false));

        mockMvc.perform(get("/student/activities/{id}", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", 30L)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.STUDENT))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("测试活动")))
                .andExpect(content().string(containsString("活动说明")))
                .andExpect(content().string(containsString("教学楼 A101")))
                .andExpect(content().string(containsString("2 / 30")))
                .andExpect(content().string(containsString("action=\"/student/activities/100/register\"")))
                .andExpect(content().string(containsString(">报名</button>")));
    }

    @Test
    void alreadyRegisteredActivityShowsReasonWithoutButton() throws Exception {
        assertUnavailableStatus(new RegistrationStatusView(1L, true, false, false), "已报名");
    }

    @Test
    void deadlinePassedActivityShowsReasonWithoutButton() throws Exception {
        assertUnavailableStatus(new RegistrationStatusView(0L, false, true, false), "报名已截止");
    }

    @Test
    void fullActivityShowsReasonWithoutButton() throws Exception {
        assertUnavailableStatus(new RegistrationStatusView(30L, false, false, true), "人数已满");
    }

    @Test
    void postRegistrationRedirectsToDetailWithSuccessFlash() throws Exception {
        mockMvc.perform(post("/student/activities/{id}/register", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", 30L)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.STUDENT))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/activities/" + ACTIVITY_ID))
                .andExpect(flash().attribute("success", "报名成功"));
    }

    @Test
    void closedRegistrationKeepsSpecificReasonWhenDetailRedirectsToList() throws Exception {
        doThrow(new BusinessException("只有已发布活动可以报名"))
                .when(registrationService).register(30L, ACTIVITY_ID);
        when(activityService.getPublishedActivity(ACTIVITY_ID))
                .thenThrow(new BusinessException("活动不存在或未发布"));

        MvcResult postResult = mockMvc.perform(post("/student/activities/{id}/register", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", 30L)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.STUDENT))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/activities/" + ACTIVITY_ID))
                .andReturn();

        mockMvc.perform(get("/student/activities/{id}", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", 30L)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.STUDENT)
                        .flashAttrs(postResult.getFlashMap()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student/activities"))
                .andExpect(flash().attribute("error", "只有已发布活动可以报名"));
    }

    private void assertUnavailableStatus(RegistrationStatusView registrationStatus,
                                         String reason) throws Exception {
        Activity published = activity();
        when(activityService.getPublishedActivity(ACTIVITY_ID)).thenReturn(published);
        when(registrationService.getStatus(30L, published)).thenReturn(registrationStatus);

        mockMvc.perform(get("/student/activities/{id}", ACTIVITY_ID)
                        .sessionAttr("LOGIN_USER_ID", 30L)
                        .sessionAttr("LOGIN_USER_ROLE", UserRole.STUDENT))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(reason)))
                .andExpect(content().string(not(containsString(">报名</button>"))));
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
