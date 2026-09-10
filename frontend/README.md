# 基于大模型场景扮演的英语口语训练系统 — 前端

面向全年龄段英语学习者的 AI 口语训练平台前端，覆盖「水平测评 → 个性化方案 → 场景对话练习 → 实时评测反馈」一站式学习闭环。

## 技术栈

- Vue 3（`<script setup>` + TypeScript）
- Element Plus（组件库）
- Vite 5 + Pinia + Vue Router 4
- ECharts（成长曲线 / 能力雷达图）

## 快速开始

```bash
npm install
npm run dev       # 启动开发服务器（默认 http://localhost:5173）
npm run build     # 生产构建
npm run type-check # 类型检查
```

## 目录结构

```
src/
├── api/           # 接口层（后端不在本次范围，仅预留）
├── router/        # 路由与守卫
├── stores/        # Pinia 状态管理
├── styles/        # 设计令牌 + 全局样式
├── layouts/       # 学习者端 / 管理员端布局
├── components/    # 基础组件 + 业务组件
└── views/         # 页面
```

详细设计见 [`docs/frontend-design.md`](docs/frontend-design.md)。
