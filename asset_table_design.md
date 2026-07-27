# 资产与持仓表设计（V2）

## 结论

当前 5 张表思路是可行的，已经覆盖了资产主数据 + 组合 + 持仓三层结构，适合作为第一版落地。

## 设计检查与建议

1. **债券字段重复**
   - 你给的字段中 `类型` 出现了两次。
   - 建议保留一个字段：`bond_type`。

2. **持仓表跨资产引用无法直接做单一外键**
   - `holding.asset_id` 会指向 `stocks` / `bonds` / `cash_assets` 三张不同表。
   - 关系型数据库无法对一个字段同时建立三个外键。
   - 解决方式：
     - 保留 `asset_type + asset_id` 组合；
     - 在业务层按 `asset_type` 校验 `asset_id` 是否存在（后端已实现）。

3. **价格和数量建议使用高精度小数**
   - 价格、利率、持仓数量使用 `DECIMAL`，避免浮点误差。

4. **时间字段统一**
   - 每张表都包含 `created_at`、`updated_at`，便于审计和同步。

5. **持仓唯一性建议**
   - 同一组合下，同一资产只保留一行：
   - 唯一约束 `UNIQUE (portfolio_id, asset_type, asset_id)`（后端已实现）。

---

## 表结构（建议）

### 1) 股票表 `stocks`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| symbol | VARCHAR(32) | NOT NULL, UNIQUE | 股票代码 |
| name | VARCHAR(128) | NOT NULL | 名称 |
| sector | VARCHAR(64) | NOT NULL | 所属行业 |
| exchange | VARCHAR(64) | NOT NULL | 交易所 |
| price | DECIMAL(19,4) | NOT NULL | 当前价格 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 2) 债券表 `bonds`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| name | VARCHAR(128) | NOT NULL | 名称 |
| bond_type | VARCHAR(64) | NOT NULL | 债券类型 |
| issuer | VARCHAR(128) | NOT NULL | 发行机构 |
| interest_rate | DECIMAL(8,4) | NOT NULL | 利率 |
| maturity_date | DATE | NOT NULL | 到期时间 |
| current_price | DECIMAL(19,4) | NOT NULL | 当前价格 |
| risk_level | VARCHAR(32) | NOT NULL | 风险等级 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 3) 现金表 `cash_assets`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| currency | VARCHAR(16) | NOT NULL, UNIQUE | 币种 |
| exchange_rate | DECIMAL(19,6) | NOT NULL | 汇率 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 4) 资产组合表 `portfolios`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| portfolio_name | VARCHAR(128) | NOT NULL, UNIQUE | 组合名称 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

### 5) 持仓表 `portfolio_holdings`

| 字段 | 类型 | 约束 | 说明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 主键 |
| portfolio_id | BIGINT | NOT NULL, FK -> portfolios(id) | 组合ID |
| asset_type | VARCHAR(16) | NOT NULL | 资产类型（STOCK/BOND/CASH） |
| asset_id | BIGINT | NOT NULL | 资产ID（由 asset_type 决定目标表） |
| quantity | DECIMAL(19,4) | NOT NULL | 持仓数量 |
| created_at | DATETIME | NOT NULL | 创建时间 |
| updated_at | DATETIME | NOT NULL | 更新时间 |

附加约束：
- `UNIQUE (portfolio_id, asset_type, asset_id)`

---

## 参考 DDL（MySQL 8）

```sql
CREATE TABLE portfolios (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    portfolio_name VARCHAR(128) NOT NULL UNIQUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE stocks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    symbol VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    sector VARCHAR(64) NOT NULL,
    exchange VARCHAR(64) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE bonds (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    bond_type VARCHAR(64) NOT NULL,
    issuer VARCHAR(128) NOT NULL,
    interest_rate DECIMAL(8,4) NOT NULL,
    maturity_date DATE NOT NULL,
    current_price DECIMAL(19,4) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE cash_assets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    currency VARCHAR(16) NOT NULL UNIQUE,
    exchange_rate DECIMAL(19,6) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE portfolio_holdings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    portfolio_id BIGINT NOT NULL,
    asset_type VARCHAR(16) NOT NULL,
    asset_id BIGINT NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_holding_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolios(id),
    CONSTRAINT uk_portfolio_asset UNIQUE (portfolio_id, asset_type, asset_id)
);
```

