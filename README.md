Portfolio Manager
Software Development Document (SDD)

Version: 1.0
Project Duration: 4 Days
Team Size: 4 Members

Table of Contents
Project Overview
System Architecture
Technology Stack
System Module Design
Database Design
Neo4j Graph Database Design
REST API Design
Frontend Design
Dashboard Visualization Design
Git Collaboration Strategy
Development Schedule
Testing Plan
Deployment Plan
Future Improvements
1. Project Overview（项目概述）
1.1 Project Background（项目背景）

随着个人投资和资产管理需求不断增加，投资者通常需要管理多个股票资产、记录交易历史，并实时了解投资组合表现。

传统 Portfolio Management 系统通常只提供简单的数据记录功能，例如：

股票信息管理
持仓管理
交易记录管理

但是缺少：

直观的数据分析
投资组合可视化
股票之间关系探索
智能化分析能力

因此，本项目设计并实现一个基于 Web 的 Portfolio Manager 系统。

该系统提供：

投资组合管理
股票管理
交易记录管理
Dashboard 数据分析
图数据库关系可视化

帮助用户更加直观地理解自己的投资资产。

1.2 Project Objectives（项目目标）

本项目主要目标：

1. 实现完整 Portfolio Management 系统

支持：

创建 Portfolio
查看 Portfolio
管理股票持仓
管理交易记录
2. 提供数据可视化能力

通过 Dashboard 展示：

总资产价值
现金余额
股票分布
收益趋势
3. 引入 Neo4j 图数据库

使用图数据库展示：

Portfolio 与 Stock 的关系
Stock 与 Sector 的关系
Stock 与 Exchange 的关系

提供更加直观的数据关系分析。

4. 实现团队协作开发流程

采用：

Git Branch Workflow
Pull Request
Code Review

保证多人开发过程中的代码质量。

1.3 Functional Requirements（功能需求）
User Management

用户可以：

登录系统
进入个人 Dashboard
Portfolio Management

用户可以：

创建投资组合
查看投资组合
修改投资组合信息
删除投资组合
Stock Management

系统支持：

添加股票
查看股票信息
修改股票信息
删除股票

股票信息包括：

股票代码
公司名称
所属行业
交易市场
Holding Management

用户可以管理：

股票持仓数量
买入价格
当前价值
Transaction Management

记录：

买入交易
卖出交易
交易时间
交易价格
Dashboard Visualization

系统展示：

Portfolio Total Value
Cash Balance
Stock Allocation
Sector Distribution
Performance Trend
Graph Visualization

通过 Neo4j 展示：

Portfolio

     |
 HAS_STOCK

     |

   Stock

     |
 BELONGS_TO

     |

  Sector

     |
 LISTED_ON

     |

 Exchange

1.4 Non-functional Requirements（非功能需求）
Performance

系统应支持：

快速页面加载
API响应时间小于数秒
Maintainability

系统采用：

前后端分离架构
模块化设计
REST API

方便后续扩展。

Scalability

未来可以扩展：

AI分析
云部署
用户权限管理
2. System Architecture（系统架构设计）
2.1 Overall Architecture

系统采用前后端分离架构。

整体结构：

                 User

                  |

                  |

             React Frontend

                  |

              REST API

                  |

            Spring Boot Backend

                  |

        ----------------------

        |                    |

      MySQL               Neo4j

 Business Data       Relationship Data

2.2 Architecture Description
Frontend Layer

负责：

用户交互
页面展示
数据可视化
图数据库展示

主要技术：

React
Axios
Ant Design
ECharts
Backend Layer

负责：

业务逻辑
数据处理
API提供
数据库访问

主要技术：

Spring Boot
Spring Data JPA
Database Layer

系统采用双数据库设计。

MySQL

负责：

结构化业务数据：

Portfolio
Stock
Holding
Transaction
Neo4j

负责：

关系型数据：

股票关系
行业关系
市场关系
2.3 Data Flow

