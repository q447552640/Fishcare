package cn.net.wangsu.fishcare.xmpp;


import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.json.JSONException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Map;

import cn.net.wangsu.fishcare.R;
import cn.net.wangsu.fishcare.receivers.XMPPReceiver;
import cn.net.wangsu.fishcare.util.JSONUtil;

public class FishcareBackService extends Service {

    LoginConfig loginconfig;
    XMPPTCPConnection connection;
    private boolean loginSuccess = false;
    public SimpleBinder sBinder;

    private Handler mHandler = new MyHandler();
    private NotificationManager nManager;

    private static Ringtone ringtone = null;

    @Override
    public void onCreate() {
        super.onCreate();
        sBinder = new SimpleBinder();
        nManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        XmppConnectionManager.getInstance().getConnection().removeSyncStanzaListener(stanzaListener);
        stopSelf();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String username="";
        String password="";
        if(intent!=null){
            username = intent.getStringExtra("username");
            password = intent.getStringExtra("password");
        }
        if(username==null || "".equals(username)){
            SharedPreferences settings = getSharedPreferences("fishcare", 0);
            username=settings.getString("username","");
            password=settings.getString("password","");
        }
        loginconfig = new LoginConfig();
        loginconfig.setXmppHost("192.168.2.139");
        loginconfig.setXmppPort(9090);
        loginconfig.setXmppServiceName("app.aqualexcel.cn");
        loginconfig.setUsername(username);
        loginconfig.setPassword(password);
        loginconfig.setNovisible(false);
        new MyThread().start();
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 在 Local Service 中我们直接继承 Binder 而不是 IBinder,因为 Binder 实现了 IBinder 接口，这样我们可以少做很多工作。
     *
     * @author newcj
     */
    public class SimpleBinder extends Binder {
        public FishcareBackService getService() {
            return FishcareBackService.this;
        }

        public boolean getLoginResult() {
            return loginSuccess;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sBinder;
    }

    public class MyThread extends Thread {
        public void run() {
            Message msg = new Message();
            msg.what = login(loginconfig);
            mHandler.sendMessage(msg);
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constant.LOGIN_SECCESS: // 登录成功
                    Toast.makeText(FishcareBackService.this, "登陆成功", Toast.LENGTH_SHORT).show();
                    // 系统消息连接服务
//                    Intent imSystemMsgService = new Intent(FishcareBackService.this,
//                            XMPPService.class);
//                    FishcareBackService.this.startService(imSystemMsgService);
//                    // 注册广播接收器
//                    IntentFilter filter = new IntentFilter();
//                    // 好友请求
//                    filter.addAction(Constant.ROSTER_SUBSCRIPTION);
//                    filter.addAction(Constant.NEW_MESSAGE_ACTION);
//                    filter.addAction(Constant.ACTION_SYS_MSG);
//                    filter.addAction(Constant.ACTION_RECONNECT_STATE);
                    break;
                case Constant.LOGIN_ERROR_ACCOUNT_PASS:// 账户或者密码错误
                    Toast.makeText(FishcareBackService.this, "账户或者密码错误",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Constant.SERVER_UNAVAILABLE:// 服务器连接失败
                    Toast.makeText(FishcareBackService.this, "服务器连接失败", Toast.LENGTH_SHORT)
                            .show();
                    break;
                case Constant.LOGIN_ERROR:// 未知异常
                    Toast.makeText(FishcareBackService.this, "未知异常", Toast.LENGTH_SHORT)
                            .show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private StanzaListener stanzaListener = new StanzaListener() {
        @Override
        public void processStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException {
            if (stanza instanceof org.jivesoftware.smack.packet.Message) {//表示接收到是消息包
                org.jivesoftware.smack.packet.Message message = (org.jivesoftware.smack.packet.Message) stanza;
                if (message.getType() == org.jivesoftware.smack.packet.Message.Type.normal || message.getType() == org.jivesoftware.smack.packet.Message.Type.chat) {//表示单聊
                    Notice notice = new Notice();
                    startAlarm();

                    notice.setTitle("系统消息");
                    notice.setNoticeType(Notice.SYS_MSG);
                    notice.setFrom(stanza.getFrom().toString());
                    notice.setContent(message.getBody());
                    notice.setNoticeTime(DateUtil.date2Str(Calendar.getInstance(),
                            Constant.MS_FORMART));
                    notice.setStatus(Notice.UNREAD);

                    Intent intent = new Intent();
                    intent.setAction(Constant.ACTION_SYS_MSG);
                    intent.putExtra("notice", notice);
                    sendBroadcast(intent);

                    String messageBodyStr = message.getBody();
                    Map<String, String> map = null;
                    if (!"".equals(messageBodyStr)) {
                        try {
                            map = JSONUtil.toMap(messageBodyStr);
                        } catch (JSONException e) {
                            map = null;
                        }
                    }

                    String ticker = "消息";
                    String title = "系统消息";
                    String contentText = message.getBody();
                    if (map != null) {
                        ticker = map.get("ticker");
                        title = map.get("title");
                        contentText = map.get("message");
                    }
                    showNotification(FishcareBackService.this, ticker, title, contentText);

                }
//                if (message.getType() == Message.Type.chat) {//表示单聊
//
//                }
//                if (message.getType() == Message.Type.groupchat) {//表示群聊
//
//                }
//                if (message.getType() == Message.Type.error) {//表示错误信息
//
//                }
            }

//            if (stanza instanceof Presence) {//表示接收到的是Presence包
//
//            }
//
//            if (stanza instanceof IQ) {//表示接收到的是IQ包
//
//            }
        }

    };


    // 登录
    private Integer login(LoginConfig loginConfig) {

        boolean userChanged = false;

        SharedPreferences settings = getSharedPreferences("fishcare", 0);

        String oldUserName=settings.getString("username","");

        if(oldUserName!=null || !"".equals(oldUserName)){
            if(!loginConfig.getUsername().equals(oldUserName)){
                userChanged=true;
            }
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("username", loginConfig.getUsername());
        editor.putString("password",loginConfig.getPassword());
        editor.commit();

        String username=settings.getString("username","");
        String password=settings.getString("password","");

        if (connection != null && connection.isConnected()) {
            if(userChanged){
                connection.disconnect();
            }else{
                return Constant.ALREADY_LOGIN;
            }

        }
        XmppConnectionManager.getInstance().init(loginConfig);
        try {
            connection = XmppConnectionManager.getInstance().getConnection();
            connection.addSyncStanzaListener(stanzaListener, new StanzaFilter() {
                @Override
                public boolean accept(Stanza stanza) {
                    return true;
                }
            });
            connection.login(username, password); // 登录
            loginConfig.setOnline(true);
            return Constant.LOGIN_SECCESS;
        } catch (Exception xee) {
            if (xee instanceof XMPPException.XMPPErrorException) {
                XMPPException.XMPPErrorException xe = (XMPPException.XMPPErrorException) xee;
                final XMPPError error = xe.getXMPPError();
                if (error != null) {
                    switch (error.getCondition()) {
                        case bad_request:
                        case forbidden:
                            return Constant.LOGIN_ERROR_ACCOUNT_PASS;
                        case service_unavailable:
                            return Constant.SERVER_UNAVAILABLE;
                        default:
                            return Constant.FAIL;
                    }
                } else {
                    return Constant.FAIL;
                }
            } else {
                return Constant.LOGIN_ERROR;
            }
        }
    }

    private void setRingtoneRepeat(Ringtone ringtone) {
        Class<Ringtone> clazz = Ringtone.class;
        try {
            Field field = clazz.getDeclaredField("mLocalPlayer");//返回一个 Field 对象，它反映此 Class 对象所表示的类或接口的指定公共成员字段（※这里要进源码查看属性字段）
            field.setAccessible(true);
            MediaPlayer target = (MediaPlayer) field.get(ringtone);//返回指定对象上此 Field 表示的字段的值
            target.setLooping(true);//设置循环
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public void startAlarm() {

        if (ringtone == null) {//mMediaPlayer == null) {
            Uri soundUri = getSystemDefultRingtoneUri();

            ringtone = RingtoneManager.getRingtone(this, soundUri);
            setRingtoneRepeat(ringtone);
        }
        if (!ringtone.isPlaying()) {
            ringtone.play();
        }
    }

    public static void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying())
            ringtone.stop();
    }

    private Uri getSystemDefultRingtoneUri() {
        return RingtoneManager.getActualDefaultRingtoneUri(this,
                RingtoneManager.TYPE_ALARM);
    }

    public void showNotification(Context context, String ticker, String title, String contentText) {

        long[] pattern = {0, 1000, 500, 1000, 500, 1000, 500, 1000, 500, 1000, 500};

        Intent intentClick = new Intent(this, XMPPReceiver.class);
        intentClick.setAction("notification_clicked");
        intentClick.putExtra(XMPPReceiver.TYPE, 1);
        PendingIntent pendingIntentClick = PendingIntent.getBroadcast(this, 0, intentClick, PendingIntent.FLAG_ONE_SHOT);

        Intent intentCancel = new Intent(this, XMPPReceiver.class);
        intentCancel.setAction("notification_cancelled");
        intentCancel.putExtra(XMPPReceiver.TYPE, 2);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, 0, intentCancel, PendingIntent.FLAG_ONE_SHOT);


//        Intent clickIntent = new Intent(context, XMPPReceiver.class); //点击通知之后要发送的广播
//        int id = (int) (System.currentTimeMillis() / 1000);
//        PendingIntent contentIntent = PendingIntent.getBroadcast(this.getApplicationContext(), id, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, null)
                /**设置通知左边的大图标**/
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                /**设置通知右边的小图标**/
                .setSmallIcon(R.mipmap.ic_launcher)
                /**通知首次出现在通知栏，带上升动画效果的**/
                .setTicker(ticker)
                /**设置通知的标题**/
                .setContentTitle(title)
                /**设置通知的内容**/
                .setContentText(contentText)
                /**通知产生的时间，会在通知信息里显示**/
                .setWhen(System.currentTimeMillis())
                /**设置该通知优先级**/
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                /**设置这个标志当用户单击面板就可以让通知将自动取消**/
                .setAutoCancel(true)
                /**设置他为一个正在进行的通知。他们通常是用来表示一个后台任务,用户积极参与(如播放音乐)或以某种方式正在等待,因此占用设备(如一个文件下载,同步操作,主动网络连接)**/
                .setOngoing(false)
                /**向通知添加声音、闪灯和振动效果的最简单、最一致的方式是使用当前的用户默认设置，使用defaults属性，可以组合：**/
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentIntent(pendingIntentClick)//PendingIntent.getActivity(context, 1, new Intent(context,SDK_WebApp.class), PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(pendingIntentCancel)
                .setVibrate(pattern)//震动
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();

        /**发起通知**/
        nManager.notify(0, notification);
    }

}
