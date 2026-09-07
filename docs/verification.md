# 校园活动管理系统 V1.0 最终验证记录

## 1. 验证范围与环境

- 验证日期：2026-09-07（Asia/Shanghai）；
- 代码基线：`6e4be1ff7be656d4893b56fd2392488aad40aff2`；
- Java：25.0.4 运行、按 Java 21 编译；
- Spring Boot：4.1.1；
- Maven：3.9.16；
- 数据库：真实 MySQL 8.4.11，数据库 `campus_activity`；
- 业务验证：应用运行于本机 HTTP 端口 18080，使用真实注册、登录和 `HttpSession`；
- 验证数据标识：`v100907202223`，主要活动 ID 为 18～24。

验证只检查 V1.0 已确认需求，未增加业务功能。环境变量中的数据库凭据未写入本文件或仓库。

## 2. Maven 自动测试

执行命令：

```text
mvn clean test
```

2026-09-07 20:32 的最终实际结果：

| 指标 | 结果 |
|---|---:|
| Tests run | 100 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |
| Build | BUILD SUCCESS |

测试覆盖 `AuthInterceptor`、认证 Controller 与页面、教师/学生活动 Controller 与页面、`UserService`、`ActivityService` 和 `RegistrationService`。

## 3. TEST-01 ～ TEST-06 最终人工验收

本轮共执行 39 个真实 HTTP/Session 检查。下表中的实际结果均来自同一次最终验收。

| TEST | 前置条件 | 操作/输入 | 预期结果 | 实际结果 | 结论 |
|---|---|---|---|---|---|
| TEST-01 / REQ-01 | MySQL 可连接；无同名验证账号 | 分别注册 STUDENT/TEACHER；重复注册；正确/错误密码登录；匿名及跨角色访问；退出后再次访问业务路径；查询密码字段格式 | 两类账号可注册；重复/错误密码拒绝；正确角色首页可达；匿名跳登录；跨角色 403；退出后 Session 失效；密码非明文 | 全部行为符合预期；数据库中两条验证账号密码字段均为长度 60、以 BCrypt 标识开头的哈希，未输出哈希值 | PASS |
| TEST-02 / REQ-02 | Teacher A/B 已登录；有效活动时间与容量 | Teacher A 创建 ID 18 的 DRAFT、编辑、发布；发布后再次编辑/删除；Teacher B 修改 URL 查看 | 草稿操作成功；PUBLISHED 后编辑/删除拒绝；Teacher B 无权管理 | 创建、编辑、发布成功；非法编辑未改变标题；删除被拒；Teacher B 被重定向到本人列表并显示无权访问 | PASS |
| TEST-03 / REQ-03 | 分别准备 PUBLISHED、DRAFT、CLOSED、已截止但仍 PUBLISHED 的活动 | Student A 查看普通列表及活动详情 | 仅 PUBLISHED 出现；DRAFT/CLOSED 隐藏；PUBLISHED 详情可读；已截止 PUBLISHED 仍可浏览 | ID 18 与已截止 ID 21 可浏览；ID 19 DRAFT、ID 20 CLOSED 不在列表；截止活动详情显示不可报名原因 | PASS |
| TEST-04 / REQ-04 | Student A/B 已登录；准备普通、容量 1、截止、DRAFT、CLOSED 活动 | 正常报名；重复报名；Student B 报名已满活动；直接 POST 截止/DRAFT/CLOSED 活动 | 仅正常报名写入；其他请求明确拒绝且不产生脏数据 | 正常活动 1 条、满员活动 1 条；截止/DRAFT/CLOSED 均为 0 条；重复报名未产生第二条记录 | PASS |
| TEST-05 / REQ-05 | Student A/B；ID 23 为短截止 PUBLISHED 活动 | 比较两人的“我的报名”；A 取消；检查人数；重新报名；截止后取消；关闭后取消 | 仅显示本人报名；取消释放名额；可重新报名；截止/CLOSED 后拒绝且保留记录 | B 看不到 A 的 ID 23 报名；人数 1→0→1；截止后仍为 1 条；CLOSED 取消显示明确拒绝 | PASS |
| TEST-06 / REQ-06 | Teacher A/B；Student A/B/C；ID 24 已发布且 A/B 已报名 | A 查看名单；B 修改 URL；A 关闭；检查学生报名、教师名单；C 尝试新报名 | A 看到 2/5 与两名学生；B 无权；关闭后记录保留、不能新增报名、名单仍可看 | A 名单和人数正确；B 被拒；状态为 CLOSED、报名仍为 2 条；C 新报名为 0 条；关闭后教师仍看到 2/5 | PASS |

