package cn.net.wangsu.fishcare;

import android.app.Application;
import android.content.Context;

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.UEH;

/**
 * Created by lorytech on 2018/1/14.
 */

public class HApplication extends Application {
    private static final String a = HApplication.class.getSimpleName();
    public boolean isQihooTrafficFreeValidate = true;
    private static HApplication b;
    private static Context c = null;

    public HApplication() {
    }

    public static Context getInstance() {
        return c;
    }

    public static void setInstance(Context var0) {
        if(c == null) {
            c = var0;
        }

    }

    public static HApplication self() {
        return b;
    }

    public void onCreate() {
        super.onCreate();
        b = this;
        setInstance(this.getApplicationContext());
        UEH.catchUncaughtException(this.getApplicationContext());
    }

    public void onLowMemory() {
        super.onLowMemory();
        Logger.e(a, "onLowMemory" + Runtime.getRuntime().maxMemory());
    }

    public void onTrimMemory(int var1) {
        super.onTrimMemory(var1);
        Logger.e(a, "onTrimMemory");
    }
}
