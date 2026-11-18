#!/bin/sh
# 把仓库里的 app/ 同步到手机可访问目录，并刷新离线备份
set -e
SRC="$(cd "$(dirname "$0")/.." && pwd)/app"
DST="/sdcard/Download/DSH-Go/light"
mkdir -p "$DST"
cp "$SRC/index.html" "$SRC/manifest.json" "$SRC/sw.js" "$SRC/icon-180.png" "$SRC/icon-192.png" "$SRC/icon-512.png" "$DST/"
cp "$SRC/index.html" "/sdcard/Download/DSH-Go/轻聊-离线版.html"
echo "synced: $SRC -> $DST"