数据库补充核对：

```text
TEST01_USERS 2 ROLES=STUDENT,TEACHER
TEST01_BCRYPT_FORMAT 2
TEST04_COUNTS life=1,full=1,expired=0,draft=0,closed=0
TEST05_CANCEL_REG_COUNT 1
TEST06_STATUS CLOSED REG_COUNT=2
TEST06_STUDENT_C_COUNT 0
UNIQUE_CONSTRAINT 1
```

## 4. REQ → 设计 → 实现 → TEST 追踪矩阵

| Requirement | User Story | 设计依据 | 主要 Controller | 主要 Service | Repository / 数据约束 | 代表性自动测试 | 人工测试 | 结论 |
|---|---|---|---|---|---|---|---|---|
| REQ-01 | US-01 注册与登录 | Session 三属性、角色路径拦截、BCrypt | `AuthController`、`HomeController`、`AuthInterceptor` | `UserService` | `UserRepository`；`users.username` 唯一 | `registerTrimsFieldsAndStoresBcryptHash`、`studentLoginStoresSessionAndRedirectsToStudentHome`、`logoutInvalidatesExistingSession`、角色拦截测试 | TEST-01 | PASS |
| REQ-02 | US-02 教师创建并发布活动 | `DRAFT → PUBLISHED`；发布后不可改删；Service 所有权 | `TeacherController` | `ActivityService` | `ActivityRepository.findByIdAndCreatorId`；Activity 必须关联 creator | `teacherCanUpdateOwnDraft`、`teacherCannotUpdatePublishedActivity`、`teacherCannotUpdateAnotherTeachersDraft`、`validDraftCanBePublished` | TEST-02 | PASS |
| REQ-03 | US-03 学生浏览活动 | 普通列表/详情仅 PUBLISHED，按开始时间升序 | `StudentController` | `ActivityService` | `findByStatusOrderByStartTimeAsc`、`findByIdAndStatus` | `studentListReturnsPublishedActivitiesInRepositoryStartOrder`、`draftActivityDetailIsNotAvailableToStudents`、`deadlinePassedPublishedActivityRemainsReadable` | TEST-03 | PASS |
| REQ-04 | US-04 学生报名活动 | 角色、状态、截止、重复、容量规则；双重防重复 | `StudentController` | `RegistrationService` | `existsByStudentIdAndActivityId`、`countByActivityId`；`UNIQUE(student_id, activity_id)` | `validStudentRegistersForPublishedAvailableActivity`、重复/满员/截止/DRAFT/CLOSED/非学生拒绝测试 | TEST-04 | PASS |
| REQ-05 | US-05 学生管理自己的报名 | 本人报名倒序；仅 PUBLISHED 且未截止可取消 | `StudentController` | `RegistrationService` | `findByStudentIdOrderByRegisteredAtDesc`、`findByStudentIdAndActivityId` | `studentCanCancelOwnPublishedRegistrationBeforeDeadline`、`studentCanRegisterAgainAfterCancellation`、截止/CLOSED/他人报名拒绝测试 | TEST-05 | PASS |
| REQ-06 | US-06 教师查看并关闭活动 | 名单所有权；`PUBLISHED → CLOSED`；关闭不删报名 | `TeacherController` | `ActivityService`、`RegistrationService` | `findByIdAndCreatorId`、`findByActivityIdOrderByRegisteredAtAsc`、`countByActivityId` | `teacherCanReadOwnActivityRegistrationListAndCount`、`anotherTeacherCannotReadRegistrationListOrCount`、`teacherCanStillReadRegistrationsAfterActivityCloses` | TEST-06 | PASS |

审计未发现“需求已写但未实现”、无需求依据的业务实现、设计与代码采用不同逻辑或数据模型不一致的情况。

## 5. 范围审计

对 `src` 和 `pom.xml` 搜索以下范围外能力：

```text
ADMIN、审核、搜索、分类、推荐、评论、通知、签到、导出、报名审批、
JWT、Vue、React、Redis、Docker、微服务
```

