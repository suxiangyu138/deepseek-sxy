# 变更记录

## v3 — 2026-11-18 21:52

- 换成用户提供的头像作为应用图标（512/192/180），名称改为「苏巷雨的DeepSeek」
- 新增 Token 用量与费用：每条回答显示输入/缓存命中/输出/思考 token 与本次费用，抽屉底部显示本对话累计
- 请求加 `stream_options.include_usage`，按模型与高峰/空闲时段计价
- 界面重做：新配色与圆角、消息动效、卡片式来源、胶囊按钮、聚焦高亮
- 空态个性化：按时段问候「晚上好，苏巷雨」+ 三条建议问题
- 顶栏滚动分割线、输入框聚焦态、版本号 v3

## v2 — 2026-11-18 21:45

- 按 DeepSeek 全家桶接口重写：服务端 `deepseek_web_search` / `deepseek_read_url`，去掉第三方搜索 Key
- 模型切换：`deepseek-flash`（V4.1 Flash）/ `deepseek-v4-pro`（V4 Pro）
- 深度思考改用 `thinking` 参数，思考过程折叠 + 耗时
- 图片识别：选图、压缩、`image_url` 内联，选 Pro 时自动切回 Flash
- 时间感知：`latest_reminder` + system 提示精确时间
- 离线加固：SW 导航回落缓存、持久化存储申请

## v1 — 2026-11-18 21:36

- 首个可用版本：流式对话、新建/历史会话、Markdown 与代码高亮、复制/停止/重新生成
- 单文件 PWA + 图标 + manifest，可安装到桌面
