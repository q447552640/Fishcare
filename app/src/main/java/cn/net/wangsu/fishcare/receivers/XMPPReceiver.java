package cn.net.wangsu.fishcare.receivers;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.HBuilder.integrate.SDK_WebApp;

import cn.net.wangsu.fishcare.xmpp.XMPPService;

/**
 * Created by lorytech on 2018/3/20.
 */

public class XMPPReceiver extends BroadcastReceiver {

    public static final String TYPE = "XMPPNotice"; //这个type是为了Notification更新信息的，这个不明白的朋友可以去搜搜，很多
    @Override
    public void onReceive(Context context, Intent intent) {
        XMPPService.stopAlarm();

        String action = intent.getAction();
        int type = intent.getIntExtra(TYPE, -1);

        if (type != -1) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(type);
        }

        if (action.equals("notification_clicked")) {
            Intent newIntent = new Intent(context, SDK_WebApp.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);
        }

        if (action.equals("notification_cancelled")) {
            //处理滑动清除和点击删除事件
        }


    }
}
