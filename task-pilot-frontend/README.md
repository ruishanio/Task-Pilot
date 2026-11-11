# task-pilot-frontend

基于 `Vite + React + Ant Design` 的独立前端项目，用来承接 `task-pilot-admin` 的页面重构，并通过接口与后端解耦。

## 开发

使用 Node.js 24 LTS，与 Docker 构建环境及 `@types/node` 保持一致。

```bash
cd task-pilot-frontend
npm ci
npm run dev
```

开发环境默认不会直连后端，而是通过 Vite 代理把 `/api/manage` 转发到 `http://localhost:8080`。

如需修改后端地址，可设置环境变量：

```bash
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

## 构建

```bash
npm run typecheck
npm run build
```

构建产物按静态站点方式输出，资源路径固定使用 `/web/` 前缀，可拷贝回：

```bash
task-pilot-admin/src/main/resources/static
```

由于生产环境是静态文件回挂到后端访问，前端路由使用 `BrowserRouter`，并统一挂在 `/web/*` 下，访问形态类似：

```text
/web/task_info
```

直接访问或刷新嵌套路由时，后端需要将 `/web/*` 页面请求转发到 SPA 入口；当前管理端已提供该转发。
