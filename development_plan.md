

你们这个 Portfolio Manager 如果还是只有 Portfolio、Stock 两张表，和其他组几乎没有区别。

我建议数据库设计稍微丰富一点，但不要增加太多开发量。

---

## 我建议的数据库设计

### 1、Portfolio（投资组合）

一个 Portfolio 代表整个投资组合。

| 字段            | 类型     | 说明           |
| ------------- | ------ | ------------ |
| id            | Long   | 主键           |
| portfolioName | String | Portfolio 名称 |
| description   | String | 描述           |
| totalValue    | Double | 当前总资产        |
| cashBalance   | Double | 当前现金         |
| createdAt     | Date   | 创建时间         |
| updatedAt     | Date   | 更新时间         |

---

### 2、Stock（股票信息）

| 字段           | 类型     | 说明         |
| ------------ | ------ | ---------- |
| id           | Long   | 主键         |
| symbol       | String | 股票代码（AAPL） |
| companyName  | String | 公司名称       |
| sector       | String | 行业         |
| exchange     | String | 交易所        |
| currentPrice | Double | 当前价格       |
| marketCap    | Double | 市值         |

---

### 3、Holding（持仓）

这一张表建议一定要有。

Portfolio 和 Stock 是多对多关系。

Holding 就是中间表。

| 字段           | 类型      | 说明           |
| ------------ | ------- | ------------ |
| id           | Long    | 主键           |
| portfolioId  | Long    | Portfolio ID |
| stockId      | Long    | Stock ID     |
| quantity     | Integer | 持仓数量         |
| averagePrice | Double  | 买入均价         |
| currentValue | Double  | 当前价值         |

---

### 4、Transaction（交易记录）

后面Dashboard可以直接统计。

| 字段              | 类型      | 说明         |
| --------------- | ------- | ---------- |
| id              | Long    | 主键         |
| stockId         | Long    | 股票         |
| transactionType | String  | BUY / SELL |
| quantity        | Integer | 数量         |
| price           | Double  | 成交价格       |
| transactionDate | Date    | 成交日期       |

---

这样数据库只有四张表。

非常合理。

---

# Dashboard 建议

登录以后不要直接看到CRUD。

首页建议：

```
---------------------------------------------

 Portfolio Dashboard

---------------------------------------------

 Total Portfolio Value

 $120,000

---------------------------------------------

 Today's Profit

 +2.36%

---------------------------------------------

 Cash Balance

 $18,000

---------------------------------------------

 Holdings

 12 Stocks

---------------------------------------------

 Portfolio Allocation

 (Pie Chart)

---------------------------------------------

 Portfolio Trend

 (Line Chart)

---------------------------------------------

 Sector Distribution

 (Bar Chart)

---------------------------------------------
```

老师一登录。

第一印象就很好。

---

# Neo4j 怎么设计（重点）

这里建议**不要把整个业务迁移到Neo4j**。

MySQL负责业务。

Neo4j负责关系分析。

这样企业里面也是这么做的。

---

## Neo4j 节点（Node）

```
Portfolio

Stock

Sector

Exchange
```

例如

```
(:Portfolio)

(:Stock)

(:Sector)

(:Exchange)
```

---

## Neo4j Relationship

```
Portfolio

HAS_STOCK

↓

Stock

BELONGS_TO

↓

Sector

LISTED_ON

↓

Exchange
```

例如

```
Portfolio A

↓

Apple

↓

Technology

↓

NASDAQ
```

或者

```
Portfolio A

↓

Microsoft

↓

Technology

↓

NASDAQ
```

---

## Cypher 查询

例如

查看Portfolio所有股票

```cypher
MATCH (p:Portfolio)-[:HAS_STOCK]->(s:Stock)
RETURN p,s
```

查看科技行业

```cypher
MATCH (s:Stock)-[:BELONGS_TO]->(sec:Sector)
WHERE sec.name='Technology'
RETURN s
```

查看NASDAQ股票

```cypher
MATCH (s)-[:LISTED_ON]->(e:Exchange)
WHERE e.name='NASDAQ'
RETURN s
```

---

# 前端如何可视化Neo4j

建议不要自己画。

直接使用

## react-force-graph

或者

## vis-network

返回的数据例如：

```json
{
  "nodes":[
    {"id":"Portfolio"},
    {"id":"Apple"},
    {"id":"Technology"},
    {"id":"NASDAQ"}
  ],
  "links":[
    {"source":"Portfolio","target":"Apple"},
    {"source":"Apple","target":"Technology"},
    {"source":"Apple","target":"NASDAQ"}
  ]
}
```

页面效果：

```
                 Technology
                     ○
                     │
                     │
Portfolio ○──────Apple ○──────NASDAQ ○
                     │
                     │
                 Microsoft ○
```

支持：

* 拖动节点
* 鼠标缩放
* 点击节点查看详细信息
* 高亮关联节点

整个效果非常像知识图谱。

---

# Dashboard 图表建议

建议使用 **Apache ECharts**。

做三个图即可。

① Portfolio Allocation

```
Pie Chart

Apple

Microsoft

Tesla

Cash
```

---

② Portfolio Trend

```
Line Chart

Monday

Tuesday

Wednesday

Thursday

Friday
```

展示

```
Portfolio Value
```

---

③ Sector Distribution

```
Bar Chart

Technology

Finance

Healthcare

Energy
```

---

# 四个人建议分工

不要体现前端后端。

而是按模块划分。

| 成员  | 负责模块                                       |
| --- | ------------------------------------------ |
| 成员A | Portfolio 模块（数据模型、业务逻辑、接口）                 |
| 成员B | Stock、Holding、Transaction 模块（数据库、接口、数据管理）  |
| 成员C | Dashboard、数据可视化、Neo4j 图谱展示                 |
| 成员D | 项目集成、测试、Swagger、README、Presentation、Bug 修复 |

这样看起来更像真实企业团队。

---

