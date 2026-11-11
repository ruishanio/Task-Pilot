# Task Pilot 前端

基于 Vite 8、Vue 3、TypeScript、shadcn-vue 和 Tailwind CSS 4。使用 shadcn-vue 2.9.0 官方 CLI 默认的 **Nova（reka-nova）+ Neutral** 浅色主题、CSS 变量和 Geist 字体。字体随构建产物本地分发，不依赖 Google Fonts。

## 功能概览

- 工作台：任务、调度和执行器统计，调度趋势与结果分布，支持自定义时间范围。
- 任务管理：即时搜索、组合筛选、可选列、任务配置与详情，支持调度启停、手动执行和 GLUE 脚本编辑。
- 执行日志：按执行器、任务、状态和时间筛选，查看调度详情与实时执行日志。
- 系统管理：执行器注册、节点查看、用户角色和访问权限管理。
- 响应式界面：适配桌面和手机，长表单独立滚动，操作按钮保持可见。

文本搜索停顿 300 毫秒后自动生效，回车立即搜索；高级筛选确认后统一应用。任务表单校验覆盖所有页签，并定位首个无效字段。

## 本地开发

Node.js 24 LTS：

```bash
cd task-pilot-frontend
npm ci
VITE_DEV_PROXY_TARGET=http://localhost:8080 npm run dev
```

访问 `http://localhost:5173/web/`。开发服务器通过 Vite 将 `/api/manage` 代理到后端，默认目标为 `http://localhost:8080`。

## 检查与构建

```bash
npm run typecheck
npm test
npm run build
npm audit
```

`build` 先执行 Vue 严格类型检查，再生成 `dist/`。部署路径为 `/web/`，Docker 构建会将产物复制到管理端静态资源目录。Vue Router 使用 History 模式，管理端提供 `/web/*` 的 SPA 回退。

## 项目结构

- `src/pages`：工作台、任务、执行日志、执行器、用户和登录页。
- `public/brand`：侧栏、登录页和浏览器图标使用的 SVG 资源。
- `src/components/ui`：官方生成的 shadcn-vue 组件源码；其余组件负责业务布局与表单。
- `src/styles/index.css`：官方 Neutral 主题变量与 Tailwind 入口。
- `src/styles/shadcn.css`：从 shadcn-vue 2.9.0 复制的官方辅助样式，许可见 `THIRD_PARTY_NOTICES.md`。CLI 只在更新组件时使用，不纳入应用依赖树。
- `src/stores/auth.ts`：身份初始化、登录态与退出逻辑。
- `src/services`：类型化 API、Bearer Token 和表单参数封装。
- `src/composables`：分页请求防竞争、提交状态、日志串行轮询。
- `tests`：登录态竞争、回跳地址、接口参数、分页竞争和日志轮询回归测试。

新增官方组件：

```bash
npx shadcn-vue@2.9.0 add <组件名称>
```

CLI 读取 `components.json` 中的 Nova / Neutral 配置。更新后检查 CSS 入口；如 CLI 重新添加 `shadcn-vue/tailwind.css` 依赖，应同步对应版本的官方辅助样式与许可，保持本地 `./shadcn.css` 引用。

## 路由与接口

页面路由包括 `/dashboard`、`/task_info`、`/task_code`、`/task_log`、`/task_log/detail`、`/executor` 和 `/user`。执行器、用户页仅管理员可访问；执行器范围由后端鉴权。登录回跳保留查询参数；业务 401 和 HTTP 401 均会清除过期登录态。

任务配置通过任务接口保存，脚本编辑使用 `/task_code/save` 保存代码与版本说明。日志仅允许换行、加粗标记，其他 HTML 经 DOMPurify 清理；关闭滚动日志弹窗或离开页面会停止轮询。
