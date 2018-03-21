package cn.net.wangsu.fishcare.birdges;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import org.json.JSONArray;

import cn.net.wangsu.fishcare.xmpp.FishcareBackService;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;

public class LoginXMPP extends StandardFeature {

    private static boolean hasBind=false;
    private ServiceConnection sc=null;

    public  void login(IWebview pWebview, JSONArray array) throws Exception {
        if(sc==null){
            sc= new ServiceConnection() {

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    // TODO Auto-generated method stub

                }
            };
        }
        String username = array.optString(0);
        String password =array.optString(1);
        Context context=pWebview.getContext();
        context.startService(new Intent(context,FishcareBackService.class).putExtra("username", username).putExtra("password",password));
        if(!hasBind){
            context. bindService(new Intent(context,FishcareBackService.class), sc, Context.BIND_AUTO_CREATE);
            hasBind = true;
        }


    }

}