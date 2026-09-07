# 校园活动管理系统 V1.0 设计规格

## 0. 文档定位

本文件是《实验一：基于工程意图的软件迭代开发》的 V1.0 设计规格，也是后续实现、Git 提交和软件验证的唯一主要工程依据。

本项目是课程第一阶段最小原型，目标不是做功能丰富或界面精美的产品，而是完成一条可运行、可验证、可追踪的核心业务闭环：

> 教师发布活动 → 学生浏览活动 → 学生报名 → 教师查看报名情况

开发中若发现需要改变需求、工程意图或设计，先修改并说明工程文档，再修改实现；禁止为了“更完整”擅自扩展功能。

---

## 1. 项目目标与工程意图

### 1.1 建设目的

校园活动的信息发布和学生报名如果依赖线下通知或分散渠道，组织教师难以统一发布和掌握报名情况，学生也缺乏统一了解和参与活动的入口。

V1.0 不追求建设完整校园活动平台，而是验证以下核心业务过程能够通过一个结构合理、基本可运行的软件实现：

1. 用户能够注册、登录并按角色使用系统；
2. 教师能够创建并发布活动；
3. 学生能够浏览已发布活动并报名；
4. 学生能够查看和取消自己的报名；
5. 教师能够查看报名结果并关闭活动。

### 1.2 V1.0 范围

本轮实现：

- 用户注册、登录、退出；
- STUDENT / TEACHER 两类角色；
- 教师创建、编辑、删除 DRAFT 活动；
- 教师发布活动；
- 学生浏览 PUBLISHED 活动及详情；
- 学生报名活动；
- 学生查看自己的报名；
- 学生在允许条件下取消报名；
- 教师查看自己的活动及报名名单；
- 教师关闭 PUBLISHED 活动。

明确不实现：

- 管理员角色；
- 活动审核；
- 真实校园统一身份认证；
- 搜索、分类、筛选、推荐；
- 评论、点赞、收藏；
- 通知、邮件、短信；
- 签到、二维码；
- 报名审批；
- 教师代报名或移除报名学生；
- Excel 导出；
- 前后端分离；
- Vue / React；
- JWT / OAuth；
- Redis、Docker、消息队列、微服务；
- 复杂 UI 和动画。

### 1.3 完成依据

当且仅当以下条件成立，V1.0 认为达到预期目标：

- REQ-01 ～ REQ-06 均已实现；
- 学生和教师核心业务流程均可从页面实际完成；
- 关键业务规则能够正确拒绝非法操作；
- TEST-01 ～ TEST-06 验证完成；
- 实现与本设计规格基本一致；
- Git 历史能够反映项目从文档、基础工程到功能和验证的逐步形成过程；
- 项目可以在本地 MySQL 8.4 环境中启动并演示。

---

## 2. 用户与用户故事

### US-01 注册与登录

作为校园活动系统用户，我希望注册账号、选择学生或教师身份并登录系统，以便使用与自己角色对应的功能。

### US-02 教师创建并发布活动

作为活动组织教师，我希望创建活动、完善草稿并发布，以便学生能够了解和参与活动。

### US-03 学生浏览活动

作为学生，我希望查看当前已发布的校园活动及其详情，以便判断是否参加。

### US-04 学生报名活动

作为学生，我希望报名满足条件的校园活动，以便获得参与活动的资格。

### US-05 学生管理自己的报名

作为学生，我希望查看自己已报名的活动，并在允许条件下取消报名，以便管理自己的活动参与计划。

### US-06 教师查看和管理已发布活动

作为活动组织教师，我希望查看自己活动的报名情况并在需要时关闭活动，以便掌握活动参与情况并结束报名业务。

---

## 3. 正式需求

| 编号 | 角色 | 需求描述 | 优先级 | 验收依据 |
|---|---|---|---|---|
| REQ-01 | 用户 | 用户可以注册并选择 STUDENT/TEACHER 角色，之后登录和退出系统 | Must | 注册成功；用户名唯一；密码哈希保存；登录后进入对应角色页面；未登录不能进入业务页面 |
| REQ-02 | 教师 | 教师可以创建、编辑、删除 DRAFT 活动，并将合法草稿发布 | Must | 草稿可编辑/删除；合法活动可发布；教师不能管理他人活动；发布后不可编辑/删除 |
| REQ-03 | 学生 | 学生可以浏览 PUBLISHED 活动并查看详情 | Must | PUBLISHED 可见；DRAFT/CLOSED 不出现在普通活动列表；满员或截止活动仍可查看但不可报名 |
| REQ-04 | 学生 | 学生可以报名满足条件的活动 | Must | 仅 STUDENT 可报名；活动须 PUBLISHED、未超过报名截止时间、未满员、未重复报名 |
| REQ-05 | 学生 | 学生可以查看自己的报名，并在允许条件下取消 | Should | 只能查看/取消自己的报名；PUBLISHED 且未超过截止时间时可取消；取消后释放名额并允许重新报名 |
| REQ-06 | 教师 | 教师可以查看自己活动的报名人数、学生名单，并关闭 PUBLISHED 活动 | Should | 只能查看自己的活动报名；关闭后活动变为 CLOSED 且不能再报名；教师不能代替学生增删报名 |