用户操作：

User

↓

React Page

↓

REST API Request

↓

Spring Boot Controller

↓

Service Layer

↓

Repository

↓

Database

↓

Response

↓

Frontend Visualization

3. Technology Stack（技术栈）
Layer	Technology
Frontend	React
UI Framework	Ant Design
Visualization	Apache ECharts
Graph Visualization	react-force-graph
Backend	Spring Boot
API	REST API
ORM	Spring Data JPA
Database	MySQL
Graph Database	Neo4j
Version Control	Git + GitHub
4. System Module Design（系统模块设计）

系统划分为以下模块：

Portfolio Manager

|

├── Authentication Module

|

├── Portfolio Module

|

├── Stock Module

|

├── Holding Module

|

├── Transaction Module

|

├── Dashboard Module

|

└── Neo4j Graph Module

4.1 Portfolio Module

功能：

创建Portfolio
查询Portfolio
修改Portfolio
删除Portfolio

主要业务：

管理用户投资组合。

4.2 Stock Module

功能：

股票信息维护
股票查询

数据：

Symbol
Company Name
Sector
Exchange
4.3 Holding Module

功能：

维护：

Portfolio 和 Stock 的关系。

例如：

My Portfolio

    |

    |

   Apple

   Microsoft

   Tesla

4.4 Transaction Module

负责：

记录所有交易行为。

包括：

BUY
SELL

用于：

收益计算
历史分析
4.5 Dashboard Module

Dashboard 是系统核心展示页面。

展示：

Summary Cards
Total Value

$120000


Cash

$20000


Stocks

15


Today's Gain

+2.5%

Charts
Portfolio Allocation

饼图展示：

Apple       40%

Microsoft   25%

Tesla       15%

Cash        20%

Performance Trend

折线图：

Portfolio Value

|

|

|       *

|    *

| *

----------------

 Mon Tue Wed Thu

Sector Distribution

柱状图：

Technology

Finance

Healthcare

Energy
Part 2：数据库详细设计 + ER关系 + Neo4j建模 + Cypher查询 + REST API设计
5. Database Design（数据库详细设计）
5.1 数据库设计原则

本项目采用 关系数据库 + 图数据库混合架构（Polyglot Persistence）。

设计原则：

MySQL负责核心业务数据存储

包括：

Portfolio 信息
Stock 信息
用户持仓
交易记录
Neo4j负责复杂关系分析

包括：

股票所属行业关系
股票交易市场关系
Portfolio 与 Stock 关系
股票之间的关联分析

整体数据流：

                    User

                     |

                     |

              Spring Boot API

                     |

        ----------------------------

        |                          |

      MySQL                     Neo4j

  Business Data          Relationship Data

5.2 MySQL 数据库设计

数据库名称：

portfolio_manager
5.2.1 Portfolio Table（投资组合表）
表名称
portfolio
功能

用于保存用户创建的投资组合。

例如：

My Growth Portfolio

Retirement Portfolio

Technology Investment
Table Structure
字段	数据类型	是否为空	说明
id	BIGINT	NO	主键
portfolio_name	VARCHAR(100)	NO	投资组合名称
description	VARCHAR(255)	YES	描述信息
total_value	DECIMAL(15,2)	NO	当前资产总价值
cash_balance	DECIMAL(15,2)	NO	当前现金余额
created_at	TIMESTAMP	NO	创建时间
updated_at	TIMESTAMP	NO	更新时间
SQL 示例
CREATE TABLE portfolio
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    portfolio_name VARCHAR(100) NOT NULL,

    description VARCHAR(255),

    total_value DECIMAL(15,2) DEFAULT 0,

    cash_balance DECIMAL(15,2) DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
5.2.2 Stock Table（股票信息表）
表名称
stock
功能

保存股票基础信息。

例如：

AAPL

Apple Inc.

Technology

NASDAQ

