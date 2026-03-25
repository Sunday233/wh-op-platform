# wh-op-platform — 科捷仓内操作费用基线分析平台

精细化统计仓内操作费用的基线和影响因素，建立费用基线的相关算数应用。

**核心用途：**
- 估算新业务成本和报价
- 复盘旧业务运营效率
- 设定仓经理效率指标

**费用定义：** 操作费用 = 工时数 × 劳务单价（含固定劳务、长期劳务外包、临时劳务外包）

---

## 技术栈

| 层级 | 技术栈 | 版本/说明 |
|---|---|---|
| **前端** | Vue 3 + Vite | TypeScript, SFC |
| **UI 框架** | Ant Design Vue + Tailwind CSS | 4.x |
| **图表** | ECharts | vue-echarts 封装 |
| **后端** | Spring Boot 3 | Java 21, Maven |
| **ORM** | MyBatis-Plus | 适合复杂统计查询 |
| **数据分析服务** | Python FastAPI | 独立微服务 |
| **数据源** | MySQL 5.7 | 远端直连，只读 |
| **结果存储** | SQLite | Docker 内轻量存储预计算结果 |
| **部署** | Docker Compose | 单机部署 |

---

## 目录结构

```
wh-op-platform/
├── README.md                   # 项目说明
├── .gitignore                  # Git 忽略规则
├── .env.example                # 环境变量模板
├── docker-compose.yml          # Docker 编排（Phase 6 创建）
│
├── frontend/                   # Vue 3 前端（Nginx :80）
├── backend/                    # Spring Boot 3 后端（:8080）
├── analytics/                  # Python FastAPI 分析服务（:8000）
├── data/                       # SQLite 预计算结果（Git 忽略）
└── docs/                       # 文档
    ├── data_profiling_reports/ # 19 张表的数据 Profiling 报告
    ├── data_analysis_reports/  # 数据分析报告
    └── scripts/                # 分析脚本（已有 Python 脚本）
```

---

## 快速启动

### 环境要求

- [Docker](https://www.docker.com/) >= 20.10
- [Docker Compose](https://docs.docker.com/compose/) >= 2.0

### 配置

1. 复制环境变量模板：
   ```bash
   cp .env.example .env
   ```

2. 编辑 `.env` 文件，填写 MySQL 连接信息：
   ```bash
   MYSQL_USER=your_username
   MYSQL_PASSWORD=your_password
   ```

### 启动

```bash
docker compose up --build
```

### 访问地址

| 服务 | 地址 | 说明 |
|---|---|---|
| 前端 | http://localhost:80 | Vue 3 应用（Nginx） |
| 后端 API | http://localhost:8080 | Spring Boot RESTful API |
| 分析服务 | http://localhost:8000 | Python FastAPI |

> **注意**：各服务的详细配置请参考对应目录下的 `application.yml`（后端）和 `config.py`（分析服务）。
