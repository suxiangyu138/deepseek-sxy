# 苏巷雨的DeepSeek（轻聊）

一个只做日常对话的轻量 DeepSeek 客户端：不启动本地引擎、不跑任务、不读写文件，打开就是聊天界面。

- 形态：单页 PWA（HTML + CSS + 原生 JS），无需构建
- 后端：只有 `api.deepseek.com`，一把 DeepSeek API Key 跑通全部功能
- 部署：静态文件 + Chrome「安装应用」，或直接本地打开

## 功能

| 能力 | 实现方式 |
|---|---|
| 对话 | Chat Completions 流式（SSE） |
| 深度思考 | `thinking: {type: enabled}`，`reasoning_content` 折叠展示 |
| 联网搜索 | 服务端工具 `deepseek_web_search`（思考模式下为深度思考搜索） |
| 阅读链接 | 服务端工具 `deepseek_read_url`，消息里的链接自动读 |
| 图片识别 | `deepseek-flash` 原生视觉，`image_url` 内联 base64，前端压缩到 1600px |
| 模型切换 | `deepseek-flash`（V4.1 Flash）/ `deepseek-v4-pro`（V4 Pro） |
| 时间感知 | 每轮 `latest_reminder` + system 提示中的精确时间 |
| 用量计费 | `stream_options.include_usage`，按 token 与高峰/空闲价计算 |
| 离线 | Service Worker 预缓存外壳，断网/停引擎也能打开 |
| 记忆 | 侧边栏模块；开启后每次对话注入长期记忆；聊天里说「记住…」「添加记忆…」或陈述偏好会自动归纳保存 |
| 个性化 | 一段模板文本，开启后每次对话注入系统提示，优先于默认风格 |
| 批量删除 | 会话列表「管理」进入多选，勾选后一次删除 |
| 双语 | 设置里切换中文 / English，界面、日期、提示词同步切换 |
| 外观 | 正文字号、代码字号可调，实时生效 |
| 代码 | 语法高亮、最大高度限制、独立滚动 |
| 渲染 | 表格、任务列表、嵌套列表、图片、KaTeX 公式 |
| 指标 | 每条回答的缓存命中率、输出速率；顶栏上下文占用 / 1M |
| 余额 | 顶栏显示当前 Key 余额，点击刷新 |
| 连接测试 | 设置里一键校验 Key、列出可用模型与余额 |
| 附件 | 拍照、相册、文件（PDF/Office 走 Files API 解析） |
| 编辑重发 | 用户消息可编辑，保存后截断后续内容并重新回答 |

## 计费

单价（元 / 百万 token，来自官方模型页）：

| 模型 | 输入（缓存命中） | 输入（未命中） | 输出 |
|---|---|---|---|
| deepseek-flash | 0.02 / 0.04 | 1 / 2 | 4 / 8 |
| deepseek-v4-pro | 0.15 / 0.30 | 4.5 / 9 | 13.5 / 27 |

斜杠前为空闲时段，斜杠后为高峰时段（北京时间工作日 9:00–12:00、14:00–18:00，不含法定节假日；节假日未在代码内判定）。

每条回答下方显示：输入 token（含缓存命中）、输出 token、思考 token、本次费用；抽屉底部显示本对话累计用量与费用。

## 本地运行

```bash
# 任意静态服务器即可
cd app && python3 -m http.server 8899
```

手机 Chrome 打开 `http://127.0.0.1:8899`，菜单里「安装应用 / 添加到主屏幕」。

`tools/sync.sh` 把 `app/` 同步到手机的可访问目录 `/sdcard/Download/DSH-Go/light`。

## 目录

```
app/           前端源码（index.html 全部逻辑内联）
tools/sync.sh  同步到手机目录
README.md      本文件
ARCHITECTURE.md 架构与接口契约
CHANGELOG.md   迭代记录
```

## 作者

- 邮箱：suxiangyu_dev@foxmail.com
- GitHub / Gitee：suxiangyu138

## 隐私

- API Key、对话、图片都只存在手机浏览器本地（localStorage），不上传第三方
- 请求只发往 `api.deepseek.com`
- 导出的任何内容都不包含 Key