Table Structure
字段	类型	说明
id	BIGINT	主键
symbol	VARCHAR(20)	股票代码
company_name	VARCHAR(100)	公司名称
sector	VARCHAR(100)	所属行业
exchange	VARCHAR(50)	交易市场
current_price	DECIMAL(10,2)	当前价格
market_cap	DECIMAL(15,2)	公司市值
SQL 示例
CREATE TABLE stock
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    symbol VARCHAR(20) NOT NULL,

    company_name VARCHAR(100),

    sector VARCHAR(100),

    exchange VARCHAR(50),

    current_price DECIMAL(10,2),

    market_cap DECIMAL(15,2)
);
5.2.3 Holding Table（持仓关系表）
表名称
holding
设计目的

Portfolio 和 Stock 是多对多关系。

例如：

一个 Portfolio：

Technology Portfolio

包含：

Apple

Microsoft

Nvidia


同时：

Apple

也可以属于：

Portfolio A

Portfolio B

Portfolio C


因此需要中间表。

ER关系
Portfolio

    |

    |

 Holding

    |

    |

 Stock

Table Structure
字段	类型	说明
id	BIGINT	主键
portfolio_id	BIGINT	投资组合ID
stock_id	BIGINT	股票ID
quantity	INT	持仓数量
average_price	DECIMAL(10,2)	平均买入价格
current_value	DECIMAL(15,2)	当前价值
SQL
CREATE TABLE holding
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    portfolio_id BIGINT NOT NULL,

    stock_id BIGINT NOT NULL,

    quantity INT DEFAULT 0,

    average_price DECIMAL(10,2),

    current_value DECIMAL(15,2),


    FOREIGN KEY(portfolio_id)
    REFERENCES portfolio(id),


    FOREIGN KEY(stock_id)
    REFERENCES stock(id)
);
5.2.4 Transaction Table（交易记录表）
表名称
transaction_record
功能

记录：

买入
卖出

行为。

Table Structure
字段	类型	说明
id	BIGINT	主键
stock_id	BIGINT	股票ID
transaction_type	VARCHAR(10)	BUY/SELL
quantity	INT	交易数量
price	DECIMAL(10,2)	交易价格
transaction_date	DATE	交易日期
SQL
CREATE TABLE transaction_record
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    stock_id BIGINT,

    transaction_type VARCHAR(10),

    quantity INT,

    price DECIMAL(10,2),

    transaction_date DATE,


    FOREIGN KEY(stock_id)

    REFERENCES stock(id)
);
5.3 Entity Relationship Diagram（ER图）

整体关系：

                 Portfolio

                    |

                    |

                1:N

                    |

                    |

                Holding

                    |

                    |

                N:1

                    |

                    |

                  Stock

                    |

          ---------------------

          |                   |

          |                   |

       Sector             Exchange



Stock

 |

 |

1:N

 |

Transaction

关系说明
Portfolio - Holding

关系：

One Portfolio

has many Holdings

Stock - Holding

关系：

One Stock

can appear in many Portfolios

Stock - Transaction

关系：

One Stock

has many Transaction records

6. Neo4j Graph Database Design（图数据库设计）
6.1 为什么使用 Neo4j

传统关系数据库适合：

查询单个对象
表之间简单关联

但是对于：

股票

行业

交易所

投资组合


之间复杂关系：

SQL需要大量JOIN。

Neo4j通过图结构：

Node + Relationship

可以快速展示关系。

6.2 Neo4j 数据模型
Node Design

系统包含四类节点：

1. Portfolio Node

Label:

Portfolio

Properties:

{
"id":1,
"name":"Technology Portfolio",
"value":120000
}
2. Stock Node

Label:

Stock

Properties:

{
"symbol":"AAPL",
"name":"Apple Inc.",
"price":200
}
3. Sector Node

Label:

Sector

Properties:

{
"name":"Technology"
}
4. Exchange Node

Label:

Exchange

Properties:

{
"name":"NASDAQ"
}
6.3 Relationship Design
Portfolio 与 Stock

