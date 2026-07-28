# Portfolio Manager | 投资组合管理系统

A training project for building a portfolio management system with a REST API first approach.
这是一个以 REST API 为核心的投资组合管理训练项目。

## 1) Overview | 项目概览

- Goal: manage stocks, bonds, and cash in portfolios.
  目标：管理组合中的股票、债券和现金资产。
- Goal: support browse/add/update/remove workflows.
  目标：支持浏览、新增、修改、删除流程。
- Goal: provide dashboard-style analytics and visualization.
  目标：提供 Dashboard 分析与可视化。
- Stretch goals: Neo4j graph modeling, AI, and Quantum exploration.
  延展目标：Neo4j 图建模、AI 与量子计算探索。

## 2) Current Status | 当前状态

- Backend: implemented in `backend/` with Spring Boot.
  后端：已在 `backend/` 目录基于 Spring Boot 实现。
- Frontend: scaffold stage in `frontend/` (ready for page development).
  前端：`frontend/` 目前为脚手架阶段（可继续页面开发）。
- API focus now: full Holding CRUD + update APIs for Portfolio/Stock/Bond/Cash.
  当前 API 重点：Holding 完整 CRUD + Portfolio/Stock/Bond/Cash 的更新接口。

## 3) Repository Structure | 目录结构

```text
nova/
  README.md
  asset_table_design.md
  development_plan.md
  docker-compose.yml
  backend/
    pom.xml
    src/main/java/com/nova/portfolio/
      PortfolioApplication.java
      controller/
      dto/
      exception/
      mapper/
      model/
      repository/
      service/
    src/main/resources/
      application.yml
      application-dev.yml
      application-test.yml
    src/test/java/com/nova/portfolio/
      PortfolioApplicationTests.java
      UpdateApiMockMvcTests.java
    update_test_data.md
  frontend/
    README.md
```

## 4) Tech Stack | 技术栈

### Implemented now | 当前已实现

- Java 21
- Spring Boot 3.3.2
- Spring Web / Validation / Data JPA
- OpenAPI UI (`springdoc-openapi`)
- H2 (default) + MySQL driver (dev profile)
- JUnit + MockMvc integration tests

### Planned target | 规划中

- React + Axios + UI framework (e.g. Ant Design)
  React + Axios + UI 组件库（如 Ant Design）
- Dashboard charting (e.g. ECharts)
  Dashboard 图表（如 ECharts）
- Neo4j graph visualization
  Neo4j 图关系可视化
- AI / Quantum exploration modules
  AI / 量子计算探索模块

## 5) Data Model (Confirmed) | 数据模型（已确认）

Schema is defined by entities in `backend/src/main/java/com/nova/portfolio/model/`.
表结构以 `backend/src/main/java/com/nova/portfolio/model/` 实体定义为准。

- `portfolios`: `id`, `portfolio_name`(unique), `created_at`, `updated_at`
- `stocks`: `id`, `symbol`(unique), `name`, `sector`, `exchange`, `price`, timestamps
- `bonds`: `id`, `name`, `bond_type`, `issuer`, `interest_rate`, `maturity_date`, `current_price`, `risk_level`, timestamps
- `cash_assets`: `id`, `currency`(unique), `exchange_rate`, timestamps
- `portfolio_holdings`: `id`, `portfolio_id`, `asset_type`(`STOCK`/`BOND`/`CASH`), `asset_id`, `quantity`, timestamps, unique(`portfolio_id`,`asset_type`,`asset_id`)

## 6) API Summary | 接口摘要

Base path: `/api`

### Implemented | 已实现

- Holding:
  - `POST /api/holdings`
  - `GET /api/holdings`
  - `GET /api/holdings/{id}`
  - `PUT /api/holdings/{id}`
  - `DELETE /api/holdings/{id}`
- Update endpoints:
  - `PUT /api/portfolios/{id}`
  - `PUT /api/stocks/{id}`
  - `PUT /api/bonds/{id}`
  - `PUT /api/cash-assets/{id}`

### Error mapping | 错误状态

- `400` validation errors | 参数校验错误
- `404` resource not found | 资源不存在
- `409` conflict (duplicate/unique conflict) | 业务冲突（唯一约束等）
- `500` unexpected errors | 未预期异常

## 7) Feature Status Matrix | 功能状态矩阵

| Feature | Status | Notes |
|---|---|---|
| Portfolio management | Partially implemented | `PUT` exists; full CRUD planned |
| Stock management | Partially implemented | `PUT` exists; full CRUD planned |
| Bond management | Partially implemented | `PUT` exists; full CRUD planned |
| Cash asset management | Partially implemented | `PUT` exists; full CRUD planned |
| Holding management | Implemented | Full CRUD exists |
| Transaction management | Planned | Not in current backend code |
| Authentication/Login | Planned | Single-user assumption currently |
| Dashboard visualization | Planned | Frontend phase |
| Neo4j graph module | Planned | Stretch goal |
| AI/Quantum exploration | Planned | Stretch goal |

## 8) Frontend Direction | 前端方向

Priority order:
优先级顺序：

1. Browse portfolio/holdings | 浏览组合与持仓
2. Add and remove items | 新增与删除资产
3. Update existing records | 修改已有记录
4. Portfolio performance visualization | 组合表现可视化
5. Graph and AI/Quantum experiments | 图谱与 AI/量子实验页

Suggested pages:
建议页面：

- Login (optional MVP)
- Dashboard
- Portfolio
- Stock / Bond / Cash
- Holding
- Transaction (planned)
- Graph (planned)

## 9) Local Run | 本地运行

Prerequisites: JDK 21
前置要求：JDK 21

```powershell
Set-Location "C:\Users\Administrator\IdeaProjects\nova\backend"
.\mvnw.cmd spring-boot:run
```

Run tests:
执行测试：

```powershell
Set-Location "C:\Users\Administrator\IdeaProjects\nova\backend"
.\mvnw.cmd test
```

API docs after startup:
启动后接口文档：

- `http://localhost:8080/swagger-ui.html`
- `http://localhost:8080/swagger-ui/index.html`

## 10) Testing Snapshot | 测试覆盖概览

- `PortfolioApplicationTests`: context load smoke test
  `PortfolioApplicationTests`：应用上下文启动冒烟测试
- `UpdateApiMockMvcTests`: update API integration tests (200/404/409)
  `UpdateApiMockMvcTests`：更新接口集成测试（200/404/409）
- Manual test payloads: `backend/update_test_data.md`
  手工联调数据：`backend/update_test_data.md`

## 11) Collaboration | 协作规范

- Branch from `develop` for features.
  功能开发从 `develop` 分支切出。
- Use PR + code review.
  使用 PR + Code Review。
- Follow Conventional Commits (`feat`, `fix`, `docs`, `refactor`, `test`).
  提交信息遵循 Conventional Commits 规范。

## 12) Future Improvements | 后续规划

- Complete full CRUD for all modules.
  完成各模块完整 CRUD。
- Add transaction records and PnL analytics.
  增加交易记录与盈亏分析。
- Add portfolio performance/allocation dashboards.
  增加组合表现与配置可视化。
- Add Neo4j graph APIs and visualization.
  增加 Neo4j 图关系接口与可视化。
- Explore AI/Quantum use cases.
  探索 AI 与量子优化场景。

## 13) Notes | 说明

- This repository is currently backend-first.
  当前仓库处于后端优先阶段。
- Product goals remain unchanged from the original brief.
  产品目标保持与原需求一致。
- This document clearly separates implemented vs planned scope.
  本文档明确区分“已实现”与“规划中”范围。
