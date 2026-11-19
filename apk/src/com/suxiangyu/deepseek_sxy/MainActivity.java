package com.suxiangyu.deepseek_sxy;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Toast;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private byte[] pendingData;
    private static final String INSTALL_ACTION = "com.suxiangyu.deepseek_sxy.INSTALL_RESULT";
    private ValueCallback<Uri[]> fileCallback;
    private static final int REQ_FILE = 1001;
    private static final int REQ_SAVE = 2002;
    private static final int REQ_CAMERA = 1003;
    private Uri captureUri;
    private Uri pendingPhoto;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setBackgroundColor(Color.parseColor("#f5f5f7"));
        web.addJavascriptInterface(new Bridge(), "AndroidSaver");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                if (p.isCaptureEnabled()) {
                    try {
                        Uri out = newPhotoUri();
                        if (out != null) {
                            Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            cam.putExtra(MediaStore.EXTRA_OUTPUT, out);
                            cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            captureUri = out;
                            startActivityForResult(cam, REQ_CAMERA);
                            return true;
                        }
                    } catch (Exception e) {
                        captureUri = null;
                    }
                }
                try {
                    Intent i = p.createIntent();
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(i, REQ_FILE);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    public class Bridge {

        @JavascriptInterface
        public void saveText(String name, String mime, String content) {
            try {
                beginSave(name, mime, content.getBytes("UTF-8"));
            } catch (Exception e) {
                toast("保存失败");
            }
        }

        @JavascriptInterface
        public void saveBase64(String name, String mime, String b64) {
            try {
                beginSave(name, mime, Base64.decode(b64, Base64.DEFAULT));
            } catch (Exception e) {
                toast("保存失败");
            }
        }

        @JavascriptInterface
        public String fetchText(String url) {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                c.setConnectTimeout(8000);
                c.setReadTimeout(8000);
                c.setRequestProperty("User-Agent", "PicoUpdater");
                java.io.InputStream in = c.getInputStream();
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                in.close();
                return bo.toString("UTF-8");
            } catch (Exception e) {
                return "";
            }
        }

        @JavascriptInterface
        public void installApk(final String url) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        toast("开始下载更新");
                        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                        c.setConnectTimeout(15000);
                        c.setReadTimeout(120000);
                        c.setInstanceFollowRedirects(true);
                        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) PicoUpdater/2.1.6");
                        c.setRequestProperty("Referer", "https://gitee.com/");
                        java.io.InputStream in = c.getInputStream();
                        byte[] head = new byte[2];
                        int got = 0;
                        while (got < 2) {
                            int r = in.read(head, got, 2 - got);
                            if (r <= 0) break;
                            got += r;
                        }
                        if (got < 2 || head[0] != 'P' || head[1] != 'K') throw new Exception("not apk");
                        PackageInstaller pi = getPackageManager().getPackageInstaller();
                        PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                        int id = pi.createSession(p);
                        PackageInstaller.Session session = pi.openSession(id);
                        java.io.OutputStream os = session.openWrite("pico.apk", 0, -1);
                        byte[] buf = new byte[8192];
                        int n;
                        os.write(head, 0, 2);
                        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                        session.fsync(os);
                        os.close();
                        in.close();
                        Intent i = new Intent(INSTALL_ACTION).setPackage(getPackageName());
                        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                        if (Build.VERSION.SDK_INT >= 31) flags |= 0x02000000;
                        PendingIntent pending = PendingIntent.getBroadcast(MainActivity.this, id, i, flags);
                        session.commit(pending.getIntentSender());
                        runOnUiThread(new Runnable() {
                            public void run() { Toast.makeText(MainActivity.this, "已下载，请在系统弹窗确认安装", Toast.LENGTH_LONG).show(); }
                        });
                    } catch (Exception e) {
                        toast("更新失败，请到下载页手动安装");
                    }
                }
            }).start();
        }

        @JavascriptInterface
        public void dropLastPhoto() {
            dropPendingPhoto();
        }

        @JavascriptInterface
        public void openUrl(final String url) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        toast("打不开浏览器");
                    }
                }
            });
        }

        @JavascriptInterface
        public void printPage() {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        PrintManager pm = (PrintManager) getSystemService(PRINT_SERVICE);
                        PrintDocumentAdapter adapter = web.createPrintDocumentAdapter("DeepSeek_sxy");
                        pm.print("DeepSeek_sxy", adapter, new PrintAttributes.Builder().build());
                    } catch (Exception e) {
                        toast("打印不可用");
                    }
                }
            });
        }

        private void beginSave(final String name, final String mime, final byte[] data) {
            MainActivity.this.pendingData = data;
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType(mime);
                        i.putExtra(Intent.EXTRA_TITLE, name);
                        startActivityForResult(i, REQ_SAVE);
                    } catch (Exception e) {
                        toast("无法打开保存面板");
                    }
                }
            });
        }

        private void toast(final String msg) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_SAVE) {
            if (res == RESULT_OK && data != null && data.getData() != null) {
                try {
                    java.io.OutputStream os = getContentResolver().openOutputStream(data.getData());
                    os.write(pendingData);
                    os.close();
                    Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            }
            pendingData = null;
            return;
        }
        if (req == REQ_CAMERA) {
            if (fileCallback != null) {
                Uri out = null;
                if (res == RESULT_OK && captureUri != null) {
                    out = captureUri;
                    pendingPhoto = captureUri;
                }
                fileCallback.onReceiveValue(out == null ? null : new Uri[]{out});
                fileCallback = null;
            }
            captureUri = null;
            return;
        }
        if (req == REQ_FILE) {
            if (fileCallback != null) {
                Uri[] out = null;
                if (res == RESULT_OK && data != null) {
                    if (data.getClipData() != null) {
                        int n = data.getClipData().getItemCount();
                        out = new Uri[n];
                        for (int i = 0; i < n; i++) out[i] = data.getClipData().getItemAt(i).getUri();
                    } else if (data.getData() != null) {
                        out = new Uri[]{data.getData()};
                    }
                }
                fileCallback.onReceiveValue(out);
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(req, res, data);
    }


    private Uri newPhotoUri() {
        String name = "Pico_" + new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(new java.util.Date()) + ".jpg";
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= 29) {
            cv.put("relative_path", Environment.DIRECTORY_PICTURES + "/Pico");
        }
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
    }

    private void dropPendingPhoto() {
        final Uri u = pendingPhoto;
        pendingPhoto = null;
        if (u == null) return;
        new Thread(new Runnable() {
            public void run() {
                try { getContentResolver().delete(u, null, null); } catch (Exception e) { }
            }
        }).start();
    }

    private void doBack() {
        if (web == null) { finish(); return; }
        web.evaluateJavascript(
            "(function(){try{return (window.__handleBack && window.__handleBack()) ? '1' : '0'}catch(e){return '0'}})()",
            new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    if (value == null || !value.contains("1")) finish();
                }
            }
        );
    }

    @Override
    public boolean onKeyDown(int k, KeyEvent e) {
        if (k == KeyEvent.KEYCODE_BACK) { doBack(); return true; }
        return super.onKeyDown(k, e);
    }

    @Override
    public void onBackPressed() { doBack(); }
}