关系：

HAS_STOCK

例如：

Portfolio

    |

HAS_STOCK

    |

 Apple

Stock 与 Sector

关系：

BELONGS_TO

例如：

Apple

 |

BELONGS_TO

 |

Technology

Stock 与 Exchange

关系：

LISTED_ON

例如：

Apple

 |

LISTED_ON

 |

NASDAQ

6.4 完整 Neo4j Graph
                 Technology

                      |

                 BELONGS_TO

                      |

Portfolio ----HAS_STOCK---- Apple

                      |

                  LISTED_ON

                      |

                   NASDAQ


6.5 Neo4j 创建节点
创建股票
CREATE
(a:Stock{
symbol:'AAPL',
name:'Apple Inc',
price:200
})
创建行业
CREATE
(t:Sector{
name:'Technology'
})
创建关系
MATCH

(s:Stock),
(sec:Sector)

WHERE s.symbol='AAPL'

AND sec.name='Technology'


CREATE

(s)-[:BELONGS_TO]->(sec)
6.6 Neo4j 查询示例
查询 Portfolio 所有股票
MATCH

(p:Portfolio)

-[:HAS_STOCK]->

(s:Stock)

RETURN p,s
查询某股票所属行业
MATCH

(s:Stock)

-[:BELONGS_TO]->

(sec:Sector)


WHERE s.symbol='AAPL'


RETURN s,sec
查询 Technology 行业所有股票
MATCH

(s:Stock)

-[:BELONGS_TO]->

(sec:Sector)


WHERE sec.name='Technology'


RETURN s
7. REST API Design（接口设计）

系统采用 RESTful API。

统一前缀：

/api
7.1 Portfolio API
获取所有 Portfolio
GET

/api/portfolios

Response:

[
 {
  "id":1,
  "portfolioName":"Growth Portfolio",
  "totalValue":120000
 }
]
创建 Portfolio
POST

/api/portfolios

Request:

{
"name":"Technology Portfolio",
"description":"Long term investment",
"cashBalance":20000
}
删除 Portfolio
DELETE

/api/portfolios/{id}
7.2 Stock API
获取股票列表
GET

/api/stocks

Response:

[
{
"symbol":"AAPL",
"companyName":"Apple Inc",
"sector":"Technology"
}
]
添加股票
POST

/api/stocks

Request:

{
"symbol":"TSLA",
"companyName":"Tesla",
"sector":"Automotive",
"exchange":"NASDAQ"
}
7.3 Holding API
获取Portfolio持仓
GET

/api/holdings/{portfolioId}

Response:

[
{
"stock":"AAPL",
"quantity":20,
"value":4000
}
]
添加持仓
POST

/api/holdings

Request:

{
"portfolioId":1,
"stockId":2,
"quantity":50,
"averagePrice":150
}
7.4 Transaction API
获取交易记录
GET

/api/transactions
添加交易
POST

/api/transactions

Request:

{
"stockId":1,
"type":"BUY",
"quantity":20,
"price":180
}
7.5 Neo4j Graph API
获取关系图数据
GET

/api/graph/{portfolioId}

Response:

{

"nodes":[

{
"id":"Apple",
"type":"Stock"
},

{
"id":"Technology",
"type":"Sector"
}

],


"links":[

{
"source":"Portfolio",
"target":"Apple"
}

]

}

前端收到数据后：

使用：

react-force-graph
vis-network

进行可视化展示。
chnology",
"type":"Sector"
}

],


"links":[

{
"source":"Portfolio",
"target":"Apple"
}

]

}

前端收到数据后：

使用：

react-force-graph
vis-network

进行可视化展示。



8. Frontend Design（前端系统设计）
8.1 Frontend Architecture（前端架构）

前端采用组件化开发方式。

整体结构：

frontend

|

├── src

│

├── components

│       |

│       ├── Navbar

│       ├── Sidebar

│       ├── Charts

