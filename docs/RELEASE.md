# 发行规划

版本：v1.0
日期：2026-11-19
适用范围：轻言 Pico从自用走向对外发布的阶段

---

## 1. 目标与原则

目标：让任何人拿到一个安装包就能用，不需要账号、不需要配置服务器。

原则：

- 客户端自带资源，服务端只做静态分发
- 每个用户用自己的 DeepSeek API Key，不设共享密钥
- 不在服务端保存任何用户数据
- 同一份 APK，多个渠道同时提供，内容完全一致

---

## 2. 发布物

| 产物 | 说明 |
|---|---|
| APK | `qianliao-<version>.apk`，签名后的正式包 |
| 校验值 | `SHA256SUMS`，随 APK 一起发布 |
| 下载页 | `download/index.html`，含安装说明与隐私说明 |
| 版本信息 | `release.json`，供页面与应用内检查更新 |
| 变更说明 | 每个版本一段更新日志 |

命名规则：Release 附件与站点文件统一用 `Pico-X.Y.Z.apk`，站点上另存一份 `app-X.Y.Z.apk` 供下载页引用，`versionName` 必须与文件名一致。

---

## 3. 版本号规则

- `versionName`：语义化，主版本.次版本.修订号
- `versionCode`：严格递增即可。当前 APK 工程用简单计数（1.8.7 → 6），不强制套公式
- 每次发版必须递增，否则覆盖安装会失败

发布节奏：

- 修订号：只修 bug，不改数据结构
- 次版本：加功能
- 主版本：改数据格式或需要迁移

---

## 4. 渠道一：自有云服务器

### 4.1 目录

```
/var/www/html/
├─ app/                 网页版，iOS 添加到主屏幕用
│  ├─ index.html
│  ├─ sw.js
│  ├─ manifest.json
│  └─ vendor/
└─ download/
   ├─ index.html
   ├─ privacy.html
   ├─ release.json
   ├─ icon.png
   ├─ app-1.8.7.apk    当前版本，release.json 指向它
   ├─ app-latest.apk   固定入口，可选保留
   └─ archive/
```

### 4.2 Nginx 关键配置

```nginx
server {
    listen 443 ssl http2;
    server_name dl.example.com;
    root /var/www/dl;

    location / {
        try_files $uri $uri/ =404;
    }

    location ~ \.apk$ {
        default_type application/vnd.android.package-archive;
        add_header Content-Disposition "attachment";
        add_header Cache-Control "public, max-age=86400";
    }

    location = /app/index.html { add_header Cache-Control "no-store" always; }
    location = /app/sw.js { types { } default_type text/javascript;
                            add_header Cache-Control "no-store" always; }

    gzip on;
    gzip_types text/html text/css application/json;
}
```

要点：

- APK 的 MIME 必须是 `application/vnd.android.package-archive`
- APK 用版本化文件名，可以放心设长缓存，因为换版本就是换路径
- `index.html`、`sw.js` 必须 no-store，避免 CDN 缓存旧页面
- 需要 CSP 时，至少放行 `connect-src 'self' https://api.deepseek.com data: blob:`，并允许内联脚本与样式

### 4.3 更新流程

1. 上传新 APK 为 `app-X.Y.Z.apk`，同时更新 `app-latest.apk`
2. 修改 `release.json`：version、url 指向 `app-X.Y.Z.apk`、size、updated
3. 覆盖 `/app/` 与 `/download/` 下的页面文件
4. 旧包移入 `archive/`

---

## 5. 渠道二：GitHub Release

步骤：

1. 仓库推送到 GitHub，公开
2. 打 tag：`git tag v1.7.0 && git push origin v1.7.0`
3. 在 Release 页面新建版本，标题写版本号
4. 上传 `qianliao-1.7.0.apk` 与 `SHA256SUMS`
5. 说明里写清更新内容、最低 Android 版本、安装方式
6. README 顶部放下载入口链接到 Releases

