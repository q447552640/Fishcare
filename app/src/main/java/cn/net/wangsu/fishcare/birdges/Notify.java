package cn.net.wangsu.fishcare.birdges;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;

import org.json.JSONArray;

import java.io.File;

import cn.net.wangsu.fishcare.R;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;

public class Notify extends StandardFeature {

    NotificationManager manager=null;
    Notification.Builder builder=null;
    Activity activity=null;

    public  void setProgress(IWebview pWebview, JSONArray array) throws Exception {
        int incr = Integer.parseInt(array.optString(0));
        builder.setProgress(100, incr, false);
        manager.notify(1000, builder.build());
    }

    public void setNotification(IWebview pWebview, JSONArray array){
        if(activity==null){
            activity=pWebview.getActivity();
            manager = (NotificationManager)activity.getSystemService(Activity.NOTIFICATION_SERVICE);
            builder = new Notification.Builder(activity);
            builder.setWhen(System.currentTimeMillis())
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentTitle("新版下载")
                    .setTicker("开始下载")
                    .setSmallIcon(R.drawable.icon)
                    .setVibrate(null);
        }
        String title = array.optString(0);
        String ticker =array.optString(1);
        builder.setContentTitle(title).setTicker(ticker);
        manager.notify(1000, builder.build());
    }
    public void compProgressNotification(IWebview pWebview, JSONArray array){
        String title =array.optString(0);
        builder.setContentTitle(title).setProgress(0, 0, false);
        manager.notify(1000, builder.build());
    }

    public void installApk(IWebview pWebview, JSONArray array) {
        File file = new File(array.optString(0));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri data;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//判断版本大于等于7.0
            // 通过FileProvider创建一个content类型的Uri
            data = FileProvider.getUriForFile(pWebview.getContext(), "cn.net.wangsu.fishcare.fileprovider", file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);// 给目标应用一个临时授权
        } else {
            data = Uri.fromFile(file);
        }
        intent.setDataAndType(data, "application/vnd.android.package-archive");
        pWebview.getContext().startActivity(intent);
    }

}