# task-pilot-frontend

基于 `Vite + React + Ant Design` 的独立前端项目，用来承接 `task-pilot-admin` 的页面重构，并通过接口与后端解耦。

## 开发

```bash
cd task-pilot-frontend
npm install
npm run dev
```

开发环境默认不会直连后端，而是通过 Vite 代理把 `/api`、`/auth`、`/jobinfo`、`/jobgroup`、`/joblog`、`/user`、`/chartInfo` 转发到 `http://localhost:8080`。

如需修改后端地址，可设置环境变量：

```bash
VITE_DEV_PROXY_TARGET=http://localhost:8080
```

## 构建

```bash
npm run build
```

构建产物默认按静态站点方式输出，资源路径为相对路径，适合手动拷贝回：

```bash
task-pilot-admin/src/main/resources/static
```

由于生产环境是静态文件回挂到后端访问，前端路由使用 `BrowserRouter`，并统一挂在 `/web/*` 下，访问形态类似：

```text
/web/dashboard
```

如果你把构建产物放到子目录，例如 `static/task-pilot-frontend/`，也可以直接访问：

```text
/static/task-pilot-frontend/index.html#/dashboard
```
