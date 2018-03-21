package cn.net.wangsu.fishcare.xmpp;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Calendar;
import java.util.Map;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.json.JSONException;


import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import cn.net.wangsu.fishcare.R;
import cn.net.wangsu.fishcare.receivers.XMPPReceiver;
import cn.net.wangsu.fishcare.util.JSONUtil;

public class XMPPService extends Service {
    //    private Context context;
    private NotificationManager nManager;

    //    NotificationChannel channel =null;//TODO
    @Override
    public void onCreate() {
//        context = this;
        super.onCreate();
        initChannel();
        initSysTemMsgManager();
        nManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

    }

    @TargetApi(26)
    private void initChannel() {
        //channel = new NotificationChannel("1", "报警", NotificationManager.IMPORTANCE_HIGH);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        XmppConnectionManager.getInstance().getConnection().removeAsyncStanzaListener(stanzaListener);
        super.onDestroy();
    }

    private void initSysTemMsgManager() {
        //initSoundPool();
        XMPPConnection con = XmppConnectionManager.getInstance().getConnection();
        con.addSyncStanzaListener(stanzaListener, new StanzaFilter() {
            @Override
            public boolean accept(Stanza stanza) {
                return true;
            }
        });
    }

    private StanzaListener stanzaListener = new StanzaListener() {
        @Override
        public void processStanza(Stanza stanza) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException {
            if (stanza instanceof Message) {//表示接收到是消息包
                Message message = (Message) stanza;
                if (message.getType() == Type.normal) {//表示单聊
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

                    String messageBodyStr=message.getBody();
                    Map<String, String> map=null;
                    if(!"".equals(messageBodyStr)){
                        try {
                            map= JSONUtil.toMap(messageBodyStr);
                        } catch (JSONException e) {
                            map=null;
                        }
                    }

                    String ticker="消息";
                    String title="系统消息";
                    String contentText=message.getBody();
                    if(map!=null){
                        ticker=map.get("ticker");
                        title=map.get("title");
                        contentText=map.get("message");
                    }
                    showNotification(XMPPService.this, ticker, title,contentText);

                }
                if (message.getType() == Message.Type.chat) {//表示单聊

                }
                if (message.getType() == Message.Type.groupchat) {//表示群聊

                }
                if (message.getType() == Message.Type.error) {//表示错误信息

                }
            }

            if (stanza instanceof Presence) {//表示接收到的是Presence包

            }

            if (stanza instanceof IQ) {//表示接收到的是IQ包

            }
        }

    };

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

    private static MediaPlayer mMediaPlayer = null;
    private static Ringtone ringtone=null;

    private void setRingtoneRepeat(Ringtone ringtone) {
        Class<Ringtone> clazz =Ringtone.class;
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

        if (ringtone==null ){//mMediaPlayer == null) {
            Uri soundUri=getSystemDefultRingtoneUri();

            ringtone= RingtoneManager.getRingtone(this,soundUri);
            setRingtoneRepeat(ringtone);
//            grantUriPermission("com.android.systemui", soundUri,
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            mMediaPlayer = MediaPlayer.create(this, soundUri);
        }
        //mMediaPlayer.reset();
//        mMediaPlayer.setLooping(true);
//        try {
//            mMediaPlayer.prepare();
//        } catch (IllegalStateException e) {
//           // e.printStackTrace();
//        } catch (IOException e) {
//           // e.printStackTrace();
//        }

//        if (!mMediaPlayer.isPlaying()) {
//            mMediaPlayer.seekTo(0);
//            mMediaPlayer.start();
//        }

        if(!ringtone.isPlaying()){
            ringtone.play();
        }
    }

    public static void stopAlarm() {
//        if (mMediaPlayer != null && mMediaPlayer.isPlaying())
//            mMediaPlayer.pause();

        if (ringtone != null && ringtone.isPlaying())
            ringtone.stop();
    }

    private Uri getSystemDefultRingtoneUri() {

        return RingtoneManager.getActualDefaultRingtoneUri(this,
                RingtoneManager.TYPE_ALARM);
    }


}