---

## 4. 需求取舍与范围控制

### 4.1 不设置管理员

当前业务背景已明确学生和活动组织教师两类主要用户。管理员审核并非“发布—浏览—报名”核心闭环成立的必要条件，引入管理员会额外增加权限、审核状态和管理页面，因此 V1.0 不实现管理员。

### 4.2 不实现搜索、分类和推荐

搜索、分类、筛选和推荐可以提升活动发现效率，但不影响核心业务闭环。V1.0 只提供活动列表和详情。

### 4.3 不实现签到、通知、导出与报名审批

这些能力属于活动开展或运营阶段的增强功能。V1.0 只解决活动发布、学生报名和教师查看报名结果的问题。

---

## 5. 业务规则

### 5.1 身份与访问

1. 系统只有 `STUDENT` 和 `TEACHER` 两类角色。
2. 注册时由用户自行选择角色；V1.0 不验证真实校园身份。
3. 未登录用户只能访问注册、登录和静态资源。
4. `STUDENT` 只能访问学生业务页面。
5. `TEACHER` 只能访问教师业务页面。
6. 教师只能管理自己创建的活动。

### 5.2 活动生命周期

活动状态：

```text
DRAFT --publish--> PUBLISHED --close--> CLOSED
```

规则：

- 新建活动默认为 `DRAFT`；
- `DRAFT` 可查看、编辑、删除、发布；
- `PUBLISHED` 不可编辑、不可删除，可查看报名并关闭；
- `CLOSED` 只读；
- 学生普通活动列表只展示 `PUBLISHED`；
- `CLOSED` 活动如果学生曾经报名，仍保留在“我的报名”中。

### 5.3 活动数据校验

活动必须满足：

- `title` 非空；
- `description` 非空；
- `location` 非空；
- `capacity > 0`；
- `registrationDeadline < startTime`；
- `startTime < endTime`。

### 5.4 报名规则

报名成功必须同时满足：

1. 当前用户角色为 `STUDENT`；
2. 活动状态为 `PUBLISHED`；
3. 当前时间没有超过 `registrationDeadline`；
4. 当前报名人数 `< capacity`；
5. 当前学生尚未报名该活动。

重复报名采用两层保护：

- Service 层主动判断并给出明确业务提示；
- 数据库对 `(student_id, activity_id)` 建联合唯一约束。

### 5.5 取消报名规则

学生可以取消自己的报名，当且仅当：

- 报名记录属于当前登录学生；
- 活动状态仍为 `PUBLISHED`；
- 当前时间没有超过报名截止时间。

取消报名直接删除 `Registration` 记录，不保留取消历史；取消后名额立即释放，学生之后可重新报名。

### 5.6 教师报名管理边界

教师只可以：

- 查看自己活动的当前报名人数；
- 查看报名学生姓名、用户名、报名时间；
- 关闭自己处于 `PUBLISHED` 状态的活动。

教师不能：

- 替学生报名；
- 移除学生；
- 审批报名；
- 导出名单。

---

## 6. 技术方案

### 6.1 技术栈

- Java 21
- Spring Boot 4.1.1
- Maven
- Spring MVC
- Thymeleaf
- Spring Data JPA
- MySQL 8.4
- `spring-security-crypto` 中的 BCrypt（不启用完整 Spring Security）
- `HttpSession`
- Jakarta Bean Validation
- HTML / CSS / 极少量 JavaScript
- JUnit 5 + Mockito / Spring Boot Test

### 6.2 架构原则

采用简单单体 MVC：

```text
Browser
   |
   v
Controller
   |
   v
Service
   |
   v
Repository
   |
   v
MySQL
```

职责：

- Controller：接收请求、绑定表单、组织页面跳转和提示；
- Service：业务规则和权限检查；
- Repository：数据库访问；
- Entity：核心领域数据；
- DTO/Form：页面输入对象；
- Interceptor：会话和角色访问控制；
- Thymeleaf：服务端页面渲染。

