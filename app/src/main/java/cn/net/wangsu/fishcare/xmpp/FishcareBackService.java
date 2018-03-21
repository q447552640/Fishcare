package cn.net.wangsu.fishcare.xmpp;


import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import cn.net.wangsu.fishcare.R;

public class FishcareBackService extends Service {

    LoginConfig loginconfig;
    XMPPTCPConnection connection;
    NotificationManager nManager;
    Notification notification;
    private PendingIntent pIntent;
    ProgressDialog pd;
    private ContacterReceiver receiver = null;
    private boolean loginSuccess = false;
    public SimpleBinder sBinder;

    private static String username;
    private static String password;

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        FishcareBackService.username = username;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        FishcareBackService.password = password;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        sBinder = new SimpleBinder();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        stopSelf();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        // TODO Auto-generated method stub
        //super.onLowMemory();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        nManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        initNotification();
        String username= intent.getStringExtra("username");
        String password= intent.getStringExtra("password");
        loginconfig = new LoginConfig();
        loginconfig.setXmppHost("192.168.2.139");
        loginconfig.setXmppPort(9090);
        loginconfig.setXmppServiceName("www.booway.raojianhui.com");
        loginconfig.setUsername(username);
        loginconfig.setPassword(password);
        loginconfig.setNovisible(false);
/*				pd = new ProgressDialog(FishcareBackService.this);
                pd.setTitle("请稍等");
				pd.setMessage("正在登录...");
				pd.show();*/
        new MyThread().start();
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 在 Local Service 中我们直接继承 Binder 而不是 IBinder,因为 Binder 实现了 IBinder 接口，这样我们可以少做很多工作。
     *
     * @author newcj
     */
    public class SimpleBinder extends Binder {
        /**
         * 获取 Service 实例
         *
         * @return
         */
        public FishcareBackService getService() {
            return FishcareBackService.this;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public boolean getLoginResult() {
            return loginSuccess;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 返回 SimpleBinder 对象
        return sBinder;
    }

    public class MyThread extends Thread {
        public void run() {
            //	while (!Thread.currentThread().isInterrupted()) {
            Message msg = new Message();
            msg.what = login(loginconfig);
            mHandler.sendMessage(msg);
                /*
				 * try { Thread.sleep(1000); } catch (InterruptedException e) {
				 * e.printStackTrace(); }
				 */
            //	}
        }
    }


    private void initNotification() {
        // 显示时间
        long when = System.currentTimeMillis();
        notification = new Notification();
        notification.icon = R.drawable.icon;// 设置通知的图标
        //notification.tickerText = tickerText; // 显示在状态栏中的文字
        notification.when = when; // 设置来通知时的时间
        //notification.sound = Uri.parse("android.resource://com.sun.alex/raw/dida"); // 自定义声音
        //	notification.flags = Notification.FLAG_NO_CLEAR; // 点击清除按钮时就会清除消息通知,但是点击通知栏的通知时不会消失
        //	notification.flags = Notification.FLAG_ONGOING_EVENT; // 点击清除按钮不会清除消息通知,可以用来表示在正在运行
        notification.flags |= Notification.FLAG_AUTO_CANCEL; // 点击清除按钮或点击通知后会自动消失
        //	notification.flags |= Notification.FLAG_INSISTENT; // 一直进行，比如音乐一直播放，知道用户响应
        notification.defaults = Notification.DEFAULT_SOUND; // 调用系统自带声音
        //	notification.defaults = Notification.DEFAULT_VIBRATE;// 设置默认震动
        //	notification.defaults = Notification.DEFAULT_ALL; // 设置铃声震动
        //	notification.defaults = Notification.DEFAULT_ALL; // 把所有的属性设置成默认

    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            //	pd.dismiss();
            switch (msg.what) {
                case Constant.LOGIN_SECCESS: // 登录成功
                    Toast.makeText(FishcareBackService.this, "登陆成功", Toast.LENGTH_SHORT)
                            .show();
                    // 系统消息连接服务
                    Intent imSystemMsgService = new Intent(FishcareBackService.this,
                            XMPPService.class);
                    FishcareBackService.this.startService(imSystemMsgService);
                    // 初始化广播
                    receiver = new ContacterReceiver();

                    // 注册广播接收器
                    IntentFilter filter = new IntentFilter();
                    // 好友请求
                    filter.addAction(Constant.ROSTER_SUBSCRIPTION);
                    filter.addAction(Constant.NEW_MESSAGE_ACTION);
                    filter.addAction(Constant.ACTION_SYS_MSG);

                    filter.addAction(Constant.ACTION_RECONNECT_STATE);
                    registerReceiver(receiver, filter);
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


   private Handler mHandler = new MyHandler();

    // 登录
    private Integer login(LoginConfig loginConfig) {
        XmppConnectionManager.getInstance().init(loginConfig);
        String username = loginConfig.getUsername();
        String password = loginConfig.getPassword();
        try {
            connection = XmppConnectionManager.getInstance().getConnection();
//            connection.connect();
            connection.login(username, password); // 登录
            // OfflineMsgManager.getInstance(activitySupport).dealOfflineMsg(connection);//处理离线消息
//            loginConfig.setUsername(username);
//            loginConfig.setPassword(password);
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

    private class ContacterReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Constant.ROSTER_SUBSCRIPTION.equals(action)) {
                // adapter.notifyDataSetChanged();
            } else if (Constant.NEW_MESSAGE_ACTION.equals(action)) {
                // 添加小气泡
                // adapter.notifyDataSetChanged();
            } else if (Constant.ACTION_RECONNECT_STATE.equals(action)) {
                // boolean isSuccess = intent.getBooleanExtra(
                // Constant.RECONNECT_STATE, false);
                // handReConnect(isSuccess);
            } else if (Constant.ACTION_SYS_MSG.equals(action)) {
                // adapter.notifyDataSetChanged();
				/*Notice no = new Notice();
		//		no = intent.getParcelableExtra("notice");
				no = (Notice) intent.getSerializableExtra("notice");
				if(no != null){
				//	tv.setText(no.getContent());
				}
				notification.tickerText = no.getContent();
				// 单击通知后会跳转到NotificationResult类  
	            intent = new Intent(FishcareBackService.this,
	                    MainActivity.class);  
	            // 获取PendingIntent,点击时发送该Intent  
	            pIntent = PendingIntent.getActivity(FishcareBackService.this, 0,
	                    intent, 0);  
	            // 设置通知的标题和内容  
	            notification.setLatestEventInfo(FishcareBackService.this, "标题",
	                    "内容", pIntent);  
	            // 发出通知  
	            nManager.notify(0, notification); */
            }

        }
    }
}
