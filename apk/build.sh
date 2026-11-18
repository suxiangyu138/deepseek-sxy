#!/bin/sh
# 在 ARM64 Ubuntu 上构建 APK：aapt + javac + d8 + zipalign + apksigner
set -e
ROOT="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$ROOT/.." && pwd)"
AJ="${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}"
R8="${R8_JAR:-$ROOT/r8.jar}"
KS="${KEYSTORE:-$ROOT/release.jks}"
OUT="$ROOT/build"
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/dex"

aapt package -f -M "$ROOT/AndroidManifest.xml" -S "$ROOT/res" -A "$REPO/app" -I "$AJ" -F "$OUT/base.apk"
javac -source 8 -target 8 -bootclasspath "$AJ" -d "$OUT/classes" "$ROOT"/src/com/suxiangyu/deepseek_sxy/*.java
java -cp "$R8" com.android.tools.r8.D8 --min-api 24 --lib "$AJ" --output "$OUT/dex" "$OUT"/classes/com/suxiangyu/deepseek_sxy/*.class
python3 - "$OUT" <<'PY'
import sys,zipfile,shutil,os
out=sys.argv[1]
shutil.copy(os.path.join(out,'base.apk'),os.path.join(out,'withdex.apk'))
with zipfile.ZipFile(os.path.join(out,'withdex.apk'),'a',zipfile.ZIP_DEFLATED) as z:
    z.write(os.path.join(out,'dex','classes.dex'),'classes.dex')
PY
zipalign -f 4 "$OUT/withdex.apk" "$OUT/aligned.apk"
apksigner sign --ks "$KS" --out "$OUT/DeepSeek_sxy.apk" "$OUT/aligned.apk"
apksigner verify "$OUT/DeepSeek_sxy.apk"
echo "built: $OUT/DeepSeek_sxy.apk"