禁止把报名、活动状态、活动归属等核心规则只放在 Controller 或页面中。

### 6.3 UI 原则

UI 只要求：

- 页面可阅读；
- 表单可填写；
- 按钮可操作；
- 成功/错误提示可见；
- 页面基本不乱版。

不追求：

- 动画；
- 复杂视觉；
- 响应式精修；
- 前端组件库；
- 大量 JavaScript。

优先确保业务和工程过程正确。

---

## 7. 数据设计

### 7.1 User

表名建议：`users`

字段：

- `id: Long`，主键，自增；
- `username: String`，非空，唯一；
- `passwordHash: String`，非空；
- `name: String`，非空；
- `role: UserRole`，`STUDENT` / `TEACHER`；
- `createdAt: LocalDateTime`。

### 7.2 Activity

表名建议：`activities`

字段：

- `id: Long`；
- `title: String`；
- `description: String`；
- `location: String`；
- `registrationDeadline: LocalDateTime`；
- `startTime: LocalDateTime`；
- `endTime: LocalDateTime`；
- `capacity: Integer`；
- `status: ActivityStatus`；
- `creator: User`；
- `createdAt: LocalDateTime`。

### 7.3 Registration

表名建议：`registrations`

字段：

- `id: Long`；
- `student: User`；
- `activity: Activity`；
- `registeredAt: LocalDateTime`。

数据库唯一约束：

```text
UNIQUE(student_id, activity_id)
```

### 7.4 关系

```text
User(TEACHER) 1 ---- N Activity
User(STUDENT) 1 ---- N Registration N ---- 1 Activity
```

---

## 8. 关键设计决策

### DEC-01 统一用户模型

学生和教师共用 `User` 表，通过 `role` 区分。

理由：V1.0 两类用户账户字段基本相同，拆表会造成重复结构。

### DEC-02 活动采用显式状态模型

采用 `DRAFT -> PUBLISHED -> CLOSED`。

理由：把编辑阶段、参与阶段和结束阶段明确区分，便于定义不同状态下允许的操作。

### DEC-03 发布后不可修改

`PUBLISHED` 活动不能编辑或删除，只能查看报名和关闭。

理由：已发布活动可能已有报名，继续修改时间、容量等关键数据会增加一致性和异常处理复杂度。第一阶段选择更容易验证的规则。

### DEC-04 应用层 + 数据库双重防重复报名

Service 在创建报名之前查询，数据库同时使用联合唯一约束。

理由：应用层提供友好业务错误，数据库作为最终一致性保护。

---

## 9. Java 包与文件结构

基础包：

```text
com.example.campusactivity
```

建议结构：

```text
src/main/java/com/example/campusactivity/
├── CampusActivityApplication.java
├── config/
│   ├── AuthInterceptor.java
│   └── WebConfig.java
├── controller/
│   ├── HomeController.java
│   ├── AuthController.java
│   ├── StudentController.java
│   └── TeacherController.java
├── dto/
│   ├── LoginForm.java
│   ├── RegisterForm.java
│   └── ActivityForm.java
├── entity/
│   ├── User.java
│   ├── UserRole.java
│   ├── Activity.java
│   ├── ActivityStatus.java
│   └── Registration.java
├── exception/
│   └── BusinessException.java
├── repository/
│   ├── UserRepository.java
│   ├── ActivityRepository.java
│   └── RegistrationRepository.java
└── service/
    ├── UserService.java
    ├── ActivityService.java
    └── RegistrationService.java
```

资源：

```text
src/main/resources/
├── application.properties
├── static/css/style.css
└── templates/
    ├── auth/login.html
    ├── auth/register.html
    ├── student/activities.html
    ├── student/activity-detail.html
    ├── student/my-registrations.html
    ├── teacher/activities.html
    ├── teacher/activity-form.html
    ├── teacher/activity-detail.html
    └── teacher/registrations.html
```

测试：

```text
src/test/java/com/example/campusactivity/
├── service/ActivityServiceTest.java
└── service/RegistrationServiceTest.java
```

---

## 10. 核心接口约定

### 10.1 Repository

`UserRepository`

```java
Optional<User> findByUsername(String username);
boolean existsByUsername(String username);
```

`ActivityRepository`

```java
List<Activity> findByStatusOrderByStartTimeAsc(ActivityStatus status);
List<Activity> findByCreatorIdOrderByCreatedAtDesc(Long creatorId);
Optional<Activity> findByIdAndCreatorId(Long id, Long creatorId);
```

