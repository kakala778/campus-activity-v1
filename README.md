# 校园活动管理系统 V1.0

本项目是《实验一：基于工程意图的软件迭代开发》的 V1.0 最小原型，用于验证“教师发布活动 → 学生浏览活动 → 学生报名 → 教师查看报名结果”的完整业务闭环，并保留需求、设计、实现、测试和 Git 历史之间的追踪关系。

## 已实现范围

- 用户注册、登录、退出以及 `STUDENT` / `TEACHER` 角色访问控制；
- 教师创建、查看、编辑、删除和发布 `DRAFT` 活动；
- 学生浏览 `PUBLISHED` 活动及详情；
- 学生在未截止、未满员且未重复报名时进行报名；
- 学生查看和取消自己的报名；
- 教师查看本人活动的报名人数和学生名单，并关闭活动；
- `DRAFT → PUBLISHED → CLOSED` 活动状态约束及资源所有权检查。

V1.0 不包含管理员、审核、搜索、分类、推荐、评论、通知、签到、导出、报名审批、前后端分离、JWT、Vue/React、Redis、Docker或微服务。

## 技术与环境要求

- JDK 21；
- Maven 3.9 或兼容版本；
- MySQL 8.4；
- Spring Boot 4.1.1、Spring MVC、Thymeleaf、Spring Data JPA；
- `HttpSession`；
- `spring-security-crypto` 提供的 BCrypt。

## 数据库配置

应用默认连接 `localhost:3306/campus_activity`。数据库凭据只通过环境变量提供，仓库不保存真实密码。

PowerShell 当前会话示例：

```powershell
$env:DB_USERNAME="你的 MySQL 用户名"
$env:DB_PASSWORD="你的 MySQL 密码"
```

如需使用其他数据库地址，可设置：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/campus_activity?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
```

## 启动与测试

启动应用：

```powershell
mvn spring-boot:run
```

执行完整自动测试：

```powershell
mvn clean test
```

## 工程文档

- `docs/requirements.md`：用户故事、正式需求和范围；
- `docs/engineering-intent.md`：工程目标、约束和完成依据；
- `docs/design.md`：架构、数据模型、状态模型和核心流程；
- `docs/superpowers/specs/2026-09-06-campus-activity-v1-design.md`：已确认的 V1.0 设计规格；
- `docs/verification.md`：最终测试、需求追踪、BUG 和 Git 证据。

GitHub 仓库：[kakala778/campus-activity-v1](https://github.com/kakala778/campus-activity-v1)

V1.0 已完成 REQ-01～REQ-06，并于 2026-09-07 使用 MySQL 8.4.11、真实 HTTP Session 和完整 Maven 测试通过最终验证。
