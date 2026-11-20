#!/usr/bin/env bash
# 轻言 Pico：一次配好 Gitee / GitHub 远端并推送 main
# 用法：GH_TOKEN=xxx GITEE_TOKEN=yyy bash scripts/remote-init.sh
# 仓库已存在时会跳过创建，直接同步远端并推送
set -euo pipefail

OWNER="${OWNER:-suxiangyu138}"
REPO="${REPO:-qingyan-pico}"
DESC="轻言 Pico — 2 MB 的 DeepSeek 轻客户端。自带 API Key，数据只存本机，安卓/iOS/鸿蒙/电脑通用，可整包导入官方导出的历史对话。"
BRANCH="${BRANCH:-main}"

GH_TOKEN="${GH_TOKEN:-}"
GITEE_TOKEN="${GITEE_TOKEN:-}"

if [ -z "$GH_TOKEN" ] || [ -z "$GITEE_TOKEN" ]; then
  cat <<MSG
缺少令牌。这样运行：
  GH_TOKEN=<GitHub 令牌，需要 repo 权限> \\
  GITEE_TOKEN=<Gitee 私人令牌，勾选 projects> \\
  bash scripts/remote-init.sh

可选：OWNER=别的用户名 REPO=别的仓库名 BRANCH=别的分支
MSG
  exit 1
fi

api() { python3 "$@"; }

echo "==> 检查并创建 GitHub 仓库 $OWNER/$REPO"
API_OK=$(api - "$GH_TOKEN" "$OWNER" "$REPO" "$DESC" <<'PY'
import json,sys,urllib.request,urllib.error
tok,owner,repo,desc=sys.argv[1:5]
base=f'https://api.github.com/repos/{owner}/{repo}'
hdr={'Authorization':'Bearer '+tok,'Accept':'application/vnd.github+json','User-Agent':'pico-init'}
req=urllib.request.Request(base,headers=hdr)
try:
    urllib.request.urlopen(req,timeout=20); print('exists'); sys.exit(0)
except urllib.error.HTTPError as e:
    if e.code!=404: print('error',e.code); sys.exit(2)
body=json.dumps({'name':repo,'description':desc,'private':False,'has_issues':True}).encode()
req=urllib.request.Request('https://api.github.com/user/repos',data=body,method='POST',
    headers={**hdr,'Content-Type':'application/json'})
try:
    urllib.request.urlopen(req,timeout=30); print('created')
except urllib.error.HTTPError as e:
    print('error',e.code,e.read().decode()[:200]); sys.exit(2)
PY
) || { echo "GitHub 处理失败：$API_OK"; exit 2; }
echo "    GitHub: $API_OK"

echo "==> 检查并创建 Gitee 仓库 $OWNER/$REPO"
API_OK=$(api - "$GITEE_TOKEN" "$OWNER" "$REPO" "$DESC" <<'PY'
import json,sys,urllib.parse,urllib.request,urllib.error
tok,owner,repo,desc=sys.argv[1:5]
base=f'https://gitee.com/api/v5/repos/{owner}/{repo}'
try:
    urllib.request.urlopen(f'{base}?access_token={tok}',timeout=20); print('exists'); sys.exit(0)
except urllib.error.HTTPError as e:
    if e.code!=404: print('error',e.code); sys.exit(2)
data=urllib.parse.urlencode({'access_token':tok,'name':repo,'description':desc,'private':'false','has_issues':'true'}).encode()
req=urllib.request.Request('https://gitee.com/api/v5/user/repos',data=data,method='POST')
try:
    urllib.request.urlopen(req,timeout=30); print('created')
except urllib.error.HTTPError as e:
    print('error',e.code,e.read().decode()[:200]); sys.exit(2)
PY
) || { echo "Gitee 处理失败：$API_OK"; exit 2; }
echo "    Gitee: $API_OK"

echo "==> 配置远端"
git remote remove origin 2>/dev/null || true
git remote remove github 2>/dev/null || true
git remote add origin "https://gitee.com/$OWNER/$REPO.git"
git remote add github "https://github.com/$OWNER/$REPO.git"
git remote -v

echo "==> 推送 $BRANCH"
cat > /tmp/pico-askpass.sh <<'SH'
#!/bin/sh
case "$1" in
  *Username*) echo "suxiangyu138" ;;
  *) echo "$GIT_TOKEN" ;;
esac
SH
chmod +x /tmp/pico-askpass.sh
GIT_TOKEN="$GITEE_TOKEN" GIT_ASKPASS=/tmp/pico-askpass.sh GIT_TERMINAL_PROMPT=0 git push -u origin "$BRANCH"
GIT_TOKEN="$GH_TOKEN"   GIT_ASKPASS=/tmp/pico-askpass.sh GIT_TERMINAL_PROMPT=0 git push -u github "$BRANCH"
rm -f /tmp/pico-askpass.sh

echo "==> 完成"
echo "    Gitee : https://gitee.com/$OWNER/$REPO"
echo "    GitHub: https://github.com/$OWNER/$REPO"
