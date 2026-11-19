package com.suxiangyu.deepseek_sxy;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
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
    private ValueCallback<Uri[]> fileCallback;
    private static final int REQ_FILE = 1001;
    private static final int REQ_SAVE = 2002;

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
