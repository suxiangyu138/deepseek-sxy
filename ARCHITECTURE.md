# 架构说明

## 1. 总体结构

```
┌──────────────────────────────────────────────┐
│  手机 Chrome（PWA，安装到桌面）                 │
│  ┌────────────────────────────────────────┐  │
│  │ index.html                             │  │
│  │  ├─ 视图层：会话列表 / 消息流 / 输入区     │  │
│  │  ├─ 渲染层：自写 Markdown → HTML          │  │
│  │  ├─ 状态层：S（localStorage 单一状态树）   │  │
│  │  ├─ 网络层：fetch + ReadableStream(SSE)   │  │
│  │  └─ 计费层：usage → 价格表 → 单条/累计费用  │  │
│  └────────────────────────────────────────┘  │
│  sw.js  预缓存外壳，离线启动                    │
│  manifest.json  名称/图标/standalone            │
└───────────────┬──────────────────────────────┘
                │ HTTPS（CORS 已开放，允许任意 Origin）
                ▼
       api.deepseek.com/chat/completions
        ├─ deepseek-flash / deepseek-v4-pro
        ├─ thinking: enabled/disabled
        └─ tools: deepseek_web_search / deepseek_read_url（服务端执行）
```

没有本地引擎、没有 Node 进程、没有 proot。

## 2. 请求契约

```json
POST /chat/completions
{
  "model": "deepseek-flash",
  "stream": true,
  "stream_options": { "include_usage": true },
  "thinking": { "type": "enabled" },
  "tools": [
    { "type": "deepseek_web_search" },
    { "type": "deepseek_read_url" }
  ],
  "messages": [
    { "role": "system", "content": "…你现在的时间是 …" },
    { "role": "latest_reminder", "content": "2026-11-18,星期三,中国,App,中文" },
    { "role": "user", "content": "今天几号？" },
    { "role": "assistant", "content": "…", "reasoning_content": "…" }
  ]
}
```

要点：

- 声明了服务端工具后，历史里的 `assistant` 消息必须回传 `reasoning_content`，否则 400
- `latest_reminder` 必须紧跟一条 `user` 消息
- 图片消息的 `content` 是块数组：`{type:text}` + `{type:image_url, image_url:{url:dataURL, detail:auto}}`
- 图片只支持 `deepseek-flash`，`deepseek-v4-pro` 无视觉能力

## 3. 流式响应处理

```
data: {"choices":[{"delta":{"reasoning_content":"…"}}]}
data: {"choices":[{"server_tool_event":{"type":"call_started","call":{…}}}]}
data: {"choices":[{"server_tool_event":{"type":"call_completed","call":{"sources":[…]}}}]}
data: {"choices":[{"delta":{"content":"…"}}]}
data: {"choices":[{"delta":{…},"finish_reason":"stop"}]}
data: {"choices":[],"usage":{…}}
data: [DONE]
```

前端逐行解析 `data:`，按 `delta.reasoning_content` / `delta.content` 增量渲染；工具调用事件驱动“正在搜索/阅读”的状态行并收集来源；最后的 `usage` 用于计费。

## 4. 计费

```
费用 = 命中token×命中价 + 未命中token×未命中价 + 输出token×输出价
```

价格按模型与时段（高峰/空闲）查表，单价单位为元/百万 token。高峰判定：北京时间周一至周五 9:00–12:00、14:00–18:00。单条费用存在消息对象上，对话累计由消息汇总。

## 5. 存储

```js
{
  settings: { apiKey, theme, think, search, model },
  convs: [{
    id, title, createdAt, updatedAt,
    msgs: [{ role, content, images?, reasoning?, usage?, cost?, sources?, tools?, ts }]
  }],
  current: "会话 id"
}
```

- 单一 localStorage 键 `qianliao.v2`，整体序列化
- 超配额时先丢弃历史图片再重试，避免写失败
- 图片前端压缩：最长边 1600px，JPEG 0.86

## 6. 渲染

自写 Markdown 子集：围栏代码块（带语言标签与复制）、行内代码、粗斜体、删除线、标题、有序/无序列表、引用、分割线、链接。先整体转义再按行分块，避免 XSS。

## 7. 离线

`sw.js` 预缓存 `./`、`index.html`、manifest、图标；导航请求网络优先、失败回落缓存，因此本地静态服务停掉后仍能打开外壳；`navigator.storage.persist()` 降低被回收概率。

## 8. 已知限制

- 法定节假日仍按工作日计算高峰价
- 没有多设备同步、没有推送
- 历史只在本机浏览器，清站点数据会丢
- 图片存 localStorage，数量多时会触发清理