│       └── GraphViewer

│

├── pages

│       |

│       ├── Login

│       ├── Dashboard

│       ├── Portfolio

│       ├── Stock

│       └── Transaction

│

├── services

│       |

│       └── api.js

│

└── utils

8.2 Page Design（页面设计）

系统主要包含以下页面：

8.2.1 Login Page（登录页面）
功能

用户进入系统的入口。

页面包含：

Username
Password
Login Button

示意：

--------------------------------

        Portfolio Manager


        Username

        __________


        Password

        __________


          Login


--------------------------------
8.2.2 Dashboard Page（数据大屏）

Dashboard 是系统主要展示页面。

用户登录后默认进入 Dashboard。

目标：

让用户快速了解当前投资组合情况。

页面布局：

-------------------------------------------------

Portfolio Dashboard


-------------------------------------------------

Total Value       Cash        Holdings

$120,000          $20,000        15


-------------------------------------------------

       Portfolio Allocation


              Pie Chart


-------------------------------------------------


       Portfolio Performance


              Line Chart


-------------------------------------------------


       Sector Distribution


              Bar Chart


-------------------------------------------------

8.2.3 Portfolio Page

功能：

管理投资组合。

支持：

查看 Portfolio
创建 Portfolio
修改 Portfolio
删除 Portfolio

展示：

字段	说明
Portfolio Name	组合名称
Total Value	总价值
Cash Balance	现金
Created Date	创建时间
8.2.4 Stock Page

功能：

管理股票信息。

展示：

字段	说明
Symbol	股票代码
Company Name	公司名称
Sector	行业
Exchange	交易市场
Price	当前价格
8.2.5 Transaction Page

展示历史交易。

例如：

日期	股票	类型	数量	价格
2026-07-01	AAPL	BUY	10	180
2026-07-10	TSLA	SELL	5	220
8.2.6 Neo4j Graph Page

用于展示股票关系网络。

页面：

---------------------------------

      Stock Relationship Graph


          Technology


              |

              |

Portfolio ---- Apple ---- NASDAQ


              |

              |

          Microsoft


---------------------------------


功能：

节点拖拽
缩放
点击节点
查看详情
9. Dashboard Visualization Design（Dashboard数据可视化设计）
9.1 Visualization Goal

Dashboard 不只是展示数据，而是帮助用户：

理解资产结构
分析投资表现
发现风险集中区域
9.2 Summary Cards（核心指标卡）

展示：

Total Portfolio Value

数据来源：

Portfolio.total_value

示例：

Total Value

$120,000
Cash Balance

数据来源：

Portfolio.cash_balance

示例：

Cash

$20,000
Number of Holdings

数据来源：

Holding 表统计。

示例：

Holdings

15
Daily Performance

数据来源：

Transaction + Price Data。

示例：

Today's Gain

+3.2%

9.3 Portfolio Allocation Chart

类型：

Pie Chart

目的：

展示资产比例。

数据：

来自：

Holding.current_value

示例：

Apple        40%

Microsoft    25%

Tesla        15%

Cash         20%

9.4 Performance Trend Chart

类型：

Line Chart

目的：

展示投资组合变化趋势。

X轴：

时间

Y轴：

Portfolio Value

示例：


Value

120k |              *

110k |        *

100k |   *

     --------------------

       Mon Tue Wed Thu

9.5 Sector Distribution Chart

类型：

Bar Chart

目的：

分析行业集中度。

数据来源：

Stock.sector

示例：

Technology

Finance

Healthcare

Energy

10. Git Collaboration Strategy（Git协作规范）
10.1 Branch Strategy

采用 Feature Branch Workflow。

分支结构：

main

|

develop

|

├── feature/portfolio

├── feature/stock

├── feature/dashboard

└── feature/neo4j

10.2 Branch Responsibility
Branch	功能
main	稳定版本
develop	开发集成
feature/portfolio	Portfolio模块
feature/stock	Stock模块
feature/dashboard	Dashboard
feature/neo4j	图数据库
10.3 Daily Development Workflow