实现命中数为 0。文档对这些词的出现均用于明确“不实现”，不构成范围扩展。项目仍为 Spring MVC + Thymeleaf 单体应用，只有 `STUDENT` 和 `TEACHER` 两种角色及 User、Activity、Registration 三个业务实体。

## 6. 安全与配置检查

- `application.properties` 的密码项为 `spring.datasource.password=${DB_PASSWORD}`；
- README 只使用占位符说明 `DB_USERNAME`、`DB_PASSWORD` 和可选 `DB_URL`；
- `.env`、`application-local.properties`、IDE 文件和 `target/` 已被忽略；
- 当前跟踪文件及全部 Git 补丁历史中，GitHub Token、AWS Access Key、OpenAI 风格密钥和 PEM 私钥常见模式命中数均为 0；
- 未发现真实数据库密码、Token、私钥或真实用户隐私数据；
- MySQL 验证凭据只从环境变量读取，验证输出未包含密码或哈希值。

## 7. 两个真实 BUG 证据

### BUG-01：Thymeleaf `#fields` 上下文导致 HTTP 500

- 发现阶段：Task 3 真实 HTTP 页面验证；
- 问题：登录/注册模板中的 `#fields.hasAnyErrors()` 曾在缺少当前 `th:object` 表单上下文的位置求值，页面渲染产生 HTTP 500；
- 原因：`#fields` 必须在绑定表单上下文中使用；
- 修复：将错误判断置于具有对应 `th:object` 的 `<form>` 内；
- Git 证据边界：问题在 REQ-01 阶段提交前发现并修复，因此没有伪造独立 `fix:` commit；修复后的页面随 `af90eb8` 提交；
- 回归证据：`AuthViewRenderingTest` 通过，最终 TEST-01 注册和登录页面真实 HTTP 渲染通过。

### BUG-02：CLOSED 伪造报名 POST 的错误信息被覆盖

- 发现阶段：Task 6 真实 HTTP 验证；
- 问题：对 CLOSED 活动直接提交报名 POST 后，首次拒绝信息在详情二次重定向时被“活动不存在或未发布”覆盖；
- 原因：`StudentController.detail()` 捕获第二次异常时无条件覆盖已有 flash error；
- 修复：优先保留请求模型中已有的报名业务错误，仅在没有既有错误时使用详情查询错误；
- 独立修复提交：`0e9a344 fix: preserve registration rejection message`；
- 回归证据：`closedRegistrationKeepsSpecificReasonWhenDetailRedirectsToList` 通过；最终 TEST-04/TEST-06 的 CLOSED 伪造 POST 均显示“只有已发布活动可以报名”。

最终验证期间出现的 PowerShell 响应 URI 解析及 `;jsessionid` 路径判定问题属于验收脚本问题，不属于产品 BUG，也未记录为产品缺陷。

## 8. Git 迭代证据

| 阶段 | Commit | Message |
|---|---|---|
| 工程基线与先行文档 | `af834c8` | `chore: initialize V1.0 project and engineering docs` |
| 核心数据模型 | `d8cc2fb` | `feat: add core V1.0 data model` |
| REQ-01 | `af90eb8` | `feat: implement registration and session login (REQ-01)` |
| REQ-02 | `47d366d` | `feat: implement teacher activity lifecycle (REQ-02)` |
| REQ-03 | `f96a4fa` | `feat: add student activity browsing (REQ-03)` |
| REQ-04 | `8fcdb76` | `feat: implement student activity registration rules (REQ-04)` |
| 真实 BUG-02 修复 | `0e9a344` | `fix: preserve registration rejection message` |
| REQ-05 / REQ-06 | `6e4be1f` | `feat: complete registration management and closing flow (REQ-05 REQ-06)` |

历史按真实阶段递增形成，未进行 squash、重写或事后伪造。

## 9. V1.0 完成判断

REQ-01～REQ-06 均能追踪到用户故事、设计、Controller、Service、Repository/约束、自动测试和真实 TEST-01～06；100 项 Maven 测试和 39 项真实 MySQL/HTTP 检查全部通过；未发现需求遗漏、未经确认的范围扩展、设计偏离或敏感信息风险。

依据 `docs/engineering-intent.md` 的完成条件，校园活动管理系统 V1.0 可以判定完成。
