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
        if (st == PackageInstaller.STATUS_SUCCESS) {
            Toast.makeText(c, "更新完成，请重新打开应用", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(c, "安装未完成", Toast.LENGTH_SHORT).show();
        }
    }
}