每天开始开发：

git checkout main

git pull origin main

git checkout feature/xxx

git merge main


目的：

保持代码同步。

10.4 Commit Convention

采用 Conventional Commit。

新功能
feat: add portfolio dashboard

修复
fix: solve stock api issue

文档
docs: update database design

重构
refactor: optimize service layer

10.5 Pull Request Workflow

流程：

Developer

↓

Commit

↓

Push Branch

↓

Create Pull Request

↓

Code Review

↓

Merge

↓

Delete Branch

11. Four-Day Development Plan（四天开发计划）

由于项目周期只有四天，因此采用 MVP 开发策略。

优先完成核心功能，再开发创新功能。

Day 1：项目初始化与基础开发
Team Tasks

完成：

项目需求分析
系统架构设计
数据库设计
API设计
Git仓库配置
Backend Tasks

完成：

Spring Boot 初始化
MySQL连接
Entity设计
Repository创建
Frontend Tasks

完成：

React项目初始化
页面路由
Layout设计
Login页面
Dashboard框架
Deliverables

完成：

Project Skeleton

Database Design

API Specification

Frontend Structure

Day 2：核心功能开发
Backend

完成：

Portfolio CRUD
Stock CRUD
Holding CRUD
Transaction CRUD
Frontend

完成：

Portfolio页面
Stock页面
Transaction页面

加入：

Mock Data
Axios API调用
Integration

完成：

前后端第一次联调。

Day 3：创新功能开发
Neo4j

完成：

Node设计
Relationship设计
Cypher查询
Visualization

完成：

Graph API
Graph页面
节点展示
Dashboard优化

增加：

ECharts
数据统计
页面美化
Day 4：测试与展示准备

重点：

不增加大型新功能。

完成：

Bug修复
API测试
UI优化
README
Swagger
Presentation
12. Testing Plan（测试计划）
12.1 Backend Testing

测试：

API测试

工具：

Postman
Swagger

测试：

GET
POST
PUT
DELETE
Database Testing

检查：

数据是否正确保存
外键关系
删除逻辑
12.2 Frontend Testing

测试：

页面跳转
数据加载
表单提交
图表展示
12.3 Neo4j Testing

测试：

Node创建
Relationship创建
Cypher查询
Graph展示
13. Deployment Plan（部署方案）
Development Environment

Frontend:

npm install

npm start


Backend:

mvn spring-boot:run


Database:

MySQL

Neo4j

Production Architecture（未来）

User

 |

Cloud Server

 |

Nginx

 |

-----------------

|               |

React        Spring Boot

                |

          --------------

          |            |

       MySQL        Neo4j


14. Future Improvements（未来扩展）
14.1 AI Portfolio Assistant

利用 AI：

提供：

投资总结
风险分析
Portfolio建议

例如：

用户输入：

Show my technology stocks


AI转换为查询：

MATCH (s:Stock)
WHERE s.sector='Technology'

RETURN s

14.2 Natural Language Query

支持：

What is my best performing stock?


系统自动：

自然语言

↓

AI解析

↓

数据库查询

↓

返回结果

14.3 AI Investment Recommendation

未来可以增加：

风险预测
资产调整建议
投资组合优化
14.4 Cloud Deployment

未来部署：

AWS
Google Cloud
Docker
Kubernetes
15. Conclusion（总结）

本项目通过：

Spring Boot REST API
React Web Frontend
MySQL Database
Neo4j Graph Database
Dashboard Visualization

构建一个完整的 Portfolio Management System。

系统不仅满足基础投资组合管理需求，同时通过：

数据可视化
图数据库分析
企业级 Git 工作流程

提升系统的扩展性和展示价值。

在未来可以进一步扩展：

AI智能分析
自然语言查询
云端部署

最终目标是构建一个接近真实企业环境的金融数据管理平台。
