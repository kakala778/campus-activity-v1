# 校园活动管理系统 V1.0

本项目是《实验一：基于工程意图的软件迭代开发》的第一阶段最小原型。系统围绕“教师发布活动、学生浏览并报名、教师查看报名结果”的核心业务闭环建设，重点体现需求、工程意图、设计、实现、验证和 Git 历史之间的可追踪关系。

## V1.0 范围

- 用户注册、登录和退出，角色为 `STUDENT` 或 `TEACHER`；
- 教师创建、编辑、删除和发布草稿活动；
- 学生浏览已发布活动、报名、查看和取消自己的报名；
- 教师查看本人活动的报名情况并关闭活动。

V1.0 不包含管理员、审核、搜索、分类、推荐、评论、通知、签到、导出、报名审批、前后端分离、JWT、Redis、Docker或微服务。

## 技术栈

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring MVC + Thymeleaf
- Spring Data JPA + MySQL 8.4
- HttpSession
- BCrypt（仅使用 `spring-security-crypto`）
- Jakarta Bean Validation
- JUnit 5 / Spring Boot Test

## 本地环境

需要准备：

- JDK 21；
- Maven 3.9 或兼容版本；
- MySQL 8.4。

数据库配置通过环境变量提供，仓库不保存真实数据库密码：

```powershell
$env:DB_PASSWORD="你的本机 MySQL 密码"
mvn spring-boot:run
```

如需覆盖默认连接信息，可另外设置 `DB_URL` 和 `DB_USERNAME`。

运行测试：

```powershell
mvn test
```

## 工程文档

- `docs/requirements.md`：用户故事、正式需求、验收依据与范围控制；
- `docs/engineering-intent.md`：建设目的、版本范围、约束和完成依据；
- `docs/design.md`：总体架构、数据设计、状态模型、核心流程与设计决策；
- `docs/superpowers/specs/2026-09-06-campus-activity-v1-design.md`：经确认的完整 V1.0 设计规格。

项目将按照 Task 1 至 Task 8 逐步实现和验证；Task 9 基于真实工程证据填写实验报告。