注意：

- APK 只作为 Release 附件，不提交进 git 历史
- Release 附件单文件上限 2GB，够用
- 需要公开仓库，私有仓库的 Release 无法匿名下载

---

## 6. 渠道三：Gitee Release

前提：仓库必须是公开的。当前仓库是 private，需要：

1. 新建一个公开仓库专门放代码与文档，或把现仓库改为公开
2. 在「发行版」页面新建 Release，上传 APK
3. Gitee 单附件上限 100MB，足够

说明：代码公开与不公开是两件事。如果不想公开源码，可以只建一个空的公开仓库，把 APK 放在 Release 附件里，源码仍留在私有仓库。

---

## 7. 发布流程清单

发布前：

- [ ] `versionCode` / `versionName` 已递增
- [ ] `npx cap sync` 已执行
- [ ] 真机安装、覆盖安装各一次
- [ ] 关键路径回归：对话、附件、公式、表格、记忆
- [ ] APK 已签名，`apksigner verify` 通过
- [ ] 计算 SHA-256

发布时：

- [ ] 上传 APK 到云服务器，更新 `release.json`
- [ ] 创建 GitHub Release，上传 APK 与校验值
- [ ] 创建 Gitee Release
- [ ] 下载页显示新版本号
- [ ] 通知渠道里发一条更新说明

发布后：

- [ ] 用自己的手机从下载页完整装一次
- [ ] 检查三个渠道的下载链接都可用
- [ ] 记录发布时间与版本

---

## 8. 更新与回滚

更新：客户端每次启动可选读取 `release.json`，比较版本号，提示去下载页。

回滚：

- 保留最近两个版本在 `archive/`
- 出问题把 `app-latest.apk` 换回旧包，`release.json` 版本号回退
- 已安装的用户不会被强制降级，需要手动重装

---

## 9. 小云服务器能不能跑

结论：能，而且很轻松。

这个站是纯静态文件分发，没有后端、没有数据库、没有定时任务。2GB 内存的入门服务器跑 Nginx 只需要几十 MB，CPU 在下载时几乎为空闲，真正的瓶颈是带宽和流量包。

参考数据：

- 一个 APK 约 8MB
- 100 次下载约 800MB 流量
- 1000 次下载约 8GB 流量
- 1Mbps 带宽下一个用户下载约 60 秒，10 个并发会明显排队

建议：

- 服务器带宽 ≥ 3Mbps 或使用按量带宽
- 前面套 Cloudflare，静态文件可缓存，回源压力大幅下降
- 流量包小的服务器，把大文件放对象存储（OSS/COS）或 GitHub Release，页面只做跳转

需要额外占用的资源：Nginx 常驻 10—30MB 内存，日志按天切割，磁盘占用主要是 APK 的历史版本，每个几 MB。

---

## 10. 成本与维护

| 项目 | 成本 |
|---|---|
| 服务器 | 已有 |
| 域名 | 已有 |
| HTTPS | Let's Encrypt 或 Cloudflare 免费 |
| GitHub / Gitee | 免费 |
| 签名证书 | 自签名免费 |
| 用户 Token | 用户自己承担 |

维护工作：每次发版上传一次 APK、改一次版本号、发两个 Release，约 15 分钟。

---

## 11. 风险

| 风险 | 应对 |
|---|---|
| 用户不会装未知来源应用 | 下载页分机型写清步骤，配截图 |
| 下载慢或失败 | 三个渠道互为备份，Cloudflare 缓存 |
| 签名密钥丢失 | 离线双备份，绝不提交仓库 |
| 名称商标风险 | 对外使用「轻聊」，不直接使用 DeepSeek 商标 |
| 服务器被刷流量 | 限速、Cloudflare 缓存、必要时换成对象存储 |
| 用户数据丢失 | 文档中明确「仅存本机」，后续提供导出功能 |
