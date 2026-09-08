# 校园活动管理系统 V1.0

基于 Spring Boot 的校园活动管理最小原型，围绕“教师发布活动 → 学生浏览并报名 → 教师查看报名结果”这一核心业务闭环展开。

项目重点是完成一次从用户故事、需求分析、工程意图、设计、实现、测试到版本记录的可追踪软件工程迭代，而不是堆叠非核心功能或复杂界面。

## 核心功能

- 用户注册、登录、退出以及 `STUDENT` / `TEACHER` 角色访问控制；
- 教师创建、查看、编辑、删除和发布 `DRAFT` 活动；
- 学生浏览 `PUBLISHED` 活动及详情；
- 学生在未截止、未满员且未重复报名时进行报名；
- 学生查看和取消自己的报名；
- 教师查看本人活动的报名人数和学生名单，并关闭活动；
- `DRAFT → PUBLISHED → CLOSED` 活动状态约束及资源所有权检查。

## 技术栈与环境要求

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

## 运行

启动应用：

```powershell
mvn spring-boot:run
```

## 测试

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

详细的需求追踪、测试结果和最终验证证据见 `docs/verification.md`。

## 最小原型范围

V1.0 只实现验证核心业务目标所必需的功能，不以功能数量为目标。管理员、活动审核、搜索、分类、筛选、推荐、评论、点赞、收藏、通知、邮件、短信、签到、二维码、报名审批、教师代报名或移除报名学生、Excel 导出、复杂 UI、前后端分离、JWT/OAuth、Redis、Docker、消息队列和微服务均不在本版本范围内。