`RegistrationRepository`

```java
boolean existsByStudentIdAndActivityId(Long studentId, Long activityId);
long countByActivityId(Long activityId);
List<Registration> findByStudentIdOrderByRegisteredAtDesc(Long studentId);
List<Registration> findByActivityIdOrderByRegisteredAtAsc(Long activityId);
Optional<Registration> findByStudentIdAndActivityId(Long studentId, Long activityId);
```

### 10.2 Service

`UserService`

```java
User register(RegisterForm form);
User authenticate(String username, String rawPassword);
User getRequiredUser(Long userId);
```

`ActivityService`

```java
Activity createDraft(Long teacherId, ActivityForm form);
Activity updateDraft(Long teacherId, Long activityId, ActivityForm form);
void deleteDraft(Long teacherId, Long activityId);
Activity publish(Long teacherId, Long activityId);
Activity close(Long teacherId, Long activityId);

List<Activity> listPublishedActivities();
Activity getPublishedActivity(Long activityId);

List<Activity> listTeacherActivities(Long teacherId);
Activity getTeacherActivity(Long teacherId, Long activityId);
```

`RegistrationService`

```java
Registration register(Long studentId, Long activityId);
void cancel(Long studentId, Long activityId);

List<Registration> listStudentRegistrations(Long studentId);
List<Registration> listActivityRegistrations(Long teacherId, Long activityId);
long countActivityRegistrations(Long teacherId, Long activityId);
```

Service 方法发生业务违规时抛出 `BusinessException`，Controller 捕获后通过页面错误或 flash message 告知用户。

---

## 11. Web 路由

### 公共

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/` | 根据登录状态和角色跳转 |
| GET | `/login` | 登录页 |
| POST | `/login` | 登录 |
| GET | `/register` | 注册页 |
| POST | `/register` | 注册 |
| POST | `/logout` | 退出 |

### 学生

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/student/activities` | 浏览 PUBLISHED 活动 |
| GET | `/student/activities/{id}` | 查看 PUBLISHED 活动详情 |
| POST | `/student/activities/{id}/register` | 报名 |
| GET | `/student/registrations` | 我的报名 |
| POST | `/student/registrations/{activityId}/cancel` | 取消报名 |

### 教师

| 方法 | 路径 | 功能 |
|---|---|---|
| GET | `/teacher/activities` | 我的活动 |
| GET | `/teacher/activities/new` | 新建活动表单 |
| POST | `/teacher/activities` | 创建草稿 |
| GET | `/teacher/activities/{id}` | 活动详情 |
| GET | `/teacher/activities/{id}/edit` | 编辑草稿表单 |
| POST | `/teacher/activities/{id}/edit` | 更新草稿 |
| POST | `/teacher/activities/{id}/delete` | 删除草稿 |
| POST | `/teacher/activities/{id}/publish` | 发布 |
| GET | `/teacher/activities/{id}/registrations` | 查看报名名单 |
| POST | `/teacher/activities/{id}/close` | 关闭活动 |

所有状态变更使用 POST，不使用 GET。

---

## 12. 会话与权限

Session 建议保存：

```text
LOGIN_USER_ID
LOGIN_USER_NAME
LOGIN_USER_ROLE
```

`AuthInterceptor`：

- 公共路径放行；
- 无会话访问业务路径时跳转 `/login`；
- `/student/**` 要求 `STUDENT`；
- `/teacher/**` 要求 `TEACHER`；
- 错误角色访问时返回 403 或重定向到其角色首页并提示。

Service 层仍必须重新检查资源所有权，例如教师操作活动时必须验证 `creator.id == teacherId`，不能仅依赖 URL 拦截器。

---

## 13. 数据库配置

默认连接本机 MySQL 8.4。

`application.properties` 使用环境变量，不硬编码密码：

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/campus_activity?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.thymeleaf.cache=false
```

Windows PowerShell 示例：

```powershell
$env:DB_PASSWORD="你的本机MySQL密码"
.\mvnw.cmd spring-boot:run
```

`.gitignore` 至少忽略：

```text
target/
.idea/
.vscode/
*.iml
.env
application-local.properties
```

不得提交真实密码、Token、私钥和真实个人隐私数据。

---

## 14. 核心业务流程：学生报名

```text
学生登录
  ↓
查看 PUBLISHED 活动
  ↓
进入详情
  ↓
点击报名
  ↓
检查用户为 STUDENT
  ↓
