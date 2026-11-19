package com.suxiangyu.deepseek_sxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.widget.Toast;

public class InstallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        int st = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = null;
            try { confirm = (Intent) i.getParcelableExtra(Intent.EXTRA_INTENT); } catch (Exception e) { }
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { c.startActivity(confirm); return; } catch (Exception e) { }
            }
            Toast.makeText(c, "无法打开安装确认", Toast.LENGTH_LONG).show();
            return;
        }
        if (st == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(c, "更新完成，请重新打开应用", Toast.LENGTH_LONG).show();
            return;
        }
        String msg = "";
        try { msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE); } catch (Exception e) { }
        Toast.makeText(c, "安装未完成（" + st + "）" + (msg == null ? "" : msg), Toast.LENGTH_LONG).show();
    }
}
