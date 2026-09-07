# 校园活动管理系统 V1.0 软件设计

## 1. 总体结构

系统采用服务端渲染的单体 Spring Boot MVC 结构：

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
MySQL 8.4
```

- **Controller：** 接收 HTTP 请求、绑定表单、组织页面跳转和提示，不堆积核心业务规则。
- **Service：** 承担活动状态、报名条件、角色和资源所有权等业务规则。
- **Repository：** 通过 Spring Data JPA 访问数据库。
- **Entity：** 表达用户、活动和报名三个核心领域对象。
- **DTO/Form：** 承载注册、登录和活动表单输入及 Bean Validation 约束。
- **Interceptor：** 检查会话与学生/教师路径访问权限。
- **Thymeleaf：** 渲染可读、可操作的服务端页面。

## 2. 数据设计

### 2.1 User

表名 `users`，保存 `id`、唯一 `username`、`passwordHash`、`name`、`role` 和 `createdAt`。学生与教师共用该表，通过 `STUDENT`/`TEACHER` 区分。

### 2.2 Activity

表名 `activities`，保存 `id`、`title`、`description`、`location`、`registrationDeadline`、`startTime`、`endTime`、`capacity`、`status`、`creator` 和 `createdAt`。每项活动必须属于一名教师创建者。

### 2.3 Registration

表名 `registrations`，保存 `id`、`student`、`activity` 和 `registeredAt`。数据库对 `(student_id, activity_id)` 建立联合唯一约束。

### 2.4 数据关系

```text
User(TEACHER) 1 ---- N Activity
User(STUDENT) 1 ---- N Registration N ---- 1 Activity
```

## 3. 活动状态模型

```text
DRAFT --publish--> PUBLISHED --close--> CLOSED
```

- 新建活动默认为 `DRAFT`，可查看、编辑、删除和发布；
- `PUBLISHED` 不可编辑或删除，可供学生查看、报名，并可由创建教师关闭；
- `CLOSED` 只读，不在学生普通活动列表出现，也不接受新报名或取消；
- 已报名学生仍可在“我的报名”中看到后来关闭的活动。

## 4. 学生报名核心流程

```text
学生登录
  -> 查看 PUBLISHED 活动
  -> 进入活动详情
  -> 提交报名
  -> Service 检查学生角色
  -> 检查活动为 PUBLISHED
  -> 检查未超过报名截止时间
  -> 检查当前报名人数小于容量
  -> 检查学生没有重复报名
  -> 创建 Registration
  -> 返回报名结果
```

任一检查失败时不写入报名记录，并向页面返回明确业务提示。页面显示的按钮状态只用于反馈，不能替代 Service 校验。

## 5. 访问与所有权设计

会话保存 `LOGIN_USER_ID`、`LOGIN_USER_NAME` 和 `LOGIN_USER_ROLE`。拦截器阻止未登录访问业务页面，并限制 `/student/**` 和 `/teacher/**` 的角色。

路径保护不能替代资源授权。教师查看、编辑、删除、发布、关闭活动或查看报名名单时，Service 必须使用活动 ID 与教师 ID 共同查询或等价方式再次验证 `creator` 所有权。

## 6. 关键数据校验

- 标题、描述和地点非空；
- `capacity > 0`；
- `registrationDeadline < startTime < endTime`；
- 报名仅允许 `STUDENT` 对 `PUBLISHED` 活动发起；
- 报名时必须未截止、未满员且未重复；
- 取消时报名必须属于当前学生，活动仍为 `PUBLISHED` 且未截止。

## 7. 关键设计决策

### DEC-01 统一用户模型

学生和教师共用 `User` 表，通过 `role` 区分。两类账户字段基本相同，拆表会造成重复结构。

### DEC-02 活动采用显式状态模型

活动采用 `DRAFT -> PUBLISHED -> CLOSED`，明确区分编辑、参与和结束阶段，使各状态允许的操作可验证。

### DEC-03 发布后不可修改

`PUBLISHED` 活动不能编辑或删除，只能查看报名并关闭，避免已有报名后继续修改时间、容量等关键数据造成一致性问题。

### DEC-04 应用层和数据库双重防重复报名

Service 在保存前主动查询并提供明确业务错误，数据库联合唯一约束作为最终一致性保护。

## 8. 设计与验证对应

| 需求 | 主要设计模块 | 主要验证 |
|---|---|---|
| REQ-01 | UserService、AuthController、AuthInterceptor | TEST-01 |
| REQ-02 | ActivityService、TeacherController、ActivityStatus | TEST-02 |
| REQ-03 | ActivityRepository、StudentController | TEST-03 |
| REQ-04 | RegistrationService、Registration 联合唯一约束 | TEST-04 |
| REQ-05 | RegistrationService、学生报名页面 | TEST-05 |
| REQ-06 | ActivityService、RegistrationService、教师报名页面 | TEST-06 |
