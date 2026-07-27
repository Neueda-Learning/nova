

1. **股票（Stock）**
2. **债券（Bond）**
3. **现金（Cash）**
4. **加密货币（Crypto）**



# 四类资产表设计方案

整体结构：

```text
Asset Management System


        Stock Table
             |
             
        Bond Table
             |
             
        Cash Table
             |
             
        Crypto Table

```

四张表分别保存不同资产类型的信息。

---

# 1. Stock（股票表）

## 作用

保存股票类资产信息。

例如：

* Apple 股票
* Microsoft 股票
* HSBC 股票

---

## 表名

```sql
stock
```

---

## 字段设计

| 字段            | 类型            | 说明   |
| ------------- | ------------- | ---- |
| id            | BIGINT        | 股票ID |
| symbol        | VARCHAR(20)   | 股票代码 |
| company_name  | VARCHAR(100)  | 公司名称 |
| sector        | VARCHAR(50)   | 所属行业 |
| exchange      | VARCHAR(50)   | 交易所  |
| current_price | DECIMAL(10,2) | 当前价格 |
| market_cap    | DECIMAL(15,2) | 市值   |
| created_at    | TIMESTAMP     | 创建时间 |

---

## 示例数据

| id | symbol | company_name | sector     | exchange | price |
| -- | ------ | ------------ | ---------- | -------- | ----- |
| 1  | AAPL   | Apple Inc    | Technology | NASDAQ   | 200   |
| 2  | MSFT   | Microsoft    | Technology | NASDAQ   | 450   |

---

# 2. Bond（债券表）

## 作用

保存债券类资产。

例如：

* 美国国债
* 公司债券

---

## 表名

```sql
bond
```

---

## 字段设计

| 字段            | 类型            | 说明   |
| ------------- | ------------- | ---- |
| id            | BIGINT        | 债券ID |
| bond_name     | VARCHAR(100)  | 债券名称 |
| issuer        | VARCHAR(100)  | 发行机构 |
| bond_type     | VARCHAR(50)   | 债券类型 |
| interest_rate | DECIMAL(5,2)  | 利率   |
| maturity_date | DATE          | 到期日期 |
| current_price | DECIMAL(10,2) | 当前价格 |
| risk_level    | VARCHAR(20)   | 风险等级 |

---

## 示例数据

| bond_name            | issuer        | interest_rate | risk   |
| -------------------- | ------------- | ------------- | ------ |
| US Treasury 10Y      | US Government | 4.2%          | Low    |
| Apple Corporate Bond | Apple Inc     | 3.8%          | Medium |

---

# 3. Cash（现金表）

## 作用

保存现金资产。

现金和股票不同：

股票有价格波动；

现金主要关注：

* 币种
* 金额
* 汇率

---

## 表名

```sql
cash
```

---

## 字段设计

| 字段            | 类型            | 说明   |
| ------------- | ------------- | ---- |
| id            | BIGINT        | 现金ID |
| currency      | VARCHAR(10)   | 货币类型 |
| amount        | DECIMAL(15,2) | 金额   |
| exchange_rate | DECIMAL(10,4) | 汇率   |
| created_at    | TIMESTAMP     | 创建时间 |

---

## 示例数据

| currency | amount |
| -------- | ------ |
| USD      | 50000  |
| EUR      | 30000  |

---

# 4. Crypto（加密货币表）

## 作用

保存数字资产。

例如：

* Bitcoin
* Ethereum

---

## 表名

```sql
crypto
```

---

## 字段设计

| 字段            | 类型            | 说明     |
| ------------- | ------------- | ------ |
| id            | BIGINT        | 加密货币ID |
| symbol        | VARCHAR(20)   | 代码     |
| crypto_name   | VARCHAR(100)  | 名称     |
| blockchain    | VARCHAR(50)   | 区块链    |
| current_price | DECIMAL(15,2) | 当前价格   |
| market_cap    | DECIMAL(20,2) | 市值     |
| volatility    | DECIMAL(5,2)  | 波动率    |

---

## 示例数据

| symbol | name     | blockchain | price |
| ------ | -------- | ---------- | ----- |
| BTC    | Bitcoin  | Bitcoin    | 65000 |
| ETH    | Ethereum | Ethereum   | 3500  |

---

# 四张表关系

目前没有关系表，所以四张表属于：

> 独立资产数据源

结构：

```
                 Asset Management


        +-----------+


        Stock


        +-----------+



        Bond


        +-----------+



        Cash


        +-----------+



        Crypto


        +-----------+

```

---

# 后续前端 Dashboard 可以直接基于这四张表展示

## 1. Asset Overview（资产总览）

四类资产数量：

```
Stock:

120


Bond:

30


Cash:

5


Crypto:

10

```

---

## 2. Asset Distribution（资产分布）

饼图：

```
Stock       50%

Bond        20%

Cash        20%

Crypto      10%

```

---

## 3. Market Data Dashboard

股票：

展示：

* 当前价格
* 行业

债券：

展示：

* 利率
* 到期时间

现金：

展示：

* 余额

加密货币：

展示：

* 价格
* 波动率

---

# Neo4j 如果只基于四个资产表，也可以设计

节点：

```
Stock

Bond

Cash

Crypto

```

关系：

```
Stock -------- BELONGS_TO -------- Sector


Bond -------- ISSUED_BY -------- Organization


Crypto -------- BASED_ON -------- Blockchain


Cash -------- USES -------- Currency

```

例如：

```
Apple Stock

      |
 BELONGS_TO

      |

Technology Sector


Bitcoin

      |

BASED_ON

      |

Bitcoin Blockchain

```

---

# 当前阶段建议

你们四天项目，如果先只做四个资产表：

## Day 1

完成：

✅ 四张表设计
✅ MySQL创建
✅ Spring Boot Entity
✅ CRUD接口规划

## Day 2

完成：

✅ 四类资产 CRUD
✅ 前端展示页面

## Day 3

完成：

✅ Dashboard大屏
✅ Neo4j关系可视化

## Day 4

完成：

✅ 测试
✅ UI优化
✅ Demo准备

---

* 后续可以再扩展 Portfolio/Holding

目前先围绕这四张资产表开发是合理的。你们项目定位可以改成：

> **Multi-Asset Management System（多资产管理系统）**

而不是单纯股票管理系统。