检查活动为 PUBLISHED
  ↓
检查当前时间未超过 registrationDeadline
  ↓
检查当前人数 < capacity
  ↓
检查未重复报名
  ↓
创建 Registration
  ↓
报名成功
```

任何检查失败：

```text
不写入 Registration
→ 返回明确错误提示
→ 当前数据保持不变
```

---

## 15. 验证方案

### TEST-01 → REQ-01

场景：注册、登录和访问控制。

至少验证：

- 新用户注册成功；
- 重复用户名被拒绝；
- 正确密码登录成功；
- 错误密码登录失败；
- 学生不能访问教师路径；
- 未登录不能进入业务路径。

### TEST-02 → REQ-02

场景：教师创建并发布活动。

步骤：

1. 教师创建草稿；
2. 编辑草稿；
3. 发布；
4. 尝试再次编辑。

预期：

- 创建、编辑、发布成功；
- 发布后编辑被拒绝；
- 另一教师不能管理此活动。

### TEST-03 → REQ-03

场景：学生浏览。

预期：

- PUBLISHED 出现在列表；
- DRAFT/CLOSED 不在普通列表；
- 已截止或满员的 PUBLISHED 活动仍可查看，但报名不可用或报名请求被明确拒绝。

### TEST-04 → REQ-04

场景：报名。

至少验证：

- 正常报名成功；
- 重复报名失败；
- 满员失败；
- 截止后失败；
- CLOSED 失败。

### TEST-05 → REQ-05

场景：我的报名和取消。

预期：

- 学生只看到自己的报名；
- 可取消符合条件的报名；
- 取消后记录删除，名额释放；
- 可以重新报名；
- CLOSED 或截止后的活动不能取消。

### TEST-06 → REQ-06

场景：教师查看报名并关闭活动。

预期：

- 教师只看到自己活动的报名名单；
- 报名人数正确；
- 关闭后状态为 CLOSED；
- 关闭后学生不能继续报名。

---

## 16. 需求追踪矩阵

| User Story | Requirement | 核心设计/模块 | 主要验证 |
|---|---|---|---|
| US-01 | REQ-01 | UserService / AuthController / AuthInterceptor | TEST-01 |
| US-02 | REQ-02 | ActivityService / TeacherController / ActivityStatus | TEST-02 |
| US-03 | REQ-03 | ActivityRepository / StudentController | TEST-03 |
| US-04 | REQ-04 | RegistrationService / Registration UNIQUE | TEST-04 |
| US-05 | REQ-05 | RegistrationService / student registrations view | TEST-05 |
| US-06 | REQ-06 | ActivityService + RegistrationService / teacher registrations view | TEST-06 |

---

## 17. AI 使用原则

AI 可以参与需求检查、设计检查、编码、测试、缺陷定位和修复，但所有结果先视为候选成果。

每次重要 AI 协作至少保留以下信息：

1. 提供给 AI 的真实上下文；
2. AI 给出的主要建议；
3. 哪些建议被采纳、修改或拒绝；
4. 判断理由；
5. 最后如何验证。

建议保留三类代表性证据：

- 需求范围取舍；
- 重复报名的“Service + 数据库唯一约束”设计；
- 最终按 REQ-01 ～ REQ-06 进行代码与业务检查。

禁止为了实验报告伪造不存在的 AI 判断、BUG 或 Git 历史。

---

## 18. 变更规则

实现期间若发现：

- 原需求无法实现；
- 规则冲突；
- 设计明显不合理；
- 实际实现必须改变接口或数据关系；

先：

1. 说明问题；
2. 判断是否需要修改需求或工程意图；
3. 更新本设计文档；
4. 再修改代码；
5. 用 Git 记录变化；
6. 对受影响需求回归验证。

普通代码修复不需要修改工程意图。

---

## 19. Definition of Done

V1.0 完成时必须满足：

- [ ] Maven 测试通过；
- [ ] 应用可以连接 MySQL 启动；
- [ ] REQ-01 ～ REQ-06 均可人工演示；
- [ ] TEST-01 ～ TEST-06 有实际结果；
- [ ] 核心异常规则验证完成；
- [ ] Git 有多次真实阶段性提交；
- [ ] `docs/requirements.md`、`docs/engineering-intent.md`、`docs/design.md` 与实现一致；
- [ ] README 有启动方法；
- [ ] 仓库无数据库密码或其他敏感信息；
- [ ] UI 能正常操作即可，不额外投入审美优化；
- [ ] 不存在未经确认的范围扩展。
